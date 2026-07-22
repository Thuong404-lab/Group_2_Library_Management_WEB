package com.lms.template;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AdminLayoutFeedbackTemplateTest {

    @Test
    void adminLayoutProvidesTheSharedFeedbackSystem() throws IOException {
        String html = Files.readString(Path.of("src/main/resources/templates/layout/admin_base.html"));

        assertThat(html).contains("id=" + (char) 34 + "appFeedbackRegion" + (char) 34);
        assertThat(html).contains("id=" + (char) 34 + "customToast" + (char) 34);
        assertThat(html).contains("@{/js/staff-feedback-toast.js}");
    }
}
