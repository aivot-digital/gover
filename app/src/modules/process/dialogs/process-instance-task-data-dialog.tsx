import {Alert, Button, Dialog, DialogActions, DialogContent, Stack, Typography} from '@mui/material';
import {useId} from 'react';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {ExpandableCodeBlock} from '../../../components/expandable-code-block/expandable-code-block';
import {compareProcessData} from '../utils/process-data-comparison';
import {
    ProcessInstanceAttachmentSetList,
    type ProcessInstanceAttachmentSetListItem,
} from '../components/process-instance-attachment-set-list';
import {type ProcessInstanceAttachmentEntity} from '../entities/process-instance-attachment-entity';
import {type ProcessInstanceTaskEntity} from '../entities/process-instance-task-entity';

export type ProcessInstanceTaskDataKind = 'changes' | 'element-data' | 'process-data';

interface ProcessInstanceTaskDataDialogProps {
    task: ProcessInstanceTaskEntity;
    taskLabel: string;
    previousProcessData: Record<string, unknown> | null;
    kind: ProcessInstanceTaskDataKind;
    attachmentItems?: ProcessInstanceAttachmentSetListItem[];
    onDownloadAttachment?: (attachment: ProcessInstanceAttachmentEntity) => void | Promise<void>;
    onClose: () => void;
}

export function ProcessInstanceTaskDataDialog({
                                                  task,
                                                  taskLabel,
                                                  previousProcessData,
                                                  kind,
                                                  attachmentItems,
                                                  onDownloadAttachment,
                                                  onClose,
                                              }: ProcessInstanceTaskDataDialogProps) {
    const titleId = useId();
    const isComparison = kind === 'changes';
    const value = kind === 'element-data' ? task.nodeData : task.processData;
    const comparison = isComparison ? compareProcessData(previousProcessData, task.processData) : null;

    return (
        <Dialog open
                onClose={onClose}
                fullWidth
                maxWidth="md"
                aria-labelledby={titleId}>
            <DialogTitleWithClose id={titleId}
                                  onClose={onClose}>
                {isComparison ? 'Änderungen an den Vorgangsdaten' : kind === 'process-data' ? 'Vorgangsdaten' : 'Elementdaten'}
            </DialogTitleWithClose>
            <DialogContent>
                <Stack spacing={2}>
                    <Typography sx={{overflowWrap: 'anywhere'}}>
                        {taskLabel}
                    </Typography>
                    {task.finished == null && (
                        <Alert severity="info">
                            Die Aufgabe ist noch nicht abgeschlossen. Die angezeigten Daten sind ein Zwischenstand.
                        </Alert>
                    )}
                    {isComparison && previousProcessData == null && (
                        <Alert severity="info">
                            Der vorherige Datenstand ist nicht verfügbar. Es werden nur die gespeicherten Vorgangsdaten
                            dieser Aufgabe angezeigt.
                        </Alert>
                    )}
                    {comparison?.unchanged &&
                        <Alert severity="info">Diese Aufgabe hat die Vorgangsdaten nicht verändert.</Alert>}
                    {comparison != null && previousProcessData != null && !comparison.unchanged && (
                        <Typography variant="body2"
                                    sx={{color: 'text.secondary'}}>
                            Hinzugefügte Felder: {comparison.counts.added} · Geänderte
                            Felder: {comparison.counts.changed} · Entfernte Felder: {comparison.counts.removed}
                        </Typography>
                    )}
                    {kind === 'element-data' && Object.keys(value).length === 0 ? (
                        <Alert severity="info">Für diese Aufgabe sind keine Elementdaten gespeichert.</Alert>
                    ) : (
                        <ExpandableCodeBlock
                            value={comparison?.currentJson ?? JSON.stringify(value, null, 4)}
                            previousValue={comparison?.previousJson}
                            language="json"
                        />
                    )}
                    {kind === 'element-data' && attachmentItems != null &&
                        <ProcessInstanceAttachmentSetList items={attachmentItems}
                                                          onDownload={onDownloadAttachment}/>}
                </Stack>
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose}>Schließen</Button>
            </DialogActions>
        </Dialog>
    );
}
