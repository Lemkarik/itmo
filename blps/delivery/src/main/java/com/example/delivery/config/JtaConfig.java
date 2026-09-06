package com.example.delivery.config;

import com.arjuna.ats.jta.TransactionManager;
import com.arjuna.ats.jta.UserTransaction;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.jta.JtaTransactionManager;

@Configuration
@Profile("!helios")
@EnableTransactionManagement
public class JtaConfig {

    @Bean
    public JtaTransactionManager transactionManager() {
        jakarta.transaction.UserTransaction userTransaction = UserTransaction.userTransaction();
        jakarta.transaction.TransactionManager transactionManager = TransactionManager.transactionManager();
        return new JtaTransactionManager(userTransaction, transactionManager);
    }
}
