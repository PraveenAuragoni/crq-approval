package com.crq.approval.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ExcelService {

    /**
     * Parses the Excel file and returns a list of CRQ records.
     *
     * Expected Excel columns (row 1 = headers):
     *   A: CRQ Number
     *   B: Title
     *   C: Assignee
     *   D: Description
     *   E: Last Updated (date/time cell)
     */
    public List<ExcelCrqRow> parseExcel(InputStream inputStream) {
        List<ExcelCrqRow> rows = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            boolean firstRow = true;

            for (Row row : sheet) {
                if (firstRow) {
                    firstRow = false; // skip header
                    continue;
                }
                if (isRowEmpty(row)) continue;

                ExcelCrqRow crqRow = new ExcelCrqRow();
                crqRow.setCrqNumber(getCellString(row, 0));
                crqRow.setTitle(getCellString(row, 1));
                crqRow.setAssignee(getCellString(row, 2));
                crqRow.setDescription(getCellString(row, 3));
                crqRow.setLastUpdated(getCellDateTime(row, 4));

                if (crqRow.getCrqNumber() != null && !crqRow.getCrqNumber().isBlank()) {
                    rows.add(crqRow);
                }
            }
            log.info("Parsed {} CRQ rows from Excel", rows.size());
        } catch (Exception e) {
            log.error("Failed to parse Excel file: {}", e.getMessage(), e);
            throw new RuntimeException("Excel parsing failed: " + e.getMessage(), e);
        }

        return rows;
    }

    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    private LocalDateTime getCellDateTime(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue();
            }
        } catch (Exception e) {
            log.warn("Could not parse date cell at col {}: {}", col, e.getMessage());
        }
        return null;
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK) return false;
        }
        return true;
    }

    public static class ExcelCrqRow {
        private String crqNumber;
        private String title;
        private String assignee;
        private String description;
        private LocalDateTime lastUpdated;

        public String getCrqNumber() { return crqNumber; }
        public void setCrqNumber(String crqNumber) { this.crqNumber = crqNumber; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getAssignee() { return assignee; }
        public void setAssignee(String assignee) { this.assignee = assignee; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    }
}
