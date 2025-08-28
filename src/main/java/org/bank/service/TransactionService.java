package org.bank.service;

import org.bank.entities.Customer;
import org.bank.entities.Transaction;
import org.bank.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    @Autowired
    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public List<Transaction> findAll() {
        return transactionRepository.findAll();
    }

    public Optional<Transaction> findById(Long id) {
        return transactionRepository.findById(id);
    }

    public Transaction save(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    public void deleteById(Long id) {
        transactionRepository.deleteById(id);
    }

    public List<Transaction> findByAccountId(Long accountId) {
        return transactionRepository.findByAccountAccountIdOrderByTimestampDesc(accountId);
    }

    public List<Transaction> findBySourceAccountId(Long accountId) {
        return transactionRepository.findBySourceAccountAccountIdOrderByTimestampDesc(accountId);
    }

    public List<Transaction> findByDestinationAccountId(Long accountId) {
        return transactionRepository.findByDestinationAccountAccountIdOrderByTimestampDesc(accountId);
    }

    public List<Transaction> findByCustomer(Customer customer) {
        return transactionRepository.findByAccountCustomerOrderByTimestampDesc(customer);
    }
}
