//package com.example.transliterateservice.service;
//
//import jakarta.annotation.PostConstruct;
//import jakarta.annotation.PreDestroy;
//import lombok.Data;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import net.sourceforge.tess4j.Tesseract;
//import net.sourceforge.tess4j.TesseractException;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.stereotype.Service;
//
//import java.awt.image.BufferedImage;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//@Data
//public class TessearctOcrService {
//
//    @Qualifier("tesseractInstance")
//    private final Tesseract tesseract;
//
//    @PostConstruct
//    public void init() {
//        log.info("TesseractOcrService initialized");
//    }
//
//    @PreDestroy
//    public void cleanup() {
//        log.info("TesseractOcrService shutting down");
//    }
//
//    public String extractText(BufferedImage image) {
//        try {
//            log.debug("Starting Tesseract OCR extraction");
//            String text = tesseract.doOCR(image);
//            log.debug("Tesseract OCR completed: {} characters extracted",
//                    text != null ? text.length() : 0);
//            return text != null ? text.trim() : "";
//        } catch (TesseractException e) {
//            log.error("Tesseract OCR failed: {}", e.getMessage());
//            return "";
//        }
//    }
//}
//
//
