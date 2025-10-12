package com.example.pdftoexcelservice.service;

import com.example.pdftoexcelservice.dtos.AllExcelDetailsDto;
import com.example.pdftoexcelservice.dtos.BatchProcessingResult;
import com.example.pdftoexcelservice.dtos.ProcessingResult;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@AllArgsConstructor
public class BatchProcessingService{

    private final PdfOcrService pdfOcrService;
    private final ExcelGeneratorService excelGeneratorService;

    public BatchProcessingResult processFolder(String pdfFolder, String excelOutputFolder) {
        long startTime = System.currentTimeMillis();

        File dir = new File(pdfFolder);
        List<File> pdfFiles = findAllPdfFiles(pdfFolder);
        if (pdfFiles.isEmpty()) {
            return BatchProcessingResult.builder()
                    .totalFiles(0).successCount(0).failureCount(0).timeTakenMs(0L)
                    .build();
        }

        // ensure output folder directory exists
        File outDir = new File(excelOutputFolder);
        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<String> processed = new ArrayList<>();
        List<String> failed = new ArrayList<>();

        // you can decide batch size, thread pool size locally here
        int batchSize = 5;
        int threadPoolSize = 10;

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threadPoolSize);
        executor.setMaxPoolSize(threadPoolSize * 2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("pdf-processor-");
        executor.initialize();

        // split into batches
        List<List<File>> batches = splitIntoBatches(pdfFiles, batchSize);

        for (int bi = 0; bi < batches.size(); bi++) {
            List<File> batch = batches.get(bi);

            List<CompletableFuture<ProcessingResult>> futures = batch.stream()
                    .map(file -> CompletableFuture.supplyAsync(() -> {
                        try {
                            AllExcelDetailsDto dto = pdfOcrService.extractDataFromFile(file);
                            byte[] excelBytes = excelGeneratorService.createExcel(dto);
                            String excelName = file.getName().replaceAll("(?i)\\.pdf$", ".xlsx");
                            Path outPath = Paths.get(excelOutputFolder, excelName);
                            Files.write(outPath, excelBytes);
                            return ProcessingResult.builder()
                                    .filename(file.getName()).success(true).build();
                        } catch (Exception e) {
                            return ProcessingResult.builder()
                                    .filename(file.getName()).success(false)
                                    .error(e.getMessage()).build();
                        }
                    }, executor))
                    .collect(Collectors.toList());

            // wait
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            for (CompletableFuture<ProcessingResult> f : futures) {
                ProcessingResult r = f.join();
                if (r.isSuccess()) {
                    successCount.incrementAndGet();
                    processed.add(r.getFilename());
                } else {
                    failureCount.incrementAndGet();
                    failed.add(r.getFilename() + " : " + r.getError());
                }
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;

        return BatchProcessingResult.builder()
                .totalFiles(pdfFiles.size())
                .successCount(successCount.get())
                .failureCount(failureCount.get())
                .timeTakenMs(totalTime)
                .processedFiles(processed)
                .failedFiles(failed)
                .build();
    }

    // reuse your findAllPdfFiles, splitIntoBatches etc.

    private List<File> findAllPdfFiles(String folder) {
        try (Stream<Path> stream = Files.walk(Paths.get(folder), 1)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(f -> f.getName().toLowerCase().endsWith(".pdf"))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Error reading folder", e);
        }
    }

    private List<List<File>> splitIntoBatches(List<File> list, int batchSize) {
        List<List<File>> batches = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            batches.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return batches;
    }

    // define the inner ProcessingResult class, etc.
}

