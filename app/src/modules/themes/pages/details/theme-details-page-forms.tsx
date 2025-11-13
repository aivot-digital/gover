import React, {useContext} from 'react';
import {GenericDetailsPageContext, GenericDetailsPageContextType} from '../../../../components/generic-details-page/generic-details-page-context';
import {GenericList} from '../../../../components/generic-list/generic-list';
import {FormsApiService} from '../../../forms/forms-api-service';
import {GridColDef} from '@mui/x-data-grid';
import {EditOutlined} from '@mui/icons-material';
import {Box, Typography} from '@mui/material';
import {CellLink} from '../../../../components/cell-link/cell-link';
import type {Theme} from '../../models/theme';
import {FormDetailsResponseDTO} from '../../../forms/dtos/form-details-response-dto';
import {FormStatusChip} from '../../../forms/components/form-status-chip';

const columns: GridColDef<FormDetailsResponseDTO>[] = [
    {
        field: 'internalTitle',
        headerName: 'Titel des Formulars',
        flex: 2,
        renderCell: (params) => (
            <CellLink
                to={`/forms/${params.id}/${params.row.version}`}
                title="Formular anzeigen"
            >
                {String(params.value)}
            </CellLink>
        ),
    },
    {
        field: 'version',
        headerName: 'Version',
        flex: 1,
    },
    {
        field: 'status',
        headerName: 'Status',
        flex: 1,
        renderCell: (params) => (
            <FormStatusChip
                status={params.row.status}
                size="small"
                variant="outlined"
            />
        ),
    },
];

export function ThemeDetailsPageForms() {
    const {
        item,
    } = useContext(GenericDetailsPageContext) as GenericDetailsPageContextType<Theme, undefined>;

    if (item == null) {
        return null;
    }

    return (
        <Box>
            <Typography
                variant="h5"
                sx={{mt: 1.5, mb: 1}}
            >
                Formulare mit diesem Farbschema
            </Typography>

            <Typography sx={{mb: 2, maxWidth: 900}}>
                Eine Liste aller Formulare, die dieses Farbschema für ihr optisches Erscheinungsbild verwenden.
            </Typography>

            <GenericList<FormDetailsResponseDTO>
                disableFullWidthToggle={true}
                sx={{
                    mx: '-16px',
                    mb: '-16px',
                }}
                columnDefinitions={columns}
                defaultFilter="dev"
                fetch={(options) => {
                    return new FormsApiService(options.api)
                        .listVersions(
                            options.page,
                            options.size,
                            options.sort,
                            options.order,
                            {
                                internalTitle: options.search,
                                themeId: item.id ?? undefined,
                            },
                        );
                }}
                getRowIdentifier={(item) => `${item.id}_${item.version}`}
                searchLabel="Formular suchen"
                searchPlaceholder="Titel des Formulars eingeben…"
                defaultSortField="internalTitle"
                rowMenuItems={[]}
                noDataPlaceholder="Keine Formulare vorhanden"
                loadingPlaceholder="Lade Formulare…"
                noSearchResultsPlaceholder="Keine Formulare gefunden"
                rowActions={(item: FormDetailsResponseDTO) => [{
                    icon: <EditOutlined />,
                    to: `/forms/${item.id}/${item.version}`,
                    tooltip: 'Formular anzeigen',
                }]}
                preSearchElements={[]}
            />
        </Box>
    );
}