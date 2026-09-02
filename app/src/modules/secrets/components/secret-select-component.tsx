import {type ReactNode, useEffect, useState} from 'react';
import {Box, type SxProps, type Theme} from '@mui/material';
import KeyOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Key';
import {useApi} from '../../../hooks/use-api';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar} from '../../../slices/snackbar-slice';
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

export function SecretSelectComponent(props: SecretSelectComponentProps): ReactNode {
    const api = useApi();
    const dispatch = useAppDispatch();
    const [selectedSecret, setSelectedSecret] = useState<Secret>();
    const [isLoadingSelection, setIsLoadingSelection] = useState(false);
    const [showDialog, setShowDialog] = useState(false);
    const generatedId = useNormalizedReactId();
    const dialogId = `${props.id ?? `secret-select-${generatedId}`}-dialog`;
    const secretKey = props.value;
    const hasValue = secretKey != null && secretKey.trim().length > 0;
    const isBusy = Boolean(props.busy || isLoadingSelection);

    useEffect(() => {
        if (!hasValue || secretKey == null) {
            setSelectedSecret(undefined);
            setIsLoadingSelection(false);
            return;
        }

        let active = true;
        if (selectedSecret?.key === secretKey) {
            setIsLoadingSelection(false);
            return;
        }

        setSelectedSecret(undefined);
        setIsLoadingSelection(true);

        new SecretsApiService(api)
            .retrieve(secretKey)
            .then((secret) => {
                if (active) {
                    setSelectedSecret(secret);
                }
            })
            .catch((error) => {
                if (active) {
                    dispatch(showApiErrorSnackbar(error, 'Das ausgewählte Geheimnis konnte nicht geladen werden.'));
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
    }, [api, dispatch, hasValue, secretKey, selectedSecret?.key]);

    return (
        <>
            <DialogSelectionField
                id={props.id}
                ariaLabel={props.ariaLabel}
                ariaDescribedBy={props.ariaDescribedBy}
                label={props.label}
                labelAction={props.labelAction}
                hint={props.hint}
                error={props.error}
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
                        : hasValue ? secretKey : props.placeholder ?? 'Kein Geheimnis ausgewählt'
                )}
                secondaryText={selectedSecret?.description || undefined}
                leadingVisual={(
                    <Box component="span" sx={{display: 'inline-flex', color: hasValue ? 'primary.main' : 'action.active'}}>
                        <KeyOutlinedIcon sx={{fontSize: 20}} />
                    </Box>
                )}
                onOpen={() => setShowDialog(true)}
                onClear={() => {
                    setSelectedSecret(undefined);
                    props.onChange(null);
                }}
            />

            <SecretSelectDialog
                id={dialogId}
                open={showDialog}
                onClose={() => setShowDialog(false)}
                onSelect={(secret) => {
                    setSelectedSecret(secret);
                    props.onChange(secret.key);
                    setShowDialog(false);
                }}
            />
        </>
    );
}
