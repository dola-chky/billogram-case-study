package com.dola.billogramdataprocessingapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dola.billogramdataprocessingapp.controller.RecordAggregationGroup;
import com.dola.billogramdataprocessingapp.domain.Record;
import com.dola.billogramdataprocessingapp.domain.RecordType;
import com.dola.billogramdataprocessingapp.repository.RecordRepository;
import com.dola.billogramdataprocessingapp.service.AlertService;
import com.dola.billogramdataprocessingapp.service.NotificationService;
import com.dola.billogramdataprocessingapp.service.RecordProcessingService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class RecordProcessingIntegrationTest {

    @Mock
    private RecordRepository recordRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AlertService alertService;

    @InjectMocks
    private RecordProcessingService recordProcessingService;

    @BeforeEach
    void setUp() {
        // no-op setup needed for the void async notification method
    }

    @Test
    void shouldPersistRecordAndAggregateByDestination() {
        Record record1 = new Record(
            "r-1",
            Instant.parse("2026-10-08T10:00:00Z"),
            "src-1",
            "dest-1",
            RecordType.POSITIVE,
            new BigDecimal("12.50"),
            "SEK",
            "ref-1"
        );

        Record record2 = new Record(
            "r-2",
            Instant.parse("2026-10-08T11:00:00Z"),
            "src-2",
            "dest-1",
            RecordType.NEGATIVE,
            new BigDecimal("5.00"),
            "SEK",
            "ref-1"
        );

        when(recordRepository.saveAndFlush(record1)).thenReturn(record1);
        when(recordRepository.saveAndFlush(record2)).thenReturn(record2);
        when(recordRepository.findMatching(any(), any(), any(), any())).thenReturn(List.of(record1, record2));

        Record saved1 = recordProcessingService.processRecord(record1);
        Record saved2 = recordProcessingService.processRecord(record2);

        assertThat(saved1.getRecordId()).isEqualTo("r-1");
        assertThat(saved2.getRecordId()).isEqualTo("r-2");

        List<RecordAggregationGroup> result = recordProcessingService.aggregate(
            Instant.parse("2026-10-08T00:00:00Z"),
            Instant.parse("2026-10-09T00:00:00Z"),
            null,
            null
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDestinationId()).isEqualTo("dest-1");
        assertThat(result.get(0).getTotalValue()).isEqualByComparingTo(new BigDecimal("17.50"));

        verify(notificationService, times(2)).emitNotification(any(Record.class));
        verify(alertService, times(2)).checkAndEmit(any(Record.class));
    }

    @Test
    void shouldReturnExistingRecordWhenDuplicateInsertFails() {
        Record record = new Record(
            "r-dup",
            Instant.parse("2026-10-08T10:00:00Z"),
            "src-1",
            "dest-2",
            RecordType.POSITIVE,
            new BigDecimal("100.00"),
            "SEK",
            "ref-2"
        );

        when(recordRepository.findById("r-dup")).thenReturn(java.util.Optional.of(record));
        when(recordRepository.saveAndFlush(record)).thenThrow(new DataIntegrityViolationException("duplicate key"));

        Record saved = recordProcessingService.processRecord(record);

        assertThat(saved.getRecordId()).isEqualTo("r-dup");
        verify(notificationService, never()).emitNotification(any(Record.class));
        verify(alertService, never()).checkAndEmit(any(Record.class));
    }

    @Test
    void shouldPropagateUnexpectedPersistenceFailures() {
        Record record = new Record(
            "r-fail",
            Instant.parse("2026-10-08T10:00:00Z"),
            "src-1",
            "dest-2",
            RecordType.POSITIVE,
            new BigDecimal("100.00"),
            "SEK",
            "ref-2"
        );

        when(recordRepository.saveAndFlush(record)).thenThrow(new IllegalStateException("db unavailable"));

        assertThatThrownBy(() -> recordProcessingService.processRecord(record))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("db unavailable");

        verify(notificationService, never()).emitNotification(any(Record.class));
        verify(alertService, never()).checkAndEmit(any(Record.class));
    }
}
