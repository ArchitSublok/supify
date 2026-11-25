package com.supify.model;

public class Evidence {
    private final String id;
    private final String claimId;
    private final String title;
    private final String docType;
    private final String filePath;

    public Evidence(String id, String claimId, String title, String docType, String filePath) {
        this.id = id;
        this.claimId = claimId;
        this.title = title;
        this.docType = docType;
        this.filePath = filePath;
    }

    public String getId() { return id; }
    public String getClaimId() { return claimId; }
    public String getTitle() { return title; }
    public String getDocType() { return docType; }
    public String getFilePath() { return filePath; }

    @Override
    public String toString() {
        return id + " [" + docType + "] " + title + " (" + filePath + ")";
    }
}
