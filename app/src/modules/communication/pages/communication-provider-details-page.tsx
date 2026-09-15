import {Typography} from '@mui/material';
import {GenericDetailsPage} from '../../../components/generic-details-page/generic-details-page';
import {PageWrapper} from '../../../components/page-wrapper/page-wrapper';
import {Permission} from '../../../data/permissions/permission';
import {ModuleIcons} from '../../../shells/staff/data/module-icons';
import {CommunicationProvidersApiService} from '../communication-providers-api-service';
import {type CommunicationProvider} from '../models';
import {type CommunicationProviderAdditionalData} from './communication-provider-details-page-additional-data';

export function CommunicationProviderDetailsPage() {
    return (
        <PageWrapper
            title="Kommunikationsanbieter bearbeiten"
            fullWidth
            background
        >
            <GenericDetailsPage<CommunicationProvider, string, CommunicationProviderAdditionalData>
                permissionCheck={{
                    create: Permission.COMMUNICATION_PROVIDER_CREATE,
                    read: Permission.COMMUNICATION_PROVIDER_READ,
                    update: Permission.COMMUNICATION_PROVIDER_UPDATE,
                    scope: {
                        type: 'system',
                    },
                }}
                header={{
                    icon: ModuleIcons.communication,
                    title: 'Kommunikationsanbieter bearbeiten',
                    helpDialog: {
                        title: 'Hilfe zu Kommunikationsanbietern',
                        tooltip: 'Hilfe anzeigen',
                        content: (
                            <Typography variant="body1">
                                Kommunikationsanbieter verbinden Prosuna mit Versanddiensten wie E-Mail oder
                                FIT-Connect. Sie versenden Nachrichten und – sofern unterstützt – Anhänge an Identitäten.
                                Die technische Konfiguration wird anschließend passenden Identitätsanbietern als
                                Kommunikationsweg zugeordnet.
                            </Typography>
                        ),
                    },
                }}
                tabs={[
                    {
                        path: '/communication-providers/:id',
                        label: 'Konfiguration',
                    },
                    {
                        path: '/communication-providers/:id/test',
                        label: 'Testen',
                        onlyExisting: true,
                        requiredPermission: Permission.COMMUNICATION_PROVIDER_UPDATE,
                    },
                    {
                        path: '/communication-providers/:id/identity-providers',
                        label: 'Verknüpfte Identitätsanbieter',
                        onlyExisting: true,
                        requiredPermission: Permission.IDENTITY_PROVIDER_READ,
                    },
                ]}
                initializeItem={() => new CommunicationProvidersApiService().initializeProvider()}
                fetchData={(_, id) => new CommunicationProvidersApiService().retrieveProvider(Number(id))}
                fetchAdditionalData={{
                    definitions: () => new CommunicationProvidersApiService().listDefinitions(),
                }}
                getTabTitle={(provider) => provider.id === 0
                    ? 'Neuer Kommunikationsanbieter'
                    : provider.name}
                getHeaderTitle={(provider, isNewItem, notFound) => {
                    if (notFound) return 'Kommunikationsanbieter nicht gefunden';
                    if (isNewItem) return 'Neuen Kommunikationsanbieter anlegen';
                    return `Kommunikationsanbieter: ${provider?.name ?? 'Unbenannt'}`;
                }}
                parentLink={{
                    label: 'Liste der Kommunikationsanbieter',
                    to: '/communication-providers',
                }}
            />
        </PageWrapper>
    );
}
