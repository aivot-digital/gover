// Mirrors ProcessNodeDefinitionMetadata.ForwardedProcessDataKey.CodeListValueMode in the backend.
export enum CodeListValueMode {
    // The data contains the item value; the item label is used for display.
    ValueAndLabel = 'ValueAndLabel',
    // The data contains the item label itself, for example for chip inputs.
    LabelOnly = 'LabelOnly',
}
