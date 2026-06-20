package com.arquivodigital;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ArquivoDigitalApplication {
    public static void main(String[] args) {
        SpringApplication.run(ArquivoDigitalApplication.class, args);
    }
}
