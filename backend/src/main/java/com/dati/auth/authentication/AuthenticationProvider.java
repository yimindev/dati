package com.dati.auth.authentication;

import com.dati.auth.server.pojo.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

public interface AuthenticationProvider {

    /**
     * 快速判断：这个 HTTP 请求是否由本 Provider 处理（看 Header/参数/路径等）。
     * 只判断"形式匹配"，不执行实际验证。
     *
     * @return 请求特征匹配（如携带了本 Provider 所需的 Header）
     */
    boolean canAuthenticate(HttpServletRequest request);

    /**
     * 执行认证。只有 {@link #canAuthenticate(HttpServletRequest)} 返回 true 时才会被调用。
     *
     * @return Optional.of(user) 认证成功，Optional.empty()  认证失败（如 Token 过期、签名错误）
     */
    Optional<User> authenticate(HttpServletRequest request);

    /**
     * 用户登录。由 AuthenticationService 按 type 路由调用。
     *
     * @return 签发的 Token 字符串
     */
    String login(LoginRequest request);

    /**
     * 是否支持该认证类型（用于登录路由）
     */
    boolean supports(String type);
}
