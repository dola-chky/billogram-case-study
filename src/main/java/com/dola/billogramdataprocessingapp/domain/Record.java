package com.dola.billogramdataprocessingapp.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "records",
    indexes = {
        @Index(name = "idx_record_time", columnList = "time"),
        @Index(name = "idx_record_destination", columnList = "destinationId"),
        @Index(name = "idx_record_type", columnList = "type"),
        @Index(name = "idx_record_destination_reference", columnList = "destinationId, reference")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Record {

    @Id
    @NotBlank
    @Column(nullable = false, unique = true)
    private String recordId;

    @NotNull
    @Column(nullable = false)
    private Instant time;

    @NotBlank
    @Column(nullable = false)
    private String sourceId;

    @NotBlank
    @Column(nullable = false)
    private String destinationId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecordType type;

    @NotNull
    @DecimalMin("0.00")
    @Column(name = "\"value\"", nullable = false, precision = 19, scale = 4)
    private BigDecimal value;

    @NotBlank
    @Column(nullable = false)
    private String unit;

    @NotBlank
    @Column(nullable = false)
    private String reference;
}

