import React, {type PropsWithChildren, type ReactNode, useMemo, useState} from 'react';
import {Box, Button, Dialog, DialogActions, DialogContent, Typography} from '@mui/material';
import {DialogTitleWithClose} from '../../components/dialog-title-with-close/dialog-title-with-close';
import {TextFieldComponent} from '../../components/text-field/text-field-component';
import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';
import {CopyToClipboardButton} from '../../components/copy-to-clipboard-button/copy-to-clipboard-button';

interface ConfirmDialogProps {
    title: string;
    onCancel: () => void;
    onConfirm?: () => void;
    confirmationText?: string;
    inputLabel?: string;
    inputPlaceholder?: string;
    isDestructive?: boolean;
    confirmButtonText?: string;
    hideCancelButton?: boolean;
    width?: 'sm' | 'md' | 'lg' | 'xl';
}

export function ConfirmDialog(props: PropsWithChildren<ConfirmDialogProps>): ReactNode {
    const [inputValue, setInputValue] = useState('');

    const requiresInput = !!props.confirmationText;
    const isConfirmDisabled = requiresInput ? inputValue !== props.confirmationText : false;

    const mismatch = useMemo(() => {
        if (!requiresInput) {
            return false;
        }
        if (!inputValue) {
            return false;
        }
        return inputValue !== props.confirmationText;
    }, [inputValue, props.confirmationText, requiresInput]);

    return (
        <Dialog
            open={props.onConfirm != null}
            onClose={props.onCancel}
            fullWidth={true}
            maxWidth={props.width || 'sm'}
        >
            <DialogTitleWithClose onClose={props.onCancel}>
                {props.title}
            </DialogTitleWithClose>

            <DialogContent tabIndex={0}>
                {props.children}
                {requiresInput && (
                    <>
                        <Typography variant="body2" sx={{mt: 2}}>
                            Bitte geben Sie den folgenden Text ein, um die Aktion zu bestätigen:
                        </Typography>

                        <Box
                            sx={{
                                mt: 2,
                                display: 'flex',
                                alignItems: 'center',
                                gap: 1,
                                px: 1.25,
                                py: 1,
                                borderRadius: 1,
                                border: '1px solid',
                                borderColor: 'divider',
                                bgcolor: 'action.hover',
                            }}
                        >
                            <Typography
                                component="code"
                                sx={{
                                    fontFamily: 'monospace',
                                    fontSize: 14,
                                    flex: 1,
                                    minWidth: 0,
                                    wordBreak: 'break-word',
                                    textAlign: 'center',
                                }}
                            >
                                {props.confirmationText}
                            </Typography>

                            <CopyToClipboardButton
                                text={props.confirmationText ?? ''}
                                tooltip="Text kopieren"
                                copiedTooltip="Kopiert!"
                                ariaLabel="Bestätigungstext kopieren"
                                size="small"
                                disabled={!props.confirmationText}
                            />
                        </Box>

                        <TextFieldComponent
                            label={props.inputLabel || 'Eingabe zur Bestätigung'}
                            value={inputValue}
                            onChange={(val) => {setInputValue(val ?? '');}}
                            error={mismatch ? 'Der Text muss exakt übereinstimmen.' : undefined}
                            debounce={600}
                            required
                        />
                    </>
                )}
            </DialogContent>

            <DialogActions sx={{justifyContent: 'flex-start'}}>
                <Button
                    onClick={props.onConfirm}
                    variant="contained"
                    color={props.isDestructive ? 'error' : 'primary'}
                    disabled={isConfirmDisabled}
                    startIcon={props.isDestructive ? <Delete /> : undefined}
                >
                    {props.confirmButtonText || (props.isDestructive ? 'Löschen' : 'Bestätigen')}
                </Button>

                {
                    !props.hideCancelButton &&
                    <Button
                        onClick={props.onCancel}
                        variant="outlined"
                    >
                        Abbrechen
                    </Button>
                }
            </DialogActions>
        </Dialog>
    );
}
