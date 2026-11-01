package com.AadTex.Store.Ingestion.Engine.AadTex.Store.Ingestion.Engine.util;
import lombok.experimental.UtilityClass;

import java.nio.file.Path;

@UtilityClass
public class Utils {

    // Devuelve la extensión en minúsculas: "daily_sales_spain.csv" -> "csv"
    public static String extensionOf(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        int dot = name.lastIndexOf('.');
        return dot == -1 ? "" : name.substring(dot + 1);
    }
}