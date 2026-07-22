package com.lms.config;

import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FlywayMigrationConventionTest {

    private static final Pattern MIGRATION_NAME =
            Pattern.compile("^V([0-9]+)__([a-z0-9]+(?:_[a-z0-9]+)*)\\.sql$");
    private static final DateTimeFormatter TIMESTAMP_VERSION =
            DateTimeFormatter.ofPattern("uuuuMMddHHmmss").withResolverStyle(ResolverStyle.STRICT);
    private static final long LAST_LEGACY_VERSION = 14L;

    @Test
    void migrationVersionsAreUniqueAndFollowTheTeamConvention() throws Exception {
        List<String> violations = new ArrayList<>();
        Map<String, List<String>> filesByVersion = new HashMap<>();

        for (Path migration : migrationFiles()) {
            String fileName = migration.getFileName().toString();
            Matcher matcher = MIGRATION_NAME.matcher(fileName);
            if (!matcher.matches()) {
                violations.add(fileName + ": expected V<version>__<snake_case_description>.sql");
                continue;
            }

            String version = matcher.group(1);
            filesByVersion.computeIfAbsent(version, ignored -> new ArrayList<>()).add(fileName);
            validateVersion(fileName, version, violations);
        }

        filesByVersion.forEach((version, files) -> {
            if (files.size() > 1) {
                violations.add("duplicate Flyway version " + version + ": " + String.join(", ", files));
            }
        });

        assertTrue(violations.isEmpty(), () -> "Invalid Flyway migrations:\n- " + String.join("\n- ", violations));
    }

    private static void validateVersion(String fileName, String version, List<String> violations) {
        long numericVersion;
        try {
            numericVersion = Long.parseLong(version);
        } catch (NumberFormatException exception) {
            violations.add(fileName + ": version is not a supported integer");
            return;
        }

        if (numericVersion <= LAST_LEGACY_VERSION) {
            return;
        }
        if (version.length() != 14) {
            violations.add(fileName + ": migrations after V14 must use VyyyyMMddHHmmss");
            return;
        }

        try {
            LocalDateTime.parse(version, TIMESTAMP_VERSION);
        } catch (DateTimeParseException exception) {
            violations.add(fileName + ": timestamp version is not a valid date and time");
        }
    }

    private static List<Path> migrationFiles() throws Exception {
        Path directory = migrationDirectory();
        try (Stream<Path> files = Files.list(directory)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".sql"))
                    .sorted()
                    .toList();
        }
    }

    private static Path migrationDirectory() throws URISyntaxException {
        return Path.of(Objects.requireNonNull(
                FlywayMigrationConventionTest.class.getClassLoader().getResource("db/migration"),
                "db/migration is missing from the test classpath"
        ).toURI());
    }
}
