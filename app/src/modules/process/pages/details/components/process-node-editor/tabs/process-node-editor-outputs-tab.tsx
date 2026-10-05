import {useEffect, useId, useState} from 'react';
import {Box, Button, Divider, Stack, Typography} from '@mui/material';
import DataObject from '@aivot/mui-material-symbols-400-n25-outlined/DataObject';
import Info from '@aivot/mui-material-symbols-400-n25-outlined/Info';
import {useProcessNodeEditorContext} from '../process-node-editor-context';
import {TextFieldComponent} from '../../../../../../../components/text-field/text-field-component';
import {ElementEditorSectionHeader} from '../../../../../../../components/element-editor-section-header/element-editor-section-header';
import {ProcessNodeOutputTypeDialog} from '../../../../../components/process-node-output-type-dialog';
import {ProcessNodeOutputReference} from '../../../../../components/process-node-output-reference';
import {type ProcessNodeOutput} from '../../../../../services/process-node-provider-api-service';
import {ProcessDataKeyInputComponent} from '../../../../../../../views/process-data-key-input-field-view';

export function ProcessNodeEditorOutputsTab() {
    const {
        node: localNode,
        setNode,
        provider,
        isEditable,
        problems,
    } = useProcessNodeEditorContext();
    const [typeDialogOutput, setTypeDialogOutput] = useState<ProcessNodeOutput | null>(null);
    const outputHintId = useId();
    const hasOutputs = provider.outputs.length > 0;

    useEffect(() => {
        setTypeDialogOutput(null);
    }, [localNode.id, provider.key, provider.majorVersion, provider.componentVersion]);

    return (
        <Box sx={{pt: 1, pb: 2}}>
            <ElementEditorSectionHeader title="Datenschlüssel des Prozesselements" disableMarginTop>
                Über diesen Schlüssel greifen Sie in weiteren Prozessschritten auf die Daten des Prozesselements und
                Informationen zur Ausführung zu.
            </ElementEditorSectionHeader>
            <TextFieldComponent
                label="Datenschlüssel"
                hint="Der Schlüssel muss innerhalb dieser Prozessversion eindeutig sein. Für die Übersicht empfehlen wir einen sprechenden Schlüssel."
                value={localNode.dataKey}
                onChange={(val) => {
                    setNode({
                        ...localNode,
                        dataKey: val ?? '',
                    }, false);
                }}
                required
                maxCharacters={32}
                error={problems?.commonErrors.dataKey}
                disabled={!isEditable}
            />

            <ElementEditorSectionHeader title="Ausgangsdaten" sx={{mt: 2}}>
                {hasOutputs && <span id={outputHintId}>
                    Die Ausgangsdaten stehen Ihnen in weiteren Prozessschritten über die Elementdaten zur Verfügung.
                    Wenn Sie einzelne Werte zusätzlich in den Vorgangsdaten bereitstellen möchten, tragen Sie einen
                    Zielpfad ein.
                </span>}
            </ElementEditorSectionHeader>

            {hasOutputs ? (
                <Stack spacing={2} divider={<Divider/>}>
                    {provider.outputs.map((output, index) => {
                        const outputTitleId = `${outputHintId}-output-${index}-title`;
                        const outputDescriptionId = `${outputHintId}-output-${index}-description`;

                        return (
                            <Box key={output.key} component="section" aria-labelledby={outputTitleId} aria-describedby={outputDescriptionId}>
                                <Box sx={{display: 'flex', alignItems: 'center', gap: 1}}>
                                    <Typography id={outputTitleId} variant="body1" component="h5" sx={{fontWeight: 600, minWidth: 0, flex: 1, overflowWrap: 'anywhere'}}>
                                        {output.label}
                                    </Typography>
                                    <Button
                                        size="small"
                                        startIcon={<Info/>}
                                        onClick={() => setTypeDialogOutput(output)}
                                        aria-label={`Details zu ${output.label} anzeigen`}
                                        aria-haspopup="dialog"
                                        sx={{flexShrink: 0}}
                                    >
                                        Details anzeigen
                                    </Button>
                                </Box>
                                <Typography id={outputDescriptionId} variant="body2" color="text.secondary" sx={{mt: 0.5}}>
                                    {output.description}
                                </Typography>
                                <Box sx={{mt: 1, px: 1, py: 0.5, bgcolor: 'action.hover', borderRadius: 1}}>
                                    <ProcessNodeOutputReference value={`_.${localNode.dataKey}.${output.key}`} kind="path"/>
                                </Box>
                                <Box sx={{mt: 1.5}}>
                                    <ProcessDataKeyInputComponent
                                        label="Zielpfad in den Vorgangsdaten"
                                        ariaDescribedBy={`${outputTitleId} ${outputDescriptionId} ${outputHintId}`}
                                        value={localNode.outputMappings?.[output.key] ?? ''}
                                        onChange={(val) => {
                                            // The output key fixes the source; mappings store only a process-data target.
                                            setNode({
                                                ...localNode,
                                                outputMappings: {
                                                    ...localNode.outputMappings,
                                                    [output.key]: val,
                                                },
                                            }, false);
                                        }}
                                        disabled={!isEditable}
                                        disableWildCards
                                        margin="none"
                                    />
                                </Box>
                            </Box>
                        );
                    })}
                </Stack>
            ) : (
                <Box
                    sx={{
                        p: 2,
                        display: 'flex',
                        alignItems: 'flex-start',
                        gap: 2,
                        border: '1px solid',
                        borderColor: 'divider',
                        borderRadius: 1.5,
                        bgcolor: 'action.hover',
                    }}
                >
                    <Box
                        aria-hidden="true"
                        sx={{
                            display: 'grid',
                            placeItems: 'center',
                            width: 40,
                            height: 40,
                            flexShrink: 0,
                            borderRadius: '50%',
                            bgcolor: 'action.selected',
                            color: 'text.secondary',
                        }}
                    >
                        <DataObject sx={{fontSize: 24}}/>
                    </Box>
                    <Box sx={{minWidth: 0}}>
                        <Typography variant="body1" component="h5" sx={{fontWeight: 600}}>
                            Keine Ausgangsdaten zum Übernehmen
                        </Typography>
                        <Typography variant="body2" color="text.secondary" sx={{mt: 0.75}}>
                            Dieses Prozesselement stellt keine Ausgangsdaten zum Übernehmen bereit. Es kann Vorgangsdaten
                            auf anderem Weg verändern.
                        </Typography>
                    </Box>
                </Box>
            )}

            <ProcessNodeOutputTypeDialog
                open={typeDialogOutput != null}
                output={typeDialogOutput}
                dataPath={typeDialogOutput == null ? undefined : `_.${localNode.dataKey}.${typeDialogOutput.key}`}
                onClose={() => setTypeDialogOutput(null)}
            />
        </Box>
    );
}
