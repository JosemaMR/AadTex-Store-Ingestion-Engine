package com.AadTex.Store.Ingestion.Engine.AadTex.Store.Ingestion.Engine.service;

import com.AadTex.Store.Ingestion.Engine.AadTex.Store.Ingestion.Engine.model.Sale;
import com.AadTex.Store.Ingestion.Engine.AadTex.Store.Ingestion.Engine.repository.CsvReader;
import com.AadTex.Store.Ingestion.Engine.AadTex.Store.Ingestion.Engine.repository.StoreFileRepository;
import com.AadTex.Store.Ingestion.Engine.AadTex.Store.Ingestion.Engine.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

@Service
public class ProcessFileService {

    private static final Logger log = LoggerFactory.getLogger(ProcessFileService.class);

    private final StoreFileRepository fileRepository;
    private final CsvReader csvReader;

    public ProcessFileService(StoreFileRepository fileRepository, CsvReader csvReader) {
        this.fileRepository = fileRepository;
        this.csvReader = csvReader;
    }

    // Lo llama el Scheduler cada 5 segundos
    public void processInput() {
        try {
            fileRepository.createFolders();

            for (Path file : fileRepository.listInputFiles()) {
                try {
                    processFile(file);
                } catch (IOException e) {
                    log.error("No se pudo procesar {}", file.getFileName(), e);
                }
            }
        } catch (IOException e) {
            log.error("No se pudo leer la carpeta input", e);
        }
    }

    // Decide qué hacer con un fichero concreto
    private void processFile(Path file) throws IOException {
        String name = file.getFileName().toString();

        // 1) ¿Está vacío? -> alerta y a la carpeta de errores
        if (fileRepository.isEmpty(file)) {
            log.error("[ALERT EMAIL SENT -> soporte@aadtex.com] CRITICAL: Empty/Corrupt batch file: {}", name);
            fileRepository.moveToError(file);
            return;
        }

        // 2) Si no, según su extensión
        switch (Utils.extensionOf(file)) {
            case "csv" -> processCsv(file);
            case "enc" -> log.info("ENC detectado: {}", name);   // pendiente: descifrarlo
            case "zip" -> log.info("ZIP detectado: {}", name);   // pendiente: descomprimirlo
            case "jpg" -> log.info("JPG detectado: {}", name);   // pendiente: copiar la firma
            default    -> log.warn("Tipo de fichero no soportado, se ignora: {}", name);
        }
    }

    // Lee un CSV y calcula el total de sus ventas
    private void processCsv(Path file) throws IOException {
        List<Sale> sales = csvReader.readFile(file);
        logSummary(file.getFileName().toString(), sales);
    }

    // Aquí se hacen las cuentas: suma de todos los importes del lote
    private void logSummary(String batchName, List<Sale> sales) {
        BigDecimal total = BigDecimal.ZERO;
        for (Sale sale : sales) {
            total = total.add(sale.amount());
        }

        String store = sales.isEmpty() ? "-" : sales.get(0).storeName();
        log.info("Lote {}: {} ventas, total {} (tienda: {})", batchName, sales.size(), total, store);
    }
}