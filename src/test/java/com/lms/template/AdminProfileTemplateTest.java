package com.lms.template;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AdminProfileTemplateTest {

    private static final Path TEMPLATE = Path.of(
            "src/main/resources/templates/admin/profile.html");

    @Test
    void profileJavaScriptIsDeclaredInTheLayoutScriptsFragment() throws IOException {
        String html = Files.readString(TEMPLATE);
        String fragment = "layout:fragment=" + (char) 34 + "scripts" + (char) 34;

        assertThat(html.indexOf(fragment)).isGreaterThanOrEqualTo(0);
        assertThat(html.indexOf("const fileInput = document.getElementById('avatarUploadAdmin')"))
                .isGreaterThan(html.indexOf(fragment));
    }

    @Test
    void readonlyEmailIsDisplayedButNeverSubmitted() throws IOException {
        String html = Files.readString(TEMPLATE);
        int idPosition = html.indexOf("id=" + (char) 34 + "adminEmail" + (char) 34);
        assertThat(idPosition).isGreaterThanOrEqualTo(0);

        String input = html.substring(html.lastIndexOf("<input", idPosition), html.indexOf('>', idPosition) + 1);
        assertThat(input).contains("disabled");
        assertThat(input).doesNotContain("name=" + (char) 34 + "email" + (char) 34);
    }

    @Test
    void avatarHasStrictClientValidationAndBrokenImageFallback() throws IOException {
        String html = Files.readString(TEMPLATE);

        assertThat(html).contains("accept=" + (char) 34 + "image/jpeg,image/png,image/webp,image/gif" + (char) 34);
        assertThat(html).contains("avatarPreview.addEventListener('error', showAvatarFallback)");
        assertThat(html).contains("<span class=" + (char) 34 + "sr-only" + (char) 34);
    }

    @Test
    void templateDoesNotUseMissingBootstrapFiveSpacingUtilities() throws IOException {
        String html = Files.readString(TEMPLATE);

        assertThat(html).doesNotContain("fw-bold", " g-3", " g-4", " me-2", " text-end", "btn-close");
    }

    @Test
    void profileUsesCanonicalHeroCardsAndToastFeedback() throws IOException {
        String html = Files.readString(TEMPLATE);

        assertThat(html).contains("class=" + (char) 34 + "admin-profile-hero" + (char) 34);
        assertThat(html).contains("class=" + (char) 34 + "admin-profile-card profile-identity-card" + (char) 34);
        assertThat(html).contains("data-app-feedback=" + (char) 34 + "success" + (char) 34);
        assertThat(html).contains("data-app-feedback=" + (char) 34 + "error" + (char) 34);
        assertThat(html).doesNotContain("alert alert-success", "alert alert-danger");
    }

    @Test
    void profilePreservesFormAndModalContracts() throws IOException {
        String html = Files.readString(TEMPLATE);

        assertThat(html).contains("id=" + (char) 34 + "profileForm" + (char) 34);
        assertThat(html).contains("th:action=" + (char) 34 + "@{/admin/profile/update}" + (char) 34);
        assertThat(html).contains("id=" + (char) 34 + "changePasswordModal" + (char) 34);
        assertThat(html).contains("data-bs-target=" + (char) 34 + "#changePasswordModal" + (char) 34);
        assertThat(html).contains("id=" + (char) 34 + "changePasswordForm" + (char) 34);
    }
}
