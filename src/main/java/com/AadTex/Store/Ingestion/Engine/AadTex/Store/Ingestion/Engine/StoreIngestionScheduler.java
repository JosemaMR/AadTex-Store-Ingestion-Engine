package com.AadTex.Store.Ingestion.Engine.AadTex.Store.Ingestion.Engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class StoreIngestionScheduler {

    private static final Logger log = LoggerFactory.getLogger(StoreIngestionScheduler.class);

    @Scheduled(fixedRate = 5000)
    public void processStoreBatches() {
        log.info("Scheduler ejecutado: buscando lotes en /input/ ...");
    }
}