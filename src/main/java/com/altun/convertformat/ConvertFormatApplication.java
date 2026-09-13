package com.altun.convertformat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class ConvertFormatApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConvertFormatApplication.class, args);
    }

}
