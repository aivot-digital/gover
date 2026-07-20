import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
import {useNavigate} from 'react-router-dom';
import {EmptyDataListPlaceholder} from '../../../../components/empty-data-list-placeholder/empty-data-list-placeholder';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import AddOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Add';
import {Typography} from '@mui/material';
import EditOutlined from '@aivot/mui-material-symbols-400-n25-outlined/Edit';
import {PaymentProvidersApiService} from '../../payment-providers-api-service';
import {useCallback, useEffect, useMemo, useState} from 'react';
import {PaymentProviderDefinitionResponseDTO} from '../../dtos/payment-provider-definition-response-dto';
import {PaymentProviderResponseDTO} from '../../dtos/payment-provider-response-dto';
import Chip from '@mui/material/Chip';
import ScienceOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Science';
import {CellLink} from '../../../../components/cell-link/cell-link';
import {CellContentWrapper} from '../../../../components/cell-content-wrapper/cell-content-wrapper';
import {useAccessGuard} from '../../../../hooks/use-admin-guard';
import {ModuleIcons} from '../../../../shells/staff/data/module-icons';
import Visibility from '@aivot/mui-material-symbols-400-n25-outlined/Visibility';
import {GenericListPropsFetchOptions} from '../../../../components/generic-list/generic-list-props';

const apiService = new PaymentProvidersApiService();

export function PaymentProvidersListPage() {
    const navigate = useNavigate();
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
    }, []);

    const header = useMemo(() => ({
        icon: ModuleIcons.payment,
        title: 'Zahlungsdienstleister',
        actions: [
            {
                label: 'Neuer Zahlungsdienstleister',
                icon: <AddOutlinedIcon/>,
                to: '/payment-providers/new',
                variant: 'contained' as const,
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
    }), [hasAccess]);

    const fetchPaymentProviders = useCallback((options: GenericListPropsFetchOptions<PaymentProviderResponseDTO>) => {
        return new PaymentProvidersApiService()
            .list(options.page, options.size, options.sort, options.order, {name: options.search});
    }, []);

    const columnDefinitions = useMemo(() => [
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
            renderCell: (params: any) => (
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
            renderCell: (params: any) => {
                const providerName = definitions.find(def => (
                    def.key === params.row.providerKey &&
                    def.version === params.row.providerVersion
                ))?.name ?? params.row.providerKey;

                return (
                    <>
                        {`${providerName} (Version ${params.row.providerVersion})`}
                        {params.row.isTestProvider && <Chip label="Test"
                                                            color="warning"
                                                            variant="outlined"
                                                            size={'small'}
                                                            sx={{ml: 1}}/>}
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
            renderCell: (params: any) => (
                <>
                    {params.row.isEnabled ?
                        <Chip label="Aktiv"
                              color="success"
                              variant="outlined"
                              size={'small'}/>
                        :
                        <Chip label="Inaktiv"
                              color="default"
                              variant="outlined"
                              size={'small'}/>
                    }
                </>
            ),
        },
    ], [definitions, hasAccess]);

    const getRowIdentifier = useCallback((row: PaymentProviderResponseDTO) => row.key, []);

    const rowActions = useCallback((item: PaymentProviderResponseDTO) => [
        {
            icon: hasAccess ? <EditOutlined/> : <Visibility/>,
            to: `/payment-providers/${item.key}`,
            tooltip: hasAccess ? 'Konfiguration bearbeiten' : 'Konfiguration anzeigen',
        },
        {
            icon: <ScienceOutlinedIcon/>,
            to: `/payment-providers/${item.key}/test`,
            tooltip: 'Konfiguration testen',
        },
    ], [hasAccess]);

    return (
        <>
            <PageWrapper
                title="Zahlungsdienstleister"
                fullWidth
                background
            >
                <GenericListPage<PaymentProviderResponseDTO>
                    header={header}
                    searchLabel="Zahlungsdienstleister suchen"
                    searchPlaceholder="Name der Konfiguration eingeben…"
                    fetch={fetchPaymentProviders}
                    columnDefinitions={columnDefinitions}
                    getRowIdentifier={getRowIdentifier}
                    noDataPlaceholder={
                        <EmptyDataListPlaceholder
                            title="Noch keine Zahlungsdienstleister angelegt"
                            description="Zahlungsdienstleister binden Bezahlverfahren ein, damit Gebühren in Formularen und Vorgängen abgewickelt werden können."
                            addText={hasAccess ? "Neuen Zahlungsdienstleister anlegen" : undefined}
                            onAdd={hasAccess ? () => navigate('/payment-providers/new') : undefined}
                        />
                    }
                    noSearchResultsPlaceholder="Keine Zahlungsdienstleister gefunden"
                    rowActionsCount={2}
                    rowActions={rowActions}
                    defaultSortField="name"
                    disableFullWidthToggle={true}
                />
            </PageWrapper>
        </>
    );
}
