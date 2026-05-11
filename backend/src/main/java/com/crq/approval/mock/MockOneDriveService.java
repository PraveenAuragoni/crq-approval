package com.crq.approval.mock;

import com.crq.approval.service.OneDrivePort;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@Profile("mock")
public class MockOneDriveService implements OneDrivePort {

    // Columns: S.No | Created | Application | Country | CRQ Number | Type | (col6 unused) | Description
    private static final Object[][] MOCK_CRQS = {
        // { crqNumber, created, application, country, type, description }
        { "CRQ000123001", "Yes", "Auth Service",      "SG", "Standard", "Upgrade auth service to version 2.1" },
        { "CRQ000123002", "Yes", "DB Operations",     "MY", "Standard", "Add new columns to users table" },
        { "CRQ000123003", "Yes", "Infrastructure",    "SG", "Standard", "Renew SSL certificates expiring next month" },
        { "CRQ000123004", "Yes", "Network",           "ID", "Emergency","Allow port 8443 for API gateway" },
        { "CRQ000123005", "Yes", "Platform",          "MY", "Standard", "Apply July security patches to Web01" },
        { "CRQ000123006", "Yes", "Payment Service",   "SG", "Standard", "Release new payment integration v3.0" },
        { "CRQ000123007", "Yes", "Middleware",        "TH", "Standard", "Increase Redis session timeout to 30 min" },
        { "CRQ000123008", "Yes", "Platform",          "SG", "Standard", "Framework upgrade Spring Boot 3.2 to 3.3" },
        { "CRQ000123009", "Yes", "DNS / CDN",         "MY", "Standard", "Point cdn.example.com to new IP address" },
        { "CRQ000123010", "Yes", "Admin Portal",      "SG", "Standard", "Enforce MFA for all admin accounts" },
    };

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("M/d/yyyy");

    @Override
    public InputStream downloadExcelFile() {
        log.info("[MOCK] Generating in-memory Excel (new format with green date rows)");
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("CRQs");

            // ── Green date-row style ──────────────────────────────────────
            XSSFCellStyle greenStyle = (XSSFCellStyle) wb.createCellStyle();
            greenStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            // Standard Excel green: RGB(0, 176, 80)
            greenStyle.setFillForegroundColor(new XSSFColor(new byte[]{0, (byte) 176, 80}, null));
            Font greenFont = wb.createFont();
            greenFont.setBold(true);
            greenStyle.setFont(greenFont);

            // ── Header row (row 0) ────────────────────────────────────────
            Row header = sheet.createRow(0);
            String[] headers = { "S.No", "Created", "Application", "Country",
                                  "CRQ Number", "Type", "", "Description / Justification" };
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            int rowNum = 1;

            // ── Yesterday's date group (for ad-hoc demo) ──────────────────
            LocalDate yesterday = LocalDate.now().minusDays(1);
            rowNum = writeDataGroup(sheet, greenStyle, yesterday, rowNum,
                    MOCK_CRQS[0], MOCK_CRQS[1], MOCK_CRQS[2]);

            // ── Today's date group (used by scheduled job) ─────────────────
            LocalDate today = LocalDate.now();
            rowNum = writeDataGroup(sheet, greenStyle, today, rowNum,
                    MOCK_CRQS[3], MOCK_CRQS[4], MOCK_CRQS[5],
                    MOCK_CRQS[6], MOCK_CRQS[7], MOCK_CRQS[8], MOCK_CRQS[9]);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            log.info("[MOCK] Excel written: {} data rows across 2 date groups", MOCK_CRQS.length);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Mock Excel generation failed", e);
        }
    }

    private int writeDataGroup(Sheet sheet, CellStyle greenStyle,
                               LocalDate date, int startRow, Object[]... crqs) {
        // Green date-separator row
        Row dateRow = sheet.createRow(startRow++);
        Cell dateCell = dateRow.createCell(0);
        dateCell.setCellValue(date.format(DATE_FMT));
        dateCell.setCellStyle(greenStyle);

        // CRQ data rows
        int sno = 1;
        for (Object[] crq : crqs) {
            Row row = sheet.createRow(startRow++);
            row.createCell(0).setCellValue(sno++);           // S.No
            row.createCell(1).setCellValue((String) crq[1]); // Created
            row.createCell(2).setCellValue((String) crq[2]); // Application
            row.createCell(3).setCellValue((String) crq[3]); // Country
            row.createCell(4).setCellValue((String) crq[0]); // CRQ Number
            row.createCell(5).setCellValue((String) crq[4]); // Type
            // col 6 intentionally blank
            row.createCell(7).setCellValue((String) crq[5]); // Description
        }
        return startRow;
    }

    @Override
    public OffsetDateTime getFileLastModifiedTime() {
        return OffsetDateTime.now();
    }
}
