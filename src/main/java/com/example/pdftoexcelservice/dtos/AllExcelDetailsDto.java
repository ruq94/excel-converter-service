package com.example.pdftoexcelservice.dtos;

import lombok.Builder;
import lombok.Data;

import java.util.List;
@Builder
@Data
public class AllExcelDetailsDto {
        private BoothDetailsDto boothDetailsDto;
        private List<VoterDetailsDto> voterDetailsDtoList;
int totalPage;
    }
