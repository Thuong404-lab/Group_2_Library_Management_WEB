package com.lms.domain;

import com.lms.enums.AcquisitionRequestStatus;

import java.util.List;

public final class AcquisitionRequestPolicy {
    public static final int TITLE_MIN_LENGTH = 2;
    public static final int TITLE_MAX_LENGTH = 255;
    public static final int AUTHOR_MIN_LENGTH = 2;
    public static final int AUTHOR_MAX_LENGTH = 255;
    public static final int REASON_MIN_LENGTH = 10;
    public static final int REASON_MAX_LENGTH = 1000;
    public static final int DECISION_NOTE_MIN_LENGTH = 5;
    public static final int DECISION_NOTE_MAX_LENGTH = 500;
    public static final int PUBLISHER_MAX_LENGTH = 255;
    public static final int ISBN_INPUT_MAX_LENGTH = 20;
    public static final int REFERENCE_URL_MAX_LENGTH = 500;
    public static final int MIN_PUBLICATION_YEAR = 1000;
    public static final int SEARCH_KEYWORD_MAX_LENGTH = 100;
    public static final int PAGE_SIZE = 10;
    public static final List<AcquisitionRequestStatus> ACTIVE_STATUSES = List.of(
            AcquisitionRequestStatus.PENDING, AcquisitionRequestStatus.APPROVED);

    private AcquisitionRequestPolicy() {
    }
}
