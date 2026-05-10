package com.crq.approval.service;

import java.io.InputStream;
import java.time.OffsetDateTime;

public interface OneDrivePort {
    InputStream downloadExcelFile();
    OffsetDateTime getFileLastModifiedTime();
}
