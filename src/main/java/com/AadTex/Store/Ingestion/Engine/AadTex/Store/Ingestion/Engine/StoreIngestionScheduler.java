package com.AadTex.Store.Ingestion.Engine.AadTex.Store.Ingestion.Engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;

@Component
public class StoreIngestionScheduler {

    private static final Logger log = LoggerFactory.getLogger(StoreIngestionScheduler.class);

    private final StorePaths paths;

    public StoreIngestionScheduler(StorePaths paths) {
        this.paths = paths;
    }

    @Scheduled(fixedRate = 5000)
    public void processStoreBatches() {
        try {
            Files.createDirectories(paths.input());
            Files.createDirectories(paths.signatures());
            Files.createDirectories(paths.error());
            Files.createDirectories(paths.bucket());
            log.info("Carpetas listas. Buscando lotes en {}", paths.input());
        } catch (IOException e) {
            log.error("No se pudieron crear las carpetas", e);
        }
    }
}