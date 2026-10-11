package com.dola.billogramdataprocessingapp.service;

import com.dola.billogramdataprocessingapp.controller.RecordAggregationGroup;
import com.dola.billogramdataprocessingapp.domain.Record;
import com.dola.billogramdataprocessingapp.domain.RecordType;
import com.dola.billogramdataprocessingapp.repository.RecordRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecordProcessingService {

    private final RecordRepository recordRepository;
    private final NotificationService notificationService;
    private final AlertService alertService;

    @Transactional
    public Record processRecord(Record incomingRecord) {
        try {
            Record saved = recordRepository.saveAndFlush(incomingRecord);
            notificationService.emitNotification(saved);
            alertService.checkAndEmit(saved);
            return saved;
        } catch (DataIntegrityViolationException e) {
            return recordRepository.findById(incomingRecord.getRecordId()).orElseThrow(() -> e);
        }
    }

    public List<Record> getAllRecords() {
        return recordRepository.findAll();
    }

    public List<RecordAggregationGroup> aggregate(Instant startTime, Instant endTime, RecordType type, String destinationId) {
        List<Record> filtered = recordRepository.findMatching(startTime, endTime, type, destinationId);
        Map<String, List<Record>> grouped = new HashMap<>();
        for (Record record : filtered) {
            grouped.computeIfAbsent(record.getDestinationId(), ignored -> new ArrayList<>()).add(record);
        }

        List<RecordAggregationGroup> groups = new ArrayList<>();
        for (Map.Entry<String, List<Record>> entry : grouped.entrySet()) {
            List<Record> records = entry.getValue();
            records.sort(Comparator.comparing(Record::getTime));
            BigDecimal total = records.stream()
                .map(Record::getValue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            groups.add(new RecordAggregationGroup(entry.getKey(), total, records));
        }

        groups.sort(Comparator.comparing(RecordAggregationGroup::getDestinationId));
        return groups;
    }
}

