package com.appmcore.mapapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class MapAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(MapAppApplication.class, args);
    }
}
