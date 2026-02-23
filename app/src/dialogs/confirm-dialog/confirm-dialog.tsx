import React, {type PropsWithChildren, type ReactNode, useState} from 'react';
import {Button, Dialog, DialogActions, DialogContent, Typography} from '@mui/material';
import {DialogTitleWithClose} from '../../components/dialog-title-with-close/dialog-title-with-close';
import {TextFieldComponent} from '../../components/text-field/text-field-component';
import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';

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
                        <Typography
                            variant="body2"
                            sx={{mt: 2, mb: 1}}
                        >
                            Bitte geben Sie den folgenden Text ein, um die Aktion zu bestätigen:
                            <Typography
                                component="pre"
                                variant="body2"
                                sx={{fontFamily: 'monospace', fontSize: 14, fontWeight: 'bold', backgroundColor: '#f0f0f0', py: 0.5, px: 1, borderRadius: 2, mt: 1}}
                            >
                                {props.confirmationText}
                            </Typography>
                        </Typography>

                        <TextFieldComponent
                            sx={{mt: 2}}
                            label={props.inputLabel || 'Eingabe zur Bestätigung'}
                            value={inputValue}
                            onChange={(val) => {setInputValue(val ?? '');}}
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
