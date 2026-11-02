package com.AadTex.Store.Ingestion.Engine.AadTex.Store.Ingestion.Engine.service;

import com.AadTex.Store.Ingestion.Engine.AadTex.Store.Ingestion.Engine.model.BatchSummary;
import com.AadTex.Store.Ingestion.Engine.AadTex.Store.Ingestion.Engine.model.Sale;
import com.AadTex.Store.Ingestion.Engine.AadTex.Store.Ingestion.Engine.repository.CsvReader;
import com.AadTex.Store.Ingestion.Engine.AadTex.Store.Ingestion.Engine.repository.StoreFileRepository;
import com.AadTex.Store.Ingestion.Engine.AadTex.Store.Ingestion.Engine.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
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
    private final CloudBucketService cloudBucketService;

    public ProcessFileService(StoreFileRepository fileRepository, CsvReader csvReader,
                              CryptoService cryptoService, CloudBucketService cloudBucketService) {
        this.fileRepository = fileRepository;
        this.csvReader = csvReader;
        this.cryptoService = cryptoService;
        this.cloudBucketService = cloudBucketService;
    }

    // Lo llama el Scheduler cada 5 segundos
    public void processInput() {
        try {
            fileRepository.createFolders();

            for (Path file : fileRepository.listInputFiles()) {
                try {
                    processFile(file);
                } catch (IOException e) {
                    log.error("[ALERT EMAIL SENT -> soporte@aadtex.com] CRITICAL: Corrupt batch file: {} ({})",
                            file.getFileName(), e.getMessage());
                    moveToError(file);
                }
            }
        } catch (IOException e) {
            log.error("No se pudo leer la carpeta input", e);
        }
    }


    // Mueve a error/ un fichero que ha fallado (si el propio movimiento falla, solo lo anota)
    private void moveToError(Path file) {
        try {
            fileRepository.moveToError(file);
        } catch (IOException e) {
            log.error("No se pudo mover {} a error/", file.getFileName(), e);
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
            default    -> {
                log.warn("Tipo de fichero no soportado, se ignora: {}", name);
                return;   // este no se mueve a processed
            }
        }
        // 3) Si ha ido bien, el lote sale de input/ para no procesarlo otra vez
        fileRepository.moveToProcessed(file);
    }

    // Lee un CSV y calcula el total de sus ventas
    private void processCsv(Path file) throws IOException {
        requireSignature(file);
        List<Sale> sales = csvReader.readFile(file);
        String name = file.getFileName().toString();
        summarize(Utils.baseNameOf(name), name, sales);
    }

    // Descifra un .enc en memoria y lee el CSV que lleva dentro
    private void processEnc(Path file) throws IOException {
        requireSignature(file);
        String secret = fileRepository.readKey();
        byte[] encrypted = fileRepository.readBytes(file);

        try {
            byte[] plain = cryptoService.decrypt(encrypted, secret);
            List<Sale> sales = csvReader.readStream(new ByteArrayInputStream(plain));
            String name = file.getFileName().toString();
            summarize(Utils.baseNameOf(name), name, sales);
        } catch (GeneralSecurityException e) {
            throw new IOException("No se pudo descifrar " + file.getFileName(), e);
        }
    }

    // Abre un ZIP y recorre lo que lleva dentro, todo en memoria (sin ficheros temporales)
    // Abre un ZIP y recorre lo que lleva dentro, todo en memoria (sin ficheros temporales)
    private void processZip(Path file) throws IOException {
        String zipName = file.getFileName().toString();
        int entries = 0;
        List<Sale> sales = null;
        String csvName = null;
        boolean hasSignature = false;

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
                        sales = csvReader.readStream(zip);   // sin cerrar: lo cierra el try de arriba
                        csvName = entryName;
                    }
                    case "jpg" -> {
                        long bytes = fileRepository.copySignatureStream(zip, entryName);   // sin cerrar el zip
                        log.info("Firma copiada desde {}: {} ({} bytes)", zipName, entryName, bytes);
                        hasSignature = true;
                    }
                    default    -> log.warn("Entrada no soportada dentro de {}: {}", zipName, entryName);
                }
            }
        }

        if (entries == 0) {
            throw new IOException("El ZIP está vacío o dañado: " + zipName);
        }
        if (sales == null) {
            throw new IOException("El ZIP no trae ningún CSV: " + zipName);
        }
        if (!hasSignature) {
            throw new IOException("El ZIP no trae firma JPG: " + zipName);
        }

        summarize(Utils.baseNameOf(csvName), zipName + " > " + csvName, sales);
    }


    // Cada CSV/ENC debe traer su firma pareja: daily_sales_spain.csv -> signature_spain.jpg
    private void requireSignature(Path file) throws IOException {
        String signature = Utils.signatureNameFor(file.getFileName().toString());
        if (!fileRepository.signatureExists(signature)) {
            throw new IOException("Falta la firma pareja " + signature + " para " + file.getFileName());
        }
    }


    // Aquí se hacen las cuentas: suma de todos los importes del lote
    // Suma los importes del lote, lo anota en el log y publica el resumen JSON en el bucket
    private void summarize(String batchId, String source, List<Sale> sales) throws IOException {
        BigDecimal total = BigDecimal.ZERO;
        Set<String> stores = new LinkedHashSet<>();
        for (Sale sale : sales) {
            total = total.add(sale.amount());
            stores.add(sale.storeName());
        }
        String date = sales.isEmpty() ? LocalDate.now().toString() : sales.get(0).date();

        log.info("Lote {}: {} ventas, total {} (tiendas: {})", source, sales.size(), total, stores);

        BatchSummary summary = new BatchSummary(batchId, source, date, sales.size(), total, new ArrayList<>(stores));
        log.info("Resumen publicado en el bucket: {}", cloudBucketService.save(summary));
    }

    // Copia una firma JPG a output/processed/signatures/
    private void processSignature(Path file) throws IOException {
        long bytes = fileRepository.copySignatureFile(file);
        log.info("Firma copiada: {} ({} bytes)", file.getFileName(), bytes);
    }
}