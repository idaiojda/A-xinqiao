package com.example.xinqiaobackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class XinqiaoBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(XinqiaoBackendApplication.class, args);
    }
}
