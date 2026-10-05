import React, {useState} from 'react';
import {Box, Chip, Divider} from '@mui/material';
import ContentCopy from '@aivot/mui-material-symbols-400-n25-outlined/ContentCopy';
import {SelectionListRow} from '../../../components/selection-dialog/selection-list-row';
import {getElementIconForType} from '../../../data/element-type/element-icons';
import {ElementType} from '../../../data/element-type/element-type';
import {type AnyElement} from '../../../models/elements/any-element';
import {
    ProcessNodeDefinitionMetadataReusableUiDefinitionKind,
} from '../../../modules/process/entities/process-node-definition-metadata';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showSuccessSnackbar} from '../../../slices/snackbar-slice';
import {stringOrDefault} from '../../../utils/string-utils';
import {
    cloneReusableUiDefinitionForImport,
    type ReusableUiDefinitionImportMode,
    type ReusableUiDefinitionOption,
} from '../reusable-ui-definition-utils';
import {ReusableUiDefinitionImportDialog} from '../reusable-ui-definition-import-dialog';

interface ReusableUiDefinitionsTabProps {
    options: ReusableUiDefinitionOption[];
    onAddElements: (elements: AnyElement[]) => void;
}

const kindLabels: Record<ProcessNodeDefinitionMetadataReusableUiDefinitionKind, string> = {
    [ProcessNodeDefinitionMetadataReusableUiDefinitionKind.CompleteForm]: 'Gesamtes Formular',
    [ProcessNodeDefinitionMetadataReusableUiDefinitionKind.FormSection]: 'Formularabschnitt',
    [ProcessNodeDefinitionMetadataReusableUiDefinitionKind.UiDefinition]: 'Gesamte UI-Definition',
    [ProcessNodeDefinitionMetadataReusableUiDefinitionKind.StepperSection]: 'Abschnitt',
    [ProcessNodeDefinitionMetadataReusableUiDefinitionKind.Tab]: 'Tab',
};

export function ReusableUiDefinitionsTab(props: ReusableUiDefinitionsTabProps): React.ReactNode {
    const dispatch = useAppDispatch();
    const [selectedPartialOption, setSelectedPartialOption] = useState<ReusableUiDefinitionOption>();

    const handleImport = (option: ReusableUiDefinitionOption, mode: ReusableUiDefinitionImportMode) => {
        const elements = cloneReusableUiDefinitionForImport(option, mode);
        if (elements.length === 0) {
            return;
        }

        setSelectedPartialOption(undefined);
        props.onAddElements(elements);
        dispatch(showSuccessSnackbar(
            mode === 'flat' ?
                (elements.length === 1 ?
                    'Element wurde erfolgreich eingefügt' :
                    `${elements.length} Elemente wurden erfolgreich eingefügt`) :
                'UI-Definition wurde erfolgreich eingefügt',
        ));
    };

    return (
        <>
            <Box>
                {
                    props.options.map((option, index) => {
                        const definition = option.definition;
                        const originName = stringOrDefault(definition.origin.name, 'Unbenanntes Prozesselement');
                        // Whole definitions can have generic metadata labels, so identify them by their source.
                        const title = option.partial ? definition.label : originName;
                        const Icon = getElementIconForType(resolveIconType(option));

                        return (
                            <React.Fragment
                                key={`${definition.origin.id}-${definition.uiDefinition.id}-${definition.kind}`}
                            >
                                <SelectionListRow
                                    icon={<Icon/>}
                                    title={<span role="heading" aria-level={3}>{title}</span>}
                                    titleAdornment={
                                        <Chip
                                            size="small"
                                            label={resolveKindLabel(definition.kind)}
                                        />
                                    }
                                    description={
                                        <span>
                                            {resolveDescription(option, originName)}
                                            {
                                                definition.subLabel != null &&
                                                <><br/>{definition.subLabel}</>
                                            }
                                        </span>
                                    }
                                    primaryActionLabel="Kopieren und einfügen"
                                    primaryActionIcon={<ContentCopy/>}
                                    onPrimaryAction={() => {
                                        if (option.partial) {
                                            setSelectedPartialOption(option);
                                        } else {
                                            handleImport(option, 'complete');
                                        }
                                    }}
                                />
                                {
                                    index < props.options.length - 1 &&
                                    <Divider/>
                                }
                            </React.Fragment>
                        );
                    })
                }
            </Box>

            <ReusableUiDefinitionImportDialog
                option={selectedPartialOption}
                onImportAsGroup={() => {
                    if (selectedPartialOption != null) {
                        handleImport(selectedPartialOption, 'group');
                    }
                }}
                onImportFlat={() => {
                    if (selectedPartialOption != null) {
                        handleImport(selectedPartialOption, 'flat');
                    }
                }}
                onClose={() => {
                    setSelectedPartialOption(undefined);
                }}
            />
        </>
    );
}

function resolveKindLabel(kind: ProcessNodeDefinitionMetadataReusableUiDefinitionKind): string {
    return kindLabels[kind] ?? kindLabels[ProcessNodeDefinitionMetadataReusableUiDefinitionKind.UiDefinition];
}

function resolveIconType(option: ReusableUiDefinitionOption): ElementType {
    switch (option.definition.kind) {
        case ProcessNodeDefinitionMetadataReusableUiDefinitionKind.CompleteForm:
            return ElementType.FormLayout;
        case ProcessNodeDefinitionMetadataReusableUiDefinitionKind.FormSection:
        case ProcessNodeDefinitionMetadataReusableUiDefinitionKind.StepperSection:
            return ElementType.Step;
        case ProcessNodeDefinitionMetadataReusableUiDefinitionKind.Tab:
            return ElementType.TabLayout;
        default:
            return option.definition.uiDefinition.type;
    }
}

function resolveDescription(option: ReusableUiDefinitionOption, originName: string): React.ReactNode {
    switch (option.definition.kind) {
        case ProcessNodeDefinitionMetadataReusableUiDefinitionKind.CompleteForm:
            // The reusable form contains standard sections; optional special sections are excluded by the backend.
            return 'Alle Standardabschnitte mit ihren enthaltenen Elementen.';
        case ProcessNodeDefinitionMetadataReusableUiDefinitionKind.UiDefinition:
            return option.definition.label === 'UI-Definition' ?
                'Alle enthaltenen Elemente.' :
                <>„{option.definition.label}“ mit allen enthaltenen Elementen.</>;
        default:
            return <>Aus Prozesselement „{originName}“</>;
    }
}
