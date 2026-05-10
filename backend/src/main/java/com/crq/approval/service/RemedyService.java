package com.crq.approval.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Map;

@Slf4j
@Service
@Profile("!mock")
public class RemedyService implements RemedyPort {

    @Value("${remedy.base-url}")
    private String baseUrl;

    @Value("${remedy.username}")
    private String username;

    @Value("${remedy.password}")
    private String password;

    @Value("${remedy.approved-status}")
    private String approvedStatus;

    private final RestTemplate restTemplate;

    public RemedyService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Fetches the status of a CRQ from the Remedy API.
     * Remedy REST API: GET /api/arsys/v1/entry/CHG:ChangeInterface_Create/{crqNumber}
     *
     * Returns the status string, or null if the CRQ is not found / error.
     */
    public String getCrqStatus(String crqNumber) {
        String url = baseUrl + "/arsys/v1/entry/CHG:ChangeInterface_Create/"
                + crqNumber + "?fields=values(Status)";
        try {
            HttpHeaders headers = buildAuthHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<?, ?> values = (Map<?, ?>) response.getBody().get("values");
                if (values != null) {
                    Object status = values.get("Status");
                    String statusStr = status != null ? status.toString() : null;
                    log.info("CRQ {} status from Remedy: {}", crqNumber, statusStr);
                    return statusStr;
                }
            }
        } catch (Exception e) {
            log.error("Error fetching Remedy status for CRQ {}: {}", crqNumber, e.getMessage());
        }
        return null;
    }

    /**
     * Returns true if the given status means the CRQ is approved.
     */
    public boolean isApproved(String status) {
        return approvedStatus.equalsIgnoreCase(status);
    }

    private HttpHeaders buildAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        // Remedy supports Basic Auth; some versions use token auth — adjust as needed
        String credentials = username + ":" + password;
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes());
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + encoded);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/json");
        return headers;
    }
}
