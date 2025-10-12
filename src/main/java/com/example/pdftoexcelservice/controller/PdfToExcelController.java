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
    private final PdfOcrService pdfOcrService;
    BatchProcessingService batchProcessingService;

  /*  @PostMapping(path = "/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)

 public Mono<ResponseEntity<?>> extractToText(@RequestPart("file") Mono<FilePart> filePartMono) {
       return filePartMono.flatMap(filePart -> {
            if (filePart == null) {
                log.info("No file uploaded or file is empty");
                return Mono.just(ResponseEntity.badRequest().body("File is required and cannot be empty"));
            }
            try {
                return pdfOcrService.extractDataFromFile(filePart)
                        .map(result -> {
                            log.info("File processed successfully: {}", filePart.filename());
                            return ResponseEntity.ok(result);
                        });
            } catch (Exception e) {
                log.error("Error extracting text from PDF", e);
                return Mono.just(ResponseEntity
                        .status(500)
                        .body("Error extracting text: " + e.getMessage()));
            }
        });}
 */
    @PostMapping("/processFolder")
    public ResponseEntity processFolder(
            @RequestParam("folderName") String folderName) {
         String pdfBaseDir = "C:/voterData/";  // hardcoded or via @Value

        // And an output base directory
       String outputBaseDir = "C:/voterData/";

                    // sanitize folderName, avoid path traversal
                    if (folderName.contains("..") || folderName.contains("/") || folderName.contains("\\")) {
                        return ResponseEntity.badRequest().body(null);
                    }

                    Path folderPath = Paths.get(pdfBaseDir, folderName).normalize();
                    File folderFile = folderPath.toFile();
                    if (!folderFile.exists() || !folderFile.isDirectory()) {
                        return ResponseEntity.badRequest().body(null);
                    }

                    // Call the service method
                    BatchProcessingResult result = batchProcessingService.processFolder(
                            folderPath.toString(), outputBaseDir);

                    return ResponseEntity.ok(result);

    }}
