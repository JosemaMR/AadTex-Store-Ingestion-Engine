package com.AadTex.Store.Ingestion.Engine.AadTex.Store.Ingestion.Engine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
public class MultiLanguageDataGenerator implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MultiLanguageDataGenerator.class);

    private static final String SECRET = "AadTexSecret2026";
    private static final String HEADER = "StoreID,StoreName,SalesAmount,Category,TransactionDate\n";

    private final StorePaths paths;

    public MultiLanguageDataGenerator(StorePaths paths) {
        this.paths = paths;
    }

    @Override
    public void run(String... args) throws Exception {
        Files.createDirectories(paths.keys());
        Files.createDirectories(paths.input());

        writeKey();
        writeCsvs();
        writeSignatures();
        writeEncryptedFile();
        writeZip();
        writeBrokenFile();

        log.info("Datos de prueba generados en {}", paths.input());
    }

    // keys/AadTexSecret.key
    private void writeKey() throws IOException {
        Files.writeString(paths.keys().resolve("AadTexSecret.key"), SECRET, StandardCharsets.UTF_8);
    }

    // daily_sales_spain.csv y daily_sales_china.csv
    private void writeCsvs() throws IOException {
        writeCsv("daily_sales_spain.csv", """
                ZARA_ES_001,Zara Puerta del Sol Madrid,14500.50,WOMEN,2026-09-13
                ZARA_ES_001,Zara Puerta del Sol Madrid,3600.30,MEN,2026-09-13
                ZARA_ES_001,Zara Gran Vía Madrid,2000.00,KIDS,2026-09-13
                """);

        writeCsv("daily_sales_china.csv", """
                ZARA_CH_001,Zara 上海南京东路店,28900.00,WOMEN,2026-09-13
                ZARA_CH_001,Zara 上海南京东路店,12400.80,MEN,2026-09-13
                """);
    }

    // signature_spain.jpg, signature_china.jpg y signature_financial.jpg
    private void writeSignatures() throws IOException {
        Files.write(paths.input().resolve("signature_spain.jpg"), fakeJpg("spain"));
        Files.write(paths.input().resolve("signature_china.jpg"), fakeJpg("china"));
        Files.write(paths.input().resolve("signature_financial.jpg"), fakeJpg("financial"));
    }

    // daily_sales_financial.enc (el CSV cifrado con AES-128)
    private void writeEncryptedFile() throws Exception {
        String csv = HEADER + """
                ZARA_CENTRAL_FIN,Central Financial Office,95000.00,FINANCIAL_REPORT,2026-09-13
                """;
        Files.write(paths.input().resolve("daily_sales_financial.enc"), encrypt(csv));
    }

    // batch_usa.zip (contiene un CSV y un JPG)
    private void writeZip() throws IOException {
        String csv = HEADER + """
                ZARA_US_001,Zara Times Square New York,42000.00,WOMEN,2026-09-13
                ZARA_US_001,Zara Times Square New York,18850.75,MEN,2026-09-13
                ZARA_US_001,Zara 5th Avenue New York,7600.00,KIDS,2026-09-13
                """;

        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(paths.input().resolve("batch_usa.zip")))) {
            zip.putNextEntry(new ZipEntry("daily_sales_usa.csv"));
            zip.write(csv.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();

            zip.putNextEntry(new ZipEntry("signature_usa.jpg"));
            zip.write(fakeJpg("usa"));
            zip.closeEntry();
        }
    }

    // daily_sales_broken_store.csv (0 bytes)
    private void writeBrokenFile() throws IOException {
        Files.write(paths.input().resolve("daily_sales_broken_store.csv"), new byte[0]);
    }

    // ---------- ayudas ----------

    private void writeCsv(String fileName, String rows) throws IOException {
        Files.writeString(paths.input().resolve(fileName), HEADER + rows, StandardCharsets.UTF_8);
    }

    // Un "JPG" falso: empieza con los magic bytes FF D8 FF E0, acaba con FF D9 y pesa 10 KB
    private byte[] fakeJpg(String owner) {
        byte[] jpg = new byte[10_000];
        jpg[0] = (byte) 0xFF;
        jpg[1] = (byte) 0xD8;
        jpg[2] = (byte) 0xFF;
        jpg[3] = (byte) 0xE0;

        byte[] name = owner.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(name, 0, jpg, 4, name.length);

        jpg[jpg.length - 2] = (byte) 0xFF;
        jpg[jpg.length - 1] = (byte) 0xD9;
        return jpg;
    }

    // Cifra un texto con AES-128 / ECB / PKCS5Padding usando SECRET
    private byte[] encrypt(String text) throws Exception {
        SecretKeySpec key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));
    }
}