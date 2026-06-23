package com.saas.billing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class BillifyApplication {

    public static void main(String[] args) {
        SpringApplication.run(BillifyApplication.class, args);
    }

}
