import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {Typography} from '@mui/material';
import {GenericDetailsPage} from '../../../../components/generic-details-page/generic-details-page';
import PaymentOutlinedIcon from '@mui/icons-material/PaymentOutlined';
import {PaymentProvidersApiService} from '../../../../modules/payment/payment-providers-api-service';
import {Api} from '../../../../hooks/use-api';
import {PaymentProviderAdditionalData} from './payment-provider-details-page-additional-data';
import {PaymentProviderResponseDTO} from '../../../../modules/payment/dtos/payment-provider-response-dto';
import {useAdminGuard} from "../../../../hooks/use-admin-guard";

export function PaymentProviderDetailsPage() {
    useAdminGuard();
    return (
        <>
            <PageWrapper
                title="Zahlungsdienstleister bearbeiten"
                fullWidth
                background
            >
                <GenericDetailsPage<PaymentProviderResponseDTO, string, PaymentProviderAdditionalData>
                    header={{
                        icon: <PaymentOutlinedIcon />,
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
                                        Konfigurieren Sie hier Zahlungsdienstleister, die in Ihrer Gover-Instanz global verfügbar sein sollen.
                                        Die erforderlichen Konfigurationsdaten erhalten Sie vom Zahlungsdienstleister oder finden Sie in dessen Dokumentation.
                                    </Typography>
                                    <Typography
                                        variant="body1"
                                        paragraph
                                    >
                                        Es wird empfohlen, für jeden Zahlungsdienstleister sowohl eine produktive als auch eine vorproduktive Anbindung einzurichten, um Tests zu erleichtern.
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
                            isDisabled: (item) => item?.key === '',
                        },
                        {
                            path: '/payment-providers/:id/forms',
                            label: 'Formulare',
                            isDisabled: (item) => item?.key === '',
                        },
                    ]}
                    initializeItem={(api) => new PaymentProvidersApiService(api).initialize()}
                    fetchData={(api, id: string) => new PaymentProvidersApiService(api).retrieve(id)}
                    fetchAdditionalData={{
                        definitions: (api: Api, id: string) => new PaymentProvidersApiService(api).listDefinitions(),
                    }}
                    getTabTitle={(item: PaymentProviderResponseDTO) => {
                        if (item.key === '') {
                            return 'Neuer Zahlungsdienstleister';
                        } else {
                            return item.name;
                        }
                    }}
                    getHeaderTitle={(item, isNewItem, notFound) => {
                        if (notFound) return "Zahlungsdienstleister nicht gefunden";
                        if (isNewItem) return "Neuen Zahlungsdienstleister anlegen";
                        return `Zahlungsdienstleister: ${item?.name ?? "Unbenannt"}`;
                    }}
                    parentLink={{
                        label: "Liste der Zahlungsdienstleister",
                        to: "/payment-providers",
                    }}
                />
            </PageWrapper>
        </>
    );
}