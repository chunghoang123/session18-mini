package com.rikkeibank.transaction.saga;

import com.rikkeibank.common.dto.ApiResponse;
import com.rikkeibank.common.enums.TransactionStatus;
import com.rikkeibank.common.exception.AppException;
import com.rikkeibank.common.exception.ErrorCode;
import com.rikkeibank.transaction.client.AccountClient;
import com.rikkeibank.transaction.client.dto.AccountActionRequest;
import com.rikkeibank.transaction.dto.TransactionResponse;
import com.rikkeibank.transaction.dto.TransferRequest;
import com.rikkeibank.transaction.entity.TransactionRecord;
import com.rikkeibank.transaction.kafka.TransactionEventProducer;
import com.rikkeibank.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferSagaOrchestratorTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountClient accountClient;

    @Mock
    private TransactionEventProducer eventProducer;

    @InjectMocks
    private TransferSagaOrchestrator sagaOrchestrator;

    private TransferRequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleRequest = TransferRequest.builder()
                .fromAccountNumber("1000000001")
                .toAccountNumber("1000000002")
                .amount(BigDecimal.valueOf(1000000))
                .note("Payment for bill")
                .build();
    }

    @Test
    @DisplayName("Saga Success: Debit and Credit succeed -> Transaction status is SUCCESS")
    void testTransferSaga_Success() {
        when(transactionRepository.save(any(TransactionRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountClient.debit(eq("1000000001"), any(AccountActionRequest.class))).thenReturn(ApiResponse.success(Map.of("status", "OK")));
        when(accountClient.credit(eq("1000000002"), any(AccountActionRequest.class))).thenReturn(ApiResponse.success(Map.of("status", "OK")));

        TransactionResponse response = sagaOrchestrator.executeTransferSaga(sampleRequest, null, "customer1");

        assertNotNull(response);
        assertEquals(TransactionStatus.SUCCESS, response.getStatus());
        assertEquals("1000000001", response.getFromAccountNumber());
        assertEquals("1000000002", response.getToAccountNumber());
        assertEquals(BigDecimal.valueOf(1000000), response.getAmount());

        verify(accountClient, times(1)).debit(eq("1000000001"), any(AccountActionRequest.class));
        verify(accountClient, times(1)).credit(eq("1000000002"), any(AccountActionRequest.class));
        verify(accountClient, never()).compensateDebit(any(), any());
        verify(eventProducer, times(1)).publishTransferCompleted(any());
    }

    @Test
    @DisplayName("Saga Rollback: Credit fails -> Compensating refund called and status is FAILED")
    void testTransferSaga_CreditFails_TriggersCompensation() {
        when(transactionRepository.save(any(TransactionRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountClient.debit(eq("1000000001"), any(AccountActionRequest.class))).thenReturn(ApiResponse.success(Map.of("status", "OK")));
        // Simulate destination credit failure (e.g. Account locked or not found)
        when(accountClient.credit(eq("1000000002"), any(AccountActionRequest.class)))
                .thenThrow(new RuntimeException("Destination account is locked"));
        when(accountClient.compensateDebit(eq("1000000001"), any(AccountActionRequest.class)))
                .thenReturn(ApiResponse.success(Map.of("status", "OK")));

        AppException ex = assertThrows(AppException.class, () ->
                sagaOrchestrator.executeTransferSaga(sampleRequest, null, "customer1"));

        assertTrue(ex.getMessage().contains("Destination account is locked") || ex.getMessage().contains("refunded"));

        // Verify Compensating refund was invoked!
        verify(accountClient, times(1)).compensateDebit(eq("1000000001"), any(AccountActionRequest.class));
        verify(eventProducer, times(1)).publishTransferFailed(any());
    }

    @Test
    @DisplayName("Saga Validation: Transfer to same account should fail immediately")
    void testTransferSaga_SameAccount_Fails() {
        TransferRequest sameAccountReq = TransferRequest.builder()
                .fromAccountNumber("1000000001")
                .toAccountNumber("1000000001")
                .amount(BigDecimal.valueOf(500000))
                .build();

        AppException ex = assertThrows(AppException.class, () ->
                sagaOrchestrator.executeTransferSaga(sameAccountReq, null, "customer1"));

        assertEquals(ErrorCode.SAME_ACCOUNT_TRANSFER, ex.getErrorCode());
        verify(accountClient, never()).debit(any(), any());
    }
}
