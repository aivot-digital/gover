import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {Typography} from '@mui/material';
import {GenericDetailsPage} from '../../../../components/generic-details-page/generic-details-page';
import {StorageProvidersApiService} from '../../storage-providers-api-service';
import {type StorageProviderAdditionalData} from './storage-provider-details-page-additional-data';
import {ServerEntityType} from '../../../../shells/staff/data/server-entity-type';
import {ModuleIcons} from '../../../../shells/staff/data/module-icons';
import React, {createContext, type ReactNode, useCallback, useContext, useRef, useState} from 'react';
import {type StorageProviderEntity} from '../../entities/storage-provider-entity';
import {StorageStatusChip} from '../../components/storage-status-chip';
import Sync from '@aivot/mui-material-symbols-400-n25-outlined/Sync';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
import {GenericDetailsPageControlRef} from '../../../../components/generic-details-page/generic-details-page-props';
import {Permission} from '../../../../data/permissions/permission';
import {useHasSystemPermission} from '../../../permissions/hooks/use-permissions';
import {formatMissingPermissionTooltip} from '../../../permissions/utils/permission-utils';

type StorageProviderSyncPreparationHandler = () => Promise<boolean>;

interface StorageProviderDetailsPageSyncContextValue {
    registerSyncPreparationHandler: (handler: StorageProviderSyncPreparationHandler | null) => void;
}

// The sync action lives in the page header while the form state lives in the outlet page.
const StorageProviderDetailsPageSyncContext = createContext<StorageProviderDetailsPageSyncContextValue>({
    registerSyncPreparationHandler: () => {
    },
});

export function useStorageProviderDetailsPageSyncContext(): StorageProviderDetailsPageSyncContextValue {
    return useContext(StorageProviderDetailsPageSyncContext);
}

export function StorageProviderDetailsPage(): ReactNode {
    const dispatch = useAppDispatch();
    const canUpdateStorageProvider = useHasSystemPermission(Permission.STORAGE_PROVIDER_UPDATE);

    const [provider, setProvider] = useState<StorageProviderEntity>();
    const [isSyncing, setIsSyncing] = useState(false);
    const detailsPageControlRef = useRef<GenericDetailsPageControlRef | null>(null);
    const syncPreparationHandlerRef = useRef<StorageProviderSyncPreparationHandler | null>(null);

    const registerSyncPreparationHandler = useCallback((handler: StorageProviderSyncPreparationHandler | null): void => {
        syncPreparationHandlerRef.current = handler;
    }, []);

    const handleSync = async (): Promise<void> => {
        if (provider == null || isSyncing || !canUpdateStorageProvider) {
            return;
        }

        const canProceed = await (syncPreparationHandlerRef.current?.() ?? Promise.resolve(true));
        if (!canProceed) {
            return;
        }

        setIsSyncing(true);
        try {
            await new StorageProvidersApiService().resync(provider.id);
            dispatch(showSuccessSnackbar('Die Synchronisierung wurde gestartet'));
            detailsPageControlRef.current?.refresh();
        } catch (err) {
            dispatch(showApiErrorSnackbar(err, 'Beim Starten der Synchronisierung ist ein Fehler aufgetreten.'));
        } finally {
            setIsSyncing(false);
        }
    };

    return (
        <StorageProviderDetailsPageSyncContext.Provider value={{registerSyncPreparationHandler}}>
            <PageWrapper
                title="Speicheranbieter bearbeiten"
                fullWidth
                background
            >
                <GenericDetailsPage<StorageProviderEntity, number, StorageProviderAdditionalData>
                    permissionCheck={{
                        create: Permission.STORAGE_PROVIDER_CREATE,
                        read: Permission.STORAGE_PROVIDER_READ,
                        update: Permission.STORAGE_PROVIDER_UPDATE,
                        scope: {
                            type: 'system',
                        },
                    }}
                    header={{
                        icon: ModuleIcons.storage,
                        title: 'Speicheranbieter bearbeiten',
                        badge: provider != null && provider?.id !== 0 ? <StorageStatusChip status={provider.status} lastSync={provider.lastSync}/> : undefined,
                        actions: [{
                            tooltip: 'Speicher synchronisieren',
                            icon: <Sync/>,
                            onClick: () => {
                                void handleSync();
                            },
                            disabled: provider?.id === 0 || isSyncing || !canUpdateStorageProvider,
                            disabledTooltip: !canUpdateStorageProvider
                                ? formatMissingPermissionTooltip(Permission.STORAGE_PROVIDER_UPDATE)
                                : undefined,
                        }],
                        helpDialog: {
                            title: 'Hilfe zu Speicheranbietern',
                            tooltip: 'Hilfe anzeigen',
                            content: (
                                <>
                                    <Typography
                                        variant="body1"
                                        paragraph
                                    >
                                        Konfigurieren Sie hier Zahlungsdienstleister, die in Ihrer Prosuna-Instanz global
                                        verfügbar sein sollen.
                                        Die erforderlichen Konfigurationsdaten erhalten Sie vom Zahlungsdienstleister
                                        oder finden Sie in dessen Dokumentation.
                                    </Typography>
                                    <Typography
                                        variant="body1"
                                        paragraph
                                    >
                                        Es wird empfohlen, für jeden Zahlungsdienstleister sowohl eine produktive als
                                        auch eine vorproduktive Anbindung einzurichten, um Tests zu erleichtern.
                                    </Typography>
                                </>
                            ),
                        },
                    }}
                    tabs={[
                        {
                            path: '/storage-providers/:id',
                            label: 'Konfiguration',
                        },
                        {
                            path: '/storage-providers/:id/explore',
                            label: 'Dateiexplorer',
                            onlyExisting: true,
                            requiredPermission: Permission.STORAGE_PROVIDER_READ,
                        },
                        {
                            path: '/storage-providers/:id/test',
                            label: 'Testen',
                            onlyExisting: true,
                            requiredPermission: Permission.STORAGE_PROVIDER_UPDATE,
                        },
                    ]}
                    initializeItem={() => {
                        const newItem = new StorageProvidersApiService()
                            .initialize();
                        setProvider(newItem);
                        return newItem;
                    }}
                    fetchData={(api, id: number) => {
                        return new StorageProvidersApiService()
                            .retrieve(id)
                            .then((item) => {
                                setProvider(item);
                                return item;
                            });
                    }}
                    fetchAdditionalData={{
                        definitions: () => new StorageProvidersApiService().listDefinitions(),
                    }}
                    getTabTitle={(item: StorageProviderEntity) => {
                        if (item.id === 0) {
                            return 'Neuer Speicheranbieter';
                        } else {
                            return item.name;
                        }
                    }}
                    getHeaderTitle={(item, isNewItem, notFound) => {
                        if (notFound ?? false) return 'Speicheranbieter nicht gefunden';
                        if (isNewItem ?? false) return 'Neuen Speicheranbieter anlegen';
                        return `Speicheranbieter: ${item?.name ?? 'Unbenannt'}`;
                    }}
                    parentLink={{
                        label: 'Liste der Speicheranbieter',
                        to: '/storage-providers',
                    }}
                    entityType={ServerEntityType.StorageProviders}
                    controlRef={detailsPageControlRef}
                />
            </PageWrapper>
        </StorageProviderDetailsPageSyncContext.Provider>
    );
}
