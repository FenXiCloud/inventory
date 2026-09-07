package com.flyemu.share.interceptor;

import cn.dev33.satoken.stp.StpUtil;
import com.flyemu.share.common.Constants;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.setting.Menu;
import com.flyemu.share.entity.setting.QMenu;
import com.flyemu.share.entity.setting.QMenuRole;
import com.flyemu.share.entity.setting.QMerchantMenu;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 操作级权限拦截器（角色维度，与结账守卫 assertEditable 的账期维度互补）。
 * <p>
 * 只拦三类写操作：新增/编辑（save、importData）、审核/反审核（approved）、删除（DELETE、batchDelete）。
 * 权限码 =「URL前缀:操作」，与 jxc_menu 中 menuType=FUNCTION 行的 component 一一对应。
 * <p>
 * 安全阀：URL 推导出的权限码不在 FUNCTION 种子目录中 → 放行（未种子的前缀绝不误伤）；
 * 系统默认角色（商户主账号角色）全量旁路；任何异常 → 放行，鉴权故障不阻断业务。
 * 读/查询类接口不拦截，避免单据页跨模块选客户/选商品等 lookup 被误伤。
 */
@Slf4j
@RequiredArgsConstructor
public class PermissionInterceptor implements HandlerInterceptor {

    private static final Pattern AUDIT = Pattern.compile("^/([A-Za-z][A-Za-z0-9]*)/approved/([^/]+)$");
    private static final Pattern LOC_AUDIT = Pattern.compile("^/locationTransfer/approve/\\d+$");
    private static final Pattern DELETE = Pattern.compile("^/([A-Za-z][A-Za-z0-9]*)/\\d+$");
    private static final Pattern SAVE = Pattern.compile("^/([A-Za-z][A-Za-z0-9]*)$");
    private static final Pattern IMPORT = Pattern.compile("^/([A-Za-z][A-Za-z0-9]*)/importData$");
    private static final Pattern BATCH_DELETE = Pattern.compile("^/([A-Za-z][A-Za-z0-9]*)/batchDelete$");

    private static final QMenu qMenu = QMenu.menu;
    private static final QMenuRole qMenuRole = QMenuRole.menuRole;
    private static final QMerchantMenu qMerchantMenu = QMerchantMenu.merchantMenu;

    private final JPAQueryFactory jqf;

    /** 全量 FUNCTION 权限码目录（种子表），60s TTL 缓存 */
    private volatile Set<String> catalog;
    private volatile long catalogAt;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            String method = request.getMethod();
            if ("OPTIONS".equalsIgnoreCase(method)) {
                return true;
            }
            String path = request.getServletPath();
            String code = resolveCode(method, path);
            if (code == null || !functionCatalog().contains(code)) {
                return true;
            }
            Object sessionAccount = StpUtil.getTokenSession().get(Constants.SESSION_ACCOUNT);
            // admin-server 的 session 存的不是 AccountDto 或未登录：交给既有的登录校验处理
            if (!(sessionAccount instanceof AccountDto)) {
                return true;
            }
            AccountDto accountDto = (AccountDto) sessionAccount;
            if (accountDto.getRole() == null || Boolean.TRUE.equals(accountDto.getRole().getSystemDefault())) {
                return true;
            }
            if (grantedCodes(accountDto).contains(code)) {
                return true;
            }
            log.info("权限拦截：roleId={} 缺少 {}，拒绝 {} {}",
                    accountDto.getRole().getId(), code, method, path);
            reject(response);
            return false;
        } catch (Exception e) {
            log.warn("权限拦截器异常，放行 {} {}", request.getMethod(), request.getServletPath(), e);
            return true;
        }
    }

    /** URL → 权限码；与三类写操作规则都不匹配（读操作、多段子路径、状态非审核/反审核）返回 null 放行 */
    private String resolveCode(String method, String path) {
        Matcher m;
        if ("POST".equalsIgnoreCase(method)) {
            if ((m = AUDIT.matcher(path)).matches()) {
                String state = m.group(2);
                if (state.contains("%")) {
                    state = URLDecoder.decode(state, StandardCharsets.UTF_8);
                }
                // 审核=已审核、反审核=已保存，同一权限码；已取消等其它流转不拦
                if ("已审核".equals(state) || "已保存".equals(state)) {
                    return m.group(1) + ":audit";
                }
                return null;
            }
            if ((m = IMPORT.matcher(path)).matches()) {
                return m.group(1) + ":edit";
            }
            if ((m = SAVE.matcher(path)).matches()) {
                return m.group(1) + ":edit";
            }
        } else if ("PUT".equalsIgnoreCase(method)) {
            if (LOC_AUDIT.matcher(path).matches()) {
                return "locationTransfer:audit";
            }
            if ((m = BATCH_DELETE.matcher(path)).matches()) {
                return m.group(1) + ":delete";
            }
            if ((m = SAVE.matcher(path)).matches()) {
                return m.group(1) + ":edit";
            }
        } else if ("DELETE".equalsIgnoreCase(method)) {
            if ((m = DELETE.matcher(path)).matches()) {
                return m.group(1) + ":delete";
            }
        }
        return null;
    }

    private Set<String> functionCatalog() {
        Set<String> c = catalog;
        long now = System.currentTimeMillis();
        if (c != null && now - catalogAt < 60_000L) {
            return c;
        }
        c = new HashSet<>(jqf.select(qMenu.component).from(qMenu)
                .where(qMenu.menuType.eq(Menu.MenuType.FUNCTION).and(qMenu.enabled.isTrue()))
                .fetch());
        catalog = c;
        catalogAt = now;
        return c;
    }

    /**
     * 当前角色已授权的操作码。现查 DB（不依赖登录时的 session 快照），
     * 使「授权变更」无需重登即时生效；与 AdminService.loadFunction 同语义。
     */
    private List<String> grantedCodes(AccountDto accountDto) {
        return jqf.select(qMenu.component).from(qMenuRole)
                .join(qMenu).on(qMenuRole.menuId.eq(qMenu.id))
                .where(qMenuRole.roleId.eq(accountDto.getRole().getId())
                        .and(qMenu.menuModule.eq(Menu.MenuModule.MERCHANT))
                        .and(qMenu.enabled.isTrue())
                        .and(qMenu.menuType.eq(Menu.MenuType.FUNCTION))
                        .and(qMenuRole.menuId.in(jqf.select(qMerchantMenu.menuId).from(qMerchantMenu)
                                .where(qMerchantMenu.merchantId.eq(accountDto.getMerchantId())))))
                .fetch();
    }

    private void reject(HttpServletResponse response) throws Exception {
        // HTTP 200 + success=false：走前端 Request.js 既有 msg toast 通道，避免 403 被当作登录失效整页跳转
        response.setStatus(200);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"success\":false,\"code\":-1,\"msg\":\"当前角色无此操作权限~\",\"data\":null}");
    }
}
