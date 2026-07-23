package com.lms.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class SystemLogRepositoryIntegrationTest {
    @Autowired
    private SystemLogRepository systemLogRepository;

    @Test
    void insertsAuditWithoutRequestingGeneratedIdentityResultSet() {
        int inserted = systemLogRepository.insertAudit(
                null,
                "LOGIN",
                "127.0.0.1",
                "integration-test",
                "Audit writer integration test");

        assertThat(inserted).isEqualTo(1);
    }
}
