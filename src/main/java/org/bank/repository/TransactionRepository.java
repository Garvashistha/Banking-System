package org.bank.repository;

import org.bank.entities.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByAccountAccountIdOrderByTimestampDesc(Long accountId);
}
