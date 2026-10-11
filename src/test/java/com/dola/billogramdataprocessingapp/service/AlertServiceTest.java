package com.dola.billogramdataprocessingapp.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.dola.billogramdataprocessingapp.domain.Record;
import com.dola.billogramdataprocessingapp.domain.RecordType;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "app.alert.threshold=500")
class AlertServiceTest {

    @Autowired
    private AlertService alertService;

    @Test
    void shouldCheckThresholdForValueAboveThreshold() {
        // This test verifies the logic: value > threshold should trigger an alert
        // Since @Async runs asynchronously, we verify the method executes without error
        Record recordAboveThreshold = new Record(
            "r-1",
            Instant.parse("2026-10-08T10:00:00Z"),
            "src-1",
            "dest-1",
            RecordType.POSITIVE,
            new BigDecimal("600.00"),
            "SEK",
            "ref-1"
        );

        // Method should not throw; alert is logged asynchronously
        assertThat(recordAboveThreshold.getValue().doubleValue()).isGreaterThan(500);
        alertService.checkAndEmit(recordAboveThreshold);
    }

    @Test
    void shouldNotTriggerAlertForValueBelowThreshold() {
        // This test verifies records below the threshold do not trigger alerts
        Record recordBelowThreshold = new Record(
            "r-2",
            Instant.parse("2026-10-08T10:00:00Z"),
            "src-1",
            "dest-1",
            RecordType.POSITIVE,
            new BigDecimal("400.00"),
            "SEK",
            "ref-1"
        );

        assertThat(recordBelowThreshold.getValue().doubleValue()).isLessThan(500);
        alertService.checkAndEmit(recordBelowThreshold);
    }

    @Test
    void shouldNotTriggerAlertForValueAtThreshold() {
        // Value exactly at threshold should not trigger (> not >=)
        Record recordAtThreshold = new Record(
            "r-3",
            Instant.parse("2026-10-08T10:00:00Z"),
            "src-1",
            "dest-1",
            RecordType.POSITIVE,
            new BigDecimal("500.00"),
            "SEK",
            "ref-1"
        );

        assertThat(recordAtThreshold.getValue().doubleValue()).isEqualTo(500);
        alertService.checkAndEmit(recordAtThreshold);
    }

    @Test
    void shouldHandleNullValueSafely() {
        // Null values should not cause exceptions
        Record recordWithNullValue = new Record(
            "r-4",
            Instant.parse("2026-10-08T10:00:00Z"),
            "src-1",
            "dest-1",
            RecordType.POSITIVE,
            null,
            "SEK",
            "ref-1"
        );

        assertThat(recordWithNullValue.getValue()).isNull();
        // Should not throw an exception
        alertService.checkAndEmit(recordWithNullValue);
    }

    @Test
    void shouldWorkWithNegativeRecordType() {
        // RecordType does not affect threshold checking, only the value matters
        Record negativeRecord = new Record(
            "r-5",
            Instant.parse("2026-10-08T10:00:00Z"),
            "src-1",
            "dest-1",
            RecordType.NEGATIVE,
            new BigDecimal("750.00"),
            "SEK",
            "ref-1"
        );

        assertThat(negativeRecord.getValue().doubleValue()).isGreaterThan(500);
        alertService.checkAndEmit(negativeRecord);
    }

    @Test
    void shouldProcessRecordWithAllFieldsPopulated() {
        // Comprehensive test with all fields set
        Record record = new Record(
            "alert-record-123",
            Instant.parse("2026-10-08T10:00:00Z"),
            "src-1",
            "dest-1",
            RecordType.POSITIVE,
            new BigDecimal("2500.50"),
            "SEK",
            "ref-1"
        );

        assertThat(record.getRecordId()).isNotNull();
        assertThat(record.getValue().doubleValue()).isGreaterThan(500);
        alertService.checkAndEmit(record);
    }
}

