package de.aivot.gover.backend.process.models.executionResult;

public class ProcessNodeExecutionResultPaymentRequested extends ProcessNodeExecutionResult {
    private final String transactionKey;
    private final String paymentProviderName;

    public ProcessNodeExecutionResultPaymentRequested(String transactionKey, String paymentProviderName) {
        this.transactionKey = transactionKey;
        this.paymentProviderName = paymentProviderName;
    }

    public String getTransactionKey() {
        return transactionKey;
    }

    public String getPaymentProviderName() {
        return paymentProviderName;
    }
}
