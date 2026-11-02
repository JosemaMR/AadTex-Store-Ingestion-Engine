# AadTex Store Ingestion Engine (App 1)

Proyecto de **Acceso a Datos (AAD) – RA1: Manejo de ficheros** (`ra1_act1`).

Repositorio en GitHub: https://github.com/JosemaMR/AadTex-Store-Ingestion-Engine

## Qué hace

Cada 5 segundos (Spring `@Scheduled`) revisa `workspace/store_data/input/` y procesa los lotes que dejan las tiendas:

| Fichero | Tratamiento |
| :--- | :--- |
| `.csv` | Lectura en UTF-8 (tildes y caracteres chinos) y suma de ventas |
| `.enc` | Descifrado AES-128 en memoria (clave en `keys/AadTexSecret.key`) |
| `.zip` | Lectura con `ZipInputStream` sin ficheros temporales (CSV + firma) |
| `.jpg` | Copia con buffer de 4 KB a `output/processed/signatures/` |
| 0 bytes / corrupto | Alerta a `soporte@aadtex.com` (log) y movido a `output/error/` |

Validaciones: cada CSV/ENC necesita su firma pareja (`daily_sales_x` -> `signature_x.jpg`), y todo JPG debe empezar por los bytes `FF D8 FF`.

## Resultado (`workspace/store_data/output/`)

- `processed/`: lotes ya procesados (y `processed/signatures/` con las firmas copiadas).
- `error/`: ficheros vacíos, corruptos o inválidos.
- `bucket/aadtex-daily-reports/AÑO/MES/summary_batch_[ID].json`: resumen de cada lote para la App 2.

## Cómo ejecutarlo

Requisitos: Java 21 o superior.

1. Descomprimir el proyecto.
2. Arrancar con `mvnw spring-boot:run` (o ejecutar `AadTexStoreIngestionEngineApplication` desde el IDE) **desde la raíz del proyecto**.
3. Al arrancar, `MultiLanguageDataGenerator` crea los datos de prueba (clave, CSV, firmas, `.enc`, ZIP y fichero vacío) y el scheduler los procesa en unos segundos.

## Estructura

- `AadTexStoreIngestionEngineApplication`: clase principal (`@EnableScheduling`).
- `StoreIngestionScheduler`: tarea programada cada 5 s.
- `MultiLanguageDataGenerator`: genera los datos de prueba al arrancar.
- `StorePaths`: rutas de las carpetas.
- `service/ProcessFileService`: decide qué hacer con cada fichero.
- `service/CryptoService`: descifrado AES.
- `service/CloudBucketService`: escribe los JSON de resumen.
- `repository/StoreFileRepository`, `repository/CsvReader`: acceso a ficheros (NIO.2, buffers, UTF-8).# AadTex-Store-Ingestion-Engine