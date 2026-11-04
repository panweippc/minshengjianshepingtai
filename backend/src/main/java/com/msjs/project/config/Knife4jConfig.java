package com.msjs.project.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j 接口文档配置
 * 访问地址：http://localhost:8080/doc.html
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI msjsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("民生建设项目管理平台 API")
                        .description("项目全过程管理：附件、案卷档案、操作审计")
                        .version("1.0.0")
                        .contact(new Contact().name("msjs")));
    }
}
