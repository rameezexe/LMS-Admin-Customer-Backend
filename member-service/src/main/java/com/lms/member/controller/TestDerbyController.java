package com.lms.member.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestDerbyController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/api/internal/test-db")
    public String testDb() {
        try {
            jdbcTemplate.execute("INSERT INTO members (name, email, membership_number, membership_type, status, join_date, phone, address, profile_photo_url, created_at, updated_at) " +
                    "VALUES ('Test Name', 'test@test.com', 'LIB-TEST', 'STANDARD', 'ACTIVE', CURRENT_DATE, '', '', '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
            return "Insert successful!";
        } catch (Exception e) {
            return "Insert failed: " + e.getMessage();
        }
    }
}
