package com.crq.approval.service;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.OffsetDateTime;

@Slf4j
@Service
@Profile("!mock")
public class OneDriveService implements OneDrivePort {

    @Value("${onedrive.tenant-id}")
    private String tenantId;

    @Value("${onedrive.client-id}")
    private String clientId;

    @Value("${onedrive.client-secret}")
    private String clientSecret;

    @Value("${onedrive.file-path}")
    private String filePath;

    @Value("${onedrive.drive-user}")
    private String driveUser;

    private GraphServiceClient buildGraphClient() {
        ClientSecretCredential credential = new ClientSecretCredentialBuilder()
                .tenantId(tenantId)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build();

        String[] scopes = {"https://graph.microsoft.com/.default"};
        return new GraphServiceClient(credential, scopes);
    }

    /**
     * Resolves the OneDrive drive ID for the configured user.
     * In Graph SDK v6, users().byUserId().drive() only supports get().
     * Full item access requires drives().byDriveId() which has the complete API.
     */
    private String resolveDriveId(GraphServiceClient client) {
        com.microsoft.graph.models.Drive drive;
        if ("me".equalsIgnoreCase(driveUser)) {
            drive = client.me().drive().get();
        } else {
            drive = client.users().byUserId(driveUser).drive().get();
        }
        if (drive == null || drive.getId() == null) {
            throw new RuntimeException("Could not resolve OneDrive drive ID for user: " + driveUser);
        }
        return drive.getId();
    }

    /**
     * Downloads the Excel file from OneDrive and returns it as an InputStream.
     * Uses Microsoft Graph API with app-only auth (client credentials flow).
     *
     * Graph SDK v6: resolve drive ID first, then use drives().byDriveId().items()
     * with the "root:/path/to/file:" item ID notation.
     */
    public InputStream downloadExcelFile() {
        log.info("Downloading Excel file from OneDrive: {}", filePath);
        try {
            GraphServiceClient client = buildGraphClient();
            String driveId = resolveDriveId(client);
            // "root:/path/to/file.xlsx:" is Graph API's path-based item ID
            String itemId = "root:" + filePath + ":";
            InputStream content = client.drives().byDriveId(driveId)
                    .items().byDriveItemId(itemId)
                    .content()
                    .get();
            log.info("Successfully downloaded Excel file from OneDrive (driveId={})", driveId);
            return content;
        } catch (Exception e) {
            log.error("Failed to download Excel file from OneDrive: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to download Excel from OneDrive: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the last modified time of the Excel file on OneDrive.
     */
    public OffsetDateTime getFileLastModifiedTime() {
        try {
            GraphServiceClient client = buildGraphClient();
            String driveId = resolveDriveId(client);
            String itemId = "root:" + filePath + ":";
            DriveItem item = client.drives().byDriveId(driveId)
                    .items().byDriveItemId(itemId)
                    .get();
            return item != null && item.getLastModifiedDateTime() != null
                    ? item.getLastModifiedDateTime()
                    : null;
        } catch (Exception e) {
            log.warn("Could not get file last modified time: {}", e.getMessage());
            return null;
        }
    }
}
