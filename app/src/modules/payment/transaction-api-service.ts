import {Api} from '../../hooks/use-api';
import {PaymentTransactionResponseDTO} from './dtos/payment-transaction-response-dto';
import {CrudApiService} from '../../services/crud-api-service';
import {XBezahldienstePaymentStatus} from '../../data/xbezahldienste-payment-status';

interface PaymentTransactionFilters {
    paymentProviderKey: string;
    status: XBezahldienstePaymentStatus;
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
            paymentInformation: {},
            paymentProviderKey: '',
            created: new Date().toISOString(),
        };
    }
}