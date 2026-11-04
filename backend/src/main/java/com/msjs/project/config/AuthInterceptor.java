package com.msjs.project.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msjs.project.common.Constants;
import com.msjs.project.common.LoginUser;
import com.msjs.project.common.Result;
import com.msjs.project.common.UserContext;
import com.msjs.project.common.annotation.RequirePerm;
import com.msjs.project.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 鉴权拦截器
 * 1. 校验令牌（Redis，滑动过期） 2. 校验 @RequirePerm 操作权限 3. 写入用户上下文
 * 安全要求：除白名单外所有接口必须鉴权
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthService authService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 白名单路径 */
    private static final String[] WHITELIST = {
            "/api/auth/login",
            "/api/test/health",
            "/doc.html", "/v3/api-docs", "/swagger-ui", "/webjars", "/favicon.ico", "/error"
    };

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        for (String path : WHITELIST) {
            if (uri.startsWith(path)) {
                return true;
            }
        }
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        String token = resolveToken(request);
        LoginUser user = authService.getLoginUser(token);
        if (user == null) {
            writeJson(response, 401, "未登录或登录已过期，请重新登录");
            return false;
        }
        UserContext.set(user);

        // 操作权限校验：方法注解优先于类注解
        RequirePerm perm = handlerMethod.getMethodAnnotation(RequirePerm.class);
        if (perm == null) {
            perm = handlerMethod.getBeanType().getAnnotation(RequirePerm.class);
        }
        if (perm != null && !user.hasPerm(perm.value())) {
            writeJson(response, 403, "没有访问权限");
            return false;
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(Constants.HEADER_TOKEN);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return header;
    }

    private void writeJson(HttpServletResponse response, int code, String msg) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Result<Void> r = Result.fail(msg);
        r.setCode(code);
        response.getWriter().write(objectMapper.writeValueAsString(r));
    }
}
