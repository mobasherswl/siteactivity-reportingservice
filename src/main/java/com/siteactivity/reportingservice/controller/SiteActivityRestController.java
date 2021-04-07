package com.siteactivity.reportingservice.controller;

import com.siteactivity.reportingservice.dto.ActivityBody;
import com.siteactivity.reportingservice.dto.EmptyResponse;
import com.siteactivity.reportingservice.service.SiteActivityService;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/activity")
public class SiteActivityRestController {

    private final SiteActivityService siteActivityService;

    @Autowired
    public SiteActivityRestController(final SiteActivityService siteActivityService) {
        this.siteActivityService = siteActivityService;
    }

    @PostMapping(value = "/{key}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EmptyResponse> logActivity(@PathVariable("key") final String key, @RequestBody @NonNull final ActivityBody activityBody) {

        siteActivityService.logActivity(key, activityBody.getValue());

        return ResponseEntity.ok(new EmptyResponse());
    }

    @GetMapping(value = "/{key}/total", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ActivityBody> getActivityTotal(@PathVariable("key") final String key) {

        return ResponseEntity.ok(new ActivityBody(siteActivityService.getActivityTotal(key)));
    }
}
