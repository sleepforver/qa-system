package com.example.qasystem.mapper;

import com.example.qasystem.model.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

@Mapper
public interface UserMapper {
    void insertUser(User user);
    Optional<User> findByUser(String username);

    User findByUsername(String username);
    User findByEmail(String email);
}
