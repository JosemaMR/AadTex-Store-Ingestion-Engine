package com.AadTex.Store.Ingestion.Engine.AadTex.Store.Ingestion.Engine.repository;

import com.AadTex.Store.Ingestion.Engine.AadTex.Store.Ingestion.Engine.StorePaths;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Stream;

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
}