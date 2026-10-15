package com.routive.healthhabit.drug.importer;

import java.util.Map;

public record MfdsDrugRecord(String itemSeq, String itemName, String entpName,
        String efficacy, String usage, String warning, String precautions, String interactions,
        String sideEffects, String storage, String imageUrl, String updateDate) {

    static MfdsDrugRecord from(Map<?, ?> item) {
        return new MfdsDrugRecord(text(item, "itemSeq"), text(item, "itemName"), text(item, "entpName"),
                text(item, "efcyQesitm"), text(item, "useMethodQesitm"), text(item, "atpnWarnQesitm"),
                text(item, "atpnQesitm"), text(item, "intrcQesitm"), text(item, "seQesitm"),
                text(item, "depositMethodQesitm"), text(item, "itemImage"), text(item, "updateDe"));
    }

    private static String text(Map<?, ?> item, String field) {
        Object value = item.get(field);
        return value instanceof String string ? string : value instanceof Number ? value.toString() : null;
    }
}
