package com.flyemu.share.controller.invoice;

import com.flyemu.share.service.invoice.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * 税局人脸识别扫码回调（无登录态）。回调 URL 形如 /qrcode_id=xxx&amp;areaPrefix=...。
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(-100)
public class QrCodeCallbackFilter extends OncePerRequestFilter {

    private final AuthService authService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String query = request.getQueryString();

        if (path != null && path.startsWith("/qrcode_id=")) {
            log.info("拦截到二维码回调: path={}, query={}", path, query);

            String full = query != null ? path + "?" + query : path;
            String qrId = null;
            for (String part : full.split("[&?]")) {
                if (part.startsWith("qrcode_id=") || part.startsWith("/qrcode_id=")) {
                    qrId = part.substring(part.indexOf('=') + 1);
                    break;
                }
            }

            response.setContentType("application/json;charset=UTF-8");
            PrintWriter out = response.getWriter();

            if (qrId != null && !qrId.isEmpty()) {
                boolean ok = authService.checkQrResultByCallback(qrId);
                String msg = ok ? "认证成功" : "认证尚未完成或已过期";
                out.print("{\"success\":" + ok + ",\"message\":\"" + msg + "\"}");
            } else {
                log.warn("无法从回调URL解析qrId: path={}", path);
                out.print("{\"success\":false,\"message\":\"无法解析qrId\"}");
            }
            out.flush();
            return;
        }

        chain.doFilter(request, response);
    }
}
