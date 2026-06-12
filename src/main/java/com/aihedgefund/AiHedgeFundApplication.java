package com.aihedgefund;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.aihedgefund.mapper")
public class AiHedgeFundApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiHedgeFundApplication.class, args);
    }
}
