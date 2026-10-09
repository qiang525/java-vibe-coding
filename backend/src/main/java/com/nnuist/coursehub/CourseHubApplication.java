package com.nnuist.coursehub;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * CourseHub 课程资料协作平台 —— 应用启动入口
 *
 * <p>这是整个后端服务的"总开关"。运行 main 方法即可启动内嵌 Tomcat，
 * 默认监听 8080 端口。</p>
 *
 * <p>三个注解的作用：</p>
 * <ul>
 *   <li>{@code @SpringBootApplication} —— 开启自动配置、组件扫描、配置类支持</li>
 *   <li>{@code @MapperScan} —— 告诉 MyBatis 去哪个包下找 Mapper 接口，
 *       省去在每个接口上写 {@code @Mapper} 注解</li>
 *   <li>{@code @EnableScheduling} —— 开启定时任务支持（可信度评分每晚重算要用）</li>
 * </ul>
 *
 * @author CourseHub Team
 */
@Slf4j
@EnableAsync
@EnableScheduling
@SpringBootApplication
@MapperScan("com.nnuist.coursehub.mapper")
public class CourseHubApplication {

    public static void main(String[] args) {
        Environment env = SpringApplication.run(CourseHubApplication.class, args).getEnvironment();
        printStartupInfo(env);
    }

    /**
     * 启动完成后在控制台打印访问地址，方便开发者快速定位
     */
    private static void printStartupInfo(Environment env) {
        String port = env.getProperty("server.port", "8080");
        String contextPath = env.getProperty("server.servlet.context-path", "");
        String appName = env.getProperty("spring.application.name", "coursehub");
        String host;
        try {
            host = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            host = "localhost";
        }

        log.info("\n----------------------------------------------------------\n" +
                "\t应用 '{}' 启动成功！\n" +
                "\t本地访问: \t\thttp://localhost:{}{}\n" +
                "\t外部访问: \t\thttp://{}:{}{}\n" +
                "\t接口文档: \t\thttp://localhost:{}{}/doc.html\n" +
                "\t环境配置: \t\t{}\n" +
                "----------------------------------------------------------",
                appName, port, contextPath, host, port, contextPath,
                port, contextPath,
                String.join(",", env.getActiveProfiles()));
    }
}
