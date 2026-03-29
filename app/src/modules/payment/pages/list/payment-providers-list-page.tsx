import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import {Typography} from '@mui/material';
import {EditOutlined} from '@mui/icons-material';
import {useApi} from '../../../../hooks/use-api';
import {PaymentProvidersApiService} from '../../payment-providers-api-service';
import {useEffect, useMemo, useState} from 'react';
import {PaymentProviderDefinitionResponseDTO} from '../../dtos/payment-provider-definition-response-dto';
import {PaymentProviderResponseDTO} from '../../dtos/payment-provider-response-dto';
import Chip from '@mui/material/Chip';
import ScienceOutlinedIcon from '@mui/icons-material/ScienceOutlined';
import {CellLink} from '../../../../components/cell-link/cell-link';
import {CellContentWrapper} from '../../../../components/cell-content-wrapper/cell-content-wrapper';
import {useAccessGuard} from '../../../../hooks/use-admin-guard';
import {ModuleIcons} from '../../../../shells/staff/data/module-icons';
import Visibility from '@aivot/mui-material-symbols-400-outlined/dist/visibility/Visibility';

export function PaymentProvidersListPage() {
    const api = useApi();
    const apiService = useMemo(() => new PaymentProvidersApiService(api), [api]);

    const hasAccess = useAccessGuard({
        onlyGlobalAdmin: true,
        messageType: 'snackbar',
    });

    const [definitions, setDefinitions] = useState<PaymentProviderDefinitionResponseDTO[]>([]);

    useEffect(() => {
        apiService
            .listDefinitions()
            .then(setDefinitions)
            .catch(console.error);
    }, [apiService]);

    return (
        <>
            <PageWrapper
                title="Zahlungsdienstleister"
                fullWidth
                background
            >
                <GenericListPage<PaymentProviderResponseDTO>
                    header={{
                        icon: ModuleIcons.payment,
                        title: 'Zahlungsdienstleister',
                        actions: [
                            {
                                label: 'Neuer Zahlungsdienstleister',
                                icon: <AddOutlinedIcon />,
                                to: '/payment-providers/new',
                                variant: 'contained',
                                disabled: !hasAccess,
                            },
                        ],
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
                    searchLabel="Zahlungsdienstleister suchen"
                    searchPlaceholder="Name der Konfiguration eingeben…"
                    fetch={(options) => {
                        return new PaymentProvidersApiService(options.api)
                            .list(options.page, options.size, options.sort, options.order, {name: options.search});
                    }}
                    columnDefinitions={[
                        {
                            field: 'icon',
                            headerName: '',
                            renderCell: () => <CellContentWrapper>{ModuleIcons.payment}</CellContentWrapper>,
                            disableColumnMenu: true,
                            width: 24,
                            sortable: false,
                        },
                        {
                            field: 'name',
                            headerName: 'Name der Konfiguration',
                            flex: 1,
                            renderCell: (params) => (
                                <CellLink
                                    to={`/payment-providers/${params.id}`}
                                    title={hasAccess ? 'Konfiguration bearbeiten' : 'Konfiguration anzeigen'}
                                >
                                    {String(params.value)}
                                </CellLink>
                            ),
                        },
                        {
                            field: 'providerKey',
                            headerName: 'Anbieter',
                            flex: 1,
                            renderCell: (params) => {
                                const providerName = definitions.find(def => (
                                    def.key === params.row.providerKey &&
                                    def.version === params.row.providerVersion
                                ))?.name ?? params.row.providerKey;

                                return (
                                    <>
                                        {`${providerName} (Version ${params.row.providerVersion})`}
                                        {params.row.isTestProvider && <Chip label="Test" color="warning" variant="outlined" size={"small"} sx={{ml:1}}/>}
                                    </>
                                );
                            },
                        },
                        {
                            field: 'description',
                            headerName: 'Beschreibung',
                            flex: 2,
                        },
                        {
                            field: 'isEnabled',
                            headerName: 'Status',
                            renderCell: (params) => (
                                <>
                                    {params.row.isEnabled ?
                                        <Chip label="Aktiv" color="success" variant="outlined" size={"small"}/>
                                        :
                                        <Chip label="Inaktiv" color="default" variant="outlined" size={"small"}/>
                                    }
                                </>
                            ),
                        },
                    ]}
                    getRowIdentifier={row => row.key}
                    noDataPlaceholder="Keine Zahlungsdienstleister angelegt"
                    noSearchResultsPlaceholder="Keine Zahlungsdienstleister gefunden"
                    rowActionsCount={2}
                    rowActions={(item: PaymentProviderResponseDTO) => [
                        {
                            icon: hasAccess ? <EditOutlined /> : <Visibility/>,
                            to: `/payment-providers/${item.key}`,
                            tooltip: hasAccess ? 'Konfiguration bearbeiten' : 'Konfiguration anzeigen',
                        },
                        {
                            icon: <ScienceOutlinedIcon />,
                            to: `/payment-providers/${item.key}/test`,
                            tooltip: 'Konfiguration testen',
                        }]}
                    defaultSortField="name"
                    disableFullWidthToggle={true}
                />
            </PageWrapper>
        </>
    );
}
