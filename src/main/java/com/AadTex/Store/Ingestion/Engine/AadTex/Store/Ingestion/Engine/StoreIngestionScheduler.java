package com.AadTex.Store.Ingestion.Engine.AadTex.Store.Ingestion.Engine;

import com.AadTex.Store.Ingestion.Engine.AadTex.Store.Ingestion.Engine.service.ProcessFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class StoreIngestionScheduler {

    private static final Logger log = LoggerFactory.getLogger(StoreIngestionScheduler.class);

    private final ProcessFileService processFileService;

    public StoreIngestionScheduler(ProcessFileService processFileService) {
        this.processFileService = processFileService;
    }

    @Scheduled(fixedRate = 5000, initialDelay = 2000)
    public void processStoreBatches() {
        log.info("Scheduler ejecutado: buscando lotes en /input/ ...");
        processFileService.processInput();
    }
}