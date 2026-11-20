package com.punestartup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class PuneStartupApplication {

    public static void main(String[] args) {
        SpringApplication.run(PuneStartupApplication.class, args);
    }
}
