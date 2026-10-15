package com.routive.healthhabit.drug.importer;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import org.springframework.web.util.HtmlUtils;

public record MfdsDrugData(String externalCode, String name, String manufacturer, String efficacy,
        String usage, String precautions, String interactions, String sideEffects, String storage,
        String imageUrl, Instant sourceUpdatedAt) {
    public static final String SOURCE = "식품의약품안전처 e약은요";

    public com.routive.healthhabit.drug.domain.MedicationProductData toProductData() {
        return new com.routive.healthhabit.drug.domain.MedicationProductData(externalCode, name, "OTC",
                manufacturer, efficacy, usage, precautions, interactions, sideEffects, storage,
                imageUrl, SOURCE, sourceUpdatedAt);
    }

    public static MfdsDrugData from(MfdsDrugRecord row) {
        if (row.itemSeq() == null || !row.itemSeq().matches("[0-9]{1,32}")) throw new IllegalArgumentException();
        String name = plain(row.itemName());
        if (name == null || name.length() > 255) throw new IllegalArgumentException();
        String manufacturer = shortText(row.entpName(), 255);
        String image = shortText(row.imageUrl(), 1024);
        if (image != null) {
            URI uri = URI.create(image);
            if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null) throw new IllegalArgumentException();
        }
        String warning = plain(row.warning()), precautions = plain(row.precautions());
        String combined = warning == null ? precautions : precautions == null ? warning : warning + "\n\n" + precautions;
        return new MfdsDrugData("MFDS-EDRUG:" + row.itemSeq(), name, manufacturer,
                plain(row.efficacy()), plain(row.usage()), bounded(combined), plain(row.interactions()),
                plain(row.sideEffects()), plain(row.storage()), image, sourceDate(row.updateDate()));
    }

    private static String plain(String value) {
        if (value == null) return null;
        String decoded = HtmlUtils.htmlUnescape(value);
        StringBuilder output = new StringBuilder(decoded.length());
        for (int index = 0; index < decoded.length();) {
            char current = decoded.charAt(index);
            if (current == '<' && index + 1 < decoded.length()) {
                char next = decoded.charAt(index + 1);
                if (Character.isLetter(next) || next == '/' || next == '!') {
                    int end = decoded.indexOf('>', index + 1);
                    if (end < 0) {
                        output.append(decoded, index, decoded.length());
                        break;
                    }
                    String tag = decoded.substring(index + 1, end).strip().toLowerCase(java.util.Locale.ROOT);
                    if (tag.equals("br") || tag.equals("br/") || tag.startsWith("br ") || tag.equals("/p")) output.append('\n');
                    index = end + 1;
                    continue;
                }
            }
            output.append(current);
            index++;
        }
        String text = output.toString().strip();
        return text.isEmpty() ? null : bounded(text);
    }

    private static String bounded(String value) {
        if (value != null && value.getBytes(StandardCharsets.UTF_8).length > 65535) throw new IllegalArgumentException();
        return value;
    }

    private static String shortText(String value, int max) {
        String text = plain(value);
        if (text != null && text.length() > max) throw new IllegalArgumentException();
        return text;
    }

    private static Instant sourceDate(String value) {
        if (value == null || value.isBlank()) return null;
        String date = value.strip();
        try {
            LocalDate parsed = date.matches("[0-9]{8}") ? LocalDate.parse(date, DateTimeFormatter.BASIC_ISO_DATE)
                    : LocalDate.parse(date.length() >= 10 ? date.substring(0, 10) : date);
            return parsed.atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant();
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("Invalid source date");
        }
    }
}
