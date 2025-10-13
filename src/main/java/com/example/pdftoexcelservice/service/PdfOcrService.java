package com.example.pdftoexcelservice.service;

import com.example.pdftoexcelservice.dtos.AllExcelDetailsDto;
import com.example.pdftoexcelservice.dtos.BoothDetailsDto;
import com.example.pdftoexcelservice.dtos.VoterDetailsDto;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Tesseract;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import org.springframework.core.env.Environment;

import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.example.pdftoexcelservice.constant.PropertyConstant.HIN_LANG;
import static com.example.pdftoexcelservice.constant.PropertyConstant.TESS_PATH;

@Service
@Slf4j
public class PdfOcrService {

    private final Tesseract tesseract;
    private final Environment environment;
    private final ImageProcessorService imageProcessorService;

    public PdfOcrService(Environment environment) {
        this.environment = environment;
        this.tesseract = new Tesseract();
        this.imageProcessorService = new ImageProcessorService();
    }
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
String data=environment.getProperty(TESS_PATH);
String lang=environment.getProperty(HIN_LANG);
tesseract.setLanguage(lang);
tesseract.setDatapath(data);
            int dpi = Integer.parseInt(Objects.requireNonNull(environment.getProperty("pdf.dpi")));
            int totalPages = document.getNumberOfPages();
            dto.setTotalPage(totalPages);
            PDFRenderer renderer = new PDFRenderer(document);
            dto.setTotalPage(totalPages);

            List<VoterDetailsDto> allRows = new ArrayList<>();

            for (int pageIndex = 0; pageIndex < totalPages; pageIndex++) {
                // Skip page 2 no data needs to  extract
                if (pageIndex == 1) {
                    log.debug("Skipping page ");
                    continue;
                }

                BufferedImage pageImage = renderer.renderImageWithDPI(pageIndex, dpi);
                if (pageImage == null || pageImage.getWidth() <= 0 || pageImage.getHeight() <= 0) {
                    log.info("Invalid image on page {} of file {}", pageIndex + 1, file.getName());
                    continue;
                }

                String pageText = "";
                try {
                    pageText = tesseract.doOCR(pageImage);
                } catch (Exception e) {
                    log.error("OCR failed on page {} of file {}: {}", pageIndex + 1, file.getName(), e.getMessage(), e);
                    continue;
                }

                if (pageIndex == 0) {
                    // First page: extract booth
                    extractBoothData(pageText);
                } else {
                    // Pages voterdetails
                    List<VoterDetailsDto> rows = extractTableData(pageText, pageIndex + 1);
                    allRows.addAll(rows);
                }
            }

            dto.setVoterDetailsDtoList(allRows);
            log.debug("Extracted {} records from {}", allRows.size(), file.getName());

        } catch (Exception e) {
            log.error("Error extracting from file: {}", file.getName(), e);
            throw new RuntimeException("Failed to extract data", e);
        }

        return dto;    }
    //testing
    private void extractBoothData(String text) {
        List<BoothDetailsDto> rows = new ArrayList<>();
        String[] lines = text.split("\\r?\\n");
        boolean tableStarted = false;
        int recordCount = 0;
    }
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
