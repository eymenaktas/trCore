package com.aac.plugin.models;

import java.util.List;
import java.util.UUID;

public class Report {
    
    private final UUID id;
    private final String reporter;
    private final String reported;
    private final String reason;
    private final String dateTime;
    private final List<String> messages;
    
    public Report(UUID id, String reporter, String reported, String reason, String dateTime, List<String> messages) {
        this.id = id;
        this.reporter = reporter;
        this.reported = reported;
        this.reason = reason;
        this.dateTime = dateTime;
        this.messages = messages;
    }
    
    public UUID getId() {
        return id;
    }
    
    public String getReporter() {
        return reporter;
    }
    
    public String getReported() {
        return reported;
    }
    
    public String getReason() {
        return reason;
    }
    
    public String getDateTime() {
        return dateTime;
    }
    
    public List<String> getMessages() {
        return messages;
    }
    
    public String getDate() {
        return dateTime.split(" ")[0];
    }
    
    public String getTime() {
        return dateTime.split(" ")[1];
    }
}