package com.ocbs.event;

import com.ocbs.security.SecurityUtils;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

/** Sends urgent assessment alerts to Clinical over authenticated internal HTTP, not the shared event broker. */
@Service
public class PsychometricEventProducer {
    private static final Logger LOG = LoggerFactory.getLogger(PsychometricEventProducer.class);
    private final RestTemplate restTemplate;

    @Value("${application.clinical.base-url:http://localhost:8083}")
    private String clinicalBaseUrl;

    public PsychometricEventProducer() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3000);
        requestFactory.setReadTimeout(3000);
        this.restTemplate = new RestTemplate(requestFactory);
    }

    public void publishSevereScoreAlert(PsychometricAlertEvent event) {
        String jwt = SecurityUtils.getCurrentUserJWT()
            .orElseThrow(() -> new IllegalStateException("Cannot send a clinical alert without the authenticated user JWT"));
        String endpoint = clinicalBaseUrl.replaceAll("/$", "") + "/api/clinical/psychometrics-alert";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwt);
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Only identifiers and severity cross the service boundary; scores, interpretations, names and phone numbers stay private.
        Map<String, Object> alertPayload = Map.of(
            "submissionId", event.getSubmissionId(),
            "patientId", event.getPatientId(),
            "severityLevel", event.getSeverityLevel()
        );
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(alertPayload, headers);

        // Retry only transient network/5xx failures; 4xx indicates a contract or authorization problem.
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                ResponseEntity<Map> response = restTemplate.exchange(endpoint, HttpMethod.POST, request, Map.class);
                if (!response.getStatusCode().is2xxSuccessful()) {
                    throw new IllegalStateException("Clinical service did not acknowledge the alert");
                }
                LOG.info("Clinical service acknowledged high-risk alert for submissionId={} (attempt={})",
                    event.getSubmissionId(), attempt);
                return;
            } catch (HttpClientErrorException ex) {
                throw new IllegalStateException("Clinical service rejected high-risk alert (HTTP " + ex.getStatusCode() + ")", ex);
            } catch (HttpServerErrorException | ResourceAccessException ex) {
                if (attempt == 3) {
                    throw new IllegalStateException("Clinical service did not accept high-risk alert after retries", ex);
                }
                try {
                    Thread.sleep(250L << (attempt - 1));
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while retrying clinical alert delivery", interrupted);
                }
            }
        }
    }
}
