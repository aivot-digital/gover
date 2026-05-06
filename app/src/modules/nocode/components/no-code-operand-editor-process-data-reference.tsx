import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';
import Functions from '@aivot/mui-material-symbols-400-outlined/dist/functions/Functions';
import {Grid, Stack} from '@mui/material';
import {
    isNoCodeNodeDataReference,
    isNoCodeProcessDataReference,
    NoCodeInstanceDataReference,
    NoCodeNodeDataReference,
    NoCodeProcessDataReference,
} from '../../../models/functions/no-code-expression';
import {AutocompleteTextField, TextFieldComponent} from '../../../components/text-field/text-field-component';
import {Actions} from '../../../components/actions/actions';
import {
    useOptionalProcessNodeEditorContext,
} from '../../process/pages/details/components/process-node-editor/process-node-editor-context';
import {useMemo} from 'react';

interface NoCodeOperandEditorProcessDataReferenceProps {
    label: string;
    hint?: string;
    value: NoCodeProcessDataReference | NoCodeInstanceDataReference | NoCodeNodeDataReference;
    onChange: (value: NoCodeProcessDataReference | NoCodeInstanceDataReference | NoCodeNodeDataReference | undefined) => void;
    onAddEnclosingExpression: () => void;
}

export function NoCodeOperandEditorProcessDataReference(props: NoCodeOperandEditorProcessDataReferenceProps) {
    const {
        label,
        hint,
        value,
        onChange,
        onAddEnclosingExpression,
    } = props;

    const opec = useOptionalProcessNodeEditorContext();

    const isProcessDataReference = isNoCodeProcessDataReference(value);
    const isNodeDataReference = isNoCodeNodeDataReference(value);
    const sourceLabel = isProcessDataReference
        ? 'Vorgangsdaten ($)'
        : isNodeDataReference
            ? 'Pfad in den Elementdaten'
            : 'Geschützte Vorgangsdaten ($$)';

    const startIcon = isProcessDataReference
        ? '$.'
        : isNodeDataReference
            ? `_.${value.nodeDataKey}.`
            : '$$.';

    const suggestions = useMemo(() => {
        if (opec == null || opec.processDataKeyHints == null) {
            return [];
        }

        const data = opec
            .processDataKeyHints
            .filter((hint) => hint.type === (isProcessDataReference ? 'ProcessData' : isNodeDataReference ? 'ElementData' : undefined))
            .map((hint) => ({
                id: hint.key,
                label: hint.key,
                subLabel: hint.node.name ?? undefined,
            }));

        if (data.length === 0) {
            return [];
        }

        return data;
    }, [opec, isProcessDataReference, isNodeDataReference]);

    return (
        <Grid
            container
            spacing={2}
        >
            {
                isNodeDataReference &&
                <Grid size={4}>
                    <TextFieldComponent
                        label="Datenschlüssel des Prozesselementes"
                        hint="Der Datenschlüssel des Prozesselementes, aus dessen Ergebnis gelesen werden soll."
                        value={value.nodeDataKey ?? undefined}
                        onChange={(nodeDataKey) => onChange({
                            ...value,
                            nodeDataKey: nodeDataKey ?? undefined,
                        })}
                        muiPassTroughProps={{margin: 'none'}}
                        startIcon="_."
                    />
                </Grid>
            }

            <Grid size={isNodeDataReference ? 8 : 12}>
                <Stack
                    direction="row"
                    alignItems="flex-start"
                >
                    <AutocompleteTextField
                        label={`${label ?? ''} — (${sourceLabel})`}
                        hint={hint}
                        value={value.path ?? undefined}
                        onChange={(path) => {
                            onChange({
                                ...value,
                                path: path ?? undefined,
                            });
                        }}
                        startIcon={startIcon}
                        endAction={isNodeDataReference ? undefined : [
                            {
                                icon: <Delete/>,
                                tooltip: 'Diesen Vorgangsdaten-Verweis löschen',
                                onClick: () => onChange(undefined),
                            },
                            {
                                tooltip: 'Diesen Verweis mit einem Ausdruck verknüpfen',
                                icon: <Functions/>,
                                onClick: onAddEnclosingExpression,
                            },
                        ]}
                        muiPassTroughProps={{margin: 'none'}}
                        suggestions={suggestions}
                    />

                    {
                        isNodeDataReference &&
                        <Actions
                            size="small"
                            dense={true}
                            color="inherit"
                            actions={[
                                {
                                    icon: <Delete/>,
                                    tooltip: 'Diesen Vorgangsdaten-Verweis löschen',
                                    onClick: () => onChange(undefined),
                                },
                                {
                                    tooltip: 'Diesen Verweis mit einem Ausdruck verknüpfen',
                                    icon: <Functions/>,
                                    onClick: onAddEnclosingExpression,
                                },
                            ]}
                            sx={{
                                mt: 1.5,
                                ml: 1,
                                opacity: 0.66,
                            }}
                        />
                    }
                </Stack>
            </Grid>
        </Grid>
    );
}
