package com.example.pdftoexcelservice.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VoterDetailsDto {
    private Integer pageNumber;
    private Integer voterId;
    private String voterIdCard;
    private String name;
    private String age;
    private String gender;
    private String relationName;
    private String houseNumber;
}
