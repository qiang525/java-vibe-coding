package com.nnuist.coursehub.config;

import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
import io.swagger.annotations.ApiOperation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;

/**
 * 接口文档配置（Knife4j）
 *
 * <p>Knife4j 是 Swagger 的增强 UI，启动后访问
 * <a href="http://localhost:8080/doc.html">http://localhost:8080/doc.html</a>
 * 就能看到所有接口，并直接在线调试。</p>
 *
 * <p><b>为什么只扫描带 @ApiOperation 的方法</b>：避免把一些内部
 * 辅助接口也暴露到文档里，保持文档干净。</p>
 *
 * @author CourseHub Team
 */
@Configuration
@EnableKnife4j
@EnableSwagger2WebMvc
public class Knife4jConfig {

    @Bean
    public Docket defaultApi(Environment env) {
        String profile = String.join(",", env.getActiveProfiles());

        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                // 只扫描 controller 包
                .apis(RequestHandlerSelectors.basePackage("com.nnuist.coursehub.controller"))
                // 只展示有 @ApiOperation 注解的接口
                .apis(RequestHandlerSelectors.withMethodAnnotation(ApiOperation.class))
                .paths(PathSelectors.any())
                .build()
                .groupName("CourseHub 接口文档 [" + profile + "]");
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("CourseHub 课程资料协作平台 - 接口文档")
                .description("课程资料协作平台后端 API。所有接口统一返回 "
                        + "{ code, message, data, timestamp } 结构。")
                .version("1.0.0")
                .build();
    }
}
