import {GroupLayout} from '../../../models/elements/form/layout/group-layout';

export interface PaymentProviderDefinitionResponseDTO {
    key: string;
    version: number;
    name: string;
    description: string;
    documentationUrl: string | null;
    configLayout?: GroupLayout | null;
}
