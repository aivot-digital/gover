import {type UiDefinitionInputFieldElementItem} from '../../../models/elements/form/input/ui-definition-input-field-element';
import {type ProcessNodeEntity} from './process-node-entity';

export interface ProcessNodeDefinitionMetadata {
    reusableUiDefinitions: ProcessNodeDefinitionMetadataReusableUiDefinition[];
    forwardedAttachments: ProcessNodeDefinitionMetadataForwardedAttachment[];
    forwardedProcessDataKeys: ProcessNodeDefinitionMetadataForwardedProcessDataKey[];
    forwardedIdentities: ProcessNodeDefinitionMetadataForwardedIdentity[];
}

export interface ProcessNodeDefinitionMetadataReusableUiDefinition {
    label: string;
    subLabel: string | null;
    uiDefinition: UiDefinitionInputFieldElementItem;
    origin: ProcessNodeEntity;
}

export interface ProcessNodeDefinitionMetadataForwardedAttachment {
    fileName: string;
    label: string;
    subLabel: string | null;
    origin: ProcessNodeEntity;
}

export interface ProcessNodeDefinitionMetadataForwardedProcessDataKey {
    processDataKey: string;
    label: string;
    subLabel: string | null;
    origin: ProcessNodeEntity;
}

export interface ProcessNodeDefinitionMetadataForwardedIdentity {
    identityId: string;
    label: string;
    subLabel: string | null;
    origin: ProcessNodeEntity;
}
