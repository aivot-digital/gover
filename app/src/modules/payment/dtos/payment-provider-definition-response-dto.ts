import {GroupLayout} from '../../../models/elements/form/layout/group-layout';

export interface PaymentProviderDefinitionResponseDTO {
    key: string;
    name: string;
    description: string;
    configLayout?: GroupLayout | null;
}