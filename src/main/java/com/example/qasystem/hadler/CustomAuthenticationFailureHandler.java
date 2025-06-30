package com.example.qasystem.hadler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        String errorMessage = "badCredentials";

        if (exception.getClass().isAssignableFrom(LockedException.class)) {
            errorMessage = "locked"; // 账户锁定
        } else if (exception.getClass().isAssignableFrom(DisabledException.class)) {
            errorMessage = "disabled"; // 账户禁用
        }

        response.sendRedirect("/login?error=" + errorMessage);
    }
}
