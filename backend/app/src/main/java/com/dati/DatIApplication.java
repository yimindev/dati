package com.dati;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class DatIApplication {

    public static void main(String[] args) {
        SpringApplication.run(DatIApplication.class, args);
    }

}
