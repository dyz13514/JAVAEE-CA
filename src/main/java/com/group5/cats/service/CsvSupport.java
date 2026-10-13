package com.group5.cats.service;

import java.time.LocalDate;
import java.util.Locale;

public final class CsvSupport {

    public static final String UTF8_BOM = "\uFEFF";

    private static final String RECORD_SEPARATOR = "\r\n";
    private static final String FORMULA_PREFIXES = "=+-@\t\r";

    private CsvSupport() {
    }

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
