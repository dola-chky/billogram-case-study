package com.dola.billogramdataprocessingapp.controller;

import com.dola.billogramdataprocessingapp.domain.Record;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RecordAggregationGroup {
    private String destinationId;
    private BigDecimal totalValue;
    private List<Record> records;
}

