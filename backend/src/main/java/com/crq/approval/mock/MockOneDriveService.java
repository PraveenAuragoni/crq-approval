package com.crq.approval.mock;

import com.crq.approval.service.OneDrivePort;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Slf4j
@Service
@Profile("mock")
public class MockOneDriveService implements OneDrivePort {

    private static final Object[][] MOCK_CRQS = {
        {"CRQ000123001", "Deploy Auth Service v2.1",        "Alice Johnson",  "Upgrade auth service to version 2.1"},
        {"CRQ000123002", "DB Schema Migration - Users",      "Bob Smith",      "Add new columns to users table"},
        {"CRQ000123003", "Update SSL Certificates",          "Carol White",    "Renew SSL certs expiring next month"},
        {"CRQ000123004", "Firewall Rule Update - API GW",    "David Lee",      "Allow port 8443 for API gateway"},
        {"CRQ000123005", "Patch Server OS - Web01",         "Eve Martinez",   "Apply July security patches"},
        {"CRQ000123006", "Deploy Payment Service v3.0",      "Frank Brown",    "Release new payment integration"},
        {"CRQ000123007", "Config Change - Redis Timeout",    "Grace Kim",      "Increase session timeout to 30 min"},
        {"CRQ000123008", "Upgrade Spring Boot 3.2 → 3.3",   "Henry Wilson",   "Framework upgrade for all services"},
        {"CRQ000123009", "DNS Record Update - CDN",          "Iris Taylor",    "Point cdn.example.com to new IP"},
        {"CRQ000123010", "Enable MFA - Admin Portal",        "Jack Davis",     "Enforce MFA for all admin accounts"},
    };

    @Override
    public InputStream downloadExcelFile() {
        log.info("[MOCK] Generating in-memory Excel with {} CRQ rows", MOCK_CRQS.length);
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("CRQs");

            // Header row
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("CRQ Number");
            header.createCell(1).setCellValue("Title");
            header.createCell(2).setCellValue("Assignee");
            header.createCell(3).setCellValue("Description");
            header.createCell(4).setCellValue("Last Updated");

            // Data rows — last 3 rows get "today after 17:30" timestamps for ad-hoc demo
            LocalDateTime scheduledTime = LocalDateTime.now().minusHours(2);
            LocalDateTime adhocTime    = LocalDateTime.now().minusMinutes(15);

            for (int i = 0; i < MOCK_CRQS.length; i++) {
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue((String) MOCK_CRQS[i][0]);
                row.createCell(1).setCellValue((String) MOCK_CRQS[i][1]);
                row.createCell(2).setCellValue((String) MOCK_CRQS[i][2]);
                row.createCell(3).setCellValue((String) MOCK_CRQS[i][3]);
                // last 3 CRQs simulate "updated after 5:30 PM" for ad-hoc demo
                LocalDateTime ts = (i >= MOCK_CRQS.length - 3) ? adhocTime : scheduledTime;
                row.createCell(4).setCellValue(ts);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Mock Excel generation failed", e);
        }
    }

    @Override
    public OffsetDateTime getFileLastModifiedTime() {
        return OffsetDateTime.now();
    }
}
