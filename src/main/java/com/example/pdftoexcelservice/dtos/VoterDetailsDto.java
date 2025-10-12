package com.example.pdftoexcelservice.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VoterDetailsDto {
    private int voterId;
    private String name;
    private String age;
    private int pageNumber;

}
