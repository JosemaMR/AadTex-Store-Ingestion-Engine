package com.AadTex.Store.Ingestion.Engine.AadTex.Store.Ingestion.Engine;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.file.Path;


@Component
public class StorePaths {
    private final Path base;

    public StorePaths(@Value("${aadtex.base-dir}") String baseDir) {
        this.base = Path.of(baseDir).toAbsolutePath();
    }

    public Path keys()       { return base.resolve("keys"); }
    public Path input()      { return base.resolve("input"); }
    public Path processed()  { return base.resolve("output/processed"); }
    public Path signatures() { return base.resolve("output/processed/signatures"); }
    public Path error()      { return base.resolve("output/error"); }
    public Path bucket()     { return base.resolve("output/bucket/aadtex-daily-reports/2026/09"); }
}
