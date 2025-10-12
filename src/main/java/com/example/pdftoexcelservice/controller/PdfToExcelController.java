package com.example.pdftoexcelservice.controller;

import com.example.pdftoexcelservice.dtos.AllExcelDetailsDto;
import com.example.pdftoexcelservice.service.PdfOcrService;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import static javax.security.auth.callback.ConfirmationCallback.OK;


@RestController
@RequestMapping("/api/pdf")
@AllArgsConstructor
@Slf4j

public class PdfToExcelController {
    private final PdfOcrService pdfOcrService;

    @PostMapping(path = "/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> extractToText(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("File is required and cannot be empty");
            }

            AllExcelDetailsDto result= pdfOcrService.extractData(file);
            return ResponseEntity.ok(OK);

        } catch (Exception e) {
            log.error("Error extracting text from PDF", e);
            return ResponseEntity
                    .status(500)
                    .body("Error extracting text: " + e.getMessage());
        }
    }
    private boolean isPdfFile(MultipartFile file) {
        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();
        return (contentType != null && contentType.equals("application/pdf")) ||
                (filename != null && filename.toLowerCase().endsWith(".pdf"));
    }

    private String generateExcelFilename(String originalFilename) {
        if (originalFilename != null && originalFilename.toLowerCase().endsWith(".pdf")) {
            return originalFilename.substring(0, originalFilename.length() - 4) + "_extracted.xlsx";
        }
        return "extracted_data.xlsx";
    }

    @PostMapping("/extractt")
    public ResponseEntity<?> extractToExcel(
            @Parameter(description = "PDF file to extract", required = true)
            @RequestParam("file")
            String file) {

        try {

            if (file.isEmpty()) {
                log.warn("Empty file received");
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body("File is empty");

            }


        } catch (Exception e) {
            log.error("Error processing PDF: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage());
        }

return new ResponseEntity<>(ResponseEntity.ok().body(file), HttpStatus.OK);
}}
