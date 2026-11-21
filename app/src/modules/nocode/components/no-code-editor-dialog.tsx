import {useNormalizedReactId} from '../../../hooks/use-normalized-react-id';
import {ReactNode, useRef, useState} from 'react';
import {Box, Button, Dialog, DialogActions, DialogContent, Typography} from '@mui/material';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {ConfirmDialog} from '../../../dialogs/confirm-dialog/confirm-dialog';
import {
    NoCodeEditorWrapper,
} from '../../../components/element-editor-code-tab/components/no-code-editor-wrapper/no-code-editor-wrapper';
import {NoCodeOperand} from '../../../models/functions/no-code-expression';
import {AnyElement} from '../../../models/elements/any-element';
import {NoCodeDataType} from '../../../data/no-code-data-type';
import {NoCodeOperandEditorContextType} from './no-code-operand-editor';

interface NoCodeEditorDialogProps {
    id?: string;
    title: string;
    rootElement?: AnyElement;
    returnType: NoCodeDataType;
    value: NoCodeOperand | null | undefined;
    required?: boolean;
    reference?: ReactNode;
    readOnly?: boolean;
    busy?: boolean;
    contextType: NoCodeOperandEditorContextType;
    onApply: (operand: NoCodeOperand | null) => void;
    onClose: () => void;
}

/** Mount for one editing session: parent echoes must not overwrite a draft while the dialog is open. */
export function NoCodeEditorDialog(props: NoCodeEditorDialogProps) {
    const titleId = `${useNormalizedReactId()}-title`;
    const initial = useRef(props.value ?? null);
    const [draft, setDraft] = useState<NoCodeOperand | null>(initial.current);
    const [toolbarContainer, setToolbarContainer] = useState<HTMLElement | null>(null);
    const contentRef = useRef<HTMLDivElement>(null);
    const [available, setAvailable] = useState(false);
    const [validationRequest, setValidationRequest] = useState(0);
    const [valid, setValid] = useState(false);
    const [confirmDiscard, setConfirmDiscard] = useState(false);
    const locked = props.readOnly || props.busy;
    const close = () => {
        if (JSON.stringify(draft) !== JSON.stringify(initial.current)) setConfirmDiscard(true);
        else props.onClose();
    };

    return <>
        <Dialog open
                aria-labelledby={titleId}
                onClose={close}
                fullWidth
                maxWidth="lg"
                slotProps={{
                    paper: {
                        id: props.id,
                        sx: {
                            maxHeight: 'calc(100dvh - 64px)',
                            overflow: 'hidden',
                        },
                    },
                }}>
            <DialogTitleWithClose id={`${titleId}-bar`}
                                  onClose={close}
                                  sx={{
                                      display: 'flex',
                                      alignItems: 'center',
                                      position: 'relative',
                                      pl: 3,
                                      pr: 8,
                                      gap: 1,
                                      '& > .MuiTypography-root': {
                                          flex: 1,
                                          minWidth: 0,
                                      },
                                  }}><Box sx={{
                display: 'flex',
                alignItems: 'center',
                gap: 2,
                justifyContent: 'space-between',
            }}>
                <Box component="span"
                     id={titleId}
                     sx={{
                         minWidth: 0,
                         overflowWrap: 'anywhere',
                     }}>{props.title}</Box><Box ref={setToolbarContainer}
                                                sx={{flexShrink: 0}}/>
            </Box></DialogTitleWithClose>
            <DialogContent ref={contentRef}
                           sx={{
                               pt: 2,
                               pb: 3,
                           }}>
                {props.reference != null && <Box sx={{mb: 2}}>{props.reference}</Box>}
                {props.rootElement == null
                    ? <Typography>Der Ausdruckseditor ist in diesem Kontext nicht verfügbar.</Typography>
                    : <NoCodeEditorWrapper
                        parents={[props.rootElement]}
                        noCode={draft}
                        onChange={(next) => {
                            setValid(false);
                            setDraft(next ?? null);
                        }}
                        editable={!locked}
                        desiredReturnType={props.returnType}
                        contextType={props.contextType}
                        label="Ausdruck"
                        required={props.required}
                        onValidityChange={setValid}
                        onAvailabilityChange={setAvailable}
                        validationRequest={validationRequest}
                        toolbarContainer={toolbarContainer}
                        scrollContainerRef={contentRef}
                    />}
            </DialogContent>
            <DialogActions>
                {!props.readOnly && <Button variant="contained"
                                            disabled={Boolean(locked) || !available || props.rootElement == null}
                                            onClick={() => {
                                                setValidationRequest((current) => current + 1);
                                                if (!locked && available && valid && props.rootElement != null) props.onApply(draft);
                                            }}>
                    Übernehmen
                </Button>}
                <Button onClick={close}>{props.readOnly ? 'Schließen' : 'Abbrechen'}</Button>
            </DialogActions>
        </Dialog>
        <ConfirmDialog
            title="Änderungen verwerfen?"
            confirmButtonText="Änderungen verwerfen"
            onConfirm={confirmDiscard ? props.onClose : undefined}
            onCancel={() => setConfirmDiscard(false)}
            isDestructive
        >
            Die Änderungen an diesem Ausdruck werden verworfen. Der bisherige Wert bleibt erhalten.
        </ConfirmDialog>
    </>;
}
