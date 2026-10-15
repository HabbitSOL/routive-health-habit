package com.routive.healthhabit.drug.importer;

public class DrugImportException extends RuntimeException {
    public enum Code { NOT_CONFIGURED, HTTP_ERROR, RESPONSE_INVALID, UPSTREAM_REJECTED }
    private final Code code;

    public DrugImportException(Code code) {
        // Never retain the HTTP exception: its URL may contain the service key.
        super(code.name());
        this.code = code;
    }

    public Code code() { return code; }
}
