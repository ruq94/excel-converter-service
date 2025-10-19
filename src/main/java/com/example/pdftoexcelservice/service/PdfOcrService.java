package com.example.pdftoexcelservice.service;

import com.example.pdftoexcelservice.constant.BoothDetailsEnum;
import com.example.pdftoexcelservice.dtos.AllExcelDetailsDto;
import com.example.pdftoexcelservice.dtos.BoothDetailsDto;
import com.example.pdftoexcelservice.dtos.VoterDetailsDto;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Tesseract;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;

import static com.example.pdftoexcelservice.constant.PropertyConstant.HIN_LANG;
import static com.example.pdftoexcelservice.constant.PropertyConstant.TESS_PATH;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfOcrService {
    private final Tesseract tesseract;
  private final   ExtractBoothDetailsService extractBoothDetailsService;
 private final Environment environment;

    /**
     * Extract data from File (for batch processing)
     */
    public AllExcelDetailsDto extractDataFromFile(File file) {
       log.debug("Extracting from file: {}", file.getName());

        AllExcelDetailsDto dto = AllExcelDetailsDto.builder()
                .voterDetailsDtoList(new ArrayList<>())
                .build();

        try (FileInputStream fis = new FileInputStream(file);
             PDDocument document = PDDocument.load(fis)) {
            int dpi = Integer.parseInt(Objects.requireNonNull(environment.getProperty("pdf.dpi")));
            int totalPages = document.getNumberOfPages();
            dto.setTotalPage(totalPages);
            PDFRenderer renderer = new PDFRenderer(document);
            dto.setTotalPage(totalPages);

            List<VoterDetailsDto> allRows = new ArrayList<>();
            BoothDetailsDto  boothdetails=null;
            for (int pageIndex = 0; pageIndex < totalPages; pageIndex++) {
                // skip page 2 and last page entirely
                if (pageIndex == 1||pageIndex == totalPages-1) {
                    continue;
                }
// or PSM_SINGLE_BLOCK, PSM_SINGLE_LINE etc.
                BufferedImage pageImage = renderer.renderImageWithDPI(pageIndex, dpi);
                if (pageImage == null || pageImage.getWidth() <= 0 || pageImage.getHeight() <= 0) {
                    log.info("Invalid image on page {} of file {}", pageIndex + 1, file.getName());
                    continue;
                }

                String pageText = "";
                if (pageIndex == 0) {
                    pageText = tesseract.doOCR(pageImage);
                    boothdetails=   extractBoothDetailsService.parseBoothDetails(pageText);



                }
            }

           // dto.setVoterDetailsDtoList(allRows);
            dto.setBoothDetailsDto(boothdetails);
            log.debug("Extracted {} records from {}", allRows.size(), file.getName());

        } catch (Exception e) {
            log.error("Error extracting from file: {}", file.getName(), e);
            throw new RuntimeException("Failed to extract data", e);
        }

        return dto;    }




        private List<VoterDetailsDto> extractTableData(String text, int pageNumber) {
        List<VoterDetailsDto> rows = new ArrayList<>();
        String[] lines = text.split("\\r?\\n");
        boolean tableStarted = false;
        int recordCount = 0;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (!tableStarted && line.toLowerCase().matches(".*\\b(s\\.?no|serial).*\\b(name|नाम).*\\b(age|उम्र).*")) {
                tableStarted = true;
                continue;
            }
            if (!tableStarted) continue;

            String[] cols = line.split("\\s{2,}|\\t");
            if (cols.length >= 3) {
                String serialStr = cols[0].replaceAll("\\D", "");
                int serialNo = serialStr.isEmpty() ? (recordCount + 1) : Integer.parseInt(serialStr);
                String name = cols[1].trim();
                String age = cols[2].trim();

                if (!name.isEmpty()) {
                    VoterDetailsDto v = VoterDetailsDto.builder()
                            .pageNumber(pageNumber)
                            .voterId(serialNo)
                            .name(name)
                            .age(age)
                            .build();
                    rows.add(v);
                    recordCount++;
                }
            }

            if (recordCount >= 10) break;
        }
        return rows;
    }
}
