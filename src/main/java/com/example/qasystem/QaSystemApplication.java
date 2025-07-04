package com.example.qasystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class QaSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(QaSystemApplication.class, args);
    }

}
