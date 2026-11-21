package de.aivot.prosuna.backend.process.utils;

import de.aivot.prosuna.backend.utils.ApplicationTimeZone;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;
import java.util.List;
import java.util.stream.Stream;
import java.io.IOException;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;

import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.*;
import static org.junit.jupiter.api.Assertions.*;

class ExecutionSummaryMarkdownTest {
    record FormatCases(List<FormatCase> cases) {}
    record FormatCase(String name, String mode, String title, String value, String expectedMarkdown,
                      List<String> terms, List<String> contains) {}

    static Stream<FormatCase> sharedSummaryCases() throws IOException {
        try (var input = new ClassPathResource("markdown/process-execution-summary-cases.json").getInputStream()) {
            return JsonMapper.builder().build().readValue(input, FormatCases.class).cases().stream();
        }
    }

    @ParameterizedTest
    @MethodSource("sharedSummaryCases")
    void generatesTheSharedFrontendFormat(FormatCase example) {
        assertEquals(example.expectedMarkdown(), "detail".equals(example.mode())
                ? detail(example.title(), example.value()) : section(example.title(), example.value()), example.name());
    }

    @ParameterizedTest
    @CsvSource({"' Ada Beispiel ', Ada Beispiel", "'Name *A*', Name *A*", ", staff-id", "'', staff-id", "'   ', staff-id"})
    void resolvesTheSameNameForPlainTextEventsAndMarkdown(String name, String expected) {
        assertEquals(expected, ProcessHistoryLabels.nameOrId("staff-id", name));
        assertEquals("„" + expected + "“", ProcessHistoryLabels.quotedUser(new UserEntity().setId("staff-id").setFullName(name)));
        assertEquals("„" + text(expected) + "“", user("staff-id", name));
        assertEquals("das System", user(null, null));
    }

    @Test
    void escapesInsertedMarkdownHtmlNewlinesAndTableCells() {
        assertEquals("\\[x\\]\\(javascript:alert\\(1\\)\\) \\<b\\>\\& \\| \\*bold\\*",
                text("[x](javascript:alert(1))\n<b>& | *bold*"));
        assertEquals("\n\nVermerk\n:   **Freigegeben**", section("Vermerk", "**Freigegeben**"));
        assertEquals("", detail("Optional", null));
        assertEquals("„Name\\]“", user("a/b c", "Name]"));
        assertEquals("[Dokument ansehen](/api/process-instance-attachments/a%2Fb/file/?download=false)", document("a/b", null));
    }

    @Test
    void keepsRichMarkdownInsideDefinitionValues() {
        assertEquals("\n\nVermerk\n:   Erster Absatz.\n    \n    - **Freigegeben**\n    - [Details](https://example.org)",
                section("Vermerk", "Erster Absatz.\r\n\r\n- **Freigegeben**\r\n- [Details](https://example.org)"));
        assertEquals("\n\nBetreff\n:   \\*Wichtig\\*", detail("Betreff", "*Wichtig*"));
        assertEquals("\n\n\\*Titel\\*\n:   Inhalt", section("*Titel*", "Inhalt"));
        assertEquals("", section("Vermerk", "  \n\t"));
        assertEquals("\n\nBeispiel\n:       eingerückter Code", section("Beispiel", "    eingerückter Code"));
    }

    @Test
    void usesBusinessTimezoneForInstantAndPersistedOffsetString() {
        var previousZone = ApplicationTimeZone.getZoneId();
        try {
            ApplicationTimeZone.configure(ZoneId.of("Europe/Berlin"));
            assertEquals("01.10.2026 um 12:00:00 Uhr", timestamp(Instant.parse("2026-10-01T10:00:00Z")));
            assertEquals("01.01.2026 um 11:00:00 Uhr", timestamp("2026-01-01T10:00:00Z"));
            assertEquals(timestamp("2026-01-01T11:00:00+01:00"), timestamp("2026-01-01T10:00:00Z"));
            assertEquals("", timestamp("unbekannt"));
        } finally {
            ApplicationTimeZone.configure(previousZone);
        }
    }

    @Test
    void removesCredentialsQueryAndFragmentFromUrls() {
        assertEquals("https://example\\.org:8443/path", safeUrl("https://user:password@example.org:8443/path?access_token=secret#secret"));
        assertEquals("", safeUrl("javascript:alert(1)"));
        assertEquals("", safeUrl("{{url}}"));
    }

    @Test
    void preservesReceiptsAcrossRuntimeReplacementWithoutResurrectingOtherState() {
        var previous = withMetadata(Map.of("oldForm", true), Map.of(SENT_AT, "2026-01-01T10:00:00Z", DELIVERY_CHANNEL, "E-Mail"));
        var result = preserveMetadata(previous, Map.of("payment", "123"));
        assertFalse(result.containsKey("oldForm"));
        assertEquals("123", result.get("payment"));
        assertEquals(previous.get(RUNTIME_KEY), result.get(RUNTIME_KEY));
        var resend = withMetadata(result, Map.of(SENT_AT, "2026-01-02T10:00:00Z"));
        assertEquals("2026-01-02T10:00:00Z", map(resend.get(RUNTIME_KEY)).get(SENT_AT));
        assertEquals("E-Mail", map(resend.get(RUNTIME_KEY)).get(DELIVERY_CHANNEL));
        assertEquals("2026-01-01T10:00:00Z", map(previous.get(RUNTIME_KEY)).get(SENT_AT));
    }
}
