package com.example.pdftoexcelservice.controller;

import com.example.pdftoexcelservice.dtos.BatchProcessingResult;
import com.example.pdftoexcelservice.service.BatchProcessingService;
import com.example.pdftoexcelservice.service.PdfOcrService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;


@RestController
@RequestMapping("/api/pdf")
@AllArgsConstructor
@Slf4j

public class PdfToExcelController {
    BatchProcessingService batchProcessingService;

    @PostMapping("/processFolder")
    public ResponseEntity processFolder(
            @RequestParam("folderName") String folderName) {
         String pdfBaseDir = "C:/voterData/";  // hardcoded

       String outputBaseDir = "C:/voterData/";

                    if (folderName.contains("..") || folderName.contains("/") || folderName.contains("\\")) {
                        return ResponseEntity.badRequest().body(null);
                    }

                    Path folderPath = Paths.get(pdfBaseDir, folderName).normalize();
                    File folderFile = folderPath.toFile();
                    if (!folderFile.exists() || !folderFile.isDirectory()) {
                        return ResponseEntity.badRequest().body(null);
                    }

                    BatchProcessingResult result = batchProcessingService.processFolder(
                            folderPath.toString(), outputBaseDir);

                    return ResponseEntity.ok(result);

    }}
