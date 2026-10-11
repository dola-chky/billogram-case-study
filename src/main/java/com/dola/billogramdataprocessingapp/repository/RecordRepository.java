package com.dola.billogramdataprocessingapp.repository;

import com.dola.billogramdataprocessingapp.domain.Record;
import com.dola.billogramdataprocessingapp.domain.RecordType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RecordRepository extends JpaRepository<Record, String> {

    @Query("select r from Record r where (:startTime is null or r.time >= :startTime) " +
        "and (:endTime is null or r.time <= :endTime) " +
        "and (:type is null or r.type = :type) " +
        "and (:destinationId is null or r.destinationId = :destinationId)")
    List<Record> findMatching(
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime,
        @Param("type") RecordType type,
        @Param("destinationId") String destinationId
    );

    @Query("select sum(r.value) from Record r where (:startTime is null or r.time >= :startTime) " +
        "and (:endTime is null or r.time <= :endTime) " +
        "and (:type is null or r.type = :type) " +
        "and r.destinationId = :destinationId")
    BigDecimal sumByDestination(
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime,
        @Param("type") RecordType type,
        @Param("destinationId") String destinationId
    );

    List<Record> findByDestinationIdAndReferenceOrderByTimeAsc(String destinationId, String reference);
}

