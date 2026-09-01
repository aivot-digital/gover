import React, {type ReactNode, useEffect, useMemo, useState} from 'react';
import {
    Box,
    CircularProgress,
    IconButton,
    InputAdornment,
    TextField,
    Tooltip,
    Typography,
} from '@mui/material';
import ChevronRight from '@aivot/mui-material-symbols-400-n25-outlined/ChevronRight';
import Close from '@aivot/mui-material-symbols-400-n25-outlined/Close';
import Key from '@aivot/mui-material-symbols-400-n25-outlined/Key';
import {useApi} from '../../../hooks/use-api';
import {SecretsApiService} from '../secrets-api-service';
import {type Secret} from '../models/secret';
import {SecretSelectDialog} from '../dialogs/secret-select-dialog';

export interface SecretSelectComponentProps {
    label: string;
    hint?: string;
    error?: string;
    value?: string | null;
    onChange: (key: string | null) => void;
    required?: boolean;
    disabled?: boolean;
    readOnly?: boolean;
    busy?: boolean;
    placeholder?: string;
}

interface LoadedSecretState {
    key: string;
    secret?: Secret;
    unavailable: boolean;
}

export function SecretSelectComponent(props: SecretSelectComponentProps): ReactNode {
    const {
        label,
        hint,
        error,
        onChange,
        required = false,
        disabled = false,
        readOnly = false,
        busy = false,
        placeholder = 'Kein Geheimnis ausgewählt',
    } = props;

    const api = useApi();
    const [loadedSecret, setLoadedSecret] = useState<LoadedSecretState>();
    const [showDialog, setShowDialog] = useState(false);
    const secretKey = normalizeSecretKey(props.value);

    useEffect(() => {
        let active = true;

        if (secretKey == null) {
            setLoadedSecret(undefined);
            return () => {
                active = false;
            };
        }

        void new SecretsApiService(api)
            .retrieve(secretKey)
            .then((secret) => {
                if (active) {
                    setLoadedSecret({
                        key: secretKey,
                        secret,
                        unavailable: false,
                    });
                }
            })
            .catch(() => {
                if (active) {
                    setLoadedSecret({
                        key: secretKey,
                        unavailable: true,
                    });
                }
            });

        return () => {
            active = false;
        };
    }, [api, secretKey]);

    const currentLoadedSecret = loadedSecret?.key === secretKey ? loadedSecret : undefined;
    const selectedSecret = currentLoadedSecret?.secret;
    const isLoadingSecret = secretKey != null && currentLoadedSecret == null;
    const isUnavailable = currentLoadedSecret?.unavailable === true;
    const isInteractive = !disabled && !readOnly && !busy;
    const helperError = useMemo(
        () => combineErrors(error, isUnavailable ? 'Das ausgewählte Geheimnis ist nicht verfügbar.' : undefined),
        [error, isUnavailable],
    );

    const handleOpenDialog = () => {
        if (isInteractive) {
            setShowDialog(true);
        }
    };

    const handleClear = (event: React.MouseEvent<HTMLButtonElement>) => {
        event.stopPropagation();
        if (!isInteractive) {
            return;
        }

        setLoadedSecret(undefined);
        onChange(null);
    };

    const handleKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
        if (!isInteractive) {
            return;
        }

        if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();
            setShowDialog(true);
        }
    };

    const primaryContentColor = disabled ? 'text.disabled' : 'text.primary';
    const secondaryContentColor = disabled ? 'text.disabled' : 'text.secondary';
    const iconColor = disabled ? 'text.disabled' : selectedSecret != null ? 'primary.main' : 'action.active';
    const endIconColor = disabled || readOnly ? 'text.disabled' : 'action.active';
    const fieldValue = selectedSecret?.name ?? secretKey ?? '';

    return (
        <>
            <TextField
                fullWidth
                label={label}
                value={fieldValue}
                placeholder={placeholder}
                disabled={disabled}
                error={helperError != null}
                helperText={helperError ?? hint}
                required={required}
                onClick={handleOpenDialog}
                onKeyDown={handleKeyDown}
                sx={{
                    '& .MuiOutlinedInput-root': {
                        cursor: isInteractive ? 'pointer' : 'default',
                        height: 56,
                        minHeight: 56,
                    },
                    '& .MuiOutlinedInput-input': {
                        width: 0,
                        minWidth: 0,
                        flex: '0 0 0',
                        p: 0,
                        cursor: isInteractive ? 'pointer' : 'default',
                        caretColor: 'transparent',
                    },
                    '& .MuiInputAdornment-root': {
                        pointerEvents: disabled ? 'none' : 'auto',
                    },
                }}
                slotProps={{
                    input: {
                        startAdornment: (
                            <InputAdornment
                                position="start"
                                sx={{
                                    minWidth: 0,
                                    flex: 1,
                                    alignItems: 'center',
                                    mr: 1,
                                }}
                            >
                                <Box
                                    component="span"
                                    sx={{
                                        display: 'inline-flex',
                                        flexShrink: 0,
                                        mr: 1.25,
                                        color: iconColor,
                                        '& .MuiSvgIcon-root': {fontSize: 20},
                                    }}
                                >
                                    {
                                        isLoadingSecret ?
                                            <CircularProgress size={20} color="inherit"/> :
                                            <Key/>
                                    }
                                </Box>

                                <Box component="span" sx={{minWidth: 0, flex: 1}}>
                                    {
                                        isLoadingSecret ? (
                                            <FieldText color={secondaryContentColor}>
                                                Lade Geheimnis
                                            </FieldText>
                                        ) : selectedSecret != null ? (
                                            <>
                                                <FieldText color={primaryContentColor} title={selectedSecret.name} primary>
                                                    {selectedSecret.name}
                                                </FieldText>
                                                {
                                                    selectedSecret.description.trim().length > 0 &&
                                                    <FieldText color={secondaryContentColor} title={selectedSecret.description}>
                                                        {selectedSecret.description}
                                                    </FieldText>
                                                }
                                            </>
                                        ) : secretKey != null ? (
                                            <>
                                                <FieldText color={primaryContentColor} title={secretKey} primary>
                                                    {secretKey}
                                                </FieldText>
                                                <FieldText color={secondaryContentColor}>
                                                    Nicht verfügbar
                                                </FieldText>
                                            </>
                                        ) : (
                                            <FieldText color={secondaryContentColor}>
                                                {placeholder}
                                            </FieldText>
                                        )
                                    }
                                </Box>
                            </InputAdornment>
                        ),
                        endAdornment: (
                            <InputAdornment position="end">
                                <Box sx={{display: 'flex', alignItems: 'center', gap: 0.5, mr: -0.5}}>
                                    {
                                        busy &&
                                        <CircularProgress size={18} color="inherit"/>
                                    }

                                    <Tooltip
                                        title={isInteractive && secretKey != null ? 'Auswahl entfernen' : ''}
                                        arrow
                                    >
                                        <span>
                                            <IconButton
                                                size="small"
                                                onClick={handleClear}
                                                onMouseDown={(event) => {
                                                    event.preventDefault();
                                                    event.stopPropagation();
                                                }}
                                                disabled={!isInteractive || secretKey == null}
                                                aria-label="Auswahl entfernen"
                                            >
                                                <Close fontSize="small"/>
                                            </IconButton>
                                        </span>
                                    </Tooltip>

                                    <ChevronRight fontSize="small" sx={{color: endIconColor}}/>
                                </Box>
                            </InputAdornment>
                        ),
                    },
                    htmlInput: {
                        readOnly: true,
                        title: selectedSecret?.name ?? secretKey ?? undefined,
                        'aria-label': label,
                    },
                    inputLabel: {
                        title: label,
                    },
                    formHelperText: {
                        title: helperError ?? hint,
                        sx: {whiteSpace: 'normal'},
                    },
                }}
            />

            <SecretSelectDialog
                open={showDialog}
                onClose={() => setShowDialog(false)}
                onSelect={(secret) => {
                    setLoadedSecret({
                        key: secret.key,
                        secret,
                        unavailable: false,
                    });
                    onChange(secret.key);
                    setShowDialog(false);
                }}
            />
        </>
    );
}

interface FieldTextProps {
    children: ReactNode;
    color: string;
    primary?: boolean;
    title?: string;
}

function FieldText(props: FieldTextProps) {
    return (
        <Typography
            variant={props.primary ? 'body2' : 'caption'}
            component="span"
            color={props.color}
            title={props.title}
            sx={{
                display: 'block',
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap',
                fontSize: props.primary ? '1rem' : undefined,
                lineHeight: props.primary ? 1.25 : 1.2,
            }}
        >
            {props.children}
        </Typography>
    );
}

function normalizeSecretKey(value: string | null | undefined): string | null {
    const normalizedValue = value?.trim();
    return normalizedValue == null || normalizedValue.length === 0 ? null : normalizedValue;
}

function combineErrors(...errors: Array<string | undefined>): string | undefined {
    const messages = new Set(
        errors
            .map((message) => message?.trim())
            .filter((message): message is string => message != null && message.length > 0),
    );
    return messages.size > 0 ? Array.from(messages).join(' ') : undefined;
}
