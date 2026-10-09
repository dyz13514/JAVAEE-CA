package com.group5.cats.service;

import java.time.LocalDate;
import java.util.Locale;

/**
 * CSV primitives shared by the export endpoints.
 *
 * <p>Values are escaped using RFC 4180 rules (quote fields containing a comma,
 * quote or line break; double any embedded quote). Fields that would be read as
 * a formula by a spreadsheet application are prefixed with an apostrophe so an
 * exported report cannot execute anything when it is opened.
 *
 * <p>Dates are written as ISO-8601 ({@code yyyy-MM-dd}) and numbers with two
 * decimal places, matching the on-screen report.
 */
public final class CsvSupport {

    /** Byte order mark so spreadsheet applications detect the UTF-8 encoding. */
    public static final String UTF8_BOM = "\uFEFF";

    private static final String RECORD_SEPARATOR = "\r\n";
    private static final String FORMULA_PREFIXES = "=+-@\t\r";

    private CsvSupport() {
    }

    /** Escapes one field and neutralises spreadsheet formulas. */
    public static String escape(String value) {
        String guarded = guardAgainstFormula(value);
        boolean quoted = guarded.indexOf(',') >= 0
                || guarded.indexOf('"') >= 0
                || guarded.indexOf('\n') >= 0
                || guarded.indexOf('\r') >= 0;
        if (!quoted) {
            return guarded;
        }
        return '"' + guarded.replace("\"", "\"\"") + '"';
    }

    public static String guardAgainstFormula(String value) {
        if (value == null || value.isEmpty()) {
            return value == null ? "" : value;
        }
        return FORMULA_PREFIXES.indexOf(value.charAt(0)) >= 0 ? "'" + value : value;
    }

    /** Escapes and joins one record, terminated with a CRLF. */
    public static String row(String... cells) {
        StringBuilder record = new StringBuilder();
        for (int index = 0; index < cells.length; index++) {
            if (index > 0) {
                record.append(',');
            }
            record.append(escape(cells[index]));
        }
        return record.append(RECORD_SEPARATOR).toString();
    }

    public static String formatDate(LocalDate date) {
        return date == null ? "" : date.toString();
    }

    /** Plain two-decimal amount so spreadsheets read the column as a number. */
    public static String formatMoney(double amount) {
        return formatDecimal(amount);
    }

    public static String formatDecimal(double value) {
        if (!Double.isFinite(value)) {
            return "0.00";
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
