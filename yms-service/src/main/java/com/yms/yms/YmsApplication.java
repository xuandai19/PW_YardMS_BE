package com.yms.yms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import com.yms.common.exception.GlobalExceptionHandler;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
@Import(GlobalExceptionHandler.class)
public class YmsApplication {
    public static void main(String[] args) {
        SpringApplication.run(YmsApplication.class, args);
    }
}
