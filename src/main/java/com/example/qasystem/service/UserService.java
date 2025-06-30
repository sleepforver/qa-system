package com.example.qasystem.service;

import com.example.qasystem.mapper.UserMapper;
import com.example.qasystem.model.User;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService{

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public void registerUser(User user) {
        logger.info("用户注册: {}", user.getUsername());
        if (userMapper.findByUsername(user.getUsername()) != null) {
            logger.warn("用户名重复: {}", user.getUsername());
            throw new IllegalArgumentException("用户名重复，请重新修改");
        }
        if (userMapper.findByEmail(user.getEmail()) != null) {
            logger.warn("邮箱重复: {}", user.getEmail());
            throw new IllegalArgumentException("邮箱重复，请重新修改");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        userMapper.insertUser(user);
        logger.info("用户注册成功: {}", user.getUsername());
    }

    public User findByUsername(String username) {
        logger.debug("通过用户名寻找用户: {}", username);
        return userMapper.findByUsername(username);
    }

    public User findByEmail(String email) {
        logger.debug("通过邮箱寻找用户: {}", email);
        return userMapper.findByEmail(email);
    }

}
