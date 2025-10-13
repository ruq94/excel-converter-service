package com.example.pdftoexcelservice.dtos;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class BoothDetailsDto {
    private String state;
    private String constituency;
        private String boothNumber;
        private String boothName;
        private String boothAddress;
private String partNumber;
    }
