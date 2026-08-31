import {
    Box,
    Button,
    Chip,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    Paper,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableRow,
    Typography,
} from '@mui/material';
import Add from '@aivot/mui-material-symbols-400-n25-outlined/Add';
import Delete from '@aivot/mui-material-symbols-400-n25-outlined/Delete';
import Edit from '@aivot/mui-material-symbols-400-n25-outlined/Edit';
import Save from '@aivot/mui-material-symbols-400-n25-outlined/Save';
import {useEffect, useMemo, useState} from 'react';
import {CheckboxFieldComponent} from '../../../../components/checkbox-field/checkbox-field-component';
import {SelectFieldComponent} from '../../../../components/select-field/select-field-component';
import {TextFieldComponent} from '../../../../components/text-field/text-field-component';
import {useGenericDetailsPageContext} from '../../../../components/generic-details-page/generic-details-page-context';
import {Permission} from '../../../../data/permissions/permission';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {useConfirm} from '../../../../providers/confirm-provider';
import {showApiErrorSnackbar, showErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
import {CommunicationProvidersApiService} from '../../../communication/communication-providers-api-service';
import {
    CommunicationConfigurationLayout,
    CommunicationProvider,
    CommunicationProviderBinding,
    CommunicationProviderBindingRequest,
    CommunicationProviderDefinition,
} from '../../../communication/models';
import {ElementDerivationContext} from '../../../elements/components/element-derivation-context';
import {useHasSystemPermission} from '../../../permissions/hooks/use-permissions';
import {IdentityProviderDetailsDTO} from '../../models/identity-provider-details-dto';

interface BindingDraft {
    id: number | null;
    communicationProviderId: number | null;
    name: string;
    description: string;
    isEnabled: boolean;
    position: number;
    configuration: Record<string, any>;
}

const emptyDraft: BindingDraft = {
    id: null,
    communicationProviderId: null,
    name: '',
    description: '',
    isEnabled: false,
    position: 0,
    configuration: {},
};

export function IdentityProviderDetailsPageCommunication() {
    const {item: identityProvider, isBusy: pageBusy} = useGenericDetailsPageContext<IdentityProviderDetailsDTO, void>();
    const dispatch = useAppDispatch();
    const confirm = useConfirm();
    const canCreate = useHasSystemPermission(Permission.COMMUNICATION_PROVIDER_CREATE);
    const canUpdate = useHasSystemPermission(Permission.COMMUNICATION_PROVIDER_UPDATE);
    const canDelete = useHasSystemPermission(Permission.COMMUNICATION_PROVIDER_DELETE);
    const [providers, setProviders] = useState<CommunicationProvider[]>([]);
    const [definitions, setDefinitions] = useState<CommunicationProviderDefinition[]>([]);
    const [bindings, setBindings] = useState<CommunicationProviderBinding[]>([]);
    const [draft, setDraft] = useState<BindingDraft | null>(null);
    const [layout, setLayout] = useState<CommunicationConfigurationLayout | null>(null);
    const [busy, setBusy] = useState(false);

    const reload = () => {
        if (!identityProvider?.key) return;
        const api = new CommunicationProvidersApiService();
        setBusy(true);
        Promise.all([
            api.listProviders(),
            api.listDefinitions(),
            api.listBindings(identityProvider.key),
        ])
            .then(([loadedProviders, loadedDefinitions, loadedBindings]) => {
                setProviders(loadedProviders);
                setDefinitions(loadedDefinitions);
                setBindings(loadedBindings);
            })
            .catch(error => dispatch(showApiErrorSnackbar(error, 'Kommunikationsanbindungen konnten nicht geladen werden.')))
            .finally(() => setBusy(false));
    };

    useEffect(reload, [identityProvider?.key]);

    const compatibleProviders = useMemo(() => {
        if (identityProvider == null) return [];
        return providers.filter(provider => {
            const definition = definitions.find(candidate => (
                candidate.key === provider.communicationProviderDefinitionKey &&
                candidate.version === provider.communicationProviderDefinitionVersion
            ));
            return provider.isTestProvider === identityProvider.isTestProvider &&
                definition?.supportedIdentityProviderTypes.includes(identityProvider.type) === true;
        });
    }, [definitions, identityProvider, providers]);

    useEffect(() => {
        if (draft?.communicationProviderId == null || identityProvider?.key == null) {
            setLayout(null);
            return;
        }
        new CommunicationProvidersApiService()
            .getBindingConfigurationLayout(draft.communicationProviderId, identityProvider.key)
            .then(setLayout)
            .catch(error => dispatch(showApiErrorSnackbar(error, 'Konfigurationsoberfläche konnte nicht geladen werden.')));
    }, [dispatch, draft?.communicationProviderId, identityProvider?.key]);

    if (identityProvider == null) return null;

    const providerName = (providerId: number) => providers.find(provider => provider.id === providerId)?.name ?? `#${providerId}`;
    const openCreate = () => setDraft({...emptyDraft, position: bindings.length});
    const openEdit = (binding: CommunicationProviderBinding) => setDraft({
        id: binding.id,
        communicationProviderId: binding.communicationProviderId,
        name: binding.name,
        description: binding.description,
        isEnabled: binding.isEnabled,
        position: binding.position,
        configuration: binding.configuration ?? {},
    });
    const updateDraft = <K extends keyof BindingDraft>(key: K, value: BindingDraft[K]) => {
        setDraft(current => current == null ? null : {...current, [key]: value});
    };

    const save = async () => {
        if (draft == null || draft.communicationProviderId == null || !draft.name.trim() || !draft.description.trim()) {
            dispatch(showErrorSnackbar('Bitte füllen Sie Anbieter, Name und Beschreibung vollständig aus.'));
            return;
        }
        const request: CommunicationProviderBindingRequest = {
            identityProviderKey: identityProvider.key,
            communicationProviderId: draft.communicationProviderId,
            name: draft.name.trim(),
            description: draft.description.trim(),
            isEnabled: draft.isEnabled,
            position: draft.position,
            configuration: draft.configuration ?? {},
        };
        setBusy(true);
        try {
            const api = new CommunicationProvidersApiService();
            if (draft.id == null) await api.createBinding(request);
            else await api.updateBinding(draft.id, request);
            setDraft(null);
            dispatch(showSuccessSnackbar('Kommunikationsanbindung wurde gespeichert.'));
            reload();
        } catch (error) {
            dispatch(showApiErrorSnackbar(error, 'Kommunikationsanbindung konnte nicht gespeichert werden.'));
            setBusy(false);
        }
    };

    const remove = async (binding: CommunicationProviderBinding) => {
        const confirmed = await confirm({
            title: 'Kommunikationsanbindung löschen',
            confirmButtonText: 'Endgültig löschen',
            isDestructive: true,
            children: <Typography>Möchten Sie „{binding.name}“ endgültig löschen?</Typography>,
        });
        if (!confirmed) return;
        setBusy(true);
        try {
            await new CommunicationProvidersApiService().deleteBinding(binding.id);
            dispatch(showSuccessSnackbar('Kommunikationsanbindung wurde gelöscht.'));
            reload();
        } catch (error) {
            dispatch(showApiErrorSnackbar(error, 'Kommunikationsanbindung konnte nicht gelöscht werden.'));
            setBusy(false);
        }
    };

    return (
        <Box>
            <Box sx={{display: 'flex', justifyContent: 'space-between', alignItems: 'start', gap: 2, mb: 3}}>
                <Box>
                    <Typography variant="h4">Kommunikationsanbindungen</Typography>
                    <Typography color="text.secondary" sx={{mt: 1}}>
                        Jede Anbindung beschreibt eine konkrete Nutzung. Derselbe Anbieter kann mehrfach hinzugefügt werden;
                        Attributzuordnungen sind optional.
                    </Typography>
                </Box>
                <Button variant="contained" startIcon={<Add/>} onClick={openCreate} disabled={busy || pageBusy || !canCreate}>
                    Anbieter hinzufügen
                </Button>
            </Box>

            <Paper variant="outlined" sx={{overflowX: 'auto'}}>
                <Table>
                    <TableHead>
                        <TableRow>
                            <TableCell>Reihenfolge</TableCell>
                            <TableCell>Name</TableCell>
                            <TableCell>Anbieter</TableCell>
                            <TableCell>Beschreibung</TableCell>
                            <TableCell>Status</TableCell>
                            <TableCell align="right">Aktionen</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {[...bindings].sort((a, b) => a.position - b.position || a.name.localeCompare(b.name)).map(binding => (
                            <TableRow key={binding.id}>
                                <TableCell>{binding.position}</TableCell>
                                <TableCell>{binding.name}</TableCell>
                                <TableCell>{providerName(binding.communicationProviderId)}</TableCell>
                                <TableCell>{binding.description}</TableCell>
                                <TableCell><Chip size="small" label={binding.isEnabled ? 'Aktiv' : 'Inaktiv'} color={binding.isEnabled ? 'success' : 'default'}/></TableCell>
                                <TableCell align="right">
                                    <Button startIcon={<Edit/>} onClick={() => openEdit(binding)} disabled={!canUpdate}>Bearbeiten</Button>
                                    <Button color="error" startIcon={<Delete/>} onClick={() => remove(binding)} disabled={!canDelete}>Löschen</Button>
                                </TableCell>
                            </TableRow>
                        ))}
                        {bindings.length === 0 && (
                            <TableRow><TableCell colSpan={6}>
                                <Typography color="text.secondary" sx={{py: 3, textAlign: 'center'}}>
                                    Noch keine Kommunikationsanbindung konfiguriert.
                                </Typography>
                            </TableCell></TableRow>
                        )}
                    </TableBody>
                </Table>
            </Paper>

            <Dialog open={draft != null} onClose={() => !busy && setDraft(null)} fullWidth maxWidth="md">
                <DialogTitle>{draft?.id == null ? 'Kommunikationsanbieter hinzufügen' : 'Kommunikationsanbindung bearbeiten'}</DialogTitle>
                <DialogContent>
                    {draft != null && <>
                        <SelectFieldComponent
                            label="Kommunikationsanbieter"
                            required
                            value={draft.communicationProviderId == null ? undefined : String(draft.communicationProviderId)}
                            options={compatibleProviders.map(provider => ({
                                value: String(provider.id),
                                label: provider.name,
                                subLabel: provider.description,
                            }))}
                            onChange={value => {
                                updateDraft('communicationProviderId', value == null ? null : Number(value));
                                updateDraft('configuration', {});
                            }}
                            disabled={busy || draft.id != null}
                            emptyStatePlaceholder="Keine kompatiblen Kommunikationsanbieter vorhanden"
                        />
                        <TextFieldComponent label="Anzeigename" required value={draft.name} onChange={value => updateDraft('name', value ?? '')} disabled={busy}/>
                        <TextFieldComponent label="Beschreibung für Kund:innen" required multiline value={draft.description} onChange={value => updateDraft('description', value ?? '')} disabled={busy}/>
                        <TextFieldComponent
                            label="Reihenfolge"
                            value={String(draft.position)}
                            onChange={value => updateDraft('position', Number.parseInt(value ?? '', 10) || 0)}
                            disabled={busy}
                            muiPassTroughProps={{type: 'number'}}
                        />
                        {layout != null && <ElementDerivationContext
                            element={layout}
                            authoredElementValues={draft.configuration}
                            onAuthoredElementValuesChange={value => updateDraft('configuration', value)}
                            disabled={busy}
                        />}
                        <CheckboxFieldComponent
                            label="Aktiv"
                            variant="switch"
                            value={draft.isEnabled}
                            onChange={value => updateDraft('isEnabled', value)}
                            disabled={busy}
                        />
                    </>}
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setDraft(null)} disabled={busy}>Abbrechen</Button>
                    <Button variant="contained" startIcon={<Save/>} onClick={save} disabled={busy}>Speichern</Button>
                </DialogActions>
            </Dialog>
        </Box>
    );
}
