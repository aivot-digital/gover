import React, {useContext} from 'react';
import {GenericDetailsPageContext, GenericDetailsPageContextType} from '../../../../components/generic-details-page/generic-details-page-context';
import {GenericList} from '../../../../components/generic-list/generic-list';
import {Form} from '../../../../models/entities/form';
import {FormsApiService} from "../../../../modules/forms/forms-api-service";
import {GridColDef} from '@mui/x-data-grid';
import {EditOutlined} from "@mui/icons-material";
import {Box, Typography} from "@mui/material";
import {PaymentProviderResponseDTO} from '../../../../modules/payment/dtos/payment-provider-response-dto';
import {CellLink} from "../../../../components/cell-link/cell-link";

const columns: GridColDef<Form>[] = [
    {
        field: 'title',
        headerName: 'Titel des Formulars',
        flex: 2,
        renderCell: (params) => (
            <CellLink
                to={`/forms/${params.id}`}
                title={`Formular bearbeiten`}
            >
                {String(params.value)}
            </CellLink>
        )
    },
    {
        field: 'version',
        headerName: 'Version',
        flex: 1,
    },
];

export function PaymentProviderDetailsPageForms() {
    const {
        item,
    } = useContext(GenericDetailsPageContext) as GenericDetailsPageContextType<PaymentProviderResponseDTO, undefined>;

    if (item == null) {
        return null;
    }

    return (
        <Box>
            <Typography
                variant="h5"
                sx={{mt: 1.5, mb: 1}}
            >
                Formulare mit diesem Zahlungsdienstleister
            </Typography>

            <Typography sx={{mb: 3, maxWidth: 900}}>
                Eine Liste aller Formulare, die Online-Zahlungen mit dieser Konfiguration des Zahlungsdienstleisters entgegennehmen, bzw. mit ihr konfiguriert sind.
            </Typography>

            <GenericList<Form>
                disableFullWidthToggle={true}
                sx={{
                    mx: '-16px',
                    mb: '-16px',
                }}
                columnDefinitions={columns}
                defaultFilter="dev"
                fetch={(options) => {
                    return new FormsApiService(options.api)
                        .list(
                            options.page,
                            options.size,
                            options.sort,
                            options.order,
                            {
                                title: options.search,
                                paymentProvider: item.key,
                            },
                        );
                }}
                getRowIdentifier={(item) => item.id.toString()}
                searchLabel="Formular suchen"
                searchPlaceholder="Titel des Formulars eingeben…"
                defaultSortField="slug"
                rowMenuItems={[]}
                noDataPlaceholder="Keine Formulare vorhanden"
                loadingPlaceholder="Lade Formulare..."
                noSearchResultsPlaceholder="Keine Formulare gefunden"
                rowActions={(item: Form) => [{
                    icon: <EditOutlined />,
                    to: `/forms/${item.id}`,
                    tooltip: 'Formular bearbeiten',
                }]}
                preSearchElements={[]}
            />
        </Box>
    );
}