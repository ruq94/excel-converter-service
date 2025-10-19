package com.example.pdftoexcelservice.config;

import net.sourceforge.tess4j.Tesseract;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import static com.example.pdftoexcelservice.constant.PropertyConstant.HIN_LANG;
import static com.example.pdftoexcelservice.constant.PropertyConstant.TESS_PATH;

@Configuration
    public class TessaractConfig {
        @Bean
        public Tesseract tesseract(Environment env) {
            Tesseract t = new Tesseract();
            t.setDatapath(env.getProperty(TESS_PATH));
            t.setLanguage(env.getProperty(HIN_LANG));
            return t;
        }
    }

