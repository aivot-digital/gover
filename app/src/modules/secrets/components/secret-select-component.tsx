import {type ReactNode, useEffect, useState} from 'react';
import {Box, type SxProps, type Theme} from '@mui/material';
import KeyOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Key';
import {useApi} from '../../../hooks/use-api';
import {SecretsApiService} from '../secrets-api-service';
import {type Secret} from '../models/secret';
import {SecretSelectDialog} from '../dialogs/secret-select-dialog';
import {DialogSelectionField, type FormFieldLayoutProps} from '../../../components/form-field';
import {useNormalizedReactId} from '../../../hooks/use-normalized-react-id';

export interface SecretSelectComponentProps extends FormFieldLayoutProps {
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
    controlSx?: SxProps<Theme>;
}

interface LoadedSecretState {
    key: string;
    secret?: Secret;
    unavailable: boolean;
}

export function SecretSelectComponent(props: SecretSelectComponentProps): ReactNode {
    const api = useApi();
    const [loadedSecret, setLoadedSecret] = useState<LoadedSecretState>();
    const [isLoadingSelection, setIsLoadingSelection] = useState(false);
    const [showDialog, setShowDialog] = useState(false);
    const generatedId = useNormalizedReactId();
    const dialogId = `${props.id ?? `secret-select-${generatedId}`}-dialog`;
    const secretKey = normalizeSecretKey(props.value);
    const hasValue = secretKey != null;
    const currentLoadedSecret = loadedSecret?.key === secretKey ? loadedSecret : undefined;
    const selectedSecret = currentLoadedSecret?.secret;
    const isUnavailable = currentLoadedSecret?.unavailable === true;
    const isBusy = Boolean(props.busy || isLoadingSelection);
    const resolvedError = combineErrors(
        props.error,
        isUnavailable ? 'Das ausgewählte Geheimnis ist nicht verfügbar.' : undefined,
    );

    useEffect(() => {
        if (secretKey == null) {
            setLoadedSecret(undefined);
            setIsLoadingSelection(false);
            return;
        }

        if (loadedSecret?.key === secretKey) {
            setIsLoadingSelection(false);
            return;
        }

        let active = true;
        setIsLoadingSelection(true);

        new SecretsApiService(api)
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
            })
            .finally(() => {
                if (active) {
                    setIsLoadingSelection(false);
                }
            });

        return () => {
            active = false;
        };
    }, [api, loadedSecret?.key, secretKey]);

    return (
        <>
            <DialogSelectionField
                id={props.id}
                ariaLabel={props.ariaLabel}
                ariaDescribedBy={props.ariaDescribedBy}
                label={props.label}
                labelAction={props.labelAction}
                hint={props.hint}
                error={resolvedError}
                required={props.required}
                disabled={props.disabled}
                readOnly={props.readOnly}
                busy={isBusy}
                margin={props.margin}
                sx={props.sx}
                showOptionalIndicator={props.showOptionalIndicator}
                controlSx={props.controlSx}
                open={showDialog}
                dialogId={dialogId}
                hasValue={hasValue}
                primaryText={selectedSecret?.name ?? (
                    isLoadingSelection
                        ? 'Geheimnis wird geladen ...'
                        : secretKey ?? props.placeholder ?? 'Kein Geheimnis ausgewählt'
                )}
                secondaryText={selectedSecret?.description || (isUnavailable ? 'Nicht verfügbar' : undefined)}
                leadingVisual={(
                    <Box component="span" sx={{display: 'inline-flex', color: hasValue ? 'primary.main' : 'action.active'}}>
                        <KeyOutlinedIcon sx={{fontSize: 20}} />
                    </Box>
                )}
                onOpen={() => setShowDialog(true)}
                onClear={() => {
                    setLoadedSecret(undefined);
                    props.onChange(null);
                }}
            />

            <SecretSelectDialog
                id={dialogId}
                open={showDialog}
                onClose={() => setShowDialog(false)}
                onSelect={(secret) => {
                    setLoadedSecret({
                        key: secret.key,
                        secret,
                        unavailable: false,
                    });
                    props.onChange(secret.key);
                    setShowDialog(false);
                }}
            />
        </>
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
