import {PaymentProviderDefinitionResponseDTO} from '../../../../modules/payment/dtos/payment-provider-definition-response-dto';

export interface PaymentProviderAdditionalData {
    definitions: PaymentProviderDefinitionResponseDTO[];
}