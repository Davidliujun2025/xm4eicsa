package com.carepilot.forbiddenwords.dto;

public class CsvPreviewItem {
    private int rowNumber;
    private String rawLine;
    private String word;
    private String platform;
    private boolean valid;
    private String error;

    public CsvPreviewItem() {
    }

    public CsvPreviewItem(int rowNumber, String rawLine, String word, String platform, boolean valid, String error) {
        this.rowNumber = rowNumber;
        this.rawLine = rawLine;
        this.word = word;
        this.platform = platform;
        this.valid = valid;
        this.error = error;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
    }

    public String getRawLine() {
        return rawLine;
    }

    public void setRawLine(String rawLine) {
        this.rawLine = rawLine;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
