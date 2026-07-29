package com.lms.dto.response;

import com.lms.enums.UserStatus;

/**
 * Read-only librarian data exposed to the librarian directory.
 */
public record LibrarianListItemResponse(
        Integer staffId,
        Integer accountId,
        String username,
        String fullName,
        String email,
        String phone,
        String staffType,
        UserStatus status) {
}
