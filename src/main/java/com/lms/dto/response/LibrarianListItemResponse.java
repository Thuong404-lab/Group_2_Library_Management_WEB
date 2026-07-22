package com.lms.dto.response;

import com.lms.enums.UserStatus;

/**
 * Read-only librarian data exposed to the librarian directory.
 * Contact details are deliberately excluded because librarians are not
 * authorized to view another staff member's email address or phone number.
 */
public record LibrarianListItemResponse(
        Integer staffId,
        Integer accountId,
        String username,
        String fullName,
        String staffType,
        UserStatus status) {
}
