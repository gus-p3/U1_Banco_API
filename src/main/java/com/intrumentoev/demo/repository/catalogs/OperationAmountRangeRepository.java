package com.intrumentoev.demo.repository.catalogs;

import com.intrumentoev.demo.entity.catalogs.OperationAmountRange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OperationAmountRangeRepository extends JpaRepository<OperationAmountRange, Short> {
    List<OperationAmountRange> findByIsActiveTrueOrderByMinAmountAsc();
}
