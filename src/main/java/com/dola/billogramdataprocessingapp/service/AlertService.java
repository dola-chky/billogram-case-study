package com.dola.billogramdataprocessingapp.service;

import com.dola.billogramdataprocessingapp.domain.Record;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AlertService {

    @Value("${app.alert.threshold:1000}")
    private double threshold;

    @Async
    public void checkAndEmit(Record record) {
        if (record.getValue() != null && record.getValue().doubleValue() > threshold) {
            System.out.println("[ALERT] Record exceeds threshold: " + record.getRecordId() + " value=" + record.getValue());
        }
    }
}

