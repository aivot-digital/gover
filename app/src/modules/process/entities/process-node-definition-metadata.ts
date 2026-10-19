import {type UiDefinitionInputFieldElementItem} from '../../../models/elements/form/input/ui-definition-input-field-element';
import {type ProcessNodeWrite} from './process-node-entity';
import {type InputVariableSuggestion} from '../../../models/input-mode';
import {type CodeListValueMode} from '../../code-lists/enums/code-list-value-mode';

export interface ProcessNodeDefinitionMetadata {
    reusableUiDefinitions: ProcessNodeDefinitionMetadataReusableUiDefinition[];
    forwardedAttachmentSets: ProcessNodeDefinitionMetadataForwardedAttachmentSet[];
    forwardedProcessDataKeys: ProcessNodeDefinitionMetadataForwardedProcessDataKey[];
    forwardedIdentities: ProcessNodeDefinitionMetadataForwardedIdentity[];
    inputVariables?: InputVariableSuggestion[];
}

export interface ProcessNodeDefinitionMetadataReusableUiDefinition {
    label: string;
    subLabel: string | null;
    uiDefinition: UiDefinitionInputFieldElementItem;
    origin: ProcessNodeWrite;
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
    origin: ProcessNodeWrite;
}

export interface ProcessNodeDefinitionMetadataForwardedProcessDataKey {
    processDataKey: string;
    label: string;
    subLabel: string | null;
    origin: ProcessNodeWrite;
    valueOptions?: ProcessNodeDefinitionMetadataValueOption[] | null;
    // Set instead of valueOptions when the options come from a code list, which clients load themselves.
    codeListKey?: string | null;
    codeListValueMode?: CodeListValueMode | null;
}

export interface ProcessNodeDefinitionMetadataValueOption {
    value: string;
    label: string;
}

export interface ProcessNodeDefinitionMetadataForwardedIdentity {
    identityId: string;
    label: string;
    subLabel: string | null;
    identityProviderKeys: string[];
    origin: ProcessNodeWrite;
}
