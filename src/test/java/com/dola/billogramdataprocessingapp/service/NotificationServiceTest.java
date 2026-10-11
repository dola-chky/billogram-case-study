package com.dola.billogramdataprocessingapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dola.billogramdataprocessingapp.domain.Record;
import com.dola.billogramdataprocessingapp.domain.RecordType;
import com.dola.billogramdataprocessingapp.repository.RecordRepository;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private RecordRepository recordRepository;

    private NotificationService notificationService;

    private PrintStream originalStdOut;
    private ByteArrayOutputStream capturedOutput;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(recordRepository);
        originalStdOut = System.out;
        capturedOutput = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOutput));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalStdOut);
    }

    @Test
    void shouldBuildNotificationWithCurrentAndPreviousRecords() {
        Record current = new Record(
            "r-2",
            Instant.parse("2026-10-08T11:00:00Z"),
            "src-1",
            "dest-1",
            RecordType.POSITIVE,
            new BigDecimal("50.00"),
            "SEK",
            "ref-1"
        );

        Record previous = new Record(
            "r-1",
            Instant.parse("2026-10-08T10:00:00Z"),
            "src-1",
            "dest-1",
            RecordType.POSITIVE,
            new BigDecimal("25.00"),
            "SEK",
            "ref-1"
        );

        when(recordRepository.findByDestinationIdAndReferenceOrderByTimeAsc("dest-1", "ref-1"))
            .thenReturn(List.of(previous, current));

        notificationService.emitNotification(current);

        String output = capturedOutput.toString();
        assertThat(output).contains("[NOTIFICATION]");
        assertThat(output).contains("r-2");
        assertThat(output).contains("r-1");

        verify(recordRepository).findByDestinationIdAndReferenceOrderByTimeAsc("dest-1", "ref-1");
    }

    @Test
    void shouldFilterOutCurrentRecordFromPreviousRecords() {
        Record current = new Record(
            "r-3",
            Instant.parse("2026-10-08T12:00:00Z"),
            "src-1",
            "dest-1",
            RecordType.POSITIVE,
            new BigDecimal("30.00"),
            "SEK",
            "ref-1"
        );

        Record record1 = new Record(
            "r-1",
            Instant.parse("2026-10-08T10:00:00Z"),
            "src-1",
            "dest-1",
            RecordType.POSITIVE,
            new BigDecimal("10.00"),
            "SEK",
            "ref-1"
        );

        Record record2 = new Record(
            "r-2",
            Instant.parse("2026-10-08T11:00:00Z"),
            "src-1",
            "dest-1",
            RecordType.POSITIVE,
            new BigDecimal("20.00"),
            "SEK",
            "ref-1"
        );

        when(recordRepository.findByDestinationIdAndReferenceOrderByTimeAsc("dest-1", "ref-1"))
            .thenReturn(List.of(record1, record2, current));

        notificationService.emitNotification(current);

        String output = capturedOutput.toString();
        // Current record should be in notification
        assertThat(output).contains("r-3");
        // Previous records should be in notification
        assertThat(output).contains("r-1");
        assertThat(output).contains("r-2");
    }

    @Test
    void shouldHandleNoPreviousRecords() {
        Record current = new Record(
            "r-1",
            Instant.parse("2026-10-08T10:00:00Z"),
            "src-1",
            "dest-1",
            RecordType.POSITIVE,
            new BigDecimal("50.00"),
            "SEK",
            "ref-1"
        );

        when(recordRepository.findByDestinationIdAndReferenceOrderByTimeAsc("dest-1", "ref-1"))
            .thenReturn(List.of(current));

        notificationService.emitNotification(current);

        String output = capturedOutput.toString();
        assertThat(output).contains("[NOTIFICATION]");
        assertThat(output).contains("r-1");
        // Previous records list should be empty
        assertThat(output).contains("previousRecords=[]");

        verify(recordRepository).findByDestinationIdAndReferenceOrderByTimeAsc("dest-1", "ref-1");
    }

    @Test
    void shouldQueryByDestinationIdAndReference() {
        Record current = new Record(
            "r-1",
            Instant.parse("2026-10-08T10:00:00Z"),
            "src-2",
            "dest-5",
            RecordType.NEGATIVE,
            new BigDecimal("100.00"),
            "EUR",
            "ref-xyz"
        );

        when(recordRepository.findByDestinationIdAndReferenceOrderByTimeAsc("dest-5", "ref-xyz"))
            .thenReturn(List.of(current));

        notificationService.emitNotification(current);

        verify(recordRepository).findByDestinationIdAndReferenceOrderByTimeAsc("dest-5", "ref-xyz");
    }
}



