package com.lms.template;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LibrarianProfileTemplateTest {

    private static final Path TEMPLATE = Path.of(
            "src/main/resources/templates/librarian/profile.html");

    @Test
    void profileJavaScriptIsDeclaredInTheLayoutScriptsFragment() throws IOException {
        String html = Files.readString(TEMPLATE);
        String scriptsFragment = "layout:fragment=" + (char) 34 + "scripts" + (char) 34;
        int fragmentStart = html.indexOf(scriptsFragment);
        int profileScript = html.indexOf("const fileInput = document.getElementById('avatarUploadLibrarian')");

        assertThat(fragmentStart).isGreaterThanOrEqualTo(0);
        assertThat(profileScript).isGreaterThan(fragmentStart);
    }

    @Test
    void readonlyWorkEmailIsNotSubmittedByTheProfileForm() throws IOException {
        String html = Files.readString(TEMPLATE);
        String emailId = "id=" + (char) 34 + "librarianEmail" + (char) 34;
        int idPosition = html.indexOf(emailId);
        assertThat(idPosition).isGreaterThanOrEqualTo(0);

        int inputStart = html.lastIndexOf("<input", idPosition);
        int inputEnd = html.indexOf('>', idPosition);
        String emailInput = html.substring(inputStart, inputEnd + 1);

        String emailName = "name=" + (char) 34 + "email" + (char) 34;
        assertThat(emailInput).doesNotContain(emailName);
    }

    @Test
    void cameraAssistiveTextUsesTheBootstrapFourScreenReaderUtility() throws IOException {
        String html = Files.readString(TEMPLATE);

        assertThat(html).contains("<span class=" + (char) 34 + "sr-only" + (char) 34);
        assertThat(html).doesNotContain("<span class=" + (char) 34 + "visually-hidden" + (char) 34);
    }
}
