package com.AadTex.Store.Ingestion.Engine.AadTex.Store.Ingestion.Engine.model;

import java.math.BigDecimal;
import java.util.List;

// El resumen de un lote, que acabará en el JSON del bucket
public record BatchSummary(String batchId, String source, String date,
                           int salesCount, BigDecimal total, List<String> stores) {
}