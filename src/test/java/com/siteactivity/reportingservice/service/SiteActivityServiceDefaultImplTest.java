package com.siteactivity.reportingservice.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class SiteActivityServiceDefaultImplTest {

    private SiteActivityServiceDefaultImplTestFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new SiteActivityServiceDefaultImplTestFixture();
    }

    @AfterEach
    void tearDown() {
        fixture = null;
    }

    @Test
    void logActivityToRaiseErrorWhenParamsAreNull() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> fixture.whenLogActivityParamsAreNull()
        );
    }

    @Test
    void loggingActivitySequentiallyMaintainsCorrectTotal() {
        fixture.givenLogActivityValuesAre(SiteActivityServiceDefaultImplTestFixture.ACTIVITY_1, 4, 1, 5);

        fixture.whenLogActivityIsInvoked();

        fixture.thenActivityTotalIs(SiteActivityServiceDefaultImplTestFixture.ACTIVITY_1, 10);
    }

    @Test
    void loggingMultipleActivityConcurrentlyMaintainCorrectTotal() {
        fixture.givenLogActivityValuesAre(SiteActivityServiceDefaultImplTestFixture.ACTIVITY_1, 2, 7, 5);
        fixture.givenLogActivityValuesAre(SiteActivityServiceDefaultImplTestFixture.ACTIVITY_2, 1, 3, 9);

        fixture.whenLogActivityIsInvokedConcurrently();

        fixture.thenActivityTotalIs(SiteActivityServiceDefaultImplTestFixture.ACTIVITY_1, 14);
        fixture.thenActivityTotalIs(SiteActivityServiceDefaultImplTestFixture.ACTIVITY_2, 13);
    }

    @Test
    void loggingActivityWithDelaysMaintainsCorrectTotalAfterDelayedRetrievals() {
        fixture.givenLogActivityValuesAre(SiteActivityServiceDefaultImplTestFixture.ACTIVITY_1, 4, 1, 5);

        fixture.whenLogActivityIsInvokedWithDelay(1000);

        fixture.thenActivityTotalIs(SiteActivityServiceDefaultImplTestFixture.ACTIVITY_1, 10);
        fixture.thenActivityTotalAfterDelayIs(SiteActivityServiceDefaultImplTestFixture.ACTIVITY_1, 6, 2000);
        fixture.thenActivityTotalAfterDelayIs(SiteActivityServiceDefaultImplTestFixture.ACTIVITY_1, 5, 1000);
        fixture.thenActivityTotalAfterDelayIs(SiteActivityServiceDefaultImplTestFixture.ACTIVITY_1, 0, 1000);
    }

    @Test
    void retrieveActivityToRaiseErrorWhenParamIsNull() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> fixture.whenActivityTotalIsRetrieved(null)
        );
    }

    @Test
    void retrievingByIncorrectActivityToReturnZero() {
        fixture.whenActivityTotalIsRetrieved(SiteActivityServiceDefaultImplTestFixture.ACTIVITY_1);

        fixture.thenActivityTotalIs(0);
    }

    private static class SiteActivityServiceDefaultImplTestFixture {
        private static final String ACTIVITY_1 = "activity1";
        private static final String ACTIVITY_2 = "activity2";
        private final SiteActivityServiceDefaultImpl siteActivityServiceDefault = new SiteActivityServiceDefaultImpl(Duration.ofSeconds(3));
        private final Map<String, List<Long>> paramMap = new HashMap<>();
        private long actualActivityTotal = -1;

        private void givenLogActivityValuesAre(final String activity, final long... value) {
            List<Long> list = paramMap.get(activity);

            if (list == null) {
                list = new ArrayList<>();
                paramMap.put(activity, list);
            }

            Arrays.stream(value).forEach(list::add);
        }

        private void whenLogActivityParamsAreNull() {
            siteActivityServiceDefault.logActivity(null, null);
        }

        private void whenActivityTotalIsRetrieved(final String activity) {
            actualActivityTotal = siteActivityServiceDefault.getActivityTotal(activity);
        }

        private void whenLogActivityIsInvokedWithDelay(final long delayInMillis) {
            for (final Map.Entry<String, List<Long>> entry : paramMap.entrySet()) {
                final List<Long> list = entry.getValue();
                for (int i = 0; i < list.size(); i++) {
                    siteActivityServiceDefault.logActivity(entry.getKey(), list.get(i));
                    if (delayInMillis > 0 && i < (list.size() - 1)) {
                        try {
                            Thread.sleep(delayInMillis);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        private void whenLogActivityIsInvokedConcurrently() {
            final ExecutorService executorService = Executors.newWorkStealingPool();
            final Collection<Callable<Integer>> callables = new ArrayList<>();

            for (final Map.Entry<String, List<Long>> entry : paramMap.entrySet()) {
                callables.add(() -> {
                    final List<Long> list = entry.getValue();
                    for (int i = 0; i < list.size(); i++) {
                        siteActivityServiceDefault.logActivity(entry.getKey(), list.get(i));
                    }
                    return 0;
                });
            }
            try {
                executorService.invokeAll(callables);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        private void whenLogActivityIsInvoked() {
            whenLogActivityIsInvokedWithDelay(0);
        }

        private void thenActivityTotalIs(final String activity, final long expectedTotal) {
            Assertions.assertEquals(expectedTotal, siteActivityServiceDefault.getActivityTotal(activity));
        }

        private void thenActivityTotalAfterDelayIs(final String activity, final long expectedTotal, final long delayInMillis) {
            try {
                Thread.sleep(delayInMillis);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            thenActivityTotalIs(activity, expectedTotal);
        }

        private void thenActivityTotalIs(long expectedActivityTotal) {
            Assertions.assertEquals(expectedActivityTotal, actualActivityTotal);
        }
    }
}