package com.dola.billogramdataprocessingapp.service;

import com.dola.billogramdataprocessingapp.domain.Record;
import com.dola.billogramdataprocessingapp.repository.RecordRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final RecordRepository recordRepository;

    @Async
    public void emitNotification(Record record) {
        List<Record> previousRecords = recordRepository.findByDestinationIdAndReferenceOrderByTimeAsc(
                record.getDestinationId(),
                record.getReference()
            ).stream()
            .filter(existing -> !existing.getRecordId().equals(record.getRecordId()))
            .toList();

        NotificationPayload payload = new NotificationPayload(record, previousRecords);
        System.out.println("[NOTIFICATION] " + payload);
    }

    public record NotificationPayload(Record currentRecord, List<Record> previousRecords) {
    }
}
