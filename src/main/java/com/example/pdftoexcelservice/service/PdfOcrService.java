package com.example.pdftoexcelservice.service;

import com.example.pdftoexcelservice.dtos.AllExcelDetailsDto;
import com.example.pdftoexcelservice.dtos.VoterDetailsDto;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.env.Environment;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.example.pdftoexcelservice.constant.PropertyConstant.HIN_LANG;
import static com.example.pdftoexcelservice.constant.PropertyConstant.TESS_PATH;

@Service
@Slf4j
public class PdfOcrService {

    private final Tesseract tesseract;
   // private final PdfConfig pdfConfig;
    private final Environment environment;
    public PdfOcrService(Environment environment) {
        this.environment = environment;
        this.tesseract = new Tesseract();
        configureTesseract();
    }

    private void configureTesseract() {
        String tessPath = environment.getProperty(TESS_PATH);
        String lang = environment.getProperty(HIN_LANG, "eng");

        if (tessPath != null) {
            tesseract.setDatapath(tessPath);
        }
        tesseract.setLanguage(lang);
    }

    public Mono<AllExcelDetailsDto> extractData(FilePart filePart) {
        log.info("Starting extraction for file: {}", filePart.filename());

        return DataBufferUtils.join(filePart.content())
                .flatMap(dataBuffer -> Mono.fromCallable(() -> {
                    try (InputStream inputStream = dataBuffer.asInputStream(true); // true releases the buffer
                         PDDocument document = PDDocument.load(inputStream)) {

                        AllExcelDetailsDto.AllExcelDetailsDtoBuilder dtoBuilder = AllExcelDetailsDto.builder().voterDetailsDtoList(new ArrayList<>());

                        int dpi = Integer.parseInt(Objects.requireNonNull(environment.getProperty("pdf.dpi")));
                        PDFRenderer renderer = new PDFRenderer(document);
                        int totalPages = document.getNumberOfPages();
                        dtoBuilder.totalPage(totalPages);
                        log.info("Total pages in PDF: {}", totalPages);

                        if (totalPages >= 1) {
                            BufferedImage page1Image = renderer.renderImageWithDPI(0, dpi, ImageType.RGB);
                            String page1Text = doOcr(page1Image);
                            // BoothDetailsDto boothInfo = extractBoothInfo(page1Text);
                            // dtoBuilder.boothDetailsDto(boothInfo);
                            // log.info("Booth Info extracted - Number: {}, Name: {}",
                            // boothInfo.getBoothNumber(), boothInfo.getBoothName());
                        }

                        log.info("Skipping page 2");

                        List<VoterDetailsDto> allRows = new ArrayList<>();
                        for (int page = 2; page < totalPages; page++) {
                            log.info("Processing page {} for table", page + 1);
                            BufferedImage pageImage = renderer.renderImageWithDPI(page, dpi, ImageType.RGB);
                            String pageText = doOcr(pageImage);
                            List<VoterDetailsDto> rows = extractTableData(pageText, page + 1);
                            allRows.addAll(rows);
                            log.info("Page {}: {} records extracted", page + 1, rows.size());
                        }

                        dtoBuilder.voterDetailsDtoList(allRows);
                        return dtoBuilder.build();
                    } catch (Exception e) {
                        log.error("Error during PDF extraction: {}", e.getMessage(), e);
                        throw new RuntimeException("Failed to extract data from PDF", e);
                    }
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    public AllExcelDetailsDto extractData(MultipartFile file) {
        log.info("Starting extraction for file: {}", file.getOriginalFilename());

        // Use builder to create AllExcelDetailsDto
        AllExcelDetailsDto dtoBuilder = AllExcelDetailsDto.builder().voterDetailsDtoList(new ArrayList<>()).build();

        try (InputStream inputStream = file.getInputStream();
             PDDocument document = PDDocument.load(inputStream)) {
            int dpi =Integer.parseInt(Objects.requireNonNull(environment.getProperty("pdf.dpi")));

            // Configure tesseract from env properties

            PDFRenderer renderer = new PDFRenderer(document);
            int totalPages = document.getNumberOfPages();
            dtoBuilder.setTotalPage(totalPages);
            log.info("Total pages in PDF: {}", totalPages);

            // Page 1: Booth info
            if (totalPages >= 1) {
                BufferedImage page1Image = renderer.renderImageWithDPI(0, dpi, ImageType.RGB);



                String page1Text = doOcr(page1Image);
               // BoothDetailsDto boothInfo = extractBoothInfo(page1Text);
               //// dtoBuilder.setBoothDetailsDto(boothInfo);
              //  log.info("Booth Info extracted - Number: {}, Name: {}",
                       // boothInfo.getBoothNumber(), boothInfo.getBoothName());
            }

            // Skip Page 2: as per logic
            log.info("Skipping page 2");

            // Pages 3 onward: extract table
            List<VoterDetailsDto> allRows = new ArrayList<>();
            for (int page = 2; page < totalPages; page++) {
                log.info("Processing page {} for table", page + 1);
                BufferedImage pageImage = renderer.renderImageWithDPI(page, dpi, ImageType.RGB);
                String pageText = doOcr(pageImage);
                List<VoterDetailsDto> rows = extractTableData(pageText, page + 1);
                allRows.addAll(rows);
                log.info("Page {}: {} records extracted", page + 1, rows.size());
            }

            dtoBuilder.getVoterDetailsDtoList().addAll(allRows);
        } catch (Exception e) {
            log.error("Error during PDF extraction: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to extract data from PDF", e);
        }

        AllExcelDetailsDto result = dtoBuilder;
        return result;
    }

    public byte[] extractToExcel(byte[] fileContent, String filename) {
        try (InputStream inputStream = new ByteArrayInputStream(fileContent);
             PDDocument document = PDDocument.load(inputStream)) {

            PDFRenderer renderer = new PDFRenderer(document);
            int totalPages = document.getNumberOfPages();

            // Use Apache POI to build an Excel workbook
            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Extracted Data");

                int rowNum = 0;
                for (int i = 0; i < totalPages; i++) {
                    BufferedImage image = renderer.renderImageWithDPI(i, 200, ImageType.RGB);
                    String pageText = tesseract.doOCR(image);

                    // For simplicity: put each page’s text in one cell
                    Row row = sheet.createRow(rowNum++);
                    Cell cell = row.createCell(0);
                    cell.setCellValue("Page " + (i + 1) + ": " + pageText);
                }

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                workbook.write(bos);
                return bos.toByteArray();
            }
        } catch (Exception e) {
            // handle exceptions properly, maybe wrap in your own
            throw new RuntimeException("Failed to extract to Excel: " + e.getMessage(), e);
        }
    }


    private String doOcr(BufferedImage image) {
        try {
            return tesseract.doOCR(image);
        } catch (TesseractException e) {
            log.error("OCR failed: {}", e.getMessage(), e);
            return "";
        }
    }
/*
    private BoothDetailsDto extractBoothInfo(String text) {
        // Patterns for booth number / booth name
        Pattern boothNumberPattern = Pattern.compile(
                "(?:Booth\\s*(?:No|Number)|बूथ\\s*संख्या)[:\\s]+(\\d+)",
                Pattern.CASE_INSENSITIVE);
        Pattern boothNamePattern = Pattern.compile(
                "(?:Booth\\s*Name|बूथ\\s*नाम)[:\\s]+(.+?)(?:\\r?\\n|$)",
                Pattern.CASE_INSENSITIVE);

        BoothDetailsDto b = BoothDetailsDto.builder();

        Matcher m1 = boothNumberPattern.matcher(text);
        if (m1.find()) {
            b.getBoothNumber(m1.group(1).trim());
        } else {
            b.boothNumber("Not Found");
        }

        Matcher m2 = boothNamePattern.matcher(text);
        if (m2.find()) {
            b.boothName(m2.group(1).trim());
        } else {
            b.boothName("Not Found");
        }

        return b.build();
    }*/

    private List<VoterDetailsDto> extractTableData(String text, int pageNumber) {
        List<VoterDetailsDto> rows = new ArrayList<>();
        String[] lines = text.split("\\r?\\n");
        boolean tableStarted = false;
        int recordCount = 0;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // detect header
            if (!tableStarted && line.toLowerCase().matches(".*\\b(s\\.?no|serial|sno|क्र\\.?सं).*\\b(name|नाम).*\\b(age|उम्र).*")) {
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

            if (recordCount >= 10) {
                break;
            }
        }
        return rows;
    }

    private byte[] createExcel(AllExcelDetailsDto data) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet boothSheet = workbook.createSheet("Booth Info");
            Row h0 = boothSheet.createRow(0);
            h0.createCell(0).setCellValue("Booth Number");
            h0.createCell(1).setCellValue("Booth Name");
            Row d0 = boothSheet.createRow(1);
            if (data.getBoothDetailsDto() != null) {
                d0.createCell(0).setCellValue(data.getBoothDetailsDto().getBoothNumber());
                d0.createCell(1).setCellValue(data.getBoothDetailsDto().getBoothName());
            }

            Sheet tableSheet = workbook.createSheet("Voters");
            Row h1 = tableSheet.createRow(0);
            h1.createCell(0).setCellValue("Page");
            h1.createCell(1).setCellValue("Serial");
            h1.createCell(2).setCellValue("Name");
            h1.createCell(3).setCellValue("Age");
            int r = 1;
            for (VoterDetailsDto v : data.getVoterDetailsDtoList()) {
                Row rr = tableSheet.createRow(r++);
                rr.createCell(0).setCellValue(v.getPageNumber());
                rr.createCell(1).setCellValue(v.getVoterId());
                rr.createCell(2).setCellValue(v.getName());
                rr.createCell(3).setCellValue(v.getAge());
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            log.error("Error creating Excel: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create Excel", e);
        }
    }
}
