package com.example.pdftoexcelservice.dtos;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class BatchProcessingResult {
    private Integer totalFiles;
    private Integer successCount;
    private Integer failureCount;
    private Long timeTakenMs;
    private List<String> processedFiles;
    private List<String> failedFiles;
}
