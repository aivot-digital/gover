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
                title="Zahlungsanbieter bearbeiten"
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
                        title: 'Zahlungsanbieter bearbeiten',
                        helpDialog: {
                            title: 'Hilfe zu Zahlungsanbietern',
                            tooltip: 'Hilfe anzeigen',
                            content: (
                                <>
                                    <Typography
                                        variant="body1"
                                        sx={{
                                            marginBottom: "16px"
                                        }}
                                    >
                                        Konfigurieren Sie hier Zahlungsanbieter, die in Ihrer Prosuna-Instanz global
                                        verfügbar sein sollen.
                                        Die erforderlichen Konfigurationsdaten erhalten Sie vom Zahlungsdienstleister
                                        oder finden Sie in dessen Dokumentation.
                                    </Typography>
                                    <Typography
                                        variant="body1"
                                        sx={{
                                            marginBottom: "16px"
                                        }}
                                    >
                                        Es wird empfohlen, für jeden Zahlungsanbieter sowohl eine produktive als
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
                            return 'Neuer Zahlungsanbieter';
                        } else {
                            return item.name;
                        }
                    }}
                    getHeaderTitle={(item, isNewItem, notFound) => {
                        if (notFound) return 'Zahlungsanbieter nicht gefunden';
                        if (isNewItem) return 'Neuen Zahlungsanbieter anlegen';
                        return `Zahlungsanbieter: ${item?.name ?? 'Unbenannt'}`;
                    }}
                    parentLink={{
                        label: 'Liste der Zahlungsanbieter',
                        to: '/payment-providers',
                    }}
                    entityType={ServerEntityType.PaymentProviders}
                />
            </PageWrapper>
        </>
    );
}
