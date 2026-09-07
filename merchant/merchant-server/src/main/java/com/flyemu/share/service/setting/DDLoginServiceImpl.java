package com.flyemu.share.service.setting;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.Dict;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.dingtalkoauth2_1_0.models.GetAccessTokenResponse;
import com.aliyun.tea.TeaException;
import com.aliyun.teaopenapi.models.Config;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiUserGetuserinfoRequest;
import com.dingtalk.api.request.OapiV2DepartmentListsubRequest;
import com.dingtalk.api.request.OapiV2UserListRequest;
import com.dingtalk.api.response.OapiUserGetuserinfoResponse;
import com.dingtalk.api.response.OapiV2DepartmentListsubResponse;
import com.dingtalk.api.response.OapiV2UserListResponse;
import com.flyemu.share.entity.setting.Admin;
import com.flyemu.share.entity.setting.Dept;
import com.flyemu.share.entity.setting.Role;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.repository.setting.AdminRepository;
import com.taobao.api.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class DDLoginServiceImpl implements DDLoginService{

    private static String accessToken = null;

    /** 钉钉同步后台任务状态（单实例内存态，防重 + 进度轮询） */
    private static final ExecutorService DD_SYNC_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "dd-user-sync");
        t.setDaemon(true);
        return t;
    });
    private final AtomicBoolean ddSyncing = new AtomicBoolean(false);
    private volatile String ddSyncProgress = "";
    private volatile String ddSyncResult = null;

    @Value("${dingtalk.appKey:}")
    private String appKey;
    @Value("${dingtalk.appSecret:}")
    private String appSecret;
    @Value("${dingtalk.corpId:}")
    private String corpId;
    @Value("${dingtalk.agentID:}")
    private String agentID;

    @Autowired
    private AdminService adminService;
    @Autowired
    private DeptService deptService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private AdminRepository adminRepository;

    /**
     * 登录回调
     *
     * @param code
     * @Return java.lang.String
     */
    @Override
    public String getLoginAuth(String code, String corpId) {
        log.info("钉钉登录,回调code：{}", code);
        com.aliyun.dingtalkoauth2_1_0.Client client = null;
        //根据corpId找到对应的组织
        //省略校验
       /* List<JSONObject> infos = sysDeptMapper.selectDeptDDInfo(corpId);
        String appKey = "";
        String appSecret = "";
        Long deptId = null;
        for (JSONObject infoJson : infos) {
            if (ObjectUtils.isEmpty(infoJson)) {
                continue;
            }
            JSONObject jsonObject = infoJson.getJSONObject("info");
            if (ObjectUtils.isEmpty(jsonObject) || ObjectUtils.isNotEmpty(jsonObject.getString("appKey"))
                    || ObjectUtils.isNotEmpty(jsonObject.getString("appSecret"))
                    || ObjectUtils.isNotEmpty(jsonObject.getString("cropId"))   ) {
                continue;
            }
            if (!corpId.equals(jsonObject.getString("CropId"))) {
                continue;
            }
            deptId = infoJson.getLong("deptId");
            appKey = jsonObject.getString("AppKey");
            appSecret = jsonObject.getString("AppSecret");
        }
        if (ObjectUtils.isEmpty(deptId)) {
            throw new RuntimeException("该公司暂未配置钉钉免密码登录");
        }*/
        try {
            client = createClient();
            com.aliyun.dingtalkoauth2_1_0.models.GetAccessTokenRequest getAccessTokenRequest = new com.aliyun.dingtalkoauth2_1_0.models.GetAccessTokenRequest()
                    .setAppKey(appKey)
                    .setAppSecret(appSecret);

            com.aliyun.dingtalkoauth2_1_0.models.GetAccessTokenResponse res = client.getAccessToken(getAccessTokenRequest);
            com.aliyun.dingtalkoauth2_1_0.models.GetAccessTokenResponseBody body = res.body;

            String accessToken = body.getAccessToken();
            log.info("钉钉登录,获取用企业token：{}", accessToken);
            // 获取用户信息
            DingTalkClient client1 = new DefaultDingTalkClient("https://oapi.dingtalk.com/user/getuserinfo");
            OapiUserGetuserinfoRequest request = new OapiUserGetuserinfoRequest();
            request.setCode(code);
            request.setHttpMethod("GET");
            OapiUserGetuserinfoResponse response;
            try {
                response = client1.execute(request, accessToken);
                log.info("钉钉登录获取用户信息：{}", JSON.toJSONString(response));
            } catch (ApiException e) {
                log.info("钉钉登录获取用户信息，失败：{}", e);
                throw new RuntimeException("登录失败！请联系管理员！");
            }
            // 查询得到当前用户的userId
            String userId = response.getUserid();
//            String userId = "01020445356138043786";
            if(ObjectUtils.isEmpty(userId)){
                throw new RuntimeException("系统中未含该成员！请联系管理员！");
            }

    /*        SysUser user = new SysUser();
            user.setDingDingUserId(userId);
            user.setDeptId(deptId);*/
            Admin admin = adminService.selectAdminByDingDingUserId(userId);
            if (ObjectUtils.isEmpty(admin)) {
                throw new RuntimeException("系统中未含该成员！请联系管理员！");
            }
            //登录
            return admin.getMobile();
        } catch (Exception e) {
            log.error("钉钉登录回调异常：" , e);
            throw new RuntimeException(e);
        }
    }

    class Counter {
        int sumNum = 0;
        int successNum = 0;
        int failureNum = 0;
    }

    public static com.aliyun.dingtalkoauth2_1_0.Client createClient() throws Exception {
        Config config = new Config();
        config.protocol = "https";
        config.regionId = "central";
        return new com.aliyun.dingtalkoauth2_1_0.Client(config);
    }

    @Override
    public String submitUserSync() {
        if (StringUtils.isAnyBlank(appKey, appSecret)) {
            throw new ServiceException("尚未配置钉钉同步参数（dingtalk.appKey / appSecret），请先在 merchant-server 配置文件中填写~");
        }
        if (!ddSyncing.compareAndSet(false, true)) {
            throw new ServiceException("上一个钉钉同步任务仍在进行中，请稍候~");
        }
        ddSyncProgress = "任务已提交，排队中…";
        ddSyncResult = null;
        DD_SYNC_EXECUTOR.execute(() -> {
            try {
                ddSyncResult = doSyncUsers();
            } catch (Exception e) {
                log.error("钉钉同步任务异常：", e);
                ddSyncResult = "SYNC_ERROR:" + StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName());
            } finally {
                ddSyncing.set(false);
            }
        });
        return "同步任务已提交，正在后台执行，进度见工具栏提示~";
    }

    @Override
    public Dict getSyncProgress() {
        return Dict.create()
                .set("running", ddSyncing.get())
                .set("progress", ddSyncProgress)
                .set("result", ddSyncResult);
    }

    /**
     * 真正的同步流程（后台线程执行）：串行调用钉钉接口较慢，故全程更新进度供前端轮询。
     */
    private String doSyncUsers() {
        Counter counter = new Counter();
        Set<String> userIds = new HashSet<>();
        ddSyncProgress = "正在获取钉钉accessToken…";
        accessToken = this.getAccessToken();
        ddSyncProgress = "正在拉取部门列表…";
        try {
            DingTalkClient client1 = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/v2/department/listsub");
            OapiV2DepartmentListsubRequest req1 = new OapiV2DepartmentListsubRequest();
            //获取部门id列表
            OapiV2DepartmentListsubResponse rsp1 = client1.execute(req1, accessToken);
            if (rsp1 == null || !rsp1.isSuccess()) {
                throw new ServiceException("获取钉钉部门列表失败："
                        + (rsp1 == null ? "无响应" : StringUtils.defaultIfBlank(rsp1.getSubMsg(), rsp1.getMsg()))
                        + "，请检查钉钉应用权限与参数~");
            }
            //查询默认角色
            Long roleId = null;
            List<Role> roleList = roleService.systemDefaultRole();
            if(!ObjectUtils.isEmpty(roleList) && roleList.size() > 0){
                roleId = roleList.get(0).getId();
            }
            this.insertUserByDingDing(rsp1, counter, userIds, roleId);
        } catch (ApiException e) {
            log.error("获取钉钉用户异常：", e);
            throw new ServiceException("钉钉用户同步失败：" + StringUtils.defaultIfBlank(e.getErrMsg(), e.getMessage())
                    + "；中断前已拉取 " + counter.sumNum + " 条、入库 " + counter.successNum + " 条~");
        }
        String message = "共拉取到：" + counter.sumNum + "条；<br/>成功："
                + counter.successNum + "条；<br/>更新：" + counter.failureNum + "条；";
        log.info(message);
        return message;
    }

    @Override
    public String getToken() {
        return this.getAccessToken();
    }

    /**
     * 获取accessToken
     * @return accessToken信息
     */
    private String getAccessToken() {
        GetAccessTokenResponse accessTokenRsp;
        try {
            com.aliyun.dingtalkoauth2_1_0.Client client = createClient();
            com.aliyun.dingtalkoauth2_1_0.models.GetAccessTokenRequest getAccessTokenRequest = new com.aliyun.dingtalkoauth2_1_0.models.GetAccessTokenRequest()
                    .setAppKey(appKey)
                    .setAppSecret(appSecret);
            accessTokenRsp = client.getAccessToken(getAccessTokenRequest);
        } catch (TeaException err) {
            log.error("获取钉钉accessToken失败 code:{} message:{}", err.code, err.message);
            throw new ServiceException("获取钉钉accessToken失败：" + StringUtils.defaultIfBlank(err.message, StringUtils.defaultIfBlank(err.code, "未知错误"))
                    + "，请检查 appKey/appSecret~");
        } catch (Exception _err) {
            log.error("获取钉钉accessToken异常：", _err);
            throw new ServiceException("获取钉钉accessToken异常：" + _err.getMessage() + "，请检查钉钉参数配置或网络~");
        }
        String token = accessTokenRsp.getBody() == null ? null : accessTokenRsp.getBody().getAccessToken();
        if (StringUtils.isBlank(token)) {
            throw new ServiceException("未能获取钉钉accessToken，请检查 appKey/appSecret 配置~");
        }
        return token;
    }

    public void insertUserByDingDing(OapiV2DepartmentListsubResponse rsp1, Counter counter, Set<String> userIds, Long roleId) throws ApiException {
        if (ObjectUtils.isEmpty(rsp1.getResult())) {
            return;
        }
        DingTalkClient deptClient = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/v2/department/listsub");
        // 批量成员接口：一次拉 100 人（含姓名/手机号/邮箱），替代旧“listid + 每人一次 user/get”的海量串行请求
        DingTalkClient userClient = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/v2/user/list");

        //循环部门列表
        for (OapiV2DepartmentListsubResponse.DeptBaseResponse deptBaseResponse : rsp1.getResult()) {
            //判断这个组织是否存在
            String deptName = deptBaseResponse.getName();
            Dept sysDept = deptService.checkDeptNameUnique(deptName);

            if (ObjectUtils.isEmpty(sysDept)) {
                //新建部门
                sysDept = new Dept();
                sysDept.setDeptName(deptName);
                sysDept.setDelFlag("0");
                sysDept.setStatus("0");
                sysDept.setAncestors("");
                deptService.insertDept(sysDept);
            }

            //下级部门递归
            OapiV2DepartmentListsubRequest subReq = new OapiV2DepartmentListsubRequest();
            subReq.setDeptId(deptBaseResponse.getDeptId());
            OapiV2DepartmentListsubResponse subRsp = deptClient.execute(subReq, accessToken);
            this.insertUserByDingDing(subRsp, counter, userIds, roleId);

            //分页拉取本部门直属成员
            long cursor = 0L;
            boolean hasMore = true;
            while (hasMore) {
                OapiV2UserListRequest userReq = new OapiV2UserListRequest();
                userReq.setDeptId(deptBaseResponse.getDeptId());
                userReq.setSize(100L);
                userReq.setCursor(cursor);
                OapiV2UserListResponse userRsp = userClient.execute(userReq, accessToken);
                if (userRsp == null || !userRsp.isSuccess() || userRsp.getResult() == null) {
                    throw new ServiceException("拉取部门「" + deptName + "」成员失败："
                            + (userRsp == null ? "无响应" : StringUtils.defaultIfBlank(userRsp.getSubMsg(), userRsp.getMsg())) + "~");
                }
                List<OapiV2UserListResponse.ListUserResponse> users = userRsp.getResult().getList();
                if (!ObjectUtils.isEmpty(users)) {
                    for (OapiV2UserListResponse.ListUserResponse dingUser : users) {
                        this.syncOneUser(dingUser, sysDept, roleId, counter, userIds);
                    }
                }
                hasMore = Boolean.TRUE.equals(userRsp.getResult().getHasMore());
                cursor = userRsp.getResult().getNextCursor() == null ? 0L : userRsp.getResult().getNextCursor();
                ddSyncProgress = "部门「" + deptName + "」已处理；累计拉取 " + counter.sumNum
                        + " 人（新增 " + counter.successNum + "、更新 " + counter.failureNum + "）…";
            }
        }
    }

    /**
     * 落库单个钉钉成员：跨部门去重，已存在则仅回填钉钉ID，不存在则新建（密码=手机号后6位）。
     */
    private void syncOneUser(OapiV2UserListResponse.ListUserResponse dingUser, Dept sysDept, Long roleId,
                             Counter counter, Set<String> userIds) {
        String userId = dingUser.getUserid();
        if (ObjectUtils.isEmpty(userId)) {
            log.error("钉钉拉取用户userId为空，跳过");
            return;
        }
        if (!userIds.add(userId)) {
            return;
        }
        Admin admin = new Admin();
        admin.setEmail(dingUser.getEmail());
        admin.setUsername(dingUser.getMobile());
        admin.setName(dingUser.getName());
        admin.setMobile(dingUser.getMobile());
        admin.setDeptId(sysDept.getId());
        admin.setDingDingUserId(userId);
        admin.setMerchantId(1L);
        admin.setRoleId(roleId);
        //如果手机号为空 取钉钉id后6位
        if (StringUtils.isEmpty(admin.getMobile())) {
            admin.setMobile(userId);
            admin.setUsername(userId);
        }

        counter.sumNum++;
        Admin u = adminService.selectAdminByMobile(admin.getMobile());
        if (ObjectUtils.isEmpty(u)) {
            // 仅新建时算密码哈希（bcrypt 单次上百毫秒，存量用户不白算）
            String seed = StringUtils.isEmpty(dingUser.getMobile()) ? userId : admin.getMobile();
            admin.setPassword(DigestUtil.bcrypt(seed.substring(Math.max(0, seed.length() - 6))));
            this.adminSave(admin);
            counter.successNum++;
        } else {
            u.setDingDingUserId(userId);
            this.adminSave(u);
            counter.failureNum++;
        }
    }

    /**
     * 添加/更新用户
     * @param admin 实体
     */
    private void adminSave(Admin admin) {
        if (admin.getId() != null) {
            Admin original = adminService.selectByPrimaryKey(admin.getId());
            BeanUtil.copyProperties(admin, original, CopyOptions.create().ignoreNullValue());
            adminRepository.save(original);
        }else{
            admin.setSystemDefault(false);
            admin.setEnabled(true);
            adminRepository.save(admin);
        }

    }

}
