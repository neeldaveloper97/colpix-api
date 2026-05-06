package com.colpix.api;

import com.colpix.api.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class ColpixApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ColpixApiApplication.class, args);
    }
}
