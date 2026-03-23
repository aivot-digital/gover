import React, {useContext} from 'react';
import {GenericDetailsPageContext, GenericDetailsPageContextType} from '../../../../components/generic-details-page/generic-details-page-context';
import {GenericList} from '../../../../components/generic-list/generic-list';
import {GridColDef} from '@mui/x-data-grid';
import {EditOutlined} from '@mui/icons-material';
import {Box, Typography} from '@mui/material';
import {CellLink} from '../../../../components/cell-link/cell-link';
import type {Theme} from '../../models/theme';
import {FormStatusChip} from '../../../forms/components/form-status-chip';
import {VFormVersionWithDetailsService} from '../../../forms/services/v-form-version-with-details-api-service';
import {VFormVersionWithDetailsEntity} from '../../../forms/entities/v-form-version-with-details-entity';

const columns: GridColDef<VFormVersionWithDetailsEntity>[] = [
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

            <GenericList<VFormVersionWithDetailsEntity>
                disableFullWidthToggle={true}
                sx={{
                    mx: '-16px',
                    mb: '-16px',
                }}
                columnDefinitions={columns}
                defaultFilter="dev"
                fetch={(options) => {
                    return new VFormVersionWithDetailsService()
                        .list(
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
                rowActions={(item) => [{
                    icon: <EditOutlined />,
                    to: `/forms/${item.id}/${item.version}`,
                    tooltip: 'Formular anzeigen',
                }]}
                preSearchElements={[]}
            />
        </Box>
    );
}