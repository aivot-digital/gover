import {ConfigType} from '../enums/config-type';

export interface SystemConfigDefinitionResponseDTO {
    key: string;
    type: ConfigType;
    category: string;
    label: string;
    description: string;
    isPublicConfig: boolean;
}
