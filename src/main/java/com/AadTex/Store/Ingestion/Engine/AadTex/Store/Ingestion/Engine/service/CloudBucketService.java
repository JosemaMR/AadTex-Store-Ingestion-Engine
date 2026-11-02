package com.AadTex.Store.Ingestion.Engine.AadTex.Store.Ingestion.Engine.service;

import com.AadTex.Store.Ingestion.Engine.AadTex.Store.Ingestion.Engine.StorePaths;
import com.AadTex.Store.Ingestion.Engine.AadTex.Store.Ingestion.Engine.model.BatchSummary;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.stream.Collectors;

// Simula el "bucket" de la nube: escribe el resumen JSON en output/bucket/aadtex-daily-reports/AÑO/MES/
@Service
public class CloudBucketService {

    private final StorePaths paths;

    public CloudBucketService(StorePaths paths) {
        this.paths = paths;
    }

    // Guarda summary_batch_[ID].json y devuelve la ruta donde ha quedado
    public Path save(BatchSummary summary) throws IOException {
        LocalDate day;
        try {
            day = LocalDate.parse(summary.date());
        } catch (DateTimeParseException e) {
            throw new IOException("Fecha no válida en el lote: " + summary.date());
        }

        Path folder = paths.bucket()
                .resolve(String.valueOf(day.getYear()))
                .resolve(String.format("%02d", day.getMonthValue()));
        Files.createDirectories(folder);

        Path file = folder.resolve("summary_batch_" + summary.batchId() + ".json");
        Files.writeString(file, toJson(summary), StandardCharsets.UTF_8);
        return file;
    }

    // Construye el JSON a mano (es pequeño y así no hace falta ninguna librería)
    private String toJson(BatchSummary s) {
        String stores = s.stores().stream()
                .map(name -> "\"" + escape(name) + "\"")
                .collect(Collectors.joining(", "));

        return """
                {
                  "batchId": "%s",
                  "source": "%s",
                  "date": "%s",
                  "salesCount": %d,
                  "totalAmount": %s,
                  "stores": [%s]
                }
                """.formatted(escape(s.batchId()), escape(s.source()), s.date(),
                s.salesCount(), s.total().toPlainString(), stores);
    }

    // Para que una comilla o una barra en un nombre no rompa el JSON
    private String escape(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}