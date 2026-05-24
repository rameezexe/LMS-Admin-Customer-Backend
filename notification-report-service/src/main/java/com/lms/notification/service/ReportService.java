package com.lms.notification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.notification.dto.DashboardSummaryDTO;
import com.lms.notification.security.SecurityHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final SecurityHelper securityHelper;

    public ReportService(RestTemplate restTemplate, ObjectMapper objectMapper, SecurityHelper securityHelper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.securityHelper = securityHelper;
    }

    private HttpHeaders getAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String token = securityHelper.extractTokenFromContext();
        if (token != null) {
            headers.set("Authorization", "Bearer " + token);
        }
        return headers;
    }

    @Cacheable("reports")
    public DashboardSummaryDTO getAdminDashboardSummary() {
        long totalBooks = 0;
        long totalMembers = 0;
        long activeMembers = 0;
        long totalActiveBorrows = 0;
        long totalOverdue = 0;
        double totalFinesUnpaid = 0.0;

        HttpEntity<String> entity = new HttpEntity<>(getAuthHeaders());

        try {
            ResponseEntity<String> catalogResponse = restTemplate.exchange(
                    "http://catalog-service/api/admin/books", HttpMethod.GET, entity, String.class);
            JsonNode catalogNode = objectMapper.readTree(catalogResponse.getBody());
            totalBooks = catalogNode.path("data").path("totalElements").asLong(0);
        } catch (Exception e) {
            log.error("Failed to fetch from catalog-service", e);
        }

        try {
            ResponseEntity<String> memberResponse = restTemplate.exchange(
                    "http://member-service/api/admin/members/stats", HttpMethod.GET, entity, String.class);
            JsonNode memberNode = objectMapper.readTree(memberResponse.getBody());
            totalMembers = memberNode.path("data").path("totalMembers").asLong(0);
            activeMembers = memberNode.path("data").path("activeMembers").asLong(0);
        } catch (Exception e) {
            log.error("Failed to fetch from member-service", e);
        }

        try {
            ResponseEntity<String> borrowResponse = restTemplate.exchange(
                    "http://borrowing-service/api/admin/borrows/stats", HttpMethod.GET, entity, String.class);
            JsonNode borrowNode = objectMapper.readTree(borrowResponse.getBody());
            totalActiveBorrows = borrowNode.path("data").path("totalActive").asLong(0);
            totalOverdue = borrowNode.path("data").path("totalOverdue").asLong(0);
            totalFinesUnpaid = borrowNode.path("data").path("totalFinesUnpaid").asDouble(0.0);
        } catch (Exception e) {
            log.error("Failed to fetch from borrowing-service", e);
        }

        return DashboardSummaryDTO.builder()
                .totalBooks(totalBooks)
                .totalMembers(totalMembers)
                .activeMembers(activeMembers)
                .totalActiveBorrows(totalActiveBorrows)
                .totalOverdue(totalOverdue)
                .totalFinesUnpaid(totalFinesUnpaid)
                .build();
    }

    @Cacheable("reports")
    public Object getMostBorrowedBooks() {
        HttpEntity<String> entity = new HttpEntity<>(getAuthHeaders());
        try {
            // In a real scenario, borrowing-service would have a specific endpoint for this
            // We'll just fetch all borrows and aggregate, or just call a stats endpoint.
            // For now, let's just forward the request to borrowing service stats.
            ResponseEntity<Object> response = restTemplate.exchange(
                    "http://borrowing-service/api/admin/borrows/stats", HttpMethod.GET, entity, Object.class);
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Could not reach borrowing-service: " + e.getMessage());
        }
    }

    @Cacheable("reports")
    public Object getOverdueSummary() {
        HttpEntity<String> entity = new HttpEntity<>(getAuthHeaders());
        try {
            ResponseEntity<Object> response = restTemplate.exchange(
                    "http://borrowing-service/api/admin/borrows/overdue", HttpMethod.GET, entity, Object.class);
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Could not reach borrowing-service: " + e.getMessage());
        }
    }

    @Cacheable("reports")
    public Object getMonthlyBorrowStats(Integer month, Integer year) {
        HttpEntity<String> entity = new HttpEntity<>(getAuthHeaders());
        try {
            String url = "http://borrowing-service/api/admin/borrows";
            ResponseEntity<Object> response = restTemplate.exchange(url, HttpMethod.GET, entity, Object.class);
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Could not reach borrowing-service: " + e.getMessage());
        }
    }

    @Cacheable("reports")
    public Object getMonthlyRevenue(Integer month, Integer year) {
        HttpEntity<String> entity = new HttpEntity<>(getAuthHeaders());
        try {
            String url = "http://payment-service/api/admin/payments/revenue?month=" + (month != null ? month : "") + "&year=" + (year != null ? year : "");
            ResponseEntity<Object> response = restTemplate.exchange(url, HttpMethod.GET, entity, Object.class);
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Could not reach payment-service: " + e.getMessage());
        }
    }

    @Cacheable("reports")
    public Object getCategoryAvailability() {
        HttpEntity<String> entity = new HttpEntity<>(getAuthHeaders());
        try {
            ResponseEntity<Object> response = restTemplate.exchange(
                    "http://catalog-service/api/user/books/categories", HttpMethod.GET, entity, Object.class);
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Could not reach catalog-service: " + e.getMessage());
        }
    }
}
