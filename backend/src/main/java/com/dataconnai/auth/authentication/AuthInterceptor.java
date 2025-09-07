package com.dataconnai.auth.authentication;


import com.dataconnai.base.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // todo: implement authentication
        User fakeUser = new User();
        fakeUser.setId("default");
        fakeUser.setName("default");
        fakeUser.setDisplayName("default");
        RequestContext.setUser(fakeUser);
        return true;
    }

}
