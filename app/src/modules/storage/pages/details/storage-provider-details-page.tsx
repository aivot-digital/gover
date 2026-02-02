import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {Typography} from '@mui/material';
import {GenericDetailsPage} from '../../../../components/generic-details-page/generic-details-page';
import {StorageProvidersApiService} from '../../storage-providers-api-service';
import {type StorageProviderAdditionalData} from './storage-provider-details-page-additional-data';
import {ServerEntityType} from '../../../../shells/staff/data/server-entity-type';
import {ModuleIcons} from '../../../../shells/staff/data/module-icons';
import React, {type ReactNode} from 'react';
import {type StorageProviderEntity} from '../../entities/storage-provider-entity';

export function StorageProviderDetailsPage(): ReactNode {
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
                        helpDialog: {
                            title: 'Hilfe zu Speicheranbieter',
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
                            path: '/payment-providers/:id',
                            label: 'Konfiguration',
                        },
                    ]}
                    initializeItem={() => new StorageProvidersApiService().initialize()}
                    fetchData={(api, id: number) => new StorageProvidersApiService().retrieve(id)}
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
                        return `Zahlungsdienstleister: ${item?.name ?? 'Unbenannt'}`;
                    }}
                    parentLink={{
                        label: 'Liste der Zahlungsdienstleister',
                        to: '/payment-providers',
                    }}
                    entityType={ServerEntityType.PaymentProviders}
                />
            </PageWrapper>
        </>
    );
}