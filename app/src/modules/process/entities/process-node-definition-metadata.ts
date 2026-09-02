import {type UiDefinitionInputFieldElementItem} from '../../../models/elements/form/input/ui-definition-input-field-element';
import {type ProcessNodeEntity} from './process-node-entity';

export interface ProcessNodeDefinitionMetadata {
    reusableUiDefinitions: ProcessNodeDefinitionMetadataReusableUiDefinition[];
    forwardedAttachmentSets: ProcessNodeDefinitionMetadataForwardedAttachmentSet[];
    forwardedProcessDataKeys: ProcessNodeDefinitionMetadataForwardedProcessDataKey[];
    forwardedIdentities: ProcessNodeDefinitionMetadataForwardedIdentity[];
}

export interface ProcessNodeDefinitionMetadataReusableUiDefinition {
    label: string;
    subLabel: string | null;
    uiDefinition: UiDefinitionInputFieldElementItem;
    origin: ProcessNodeEntity;
    kind: ProcessNodeDefinitionMetadataReusableUiDefinitionKind;
}

export enum ProcessNodeDefinitionMetadataReusableUiDefinitionKind {
    CompleteForm = 'CompleteForm',
    FormSection = 'FormSection',
    UiDefinition = 'UiDefinition',
    StepperSection = 'StepperSection',
    Tab = 'Tab',
}

export interface ProcessNodeDefinitionMetadataForwardedAttachmentSet {
    dataKey: string;
    label: string;
    subLabel: string | null;
    isMultifile: boolean;
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
