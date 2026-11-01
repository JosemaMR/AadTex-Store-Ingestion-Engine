package com.AadTex.Store.Ingestion.Engine.AadTex.Store.Ingestion.Engine.repository;

import com.AadTex.Store.Ingestion.Engine.AadTex.Store.Ingestion.Engine.model.Sale;
import org.springframework.stereotype.Repository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Repository
public class CsvReader {

    // Lee un CSV que está guardado en disco
    public List<Sale> readFile(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            return readStream(in);
        }
    }

    // Lee un CSV desde cualquier InputStream: un fichero, bytes descifrados, una entrada de un ZIP...
    // OJO: aquí NO se cierra el stream. Lo cierra quien nos lo pasa.
    public List<Sale> readStream(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));

        List<Sale> sales = new ArrayList<>();
        reader.readLine(); // la primera línea es la cabecera, se salta

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isBlank()) {
                continue;
            }
            sales.add(parseLine(line));
        }
        return sales;
    }

    // "ZARA_ES_001,Zara Gran Vía Madrid,2000.00,KIDS,2026-09-13" -> un objeto Sale
    private Sale parseLine(String line) throws IOException {
        String[] parts = line.split(",");
        if (parts.length != 5) {
            throw new IOException("Línea con formato incorrecto: " + line);
        }
        try {
            return new Sale(parts[0].trim(), parts[1].trim(), new BigDecimal(parts[2].trim()),
                    parts[3].trim(), parts[4].trim());
        } catch (NumberFormatException e) {
            throw new IOException("Importe no válido en la línea: " + line);
        }
    }
}