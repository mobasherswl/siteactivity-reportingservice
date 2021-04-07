package com.siteactivity.reportingservice.service;

import lombok.NonNull;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class SiteActivityServiceDefaultImpl implements SiteActivityService {

    private static final Long KEY_TOTAL = Long.MAX_VALUE;
    private final Duration keepDataForDuration;
    private final Map<String, ConcurrentSkipListMap<Long, AtomicLong>> activityTotalCountMap;

    public SiteActivityServiceDefaultImpl() {
        this(Duration.ofHours(12));
    }

    public SiteActivityServiceDefaultImpl(final Duration keepDataForDuration) {
        this(keepDataForDuration, new ConcurrentHashMap<>());
    }

    public SiteActivityServiceDefaultImpl(final Duration keepDataForDuration, ConcurrentHashMap<String, ConcurrentSkipListMap<Long, AtomicLong>> map) {
        this.keepDataForDuration = keepDataForDuration;
        this.activityTotalCountMap = map;
    }

    @Override
    public void logActivity(@NonNull final String activity, @NonNull final Long value) {
        final ConcurrentSkipListMap<Long, AtomicLong> map = activityTotalCountMap.computeIfAbsent(activity, k -> new ConcurrentSkipListMap<>());
        final Instant nowInstant = Instant.now();
        final Instant beforeInstant = nowInstant.minus(keepDataForDuration);
        AtomicLong counter = map.putIfAbsent(nowInstant.getEpochSecond(), new AtomicLong(0));
        AtomicLong total = map.putIfAbsent(KEY_TOTAL, new AtomicLong(0));

        if (counter == null) {
            counter = map.get(nowInstant.getEpochSecond());
        }

        if (total == null) {
            total = map.get(KEY_TOTAL);
        }

        total.addAndGet(value);
        counter.addAndGet(value);

        removeEntriesLessThanInstant(map, beforeInstant);
    }

    @Override
    public long getActivityTotal(@NonNull final String activity) {
        final Instant beforeInstant = Instant.now().minus(keepDataForDuration);
        final ConcurrentSkipListMap<Long, AtomicLong> map = activityTotalCountMap.getOrDefault(activity, new ConcurrentSkipListMap<>());

        removeEntriesLessThanInstant(map, beforeInstant);

        return map.getOrDefault(KEY_TOTAL, new AtomicLong(0)).longValue();
    }

    private void removeEntriesLessThanInstant(final ConcurrentSkipListMap<Long, AtomicLong> map, final Instant entriesToBeLessThanInstant) {
        final AtomicLong total = map.get(KEY_TOTAL);
        Map.Entry<Long, AtomicLong> lowerEntry = map.lowerEntry(entriesToBeLessThanInstant.getEpochSecond());

        while (lowerEntry != null) {
            total.addAndGet(-lowerEntry.getValue().longValue());
            map.remove(lowerEntry.getKey());
            lowerEntry = map.lowerEntry(lowerEntry.getKey());
        }
    }
}
