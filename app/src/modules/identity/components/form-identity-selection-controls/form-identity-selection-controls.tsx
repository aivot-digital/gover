import CheckCircle from '@aivot/mui-material-symbols-400-n25-outlined/CheckCircle';
import Delete from '@aivot/mui-material-symbols-400-n25-outlined/Delete';
import Mail from '@aivot/mui-material-symbols-400-n25-outlined/Mail';
import {
    Alert,
    Box,
    Button,
    Divider,
    FormControl,
    FormControlLabel,
    FormLabel,
    Radio,
    RadioGroup,
    Stack,
    TextField,
    Typography,
} from '@mui/material';
import {forwardRef, useCallback, useEffect, useImperativeHandle, useMemo, useState} from 'react';
import type {AuthoredElementValues, DerivedRuntimeElementData} from '../../../../models/element-data';
import {createDerivedRuntimeElementData} from '../../../../models/element-data';
import {
    FormTriggerApiService,
    type FormIdentityCommunicationState,
    type FormIdentitySlot,
} from '../../../forms/services/form-trigger-api-service';
import {ElementDerivationContext} from '../../../elements/components/element-derivation-context';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar} from '../../../../slices/snackbar-slice';
import {IdentityButton} from '../identity-button/identity-button';

export interface FormIdentitySelectionControlsProps {
    slot: FormIdentitySlot;
    processSlug: string;
    formSlug: string;
    relatedProcessNodeId: number;
    testClaim?: string;
    onChange: (slot: FormIdentitySlot) => void;
    saveMode?: 'explicit' | 'deferred';
    onStatusChange?: (slotId: string, status: FormIdentitySelectionControlsStatus | null) => void;
}

export interface FormIdentitySelectionControlsHandle {
    commitPendingSelection: () => Promise<boolean>;
}

export interface FormIdentitySelectionControlsStatus {
    hasSelection: boolean;
    canCommit: boolean;
    isBusy: boolean;
}

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export const FormIdentitySelectionControls = forwardRef<
    FormIdentitySelectionControlsHandle,
    FormIdentitySelectionControlsProps
