package com.example.pdftoexcelservice.service;
import com.example.pdftoexcelservice.dtos.AllExcelDetailsDto;
import com.example.pdftoexcelservice.dtos.VoterDetailsDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Slf4j
@Service
public class ExcelGeneratorService {

    public byte[] createExcel(AllExcelDetailsDto data) {
        try (Workbook workbook = new XSSFWorkbook()) {

            // Create Voters sheet
            Sheet votersSheet = workbook.createSheet("Voters");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            // Header row
            Row headerRow = votersSheet.createRow(0);
            createCell(headerRow, 0, "Page", headerStyle);
            createCell(headerRow, 1, "Serial No", headerStyle);
            createCell(headerRow, 2, "Name", headerStyle);
            createCell(headerRow, 3, "Age", headerStyle);

            // Data rows
            int rowNum = 1;
            for (VoterDetailsDto voter : data.getVoterDetailsDtoList()) {
                Row row = votersSheet.createRow(rowNum++);
                createCell(row, 0, voter.getPageNumber(), dataStyle);
                createCell(row, 1, voter.getVoterId(), dataStyle);
                createCell(row, 2, voter.getName(), dataStyle);
                createCell(row, 3, voter.getAge(), dataStyle);
            }

            // Auto-size columns
            for (int i = 0; i < 4; i++) {
                votersSheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();

        } catch (Exception e) {
            log.error("Error creating Excel", e);
            throw new RuntimeException("Failed to create Excel", e);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private void createCell(Row row, int column, Object value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value instanceof Integer) {
            cell.setCellValue((Integer) value);
        } else if (value instanceof String) {
            cell.setCellValue((String) value);
        }
        cell.setCellStyle(style);
    }
}
