package com.AadTex.Store.Ingestion.Engine.AadTex.Store.Ingestion.Engine.model;

import java.math.BigDecimal;

// Una fila del CSV: una venta
public record Sale(String storeId, String storeName, BigDecimal amount, String category, String date) {
}