>(function FormIdentitySelectionControls(props, ref) {
    const {
        slot,
        processSlug,
        formSlug,
        relatedProcessNodeId,
        testClaim,
        onChange,
        saveMode = 'explicit',
        onStatusChange,
    } = props;
    const dispatch = useAppDispatch();
    const api = useMemo(() => new FormTriggerApiService(), []);
    const [emailAddress, setEmailAddress] = useState(slot.emailAddress ?? '');
    const [emailError, setEmailError] = useState<string | null>(null);
    const [communication, setCommunication] = useState<FormIdentityCommunicationState | null>(slot.communication);
    const [selectedBindingId, setSelectedBindingId] = useState<number | null>(
        slot.communication?.selectedBindingId ?? null,
    );
    const [customerData, setCustomerData] = useState<AuthoredElementValues>(slot.communication?.customerData ?? {});
    const [derivedData, setDerivedData] = useState<DerivedRuntimeElementData>(
        slot.communication?.derivedData ?? createDerivedRuntimeElementData(),
    );
    const [communicationChanged, setCommunicationChanged] = useState(false);
    const [busy, setBusy] = useState(false);

    useEffect(() => {
        setEmailAddress(slot.emailAddress ?? '');
        setEmailError(null);
        setCommunication(slot.communication);
        setSelectedBindingId(slot.communication?.selectedBindingId ?? null);
        setCustomerData(slot.communication?.customerData ?? {});
        setDerivedData(slot.communication?.derivedData ?? createDerivedRuntimeElementData());
        setCommunicationChanged(false);
    }, [slot]);

    const replaceSlot = useCallback((nextSlot: FormIdentitySlot) => {
        onChange(nextSlot);
    }, [onChange]);

    const handleEmailSave = useCallback(async (): Promise<boolean> => {
        const normalizedEmail = emailAddress.trim();
        if (!emailPattern.test(normalizedEmail)) {
            setEmailError('Geben Sie eine gültige E-Mail-Adresse ein.');
            return false;
        }

        setBusy(true);
        setEmailError(null);
        try {
            const nextSlot = await api.setEmailIdentity(
                processSlug,
                formSlug,
                slot.id,
                normalizedEmail,
                testClaim,
            );
            replaceSlot(nextSlot);
            return nextSlot.isReady;
        } catch (error) {
            dispatch(showApiErrorSnackbar(error, 'Die E-Mail-Adresse konnte nicht gespeichert werden.'));
            return false;
        } finally {
            setBusy(false);
        }
    }, [api, dispatch, emailAddress, formSlug, processSlug, replaceSlot, slot.id, testClaim]);

    const handleClear = async () => {
        setBusy(true);
        try {
            await api.clearIdentity(processSlug, formSlug, slot.id, testClaim);
            replaceSlot({
                ...slot,
                identityType: null,
                emailAddress: null,
                isReady: false,
                communication: null,
                availableIdentityProviders: slot.availableIdentityProviders.map(provider => ({
                    ...provider,
                    isAuthenticatedWithThis: false,
                })),
            });
        } catch (error) {
            dispatch(showApiErrorSnackbar(error, 'Die Identität konnte nicht entfernt werden.'));
        } finally {
            setBusy(false);
        }
    };

    const previewCommunication = async (bindingId: number, values: AuthoredElementValues) => {
        const state = await api.deriveCommunication(slot.id, relatedProcessNodeId, bindingId, values);
        setCommunication(state);
        setDerivedData(state.derivedData);
        return state;
    };

    const handleBindingChange = async (value: string) => {
        const bindingId = Number(value);
        setSelectedBindingId(bindingId);
        setCustomerData({});
        setDerivedData(createDerivedRuntimeElementData());
        setCommunicationChanged(true);
        setBusy(true);
        try {
            await previewCommunication(bindingId, {});
        } catch (error) {
            dispatch(showApiErrorSnackbar(error, 'Der Kommunikationsweg konnte nicht vorbereitet werden.'));
        } finally {
            setBusy(false);
        }
    };

    const handleCommunicationDerive = async (values: AuthoredElementValues) => {
        if (selectedBindingId == null) {
            return createDerivedRuntimeElementData();
        }
        const state = await previewCommunication(selectedBindingId, values);
        return state.derivedData;
    };

    const handleCommunicationSave = useCallback(async (): Promise<boolean> => {
        if (selectedBindingId == null) {
            return false;
        }

        setBusy(true);
        try {
            const state = await api.selectCommunication(
                slot.id,
                relatedProcessNodeId,
                selectedBindingId,
                customerData,
            );
            setCommunication(state);
            setDerivedData(state.derivedData);
            setCommunicationChanged(false);
            replaceSlot({
                ...slot,
                identityType: 'IdentityProvider',
                emailAddress: null,
                isReady: state.ready,
                communication: state,
            });
            return state.ready;
        } catch (error) {
            dispatch(showApiErrorSnackbar(error, 'Die Angaben zum Kommunikationsweg konnten nicht gespeichert werden.'));
            return false;
        } finally {
            setBusy(false);
        }
    }, [api, customerData, dispatch, relatedProcessNodeId, replaceSlot, selectedBindingId, slot]);

    const normalizedEmailAddress = emailAddress.trim();
    const savedEmailAddress = slot.emailAddress?.trim() ?? '';
    const hasEmailDraft = normalizedEmailAddress.length > 0;
    const emailNeedsCommit = slot.identityType === 'Email'
        ? normalizedEmailAddress !== savedEmailAddress
        : slot.identityType == null && hasEmailDraft;
    const communicationNeedsCommit = slot.identityType === 'IdentityProvider'
        && (communicationChanged || !slot.isReady);
    const hasSelection = slot.identityType != null || hasEmailDraft;
    const canCommit = !busy && (
        emailNeedsCommit
            ? hasEmailDraft
            : communicationNeedsCommit
                ? selectedBindingId != null
                : slot.isReady
    );

    const commitPendingSelection = useCallback(async (): Promise<boolean> => {
        if (busy) {
            return false;
        }
        if (emailNeedsCommit) {
            return handleEmailSave();
        }
        if (communicationNeedsCommit) {
            return handleCommunicationSave();
        }
        return slot.isReady;
    }, [busy, communicationNeedsCommit, emailNeedsCommit, handleCommunicationSave, handleEmailSave, slot.isReady]);

    useImperativeHandle(ref, () => ({
        commitPendingSelection,
    }), [commitPendingSelection]);

    useEffect(() => {
        onStatusChange?.(slot.id, {
            hasSelection,
            canCommit,
            isBusy: busy,
        });
    }, [busy, canCommit, hasSelection, onStatusChange, slot.id]);

    useEffect(() => () => {
        onStatusChange?.(slot.id, null);
    }, [onStatusChange, slot.id]);

    const hasIdentityProvider = slot.availableIdentityProviders.length > 0;

    return (
        <Stack spacing={2.5} sx={{mt: 2}}>
            {
                hasIdentityProvider &&
                slot.availableIdentityProviders
                    .map(provider => (
                        <IdentityButton
                            key={`${slot.id}-${provider.identityProviderKey}`}
                            startUri={api.createIdentityProviderStartLink(
                                processSlug,
                                formSlug,
                                slot.id,
                                provider.identityProviderKey,
                                testClaim,
                                window.location.href,
                            )}
                            identityProviderName={provider.identityProviderName}
                            identityProviderType={provider.identityProviderType}
                            identityProviderAssetKey={provider.identityProviderAssetKey}
                            isAuthenticated={provider.isAuthenticatedWithThis}
                        />
                    ))
            }

            {
                slot.allowsEmail &&
                slot.availableIdentityProviders.every(provider => !provider.isAuthenticatedWithThis) &&
                <>
                    {
                        hasIdentityProvider &&
                        <Divider>oder</Divider>
                    }
                    <Box>
                        <Typography variant="subtitle2" sx={{display: 'flex', alignItems: 'center', gap: 1, mb: 1}}>
                            <Mail fontSize="small"/>
                            Nur E-Mail-Adresse verwenden
                        </Typography>
                        <Typography variant="body2" color="text.secondary" sx={{mb: 1.5}}>
                            Es wird kein Nutzerkonto und kein Kommunikationsanbieter verwendet. Nachrichten werden
                            direkt an
                            diese Adresse versendet.
                        </Typography>
                        <Stack direction={{xs: 'column', sm: 'row'}} spacing={1} sx={{alignItems: {sm: 'flex-start'}}}>
                            <TextField
                                type="email"
                                label="E-Mail-Adresse"
                                value={emailAddress}
                                required
                                fullWidth
                                error={emailError != null}
                                helperText={emailError ?? undefined}
                                disabled={busy}
                                slotProps={{htmlInput: {maxLength: 254}}}
                                onChange={(event) => {
                                    setEmailAddress(event.target.value);
                                    setEmailError(null);
                                }}
                            />
                            {
                                saveMode === 'explicit' &&
                                <Button
                                    variant="outlined"
                                    onClick={handleEmailSave}
                                    disabled={busy || emailAddress.trim().length === 0}
                                    sx={{minHeight: 56, whiteSpace: 'nowrap'}}
                                >
                                    Übernehmen
                                </Button>
                            }
                        </Stack>
                        {slot.identityType === 'Email' && slot.emailAddress != null &&
                            <Alert severity="success" icon={<CheckCircle/>} sx={{mt: 1.5}}>
                                Nachrichten werden an {slot.emailAddress} gesendet.
                            </Alert>}
                    </Box>
                </>
            }

            {!hasIdentityProvider && !slot.allowsEmail &&
                <Alert severity="warning">
                    Für diese Identität steht aktuell keine Auswahlmöglichkeit zur Verfügung.
                </Alert>}

            {slot.identityType === 'IdentityProvider' && communication == null &&
                <Alert severity="warning">
                    Für dieses Nutzerkonto steht derzeit kein Kommunikationsweg zur Verfügung.
                </Alert>}

            {
                slot.identityType === 'IdentityProvider' &&
                communication != null &&
                <>
                    <Divider/>

                    <Typography>
                        Um Nachrichten über dieses Nutzerkonto zu empfangen, wählen Sie bitte den gewünschten Kommunikationsweg aus und geben Sie die erforderlichen Angaben ein.
                        Sie werden ausschließlich über den ausgewählten Kommunikationsweg kontaktiert. Die Angaben werden nur für die Kommunikation im Rahmen dieses Prozesses verwendet.
                    </Typography>

                    <FormControl>
                        <FormLabel id={`${slot.id}-communication-provider-label`}>
                            Kommunikationsweg auswählen
                        </FormLabel>
                        <RadioGroup
                            aria-labelledby={`${slot.id}-communication-provider-label`}
                            value={selectedBindingId?.toString() ?? ''}
                            onChange={(_, value) => void handleBindingChange(value)}
                        >
                            {communication.choices.map(choice => (
                                <FormControlLabel
                                    key={choice.id}
                                    value={choice.id.toString()}
                                    control={<Radio/>}
                                    disabled={busy}
                                    label={<Box sx={{py: .5}}>
                                        <Typography sx={{fontWeight: 600}}>{choice.name}</Typography>
                                        {choice.description.trim().length > 0 &&
                                            <Typography variant="body2" color="text.secondary">
                                                {choice.description}
                                            </Typography>}
                                    </Box>}
                                />
                            ))}
                        </RadioGroup>
                    </FormControl>

                    {
                        selectedBindingId != null &&
                        communication.customerLayout != null &&
                        <ElementDerivationContext
                            element={communication.customerLayout}
                            authoredElementValues={customerData}
                            derivedData={derivedData}
                            onAuthoredElementValuesChange={(values) => {
                                setCustomerData(values);
                                setCommunicationChanged(true);
                            }}
                            onDerivedDataChange={setDerivedData}
                            onDeriveOverride={handleCommunicationDerive}
                        />
                    }

                    {
                        saveMode === 'explicit' &&
                        <Button
                            variant="outlined"
                            onClick={handleCommunicationSave}
                            disabled={busy || selectedBindingId == null}
                        >
                            Angaben zum Kommunikationsweg übernehmen
                        </Button>
                    }
                </>
            }
        </Stack>
    );
});
