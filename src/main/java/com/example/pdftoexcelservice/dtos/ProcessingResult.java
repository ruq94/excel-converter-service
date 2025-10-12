package com.example.pdftoexcelservice.dtos;

@lombok.Data
@lombok.Builder

public  class ProcessingResult {
    private String filename;
    private boolean success;
    private String error;
}