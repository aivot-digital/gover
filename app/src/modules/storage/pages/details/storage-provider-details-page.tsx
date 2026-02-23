import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {Typography} from '@mui/material';
import {GenericDetailsPage} from '../../../../components/generic-details-page/generic-details-page';
import {StorageProvidersApiService} from '../../storage-providers-api-service';
import {type StorageProviderAdditionalData} from './storage-provider-details-page-additional-data';
import {ServerEntityType} from '../../../../shells/staff/data/server-entity-type';
import {ModuleIcons} from '../../../../shells/staff/data/module-icons';
import React, {type ReactNode, useState} from 'react';
import {type StorageProviderEntity} from '../../entities/storage-provider-entity';
import {StorageStatusChip} from '../../components/storage-status-chip';
import Sync from '@aivot/mui-material-symbols-400-outlined/dist/sync/Sync';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
import {useNavigate} from 'react-router-dom';

export function StorageProviderDetailsPage(): ReactNode {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();

    const [provider, setProvider] = useState<StorageProviderEntity>();

    const handleSync = (): void => {
        if (provider == null) {
            return;
        }
        new StorageProvidersApiService()
            .resync(provider.id)
            .then(() => {
                dispatch(showSuccessSnackbar('Die Synchronisierung wurde gestartet'));
                setTimeout(() => {
                    navigate(0);
                }, 0);
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Beim Starten der Synchronisierung ist ein Fehler aufgetreten.'));
            });
    };

    return (
        <>
            <PageWrapper
                title="Speicheranbieter bearbeiten"
                fullWidth
                background
            >
                <GenericDetailsPage<StorageProviderEntity, number, StorageProviderAdditionalData>
                    header={{
                        icon: ModuleIcons.storage,
                        title: 'Speicheranbieter bearbeiten',
                        badge: provider != null && provider?.id !== 0 ? <StorageStatusChip status={provider.status} lastSync={provider.lastSync}/> : undefined,
                        actions: [{
                            tooltip: 'Speicher synchronisieren',
                            icon: <Sync/>,
                            onClick: handleSync,
                            disabled: provider?.id === 0,
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
                                        Konfigurieren Sie hier Zahlungsdienstleister, die in Ihrer Gover-Instanz global
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
                            isDisabled: (item) => item?.id === 0,
                        },
                        {
                            path: '/storage-providers/:id/test',
                            label: 'Testen',
                            isDisabled: (item) => item?.id === 0,
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
                        to: '/payment-providers',
                    }}
                    entityType={ServerEntityType.PaymentProviders}
                />
            </PageWrapper>
        </>
    );
}