import {Api} from '../../hooks/use-api';
import {PaymentTransactionResponseDTO} from './dtos/payment-transaction-response-dto';
import {CrudApiService} from '../../services/crud-api-service';
import {PaymentStatus} from '../../data/payment-status';

interface PaymentTransactionFilters {
    paymentProviderKey: string;
    status: PaymentStatus;
    purpose: string;
    hasError: boolean;
}

export class TransactionsApiService extends CrudApiService<PaymentTransactionResponseDTO, PaymentTransactionResponseDTO, PaymentTransactionResponseDTO, PaymentTransactionResponseDTO, PaymentTransactionResponseDTO, string, PaymentTransactionFilters>{
    constructor(api: Api) {
        super(api, 'payment-transactions/');
    }

    public initialize(): PaymentTransactionResponseDTO {
        return {
            key: '',
            paymentInformation: null,
            paymentRequest: null,
            paymentProviderKey: '',
            hasError: false,
            status: PaymentStatus.Pending,
            created: new Date().toISOString(),
        };
    }
}
