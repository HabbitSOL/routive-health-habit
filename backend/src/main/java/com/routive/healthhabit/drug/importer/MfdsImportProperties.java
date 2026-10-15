package com.routive.healthhabit.drug.importer;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("drug-import")
public record MfdsImportProperties(
        @DefaultValue("false") boolean runOnStartup,
        @DefaultValue("") String serviceKey,
        @DefaultValue("1") int startPage,
        @DefaultValue("100") int maxPages,
        @DefaultValue("100") int pageSize
) {
    public MfdsImportProperties {
        if (startPage < 1 || maxPages < 1 || maxPages > 1000 || pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("Invalid drug import page limits");
        }
    }

    @Override
    public String toString() {
        return "MfdsImportProperties[runOnStartup=" + runOnStartup + ", serviceKey=<redacted>, startPage="
                + startPage + ", maxPages=" + maxPages + ", pageSize=" + pageSize + "]";
    }
}
