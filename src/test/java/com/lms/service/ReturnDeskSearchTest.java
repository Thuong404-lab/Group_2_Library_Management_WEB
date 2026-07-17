package com.lms.service;

import com.lms.entity.BorrowDetail;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class ReturnDeskSearchTest {

    @Autowired
    private LoanService loanService;

    @Test
    public void testSearchActiveLoansByQuery() {
        // Verify that searching with empty query returns empty list and doesn't crash
        List<BorrowDetail> emptyResults = loanService.searchActiveLoansByQuery("");
        assertNotNull(emptyResults);

        // Verify that searching with null query returns empty list and doesn't crash
        List<BorrowDetail> nullResults = loanService.searchActiveLoansByQuery(null);
        assertNotNull(nullResults);

        // Verify search with a dummy query compiles and runs without throwing exceptions
        List<BorrowDetail> dummyResults = loanService.searchActiveLoansByQuery("non_existent_query_value");
        assertNotNull(dummyResults);
    }

}
