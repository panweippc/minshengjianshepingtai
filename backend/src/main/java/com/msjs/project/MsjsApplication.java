package com.msjs.project;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 民生建设项目管理平台 启动类
 */
@SpringBootApplication
@MapperScan("com.msjs.project.mapper")
public class MsjsApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsjsApplication.class, args);
    }
}
