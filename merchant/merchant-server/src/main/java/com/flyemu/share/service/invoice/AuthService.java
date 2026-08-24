package com.flyemu.share.service.invoice;

import com.fenxi365.api.Fenxi365;
import com.fenxi365.api.Result;
import com.fenxi365.api.tax.AccountServer;
import com.fenxi365.api.tax.LoginServer;
import com.fenxi365.api.tax.QdfpServer;
import com.fenxi365.api.tax.TaxOrgServer;
import com.fenxi365.open.model.dto.*;
import com.fenxi365.open.model.dto.login.remote.RemoteEtaxCookieDto;
import com.fenxi365.open.model.enums.LoginType;
import com.fenxi365.open.model.enums.ServiceType;
import com.fenxi365.open.model.vo.*;
import com.fenxi365.open.model.vo.login.remote.*;
import com.flyemu.share.config.InvoicePlatformProperties;
import com.flyemu.share.dto.invoice.LoginResponse;
import com.flyemu.share.dto.invoice.QrCodeResponse;
import com.flyemu.share.dto.invoice.ScanDurationResponse;
import com.flyemu.share.entity.invoice.AccountState;
import com.flyemu.share.repository.invoice.AccountStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数电账户认证：订购产品 → 创建/复用账号 → 登录 → 人脸识别 → 扫脸时长。
 * 状态按商户隔离，每个商户一条 {@link AccountState}。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final Fenxi365 fenxi365;
    private final InvoicePlatformProperties props;
    private final AccountStateRepository stateRepo;

    /** 二维码原始文本/rzid → 商户ID，用于税局扫码回调时解析租户 */
    private final Map<String, Long> qrMerchant = new ConcurrentHashMap<>();

    private AccountState loadState(Long merchantId) {
        return stateRepo.findTopByMerchantIdOrderByIdDesc(merchantId)
                .orElseGet(() -> {
                    AccountState s = new AccountState();
                    s.setMerchantId(merchantId);
                    return s;
                });
    }

    private void saveState(AccountState state) {
        stateRepo.save(state);
    }

    private TaxOrgServer taxOrg() {
        return fenxi365.withLoginTypeAndService(LoginType.TG, ServiceType.QXY_SDKP).getTaxOrgServer();
    }

    private AccountServer account() {
        return fenxi365.withLoginTypeAndService(LoginType.TG, ServiceType.QXY_SDKP).getAccountServer();
    }

    private LoginServer login() {
        return fenxi365.withLoginTypeAndService(LoginType.TG, ServiceType.QXY_SDKP).getLoginServer();
    }

    private QdfpServer qdfp() {
        return fenxi365.withLoginTypeAndService(LoginType.TG, ServiceType.QXY_SDKP).getQdfpServer();
    }

    // ========== 0. 当前状态 ==========

    public AccountState currentState(Long merchantId) {
        AccountState state = loadState(merchantId);
        // Populate transient fields from config
        state.setRealName(props.getAccount().getRealName());
        state.setRoleName(props.getAccount().getRoleName());
        state.setPhone(props.getAccount().getPhone());
        return state;
    }

    // ========== 1. 订购产品 ==========

    public String purchaseProduct(Long merchantId, String aggOrgName, String dq, String nsrsbh, String productCode) {
        AccountState state = loadState(merchantId);

        if (props.isMockEnabled()) {
            log.info("[MOCK] 订购产品: aggOrgName={}, nsrsbh={}", aggOrgName, nsrsbh);
            state.setAggOrgId("MOCK_ORG_" + System.currentTimeMillis());
            state.setAggOrgName(aggOrgName);
            state.setDq(dq);
            state.setNsrsbh(nsrsbh);
            saveState(state);
            return state.getAggOrgId();
        }

        try {
            Result<List<ProductListDto>> existing = taxOrg().productList(nsrsbh);
            if (existing.isSuccess() && existing.getData() != null && !existing.getData().isEmpty()) {
                ProductListDto dto = existing.getData().get(0);
                state.setAggOrgId(String.valueOf(dto.getAggOrgId()));
                state.setNsrsbh(nsrsbh);
                state.setAggOrgName(aggOrgName);
                state.setDq(dq);
                saveState(state);
                log.info("产品已存在, aggOrgId={}", state.getAggOrgId());
                return state.getAggOrgId();
            }
        } catch (Exception e) {
            log.warn("查询已有产品失败: {}", e.getMessage() != null ? e.getMessage() : "网络或SDK异常");
        }

        ProductPurchaseVo vo = new ProductPurchaseVo();
        vo.setAggOrgName(aggOrgName);
        vo.setDq(dq);
        vo.setNsrsbh(nsrsbh);
        vo.setProductCodeList(productCode);

        Result<ProductPurchaseDto> result = taxOrg().productPurchase(vo);
        if (!result.isSuccess()) throw new RuntimeException("订购产品失败: " + result.getMsg());

        ProductPurchaseDto dto = result.getData();
        state.setAggOrgId(String.valueOf(dto.getAggOrgId()));
        state.setNsrsbh(nsrsbh);
        state.setAggOrgName(aggOrgName);
        state.setDq(dq);
        saveState(state);
        log.info("产品订购成功, aggOrgId={}", state.getAggOrgId());
        return state.getAggOrgId();
    }

    // ========== 2. 查询 / 创建账号 ==========

    public String queryOrCreateAccount(Long merchantId) {
        AccountState state = loadState(merchantId);

        if (props.isMockEnabled()) {
            log.info("[MOCK] 创建账号");
            state.setAccountId("MOCK_ACCOUNT_" + System.currentTimeMillis());
            saveState(state);
            return state.getAccountId();
        }

        var acc = props.getAccount();

        // 先查是否已有账号，有则直接复用（更新手机号），没有再创建
        QueryAccountVo queryVo = new QueryAccountVo();
        queryVo.setAggOrgId(Long.valueOf(state.getAggOrgId()));
        Result<QueryAccountDto[]> queryResult = account().queryAccount(queryVo);
        if (queryResult.isSuccess() && queryResult.getData() != null && queryResult.getData().length > 0) {
            QueryAccountDto existing = queryResult.getData()[0];
            String existingId = String.valueOf(existing.getAccountId());
            state.setAccountId(existingId);
            saveState(state);
            log.info("复用已有账号: accountId={}, dlfs={}, sflx={}, 更新手机号为: {}",
                    existingId, existing.getDlfs(), existing.getSflx(), acc.getPhone());

            AccountUpdateVo upd = new AccountUpdateVo();
            upd.setAccountId(existing.getAccountId());
            upd.setAggOrgId(Long.valueOf(state.getAggOrgId()));
            upd.setSjhm(acc.getPhone());
            upd.setSflx("KPY");
            upd.setDlfs(14L);
            upd.setGryhm(acc.getPhone());
            upd.setGryhmm(acc.getPassword());
            Result<Boolean> updResult = account().accountUpdate(upd);
            log.info("更新账号结果: success={}, msg={}", updResult.isSuccess(), updResult.getMsg());

            bindProduct(existingId, state.getAggOrgId());
            return state.getAccountId();
        }

        // 没有旧账号，创建新账号
        AccountCreateVo createVo = new AccountCreateVo();
        createVo.setAggOrgId(Long.valueOf(state.getAggOrgId()));
        createVo.setGryhm(acc.getPhone());
        createVo.setGryhmm(acc.getPassword());
        createVo.setSjhm(acc.getPhone());
        createVo.setProxyNsrsbh(state.getNsrsbh());
        createVo.setDlfs(14L);
        createVo.setSflx("KPY");
        createVo.setDq(Long.valueOf(state.getDq() != null ? state.getDq() : "33"));
        createVo.setLoginType(0L);
        createVo.setSpecType(0L);

        log.info("创建账号参数: gryhm={}, sjhm={}, dlfs={}, sflx={}",
                createVo.getGryhm(), createVo.getSjhm(), createVo.getDlfs(), createVo.getSflx());

        Result<AccountCreateDto> createResult = account().accountCreate(createVo);
        if (!createResult.isSuccess()) throw new RuntimeException("创建账号失败: " + createResult.getMsg());

        state.setAccountId(String.valueOf(createResult.getData().getAccountId()));
        saveState(state);
        log.info("新账号创建成功: accountId={}", state.getAccountId());

        bindProduct(state.getAccountId(), state.getAggOrgId());
        return state.getAccountId();
    }

    // ========== 3. 登录 ==========

    public LoginResponse doLogin(Long merchantId) {
        AccountState state = loadState(merchantId);
        LoginResponse resp = new LoginResponse();

        if (props.isMockEnabled()) {
            log.info("[MOCK] 登录成功");
            resp.setSuccess(true);
            resp.setToken("MOCK_TOKEN_" + System.currentTimeMillis());
            resp.setMessage("模拟登录成功");
            state.setLoggedIn(true);
            saveState(state);
            return resp;
        }

        // 流程第3步：先校验税局缓存是否有效
        if (checkCache(state)) {
            log.info("doLogin: 缓存有效，无需重新登录");
            resp.setSuccess(true);
            resp.setToken("CACHE_OK");
            resp.setMessage("已登录（缓存有效）");
            state.setLoggedIn(true);
            saveState(state);
            return resp;
        }

        // 缓存过期，校验APP快速登录 + 发送短信验证码（两个同时调用）
        boolean appQuick = checkRomoteAppCache(state);
        log.info("doLogin: 缓存过期, appQuick={}", appQuick);
        RemoteEtaxCookieDto etaxDto = etaxCookie(state);

        if (appQuick) {
            resp.setSuccess(true);
            resp.setToken("APP_QUICK_OK");
            resp.setMessage("APP快速登录成功");
            state.setLoggedIn(true);
            log.info("doLogin: APP快速登录成功");
        } else if (etaxDto != null && etaxDto.getTaskId() != null) {
            resp.setSuccess(false);
            resp.setTaskId(etaxDto.getTaskId());
            resp.setMessage("需要短信验证码");
            log.info("doLogin: 已发送短信验证码, taskId={}", etaxDto.getTaskId());
        } else {
            resp.setSuccess(false);
            resp.setMessage("登录异常：未获取到taskId，请检查账号配置");
            log.warn("doLogin: appQuick=false 且 etaxDto 无 taskId");
        }
        saveState(state);
        return resp;
    }

    public LoginResponse submitSms(Long merchantId, String taskId, String smsCode) {
        AccountState state = loadState(merchantId);

        if (props.isMockEnabled()) {
            log.info("[MOCK] 短信验证成功");
            LoginResponse resp = new LoginResponse();
            resp.setSuccess(true);
            resp.setToken("MOCK_TOKEN_SMS");
            state.setLoggedIn(true);
            saveState(state);
            return resp;
        }

        PushSmsVo vo = new PushSmsVo();
        vo.setTaskId(taskId);
        vo.setSmsCode(smsCode);
        Result<Boolean> result = login().remotePushsms(vo);
        if (!result.isSuccess()) throw new RuntimeException("短信验证失败: " + result.getMsg());

        boolean cacheOk = checkCache(state);
        log.info("pushsms 后 checkCache = {}", cacheOk);

        LoginResponse resp = new LoginResponse();
        resp.setSuccess(cacheOk);
        resp.setToken(cacheOk ? "SMS_LOGIN_OK" : null);
        resp.setMessage(cacheOk ? "短信验证成功，登录已完成" : "短信验证成功但缓存未生效，请再次点击登录");
        state.setLoggedIn(cacheOk);
        saveState(state);
        return resp;
    }

    private boolean checkCache(AccountState state) {
        try {
            CheckRomoteAppCacheVo vo = new CheckRomoteAppCacheVo();
            vo.setAccountId(state.getAccountId());
            vo.setAggOrgId(Long.valueOf(state.getAggOrgId()));
            Result<Boolean> result = login().remoteCheckCache(vo);
            return result.isSuccess() && Boolean.TRUE.equals(result.getData());
        } catch (Exception e) {
            log.warn("checkCache 异常: {}", e.getMessage());
            return false;
        }
    }

    private boolean checkRomoteAppCache(AccountState state) {
        try {
            CheckRomoteAppCacheVo vo = new CheckRomoteAppCacheVo();
            vo.setAccountId(state.getAccountId());
            vo.setAggOrgId(Long.valueOf(state.getAggOrgId()));
            Result<Boolean> result = login().remoteCheckRomoteAppCache(vo);
            return result.isSuccess() && Boolean.TRUE.equals(result.getData());
        } catch (Exception e) {
            log.warn("checkRomoteAppCache 异常: {}", e.getMessage());
            return false;
        }
    }

    private RemoteEtaxCookieDto etaxCookie(AccountState state) {
        RemoteEtaxCookieVo vo = new RemoteEtaxCookieVo();
        vo.setAccountId(state.getAccountId());
        vo.setAggOrgId(Long.valueOf(state.getAggOrgId()));
        log.info("etaxCookie 请求: accountId={}, aggOrgId={}", vo.getAccountId(), vo.getAggOrgId());
        Result<RemoteEtaxCookieDto> result = login().remoteEtaxcookie(vo);
        log.info("etaxCookie result: success={}, code={}, msg={}, data={}",
                result.isSuccess(), result.getCode(), result.getMsg(),
                result.getData() != null ? "taskId=" + result.getData().getTaskId() : "null");
        if (!result.isSuccess()) throw new RuntimeException("登录税局失败: " + result.getMsg());
        return result.getData();
    }

    // ========== 4. 人脸识别 ==========

    public QrCodeResponse getQrCode(Long merchantId) {
        AccountState state = loadState(merchantId);

        if (props.isMockEnabled()) {
            log.info("[MOCK] 获取人脸识别二维码");
            QrCodeResponse resp = new QrCodeResponse();
            resp.setQrId("MOCK_QR_" + System.currentTimeMillis());
            resp.setQrImage("data:image/png;base64,MOCK_QR_IMAGE");
            return resp;
        }

        RzewmVo vo = new RzewmVo();
        vo.setAccountId(state.getAccountId());
        vo.setAggOrgId(state.getAggOrgId());
        vo.setNsrsbh(state.getNsrsbh());
        Result<RzewmDto> result = qdfp().rzewm(vo);
        if (!result.isSuccess()) throw new RuntimeException("获取二维码失败: " + result.getMsg());
        QrCodeResponse resp = new QrCodeResponse();
        resp.setQrId(result.getData().getRzid());
        resp.setQrImage(result.getData().getEwm());
        // 保存 rzid 和 ewm，回调/查图用
        state.setQrRzid(result.getData().getRzid());
        state.setQrEwm(result.getData().getEwm());
        saveState(state);
        qrMerchant.put(result.getData().getRzid(), merchantId);
        if (result.getData().getEwm() != null) {
            qrMerchant.put(result.getData().getEwm(), merchantId);
        }
        log.info("获取二维码成功: rzid={}, ewm已保存", result.getData().getRzid());
        return resp;
    }

    public byte[] getQrImage(Long merchantId) {
        AccountState state = loadState(merchantId);
        String ewm = state.getQrEwm();
        if (ewm == null || ewm.isEmpty()) {
            throw new RuntimeException("请先获取二维码");
        }
        return com.flyemu.share.util.QrCodeUtil.toPngBytes(ewm, 200);
    }

    public boolean checkQrResult(Long merchantId) {
        AccountState state = loadState(merchantId);

        String rzid = state.getQrRzid();
        if (rzid == null || rzid.isEmpty()) {
            log.warn("checkQrResult: 没有保存的 rzid，请先调用 getQrCode");
            return false;
        }

        if (props.isMockEnabled()) {
            boolean ok = Math.random() > 0.3;
            if (ok) {
                recordScanSuccess(state);
            }
            return ok;
        }

        RzztcxVo vo = new RzztcxVo();
        vo.setAccountId(state.getAccountId());
        vo.setAggOrgId(state.getAggOrgId());
        vo.setNsrsbh(state.getNsrsbh());
        RzztcxVo.Data data = new RzztcxVo.Data();
        data.setRzid(rzid);
        vo.setData(data);
        try {
            Result<RzztcxDto> result = qdfp().rzztcx(vo);
            log.info("rzztcx 结果: rzid={}, success={}, msg={}, slzt={}",
                    rzid, result.isSuccess(), result.getMsg(),
                    result.getData() != null ? result.getData().getSlzt() : "null");
            boolean ok = result.isSuccess() && "2".equals(result.getData().getSlzt());
            if (ok) {
                recordScanSuccess(state);
            }
            return ok;
        } catch (Exception e) {
            log.warn("查询扫码结果异常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 税局扫码回调（无登录态）：根据回调中的 qrcode_id 解析商户，再复用 {@link #checkQrResult(Long)}。
     */
    public boolean checkQrResultByCallback(String qrId) {
        Long merchantId = qrMerchant.get(qrId);
        if (merchantId == null) {
            log.warn("checkQrResultByCallback: 无法根据 qrId 解析商户: {}", qrId);
            return false;
        }
        return checkQrResult(merchantId);
    }

    private void recordScanSuccess(AccountState state) {
        String now = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        state.setLastScanTime(now);
        state.setLastScanMessage("扫码成功" + now);
        saveState(state);
    }

    // ========== 5. 扫脸时长 ==========

    public ScanDurationResponse queryScanDuration(Long merchantId) {
        AccountState state = loadState(merchantId);

        if (props.isMockEnabled()) {
            ScanDurationResponse resp = new ScanDurationResponse();
            resp.setCurrentSeconds(state.getScanDuration() != null ? state.getScanDuration() : 300);
            resp.setMaxSeconds(86400);
            return resp;
        }

        SxlbCxVo vo = new SxlbCxVo();
        vo.setAccountId(state.getAccountId());
        vo.setAggOrgId(state.getAggOrgId());
        vo.setNsrsbh(state.getNsrsbh());
        Result<SxlbCxDto> result = qdfp().sxlbCx(vo);
        if (!result.isSuccess()) throw new RuntimeException("查询扫脸时长失败: " + result.getMsg());
        SxlbCxDto dto = result.getData();
        ScanDurationResponse resp = new ScanDurationResponse();
        int currentMin = dto.getPhxxz() != null ? Integer.parseInt(dto.getPhxxz()) : 0;
        resp.setCurrentSeconds(currentMin * 60);
        resp.setMaxSeconds((int) dto.getScanTime() * 60);
        return resp;
    }

    public ScanDurationResponse setScanDuration(Long merchantId, int seconds) {
        AccountState state = loadState(merchantId);

        if (props.isMockEnabled()) {
            state.setScanDuration(seconds);
            saveState(state);
            ScanDurationResponse resp = new ScanDurationResponse();
            resp.setCurrentSeconds(seconds);
            resp.setMaxSeconds(86400);
            return resp;
        }

        // API 的 phxxz 单位是分钟, 最小 1 天(1440分钟), 且需要是天的倍数
        int minutes = Math.max(1440, seconds / 60);
        minutes = (minutes / 1440) * 1440;

        SmscVo vo = new SmscVo();
        vo.setAccountId(state.getAccountId());
        vo.setAggOrgId(state.getAggOrgId());
        vo.setNsrsbh(state.getNsrsbh());
        SmscVo.Data data = new SmscVo.Data();
        data.setPhxxz(String.valueOf(minutes));
        vo.setData(data);
        Result<Boolean> result = qdfp().smsc(vo);
        if (!result.isSuccess()) throw new RuntimeException("设置扫脸时长失败: " + result.getMsg());
        state.setScanDuration(seconds);
        saveState(state);
        return queryScanDuration(merchantId);
    }

    // ========== 6. 绑定产品 ==========

    private void bindProduct(String accountId, String aggOrgId) {
        try {
            BindAccountProductVo vo = new BindAccountProductVo();
            vo.setAccountId(Long.valueOf(accountId));
            vo.setAggOrgId(Long.valueOf(aggOrgId));
            vo.setProductCode("0004"); // 数电开票
            vo.setIsDefaut(false);
            Result<BindAccountProductDto> result = account().bindAccountProduct(vo);
            if (result.isSuccess()) {
                log.info("账号绑定数电开票产品成功: accountId={}, productCode=0004", accountId);
            } else {
                log.warn("账号绑定产品失败: {}, 继续尝试...", result.getMsg());
            }
        } catch (Exception e) {
            log.warn("绑定产品异常: {}", e.getMessage());
        }
    }

    // ========== 7. 便捷方法 ==========

    public String getAggOrgId(Long merchantId) { return loadState(merchantId).getAggOrgId(); }
    public String getAccountId(Long merchantId) { return loadState(merchantId).getAccountId(); }
    public String getNsrsbh(Long merchantId) { return loadState(merchantId).getNsrsbh(); }
    public boolean isLoggedIn(Long merchantId) { return loadState(merchantId).isLoggedIn(); }
}
