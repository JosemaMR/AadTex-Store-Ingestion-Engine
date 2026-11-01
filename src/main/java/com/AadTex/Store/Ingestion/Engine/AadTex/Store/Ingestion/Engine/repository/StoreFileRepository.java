package com.AadTex.Store.Ingestion.Engine.AadTex.Store.Ingestion.Engine.repository;

import com.AadTex.Store.Ingestion.Engine.AadTex.Store.Ingestion.Engine.StorePaths;
import org.springframework.stereotype.Repository;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipInputStream;


@Repository
public class StoreFileRepository {

    private final StorePaths paths;

    public StoreFileRepository(StorePaths paths) {
        this.paths = paths;
    }

    // Crea las carpetas si no existen (si ya existen, no hace nada)
    public void createFolders() throws IOException {
        Files.createDirectories(paths.input());
        Files.createDirectories(paths.signatures());
        Files.createDirectories(paths.error());
        Files.createDirectories(paths.bucket());
    }

    // Lista los ficheros que hay ahora mismo en input/
    public List<Path> listInputFiles() throws IOException {
        try (Stream<Path> stream = Files.list(paths.input())) {
            return stream.filter(Files::isRegularFile).sorted().toList();
        }
    }

    // true si el fichero pesa 0 bytes
    public boolean isEmpty(Path file) throws IOException {
        return Files.size(file) == 0;
    }

    // Mueve el fichero a output/error/ (si ya había uno con ese nombre, lo reemplaza)
    public void moveToError(Path file) throws IOException {
        Files.move(file, paths.error().resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
    }
    // Lee la clave AES de keys/AadTexSecret.key
    public String readKey() throws IOException {
        return Files.readString(paths.keys().resolve("AadTexSecret.key"), StandardCharsets.UTF_8).trim();
    }

    // Lee un fichero entero y lo deja en memoria (RAM)
    public byte[] readBytes(Path file) throws IOException {
        return Files.readAllBytes(file);
    }

    // Abre un ZIP para leerlo entrada a entrada (quien lo abre debe cerrarlo)
    public ZipInputStream openZip(Path file) throws IOException {
        return new ZipInputStream(Files.newInputStream(file));
    }
    // Copia una firma JPG que está en input/ a output/processed/signatures/
    public long copySignatureFile(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            return copySignatureStream(in, file.getFileName().toString());
        }
    }

    // Copia una firma desde cualquier InputStream (un fichero, una entrada de un ZIP...)
    // Usa un buffer de 4 KB. NO cierra el stream de entrada: lo cierra quien lo abrió.
    public long copySignatureStream(InputStream in, String fileName) throws IOException {
        Path target = paths.signatures().resolve(onlyName(fileName));
        BufferedInputStream bufferedIn = new BufferedInputStream(in);

        long total = 0;
        try (BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(target))) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = bufferedIn.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                total += read;
            }
        }
        return total;
    }

    // "carpeta/firma.jpg" -> "firma.jpg" (así nunca se escribe fuera de signatures/)
    private String onlyName(String fileName) {
        return fileName.substring(Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\')) + 1);
    }
}