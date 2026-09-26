import {Alert, Box, Button, Chip, CircularProgress, Divider, Typography} from '@mui/material';
import Add from '@aivot/mui-material-symbols-400-n25-outlined/Add';
import Delete from '@aivot/mui-material-symbols-400-n25-outlined/Delete';
import Edit from '@aivot/mui-material-symbols-400-n25-outlined/Edit';
import ArrowUpward from '@aivot/mui-material-symbols-400-n25-outlined/ArrowUpward';
import ArrowDownward from '@aivot/mui-material-symbols-400-n25-outlined/ArrowDownward';
import {useCallback, useEffect, useMemo, useState} from 'react';
import {IconButton} from '../../../../components/icon-button/icon-button';
import {useGenericDetailsPageContext} from '../../../../components/generic-details-page/generic-details-page-context';
import {Permission} from '../../../../data/permissions/permission';
import {ModuleIcons} from '../../../../shells/staff/data/module-icons';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {useConfirm} from '../../../../providers/confirm-provider';
import {showApiErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
import {CommunicationProvidersApiService} from '../../../communication/communication-providers-api-service';
import {type CommunicationProvider, type CommunicationProviderBinding, type CommunicationProviderBindingRequest, type CommunicationProviderDefinition} from '../../../communication/models';
import {useHasSystemPermission} from '../../../permissions/hooks/use-permissions';
import {CommunicationBindingDialog} from '../../components/communication-binding-dialog';
import {type IdentityProviderDetailsDTO} from '../../models/identity-provider-details-dto';

export function IdentityProviderDetailsPageCommunication() {
    const {item, isBusy} = useGenericDetailsPageContext<IdentityProviderDetailsDTO, void>();
    return item == null ? null : <CommunicationBindings key={item.key} identityProvider={item} pageBusy={isBusy}/>;
}

function CommunicationBindings({identityProvider, pageBusy}: {identityProvider: IdentityProviderDetailsDTO; pageBusy: boolean}) {
    const dispatch = useAppDispatch();
    const confirm = useConfirm();
    const api = useMemo(() => new CommunicationProvidersApiService(), []);
    const canCreate = useHasSystemPermission(Permission.COMMUNICATION_PROVIDER_CREATE);
    const canUpdate = useHasSystemPermission(Permission.COMMUNICATION_PROVIDER_UPDATE);
    const canDelete = useHasSystemPermission(Permission.COMMUNICATION_PROVIDER_DELETE);
    const [providers, setProviders] = useState<CommunicationProvider[]>([]);
    const [definitions, setDefinitions] = useState<CommunicationProviderDefinition[]>([]);
    const [bindings, setBindings] = useState<CommunicationProviderBinding[] | null>(null);
    const [editedBinding, setEditedBinding] = useState<CommunicationProviderBinding | null | undefined>(undefined);
    const [loading, setLoading] = useState(true);
    const [loadFailed, setLoadFailed] = useState(false);
    const [busy, setBusy] = useState(false);
    const disabled = busy || loading || pageBusy || loadFailed;

    const reload = useCallback(async () => {
        setLoading(true);
        setLoadFailed(false);
        try {
            const [loadedProviders, loadedDefinitions, loadedBindings] = await Promise.all([
                api.listProviders(), api.listDefinitions(), api.listBindings(identityProvider.key),
            ]);
            setProviders(loadedProviders);
            setDefinitions(loadedDefinitions);
            // Preserve the authoritative order, including the backend's tie-breakers for older data.
            setBindings(loadedBindings);
        } catch (error) {
            setLoadFailed(true);
            dispatch(showApiErrorSnackbar(error, 'Kommunikationsanbindungen konnten nicht geladen werden.'));
        } finally {
            setLoading(false);
        }
    }, [api, dispatch, identityProvider.key]);
    useEffect(() => { void reload(); }, [reload]);

    const compatibleProviders = providers.filter(provider => provider.isEnabled && definitions.some(definition => (
        definition.key === provider.communicationProviderDefinitionKey &&
        definition.version === provider.communicationProviderDefinitionVersion &&
        definition.supportedIdentityProviderTypes.includes(identityProvider.type)
    )));

    const save = async (request: CommunicationProviderBindingRequest) => {
        if (disabled || (editedBinding == null ? !canCreate : !canUpdate)) return;
        setBusy(true);
        try {
            if (editedBinding == null) await api.createBinding(request);
            else await api.updateBinding(editedBinding.id, request);
            setEditedBinding(undefined);
            dispatch(showSuccessSnackbar('Kommunikationsanbindung wurde gespeichert.'));
            await reload();
        } catch (error) {
            dispatch(showApiErrorSnackbar(error, 'Kommunikationsanbindung konnte nicht gespeichert werden.'));
        } finally {
            setBusy(false);
        }
    };

    const remove = async (binding: CommunicationProviderBinding) => {
        if (disabled || !canDelete) return;
        const confirmed = await confirm({
            title: 'Kommunikationsanbindung löschen',
            confirmButtonText: 'Endgültig löschen',
            isDestructive: true,
            children: <Typography>Möchten Sie „{binding.name}“ endgültig löschen?</Typography>,
        });
        if (!confirmed) return;
        setBusy(true);
        try {
            await api.deleteBinding(binding.id);
            dispatch(showSuccessSnackbar('Kommunikationsanbindung wurde gelöscht.'));
            await reload();
        } catch (error) {
            dispatch(showApiErrorSnackbar(error, 'Kommunikationsanbindung konnte nicht gelöscht werden.'));
        } finally {
            setBusy(false);
        }
    };

    const move = async (index: number, direction: -1 | 1) => {
        if (bindings == null || disabled || !canUpdate) return;
        const target = index + direction;
        if (target < 0 || target >= bindings.length) return;
        const reordered = [...bindings];
        [reordered[index], reordered[target]] = [reordered[target], reordered[index]];
        setBindings(reordered);
        setBusy(true);
        try {
            setBindings(await api.reorderBindings(identityProvider.key, reordered.map(binding => binding.id)));
            dispatch(showSuccessSnackbar('Reihenfolge wurde gespeichert.'));
        } catch (error) {
            setBindings(bindings);
            dispatch(showApiErrorSnackbar(error, 'Reihenfolge konnte nicht gespeichert werden.'));
            // A concurrent addition or deletion may have invalidated this list.
            await reload();
        } finally {
            setBusy(false);
        }
    };

    return (
        <Box>
            <Typography variant="h5" component="h2" sx={{mt: 1.5, mb: 1}}>Kommunikationsanbindungen</Typography>
            <Typography sx={{maxWidth: 900, mb: 3}}>
                Legen Sie fest, über welche Anbieter die ausfüllende Person Nachrichten zu ihrem Vorgang erhalten kann.
                Verschieben Sie die Anbindungen mit den Pfeilen, um die Reihenfolge der Auswahl festzulegen.
            </Typography>
            <Box sx={{border: '1px solid', borderColor: 'divider', borderRadius: 1, overflow: 'hidden'}}>
                {loading && <Box sx={{display: 'grid', placeItems: 'center', minHeight: 160}}>
                    <CircularProgress size={28} aria-label="Kommunikationsanbindungen werden geladen"/>
                </Box>}
                {loadFailed && !loading && <Alert severity="error" sx={{m: 2}} action={
                    <Button color="inherit" size="small" onClick={() => void reload()}>Erneut versuchen</Button>
                }>Die Kommunikationsanbindungen konnten nicht geladen werden.</Alert>}
                {!loading && !loadFailed && bindings?.length === 0 && <Box sx={{px: 3, py: 4, textAlign: 'center'}}>
                    <Typography sx={{fontWeight: 600}}>Noch keine Kommunikationsanbindungen eingerichtet</Typography>
                    <Typography color="text.secondary" sx={{mt: 0.5, mx: 'auto', maxWidth: 680}}>
                        Fügen Sie eine Anbindung hinzu, um einen Kommunikationsanbieter für diesen Identitätsanbieter bereitzustellen.
                    </Typography>
                </Box>}
                {!loading && !loadFailed && bindings != null && bindings.length > 0 && (
                    <Box component="ol" aria-label="Kommunikationsanbindungen" sx={{listStyle: 'none', m: 0, p: 0}}>
                        {bindings.map((binding, index) => {
                            const provider = providers.find(candidate => candidate.id === binding.communicationProviderId);
                            return <Box component="li" key={binding.id}>
                                {index > 0 && <Divider/>}
                                <Box sx={{display: 'grid', gridTemplateColumns: '36px minmax(0, 1fr) auto', alignItems: 'center', gap: 2, px: 2, py: 1.75}}>
                                    <Box aria-hidden sx={{width: 32, height: 32, display: 'grid', placeItems: 'center', color: 'text.secondary'}}>
                                        {ModuleIcons.communication}
                                    </Box>
                                    <Box sx={{minWidth: 0, overflowWrap: 'anywhere'}}>
                                        <Box sx={{display: 'flex', alignItems: 'center', gap: 1, flexWrap: 'wrap'}}>
                                            <Typography sx={{fontWeight: 600}}>{binding.name}</Typography>
                                            <Chip size="small" variant="outlined" label={binding.isEnabled ? 'Aktiv' : 'Inaktiv'} color={binding.isEnabled ? 'success' : 'default'}/>
                                            {provider != null && !provider.isEnabled && <Chip size="small" variant="outlined" label="Anbieter inaktiv" color="warning"/>}
                                        </Box>
                                        <Typography color="text.secondary" sx={{mt: 0.5}}>{binding.description}</Typography>
                                        <Typography variant="body2" color="text.secondary" sx={{mt: 0.5}}>Kommunikationsanbieter: {provider?.name ?? `#${binding.communicationProviderId}`}</Typography>
                                    </Box>
                                    <Box sx={{display: 'flex', gap: 0.5}}>
                                        <IconButton
                                            tooltipProps={{title: 'Nach oben verschieben'}}
                                            buttonProps={{
                                                size: 'small',
                                                'aria-label': `${binding.name} nach oben verschieben`,
                                                disabled: disabled || !canUpdate || index === 0,
                                                onClick: () => void move(index, -1),
                                            }}
                                        ><ArrowUpward/></IconButton>
                                        <IconButton
                                            tooltipProps={{title: 'Nach unten verschieben'}}
                                            buttonProps={{
                                                size: 'small',
                                                'aria-label': `${binding.name} nach unten verschieben`,
                                                disabled: disabled || !canUpdate || index === bindings.length - 1,
                                                onClick: () => void move(index, 1),
                                            }}
                                        ><ArrowDownward/></IconButton>
                                        <IconButton
                                            tooltipProps={{title: 'Bearbeiten'}}
                                            buttonProps={{
                                                size: 'small',
                                                'aria-label': `${binding.name} bearbeiten`,
                                                disabled: disabled || !canUpdate,
                                                onClick: () => setEditedBinding(binding),
                                            }}
                                        ><Edit/></IconButton>
                                        <IconButton
                                            tooltipProps={{title: 'Löschen'}}
                                            buttonProps={{
                                                size: 'small',
                                                color: 'error',
                                                'aria-label': `${binding.name} löschen`,
                                                disabled: disabled || !canDelete,
                                                onClick: () => void remove(binding),
                                            }}
                                        ><Delete/></IconButton>
                                    </Box>
                                </Box>
                            </Box>;
                        })}
                    </Box>
                )}
            </Box>
            {canCreate && <Button variant="contained" startIcon={<Add/>} sx={{mt: 2}} onClick={() => setEditedBinding(null)} disabled={disabled}>
                Anbindung hinzufügen
            </Button>}
            {editedBinding !== undefined && <CommunicationBindingDialog
                binding={editedBinding}
                identityProviderKey={identityProvider.key}
                providers={editedBinding == null ? compatibleProviders : providers.filter(provider => provider.id === editedBinding.communicationProviderId)}
                busy={busy || pageBusy}
                onClose={() => setEditedBinding(undefined)}
                onSave={save}
            />}
        </Box>
    );
}
