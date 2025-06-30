package com.example.qasystem.controller;

import com.example.qasystem.model.User;
import com.example.qasystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;

    @GetMapping("/login")
    public String loginPage(Model model, @RequestParam(value = "error", required = false) String error) {
        logger.debug("显示登录页面");
        model.addAttribute("user", new User());
        if (error != null && !error.equals("")) {
            model.addAttribute("error", error);
            logger.warn("登录尝试失败,error:{}",error);
        }
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        logger.debug("显示注册页面");
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String register(User user, Model model) {
        logger.info("处理用户注册: {}", user.getUsername());
        try {
            userService.registerUser(user);
            logger.info("用户注册成功: {}", user.getUsername());
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            logger.warn("注册失败: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }
}
