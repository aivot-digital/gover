import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {Typography} from '@mui/material';
import {GenericDetailsPage} from '../../../../components/generic-details-page/generic-details-page';
import {PaymentProvidersApiService} from '../../payment-providers-api-service';
import {Api} from '../../../../hooks/use-api';
import {PaymentProviderAdditionalData} from './payment-provider-details-page-additional-data';
import {PaymentProviderResponseDTO} from '../../dtos/payment-provider-response-dto';
import {ServerEntityType} from '../../../../shells/staff/data/server-entity-type';
import {ModuleIcons} from '../../../../shells/staff/data/module-icons';
import {Permission} from '../../../../data/permissions/permission';

export function PaymentProviderDetailsPage() {
    return (
        <>
            <PageWrapper
                title="Zahlungsdienstleister bearbeiten"
                fullWidth
                background
            >
                <GenericDetailsPage<PaymentProviderResponseDTO, string, PaymentProviderAdditionalData>
                    permissionCheck={{
                        create: Permission.PAYMENT_PROVIDER_CREATE,
                        read: Permission.PAYMENT_PROVIDER_READ,
                        update: Permission.PAYMENT_PROVIDER_UPDATE,
                        scope: {
                            type: 'system',
                        },
                    }}
                    header={{
                        icon: ModuleIcons.payment,
                        title: 'Zahlungsdienstleister bearbeiten',
                        helpDialog: {
                            title: 'Hilfe zu Zahlungsdienstleistern',
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
                            path: '/payment-providers/:id',
                            label: 'Konfiguration',
                        },
                        {
                            path: '/payment-providers/:id/test',
                            label: 'Testen',
                            onlyExisting: true,
                            requiredPermission: Permission.PAYMENT_PROVIDER_UPDATE,
                        },
                        {
                            path: '/payment-providers/:id/tx',
                            label: 'Transaktionen',
                            onlyExisting: true,
                            requiredPermission: Permission.PAYMENT_PROVIDER_READ,
                        },
                    ]}
                    initializeItem={() => new PaymentProvidersApiService().initialize()}
                    fetchData={(api, id: string) => new PaymentProvidersApiService().retrieve(id)}
                    fetchAdditionalData={{
                        definitions: (api: Api, id: string) => new PaymentProvidersApiService().listDefinitions(),
                    }}
                    getTabTitle={(item: PaymentProviderResponseDTO) => {
                        if (item.key === '') {
                            return 'Neuer Zahlungsdienstleister';
                        } else {
                            return item.name;
                        }
                    }}
                    getHeaderTitle={(item, isNewItem, notFound) => {
                        if (notFound) return 'Zahlungsdienstleister nicht gefunden';
                        if (isNewItem) return 'Neuen Zahlungsdienstleister anlegen';
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
