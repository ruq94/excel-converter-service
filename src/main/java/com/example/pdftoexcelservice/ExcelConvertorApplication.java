package com.example.pdftoexcelservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication

public class ExcelConvertorApplication {


    public static void main(String[] args) {
        SpringApplication.run(ExcelConvertorApplication.class, args);
    }

}
