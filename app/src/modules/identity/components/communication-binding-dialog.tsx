import {Alert, Box, Button, CircularProgress, Dialog, DialogActions, DialogContent} from '@mui/material';
import Save from '@aivot/mui-material-symbols-400-n25-outlined/Save';
import {useEffect, useState} from 'react';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {CheckboxFieldComponent} from '../../../components/checkbox-field/checkbox-field-component';
import {SelectFieldComponent} from '../../../components/select-field/select-field-component';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';
import {deepEquals} from '../../../utils/equality-utils';
import {CommunicationProvidersApiService} from '../../communication/communication-providers-api-service';
import {type CommunicationConfigurationLayout, type CommunicationProvider, type CommunicationProviderBinding, type CommunicationProviderBindingRequest} from '../../communication/models';
import {ElementDerivationContext} from '../../elements/components/element-derivation-context';

interface CommunicationBindingDialogProps {
    binding: CommunicationProviderBinding | null;
    identityProviderKey: string;
    providers: CommunicationProvider[];
    busy: boolean;
    onClose: () => void;
    onSave: (request: CommunicationProviderBindingRequest) => Promise<void>;
}

type BindingDraft = Omit<CommunicationProviderBindingRequest, 'communicationProviderId'> & {communicationProviderId: number | null};

export function CommunicationBindingDialog({binding, identityProviderKey, providers, busy, onClose, onSave}: CommunicationBindingDialogProps) {
    const [original] = useState<BindingDraft>(() => ({
        identityProviderKey,
        communicationProviderId: binding?.communicationProviderId ?? null,
        name: binding?.name ?? '',
        description: binding?.description ?? '',
        isEnabled: binding?.isEnabled ?? false,
        configuration: binding?.configuration ?? {},
    }));
    const [request, setRequest] = useState(original);
    const [attemptedSubmit, setAttemptedSubmit] = useState(false);
    const [layout, setLayout] = useState<CommunicationConfigurationLayout | null>(null);
    const [layoutLoadFailed, setLayoutLoadFailed] = useState(false);
    const [layoutAttempt, setLayoutAttempt] = useState(0);
    const hasNotChanged = binding != null && deepEquals(original, request);

    useEffect(() => {
        let active = true;
        setLayout(null);
        setLayoutLoadFailed(false);
        if (request.communicationProviderId != null) {
            new CommunicationProvidersApiService().getBindingConfigurationLayout(request.communicationProviderId, identityProviderKey)
                .then(value => { if (active) setLayout(value); })
                .catch(() => { if (active) setLayoutLoadFailed(true); });
        }
        return () => { active = false; };
    }, [identityProviderKey, request.communicationProviderId, layoutAttempt]);

    const updateRequest = <K extends keyof BindingDraft>(key: K, value: BindingDraft[K]) => {
        setRequest(current => ({...current, [key]: value}));
    };
    const close = () => { if (!busy) onClose(); };
    const save = () => {
        setAttemptedSubmit(true);
        if (request.communicationProviderId == null || !request.name.trim() || !request.description.trim() || layout == null) return;
        void onSave({...request, communicationProviderId: request.communicationProviderId, name: request.name.trim(), description: request.description.trim()});
    };

    return (
        <Dialog open onClose={close} fullWidth maxWidth="sm" aria-labelledby="communication-binding-dialog-title">
            <DialogTitleWithClose id="communication-binding-dialog-title" onClose={close}>
                {binding == null ? 'Kommunikationsanbindung hinzufügen' : 'Kommunikationsanbindung bearbeiten'}
            </DialogTitleWithClose>
            <DialogContent>
                <SelectFieldComponent
                    label="Kommunikationsanbieter"
                    required
                    error={attemptedSubmit && request.communicationProviderId == null ? 'Wählen Sie einen Kommunikationsanbieter aus.' : undefined}
                    value={request.communicationProviderId == null ? undefined : String(request.communicationProviderId)}
                    options={providers.map(provider => ({
                        value: String(provider.id),
                        label: provider.name,
                        subLabel: provider.description,
                    }))}
                    onChange={value => {
                        setLayout(null);
                        setRequest(current => ({...current, communicationProviderId: value == null ? null : Number(value), configuration: {}}));
                    }}
                    disabled={busy || binding != null}
                    emptyStatePlaceholder="Keine aktiven kompatiblen Kommunikationsanbieter vorhanden"
                />
                <TextFieldComponent
                    label="Anzeigename"
                    hint="Der Name des Kommunikationsanbieters, welcher gegenüber der ausfüllenden Person angezeigt wird."
                    required
                    maxCharacters={64}
                    error={attemptedSubmit && !request.name.trim() ? 'Geben Sie einen Anzeigenamen ein.' : undefined}
                    value={request.name}
                    onChange={value => updateRequest('name', value ?? '')}
                    disabled={busy}
                />
                <TextFieldComponent
                    label="Beschreibung"
                    hint="Die Beschreibung dient dazu, den Kommunikationsanbieter gegenüber der ausfüllenden Person zu erläutern."
                    required
                    multiline
                    maxCharacters={255}
                    error={attemptedSubmit && !request.description.trim() ? 'Geben Sie eine Beschreibung ein.' : undefined}
                    value={request.description}
                    onChange={value => updateRequest('description', value ?? '')}
                    disabled={busy}
                />
                {layout != null && (
                    <Box sx={{
                        // The fields already supply their standard vertical margins.
                        '& > .MuiGrid-root > .MuiGrid-container': {rowGap: 0},
                    }}>
                        <ElementDerivationContext
                            element={layout}
                            authoredElementValues={request.configuration}
                            onAuthoredElementValuesChange={value => updateRequest('configuration', value)}
                            disabled={busy}
                        />
                    </Box>
                )}
                <CheckboxFieldComponent
                    label="Aktiv"
                    hint="Gibt an, ob diese Kommunikationsanbindung aktiviert ist. Bei temporären technischen Problemen o. Ä. kann die Verbindung deaktiviert werden, ohne die Konfiguration zu verlieren."
                    variant="switch"
                    value={request.isEnabled}
                    onChange={value => updateRequest('isEnabled', value)}
                    disabled={busy}
                />
                {request.communicationProviderId != null && layout == null && !layoutLoadFailed && (
                    <Box sx={{display: 'grid', placeItems: 'center', py: 2}}>
                        <CircularProgress size={24} aria-label="Konfiguration wird geladen"/>
                    </Box>
                )}
                {layoutLoadFailed && <Alert severity="error" action={<Button color="inherit" onClick={() => setLayoutAttempt(attempt => attempt + 1)}>Erneut versuchen</Button>}>
                    Die Konfiguration konnte nicht geladen werden.
                </Alert>}
            </DialogContent>
            <DialogActions sx={{justifyContent: 'flex-start'}}>
                <Button variant="contained" startIcon={<Save/>} onClick={save} disabled={busy || hasNotChanged || (request.communicationProviderId != null && layout == null)}>Speichern</Button>
                <Button variant="outlined" onClick={close} disabled={busy}>Abbrechen</Button>
            </DialogActions>
        </Dialog>
    );
}
