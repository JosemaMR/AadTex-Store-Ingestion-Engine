package com.AadTex.Store.Ingestion.Engine.AadTex.Store.Ingestion.Engine.service;

import com.AadTex.Store.Ingestion.Engine.AadTex.Store.Ingestion.Engine.model.Sale;
import com.AadTex.Store.Ingestion.Engine.AadTex.Store.Ingestion.Engine.repository.CsvReader;
import com.AadTex.Store.Ingestion.Engine.AadTex.Store.Ingestion.Engine.repository.StoreFileRepository;
import com.AadTex.Store.Ingestion.Engine.AadTex.Store.Ingestion.Engine.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.List;

@Service
public class ProcessFileService {

    private static final Logger log = LoggerFactory.getLogger(ProcessFileService.class);

    private final StoreFileRepository fileRepository;
    private final CsvReader csvReader;
    private final CryptoService cryptoService;

    public ProcessFileService(StoreFileRepository fileRepository, CsvReader csvReader, CryptoService cryptoService) {
        this.fileRepository = fileRepository;
        this.csvReader = csvReader;
        this.cryptoService = cryptoService;
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
            case "enc" -> processEnc(file);
            case "zip" -> processZip(file);
            case "jpg" -> processSignature(file);
            default    -> log.warn("Tipo de fichero no soportado, se ignora: {}", name);
        }
    }

    // Lee un CSV y calcula el total de sus ventas
    private void processCsv(Path file) throws IOException {
        List<Sale> sales = csvReader.readFile(file);
        logSummary(file.getFileName().toString(), sales);
    }

    // Descifra un .enc en memoria y lee el CSV que lleva dentro
    private void processEnc(Path file) throws IOException {
        String secret = fileRepository.readKey();
        byte[] encrypted = fileRepository.readBytes(file);

        try {
            byte[] plain = cryptoService.decrypt(encrypted, secret);
            List<Sale> sales = csvReader.readStream(new ByteArrayInputStream(plain));
            logSummary(file.getFileName().toString(), sales);
        } catch (GeneralSecurityException e) {
            throw new IOException("No se pudo descifrar " + file.getFileName(), e);
        }
    }

    // Abre un ZIP y recorre lo que lleva dentro, todo en memoria (sin ficheros temporales)
    private void processZip(Path file) throws IOException {
        String zipName = file.getFileName().toString();
        int entries = 0;

        try (ZipInputStream zip = fileRepository.openZip(file)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                entries++;
                String entryName = entry.getName();

                switch (Utils.extensionOf(entryName)) {
                    case "csv" -> {
                        List<Sale> sales = csvReader.readStream(zip);   // sin cerrar: lo cierra el try de arriba
                        logSummary(zipName + " > " + entryName, sales);
                    }
                    case "jpg" -> {
                        long bytes = fileRepository.copySignatureStream(zip, entryName);   // sin cerrar el zip
                        log.info("Firma copiada desde {}: {} ({} bytes)", zipName, entryName, bytes);
                    }   // pendiente: copiarla
                    default    -> log.warn("Entrada no soportada dentro de {}: {}", zipName, entryName);
                }
            }
        }

        if (entries == 0) {
            throw new IOException("El ZIP está vacío o dañado: " + zipName);
        }
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

    // Copia una firma JPG a output/processed/signatures/
    private void processSignature(Path file) throws IOException {
        long bytes = fileRepository.copySignatureFile(file);
        log.info("Firma copiada: {} ({} bytes)", file.getFileName(), bytes);
    }
}