package com.AadTex.Store.Ingestion.Engine.AadTex.Store.Ingestion.Engine.util;
import lombok.experimental.UtilityClass;

import java.nio.file.Path;

@UtilityClass
public class Utils {

    // Extensión de un fichero en minúsculas: "daily_sales_spain.csv" -> "csv"
    public static String extensionOf(Path file) {
        return extensionOf(file.getFileName().toString());
    }

    // Lo mismo, pero a partir de un nombre (sirve para las entradas de un ZIP)
    public static String extensionOf(String fileName) {
        String name = fileName.toLowerCase();
        int dot = name.lastIndexOf('.');
        return dot == -1 ? "" : name.substring(dot + 1);
    }
}