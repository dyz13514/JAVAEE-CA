package com.group5.cats.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CsvSupportTests {

    @Test
    void plainValuesAreWrittenWithoutQuotes() {
        assertEquals("Ben Tan", CsvSupport.escape("Ben Tan"));
        assertEquals("1250.00", CsvSupport.escape("1250.00"));
    }

    @Test
    void valuesContainingSeparatorsQuotesOrLineBreaksAreQuoted() {
        assertEquals("\"Cloud, Advanced\"", CsvSupport.escape("Cloud, Advanced"));
        assertEquals("\"He said \"\"hello\"\"\"", CsvSupport.escape("He said \"hello\""));
        assertEquals("\"line one\nline two\"", CsvSupport.escape("line one\nline two"));
        assertEquals("\"line one\r\nline two\"", CsvSupport.escape("line one\r\nline two"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"=1+1", "+1+1", "-1+1", "@SUM(A1)", "\tcmd", "\rcmd"})
    void formulaLikeValuesAreNeutralised(String value) {
        String escaped = CsvSupport.escape(value);
        assertTrue(escaped.startsWith("'") || escaped.startsWith("\"'"), escaped);
    }

    @Test
    void ordinaryTextIsNotTreatedAsAFormula() {
        assertEquals("Cloud Architecture Essentials", CsvSupport.escape("Cloud Architecture Essentials"));
        assertEquals("555.00", CsvSupport.escape("555.00"));
    }

    @Test
    void recordTerminatorIsCrlfAndFieldsAreJoinedInOrder() {
        assertEquals("a,b,c\r\n", CsvSupport.row("a", "b", "c"));
    }

    @Test
    void nullValueBecomesAnEmptyField() {
        assertEquals("", CsvSupport.escape(null));
        assertEquals(",\r\n", CsvSupport.row(null, null));
    }

    @Test
    void datesUseIsoFormatAndMissingDatesAreEmpty() {
        assertEquals("2026-03-01", CsvSupport.formatDate(LocalDate.of(2026, 3, 1)));
        assertEquals("", CsvSupport.formatDate(null));
    }

    @Test
    void amountsUseTwoDecimalsWithoutGroupingSoSpreadsheetsReadNumbers() {
        assertEquals("1250.00", CsvSupport.formatMoney(1250));
        assertEquals("0.50", CsvSupport.formatMoney(0.5));
        assertEquals("1250.57", CsvSupport.formatMoney(1250.567));
        assertEquals("0.00", CsvSupport.formatMoney(Double.NaN));
    }
}
