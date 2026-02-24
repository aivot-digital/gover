import React, {type PropsWithChildren, type ReactNode, useEffect, useMemo, useRef, useState} from 'react';
import {Box, Button, Dialog, DialogActions, DialogContent, IconButton, Tooltip, Typography} from '@mui/material';
import {DialogTitleWithClose} from '../../components/dialog-title-with-close/dialog-title-with-close';
import {TextFieldComponent} from '../../components/text-field/text-field-component';
import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';
import ContentCopy from '@mui/icons-material/ContentCopyOutlined';

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
    const [hasCopied, setHasCopied] = useState(false);
    const copyTimeoutRef = useRef<number | null>(null);

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

    const handleCopy = async (): Promise<void> => {
        if (!props.confirmationText) {
            return;
        }

        try {
            await navigator.clipboard.writeText(props.confirmationText);
            setHasCopied(true);

            if (copyTimeoutRef.current != null) {
                window.clearTimeout(copyTimeoutRef.current);
            }

            copyTimeoutRef.current = window.setTimeout(() => {
                setHasCopied(false);
            }, 1500);
        } catch {
            // Clipboard kann je nach Kontext/Permissions fehlschlagen.
            // Manuelles Kopieren ist weiterhin möglich.
        }
    };

    useEffect(() => {
        return () => {
            if (copyTimeoutRef.current != null) {
                window.clearTimeout(copyTimeoutRef.current);
            }
        };
    }, []);

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

                            <Tooltip title={hasCopied ? 'Kopiert!' : 'Text kopieren'} arrow>
                                <span>
                                    <IconButton
                                        size="small"
                                        onClick={handleCopy}
                                        aria-label="Bestätigungstext kopieren"
                                        disabled={!props.confirmationText}
                                    >
                                        <ContentCopy fontSize="small" />
                                    </IconButton>
                                </span>
                            </Tooltip>
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