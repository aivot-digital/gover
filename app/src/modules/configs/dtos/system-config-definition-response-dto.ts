import {ConfigType} from '../enums/config-type';
import {AnyFormElement} from '../../../models/elements/form/any-form-element';

export interface SystemConfigDefinitionResponseDTO {
    key: string;
    type: ConfigType;
    category: string;
    label: string;
    description: string;
    isPublicConfig: boolean;
    configElement: AnyFormElement;
}
