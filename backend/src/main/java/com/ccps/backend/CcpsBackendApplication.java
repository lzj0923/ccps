package com.ccps.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan({"com.ccps.backend.mapper", "com.ccps.backend.generated.mapper"})
public class CcpsBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CcpsBackendApplication.class, args);
    }
}
