package com.siteactivity.reportingservice.service;

public interface SiteActivityService {

    void logActivity(String key, Long value);

    long getActivityTotal(String key);
}
