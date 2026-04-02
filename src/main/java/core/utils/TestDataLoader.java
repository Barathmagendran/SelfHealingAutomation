package core.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import core.config.ConfigReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;

/**
 * Centralized test data loading utility.
 *
 * <p>Supports reading test data from:
 * <ul>
 *   <li>JSON files (via Jackson)</li>
 *   <li>CSV files (via OpenCSV)</li>
 *   <li>Excel files .xlsx (via Apache POI)</li>
 * </ul>
 *
 * <p>All paths are resolved relative to {@code testdata.path} in config.properties.
 *
 * <p>Example usage in step definitions:
 * <pre>
 *   Map<String, String> user = TestDataLoader.loadJsonAsMap("json/login.json");
 *   List<Map<String, String>> users = TestDataLoader.loadCsvAsListOfMaps("csv/users.csv");
 *   Map<String, String> row = TestDataLoader.loadExcelRow("excel/testdata.xlsx", "LoginSheet", 1);
 * </pre>
 *
 * @author Enterprise QA Team
 * @version 1.0
 */
public class TestDataLoader {

    private static final Logger log = LogManager.getLogger(TestDataLoader.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Base path for test data files, from config */
    private static final String BASE_PATH = ConfigReader.get(
        "testdata.path", "src/test/resources/testdata");

    private TestDataLoader() {}

    // ─────────────────────────────────────────────────────────
    // JSON Loaders
    // ─────────────────────────────────────────────────────────

    /**
     * Loads a JSON file relative to the testdata path into a Map.
     *
     * @param relativePath relative path from testdata root (e.g., "json/login.json")
     * @return Map of key-value pairs from JSON
     */
    public static Map<String, String> loadJsonAsMap(String relativePath) {
        String fullPath = BASE_PATH + "/" + relativePath;
        log.info("Loading JSON test data from: {}", fullPath);
        try {
            return MAPPER.readValue(new File(fullPath),
                new TypeReference<Map<String, String>>() {});
        } catch (IOException e) {
            throw new RuntimeException("Failed to load JSON test data: " + fullPath, e);
        }
    }

    /**
     * Loads a JSON array file into a list of maps.
     *
     * @param relativePath relative path from testdata root
     * @return List of Maps representing each JSON object
     */
    public static List<Map<String, String>> loadJsonAsList(String relativePath) {
        String fullPath = BASE_PATH + "/" + relativePath;
        log.info("Loading JSON list from: {}", fullPath);
        try {
            return MAPPER.readValue(new File(fullPath),
                new TypeReference<List<Map<String, String>>>() {});
        } catch (IOException e) {
            throw new RuntimeException("Failed to load JSON list: " + fullPath, e);
        }
    }

    /**
     * Deserializes a JSON file to a specific POJO class.
     *
     * @param relativePath relative path from testdata root
     * @param clazz        POJO class to deserialize into
     * @param <T>          type parameter
     * @return deserialized object
     */
    public static <T> T loadJson(String relativePath, Class<T> clazz) {
        String fullPath = BASE_PATH + "/" + relativePath;
        log.info("Loading JSON [{}] as {}", fullPath, clazz.getSimpleName());
        try {
            return MAPPER.readValue(new File(fullPath), clazz);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load JSON as " + clazz.getSimpleName() + ": " + fullPath, e);
        }
    }

    // ─────────────────────────────────────────────────────────
    // CSV Loaders
    // ─────────────────────────────────────────────────────────

    /**
     * Reads a CSV file and returns all rows as a list of string arrays.
     * First row is treated as the header and skipped.
     *
     * @param relativePath relative path from testdata root (e.g., "csv/users.csv")
     * @return list of string arrays (each row)
     */
    public static List<String[]> loadCsv(String relativePath) {
        String fullPath = BASE_PATH + "/" + relativePath;
        log.info("Loading CSV from: {}", fullPath);
        try (CSVReader reader = new CSVReader(new FileReader(fullPath))) {
            List<String[]> all = reader.readAll();
            // Skip header row
            return all.size() > 1 ? all.subList(1, all.size()) : Collections.emptyList();
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load CSV: " + fullPath, e);
        }
    }

    /**
     * Reads a CSV file and returns rows as a list of Maps (header → value).
     *
     * @param relativePath relative path from testdata root
     * @return list of Maps keyed by CSV header
     */
    public static List<Map<String, String>> loadCsvAsListOfMaps(String relativePath) {
        String fullPath = BASE_PATH + "/" + relativePath;
        log.info("Loading CSV as map list from: {}", fullPath);

        try (CSVReader reader = new CSVReader(new FileReader(fullPath))) {
            List<String[]> all = reader.readAll();
            if (all.isEmpty()) return Collections.emptyList();

            String[] headers = all.get(0);
            List<Map<String, String>> result = new ArrayList<>();

            for (int i = 1; i < all.size(); i++) {
                String[] row = all.get(i);
                Map<String, String> rowMap = new LinkedHashMap<>();
                for (int j = 0; j < headers.length; j++) {
                    rowMap.put(headers[j].trim(), j < row.length ? row[j].trim() : "");
                }
                result.add(rowMap);
            }
            return result;
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load CSV as map list: " + fullPath, e);
        }
    }

    // ─────────────────────────────────────────────────────────
    // Excel Loaders
    // ─────────────────────────────────────────────────────────

    /**
     * Reads all rows from an Excel sheet as a list of Maps (column header → cell value).
     *
     * @param relativePath relative path from testdata root (e.g., "excel/testdata.xlsx")
     * @param sheetName    name of the sheet to read
     * @return list of Maps keyed by column header
     */
    public static List<Map<String, String>> loadExcelSheet(String relativePath, String sheetName) {
        String fullPath = BASE_PATH + "/" + relativePath;
        log.info("Loading Excel sheet [{}] from: {}", sheetName, fullPath);

        try (Workbook workbook = new XSSFWorkbook(new FileInputStream(fullPath))) {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new RuntimeException("Sheet not found: " + sheetName + " in " + fullPath);
            }

            List<Map<String, String>> result = new ArrayList<>();
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) return result;

            // Read headers
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(cell.getStringCellValue().trim());
            }

