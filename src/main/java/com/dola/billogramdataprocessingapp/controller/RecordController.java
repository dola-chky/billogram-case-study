package com.dola.billogramdataprocessingapp.controller;

import com.dola.billogramdataprocessingapp.domain.Record;
import com.dola.billogramdataprocessingapp.domain.RecordType;
import com.dola.billogramdataprocessingapp.service.RecordProcessingService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RecordController {

    private final RecordProcessingService recordProcessingService;

    @PostMapping("/records")
    public ResponseEntity<Record> ingestRecord(@Valid @RequestBody Record record) {
        Record saved = recordProcessingService.processRecord(record);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/records/aggregate")
    public List<RecordAggregationGroup> aggregate(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
        @RequestParam(required = false) RecordType type,
        @RequestParam(required = false) String destinationId
    ) {
        return recordProcessingService.aggregate(startTime, endTime, type, destinationId);
    }

    @GetMapping("/records")
    public List<Record> getAllRecords() {
        return recordProcessingService.getAllRecords();
    }
}

