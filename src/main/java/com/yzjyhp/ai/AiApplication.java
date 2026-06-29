package com.yzjyhp.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 应用启动入口
 * Created by macro on 2018/4/26.
 */
@EnableScheduling
@SpringBootApplication(scanBasePackages = {"com.yzjyhp"})
@Slf4j
public class AiApplication {

    public static void main(String[] args) throws UnknownHostException {
        SpringApplication app = new SpringApplication(AiApplication.class);
        app.setBannerMode(Banner.Mode.OFF);
        ConfigurableApplicationContext applicationContext = app.run(args);
        Environment env = applicationContext.getEnvironment();
        String ip = InetAddress.getLocalHost().getHostAddress();
        String port = env.getProperty("server.port");
        String path = env.getProperty("server.servlet.context-path") == null ? "" : env.getProperty("server.servlet.context-path");

        log.info("\n----------------------------------------------------------\n\t" + "Application  profile-ai is running! Access URLs:\n\t" + "Local: \t\thttp://localhost:" + port + path + "/doc.html\n\t" + "External: \thttp://" + ip + ":" + port + path + "/\n" + "swagger-ui: \thttp://" + ip + ":" + port + path + "/swagger-ui.html\n\t" + "Doc: \t\thttp://" + ip + ":" + port + path + "/doc.html\n" + "----------------------------------------------------------");
    }

}