            // Read data rows
            for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) continue;

                Map<String, String> rowMap = new LinkedHashMap<>();
                for (int colIdx = 0; colIdx < headers.size(); colIdx++) {
                    Cell cell = row.getCell(colIdx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    rowMap.put(headers.get(colIdx), getCellValueAsString(cell));
                }
                result.add(rowMap);
            }

            log.info("Loaded {} data rows from sheet [{}]", result.size(), sheetName);
            return result;

        } catch (IOException e) {
            throw new RuntimeException("Failed to load Excel: " + fullPath, e);
        }
    }

    /**
     * Reads a specific row from an Excel sheet by row index (1-based, header=0).
     *
     * @param relativePath relative path from testdata root
     * @param sheetName    sheet name
     * @param rowIndex     1-based row index (row 1 = first data row)
     * @return Map of column header to cell value
     */
    public static Map<String, String> loadExcelRow(String relativePath, String sheetName, int rowIndex) {
        List<Map<String, String>> allRows = loadExcelSheet(relativePath, sheetName);
        if (rowIndex < 1 || rowIndex > allRows.size()) {
            throw new IndexOutOfBoundsException(
                "Row index " + rowIndex + " out of range. Sheet has " + allRows.size() + " data rows.");
        }
        return allRows.get(rowIndex - 1);
    }

    // ─────────────────────────────────────────────────────────
    // Utilities
    // ─────────────────────────────────────────────────────────

    /**
     * Safely reads a cell value as a String, regardless of cell type.
     *
     * @param cell the Excel cell (may be null)
     * @return cell value as string, or empty string if null/blank
     */
    private static String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                ? cell.getLocalDateTimeCellValue().toString()
                : String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCachedFormulaResultType() == CellType.NUMERIC
                ? String.valueOf((long) cell.getNumericCellValue())
                : cell.getStringCellValue();
            default      -> "";
        };
    }
}
