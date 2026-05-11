package com.crq.approval.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ExcelService {

    // Column indices (0-based). Update here if the sheet layout changes.
    private static final int COL_SNO         = 0; // S.No  (skipped)
    private static final int COL_CREATED     = 1; // Created (Yes/No)
    private static final int COL_APPLICATION = 2; // Application name
    private static final int COL_COUNTRY     = 3; // Country
    private static final int COL_CRQ_NUMBER  = 4; // CRQ Number
    private static final int COL_TYPE        = 5; // Type / Category
    private static final int COL_DESCRIPTION = 7; // Description / Justification

    private static final DateTimeFormatter[] DATE_FORMATS = {
        DateTimeFormatter.ofPattern("M/d/yyyy"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("d/M/yyyy"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
    };

    /**
     * Parses the Excel file and returns all CRQ data rows found across all date groups.
     *
     * Sheet layout:
     *   Row 1        – column headers (skipped)
     *   Green row    – date separator; col A holds the date for the group below it
     *   Data rows    – CRQ records; lastUpdated is inherited from the nearest green row above
     *
     * Column mapping:
     *   A (0): S.No          – skipped
     *   B (1): Created        – Yes/No
     *   C (2): Application    – application / team name
     *   D (3): Country        – country code
     *   E (4): CRQ Number     – CRQ identifier
     *   F (5): Type           – change type (Standard, Emergency, …)
     *   H (7): Description    – description / justification
     */
    public List<ExcelCrqRow> parseExcel(InputStream inputStream) {
        List<ExcelCrqRow> rows = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            boolean firstRow = true;
            LocalDateTime currentDateGroup = null;

            for (Row row : sheet) {
                if (firstRow) { firstRow = false; continue; } // skip header
                if (isRowEmpty(row)) continue;

                if (isGreenRow(row)) {
                    currentDateGroup = extractDateFromRow(row);
                    log.info("Date group row found: {}", currentDateGroup);
                    continue;
                }

                // Regular CRQ data row
                String crqNumber = getCellString(row, COL_CRQ_NUMBER);
                if (crqNumber == null || crqNumber.isBlank()) continue;

                ExcelCrqRow crqRow = new ExcelCrqRow();
                crqRow.setCrqNumber(crqNumber);
                crqRow.setCreated(getCellString(row, COL_CREATED));
                crqRow.setApplication(getCellString(row, COL_APPLICATION));
                crqRow.setCountry(getCellString(row, COL_COUNTRY));
                crqRow.setType(getCellString(row, COL_TYPE));
                crqRow.setDescription(getCellString(row, COL_DESCRIPTION));
                crqRow.setLastUpdated(currentDateGroup);

                rows.add(crqRow);
            }
            log.info("Parsed {} CRQ rows from Excel", rows.size());
        } catch (Exception e) {
            log.error("Failed to parse Excel file: {}", e.getMessage(), e);
            throw new RuntimeException("Excel parsing failed: " + e.getMessage(), e);
        }

        return rows;
    }

    // ── Green-row detection ────────────────────────────────────────────────

    private boolean isGreenRow(Row row) {
        Cell cell = row.getCell(0, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return false;
        CellStyle style = cell.getCellStyle();
        if (style == null || style.getFillPattern() == FillPatternType.NO_FILL) return false;

        if (style instanceof XSSFCellStyle xStyle) {
            XSSFColor color = xStyle.getFillForegroundColorColor();
            if (color == null) return false;
            byte[] rgb = color.getRGB();
            if (rgb == null || rgb.length < 3) return false;
            int r = Byte.toUnsignedInt(rgb[0]);
            int g = Byte.toUnsignedInt(rgb[1]);
            int b = Byte.toUnsignedInt(rgb[2]);
            // Green-dominant: green channel > 100 and clearly dominant
            return g > 100 && g > r && g > b;
        }
        return false;
    }

    private LocalDateTime extractDateFromRow(Row row) {
        Cell cell = row.getCell(COL_SNO, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;

        // Numeric date cell (Excel date format)
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            try {
                return cell.getLocalDateTimeCellValue().toLocalDate().atStartOfDay();
            } catch (Exception ignored) {}
        }

        // String cell — try known date patterns
        if (cell.getCellType() == CellType.STRING) {
            String raw = cell.getStringCellValue().trim();
            for (DateTimeFormatter fmt : DATE_FORMATS) {
                try {
                    return LocalDate.parse(raw, fmt).atStartOfDay();
                } catch (DateTimeParseException ignored) {}
            }
            log.warn("Could not parse date from green row value: '{}'", raw);
        }

        return null;
    }

    // ── Cell readers ──────────────────────────────────────────────────────

    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default      -> null;
        };
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK) return false;
        }
        return true;
    }

    // ── Row DTO ───────────────────────────────────────────────────────────

    public static class ExcelCrqRow {
        private String crqNumber;
        private String created;      // Yes/No
        private String application;  // application / team
        private String country;
        private String type;         // Standard, Emergency, …
        private String description;
        private LocalDateTime lastUpdated; // inherited from the date-separator green row

        public String getCrqNumber()            { return crqNumber; }
        public void   setCrqNumber(String v)    { this.crqNumber = v; }
        public String getCreated()              { return created; }
        public void   setCreated(String v)      { this.created = v; }
        public String getApplication()          { return application; }
        public void   setApplication(String v)  { this.application = v; }
        public String getCountry()              { return country; }
        public void   setCountry(String v)      { this.country = v; }
        public String getType()                 { return type; }
        public void   setType(String v)         { this.type = v; }
        public String getDescription()          { return description; }
        public void   setDescription(String v)  { this.description = v; }
        public LocalDateTime getLastUpdated()           { return lastUpdated; }
        public void          setLastUpdated(LocalDateTime v) { this.lastUpdated = v; }
    }
}
