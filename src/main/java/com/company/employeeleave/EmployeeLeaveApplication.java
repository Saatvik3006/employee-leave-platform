package com.company.employeeleave;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class EmployeeLeaveApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmployeeLeaveApplication.class, args);
    }
}
