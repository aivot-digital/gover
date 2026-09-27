package de.aivot.prosuna.backend.process.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class CrockfordCaseNumberTest {
    @Test
    void generatesTwelveCanonicalCharactersInThreeGroups() {
        for (int i = 0; i < 100; i++) {
            assertTrue(CrockfordCaseNumber.generate().matches("[0-9A-HJKMNP-TV-Z]{4}(-[0-9A-HJKMNP-TV-Z]{4}){2}"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"7K0M-9X1Q-0042", "7k0m9x1q0042", "7kom-9xlq-0042", "7KOM 9XIQ 0042", "7K0M\u00a09X1Q\u00a00042"})
    void acceptsEquivalentFullInputs(String value) {
        assertEquals("7K0M9X1Q0042", CrockfordCaseNumber.normalizeSearch(value));
        assertEquals("7K0M-9X1Q-0042", CrockfordCaseNumber.format(CrockfordCaseNumber.normalizeSearch(value)));
    }

    @Test
    void acceptsPartialSearchFromFourCharacters() {
        assertEquals("9X1Q", CrockfordCaseNumber.normalizeSearch("9xlq"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "---", "abc", "abcdefghijklmnop", "ABCU", "AB%_", "AB.C", "ÄBCD"})
    void rejectsOtherSearchTerms(String value) {
        assertNull(CrockfordCaseNumber.normalizeSearch(value));
    }
}
