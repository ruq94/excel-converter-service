package com.example.pdftoexcelservice.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BoothDetailsDto {
   // private String state;
    private String constituency;
    private String boothNumber;
    private String boothName;
    private String boothAddress;

}

