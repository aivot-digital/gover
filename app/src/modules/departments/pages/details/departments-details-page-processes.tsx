import React, {useContext} from 'react';
import {format} from 'date-fns/format';
import {Box, Typography} from '@mui/material';
import {GridColDef} from '@mui/x-data-grid';
import Edit from '@aivot/mui-material-symbols-400-n25-outlined/Edit';
import Visibility from '@aivot/mui-material-symbols-400-n25-outlined/Visibility';
import {EmptyDataListPlaceholder} from '../../../../components/empty-data-list-placeholder/empty-data-list-placeholder';
import {GenericDetailsPageContext, GenericDetailsPageContextType} from '../../../../components/generic-details-page/generic-details-page-context';
import {GenericList} from '../../../../components/generic-list/generic-list';
import {CellLink} from '../../../../components/cell-link/cell-link';
import {DepartmentEntity} from '../../entities/department-entity';
import {ProcessEntity} from '../../../process/entities/process-entity';
import {ProcessDefinitionApiService} from '../../../process/services/process-definition-api-service';
import {ProcessStatusChipGroup} from '../../../process/components/process-status/process-status-chip-group';

const filters = [
    {
        label: 'Alle Prozesse',
        value: 'all',
    },
    {
        label: 'Entwürfe',
        value: 'drafted',
    },
    {
        label: 'Veröffentlicht',
        value: 'published',
    },
    {
        label: 'Zurückgezogen',
        value: 'revoked',
    },
];

function getProcessDetailsPath(processId: number, version: number | null): string {
    if (version == null) {
        return '/processes';
    }

    return `/processes/${processId}/versions/${version}`;
}

const columns: GridColDef<ProcessEntity>[] = [
    {
        field: 'internalTitle',
        headerName: 'Titel des Prozesses',
        flex: 2,
        renderCell: (params) => (
            <CellLink
                to={getProcessDetailsPath(params.row.id, params.row.draftedVersion ?? params.row.publishedVersion)}
                title="Prozess anzeigen"
            >
                {String(params.value)}
            </CellLink>
        ),
    },
    {
        field: 'updated',
        headerName: 'Zuletzt bearbeitet',
        flex: 1,
        renderCell: (params) => (
            <>
                {format(params.row.updated, 'dd.MM.yyyy - HH:mm')} Uhr
            </>
        ),
    },
    {
        field: 'publishedVersion',
        headerName: 'Status',
        flex: 1,
        sortable: false,
        renderCell: (params) => (
            <ProcessStatusChipGroup process={params.row}/>
        ),
    },
];

export function DepartmentsDetailsPageProcesses() {
    const {
        item,
    } = useContext(GenericDetailsPageContext) as GenericDetailsPageContextType<DepartmentEntity, undefined>;

    if (item == null) {
        return null;
    }

    return (
        <Box>
            <Typography
                variant="h5"
                sx={{mt: 1.5, mb: 1}}
            >
                Prozesse der Organisationseinheit
            </Typography>

            <Typography sx={{mb: 3, maxWidth: 900}}>
                Eine Liste aller Prozesse, die von dieser Organisationseinheit verwaltet werden.
            </Typography>

            <GenericList<ProcessEntity>
                disableFullWidthToggle={true}
                sx={{
                    mx: '-16px',
                    mb: '-16px',
                }}
                columnDefinitions={columns}
                filters={filters}
                defaultFilter="all"
                fetch={(options) => {
                    return new ProcessDefinitionApiService()
                        .list(
                            options.page,
                            options.size,
                            options.sort as any,
                            options.order,
                            {
                                internalTitle: options.search,
                                departmentId: item.id,
                                isPublished: options.filter === 'published' ? true : undefined,
                                isDrafted: options.filter === 'drafted' ? true : undefined,
                                isRevoked: options.filter === 'revoked' ? true : undefined,
                            },
                        );
                }}
                getRowIdentifier={(item) => item.id.toString()}
                searchLabel="Prozess suchen"
                searchPlaceholder="Titel des Prozesses eingeben..."
                defaultSortField="internalTitle"
                rowMenuItems={[]}
                noDataPlaceholder={
                    <EmptyDataListPlaceholder
                        title="Keine Prozesse zugeordnet"
                        description="Diese Zuordnung zeigt, welche Prozesse von dieser Organisationseinheit verwaltet werden."
                    />
                }
                loadingPlaceholder="Lade Prozesse..."
                noSearchResultsPlaceholder="Keine Prozesse gefunden"
                rowActions={(item) => [
                    {
                        icon: <Edit />,
                        to: `/processes/${item.id}/versions/${item.draftedVersion}`,
                        tooltip: 'Prozess bearbeiten',
                        visible: item.draftedVersion != null,
                    },
                    {
                        icon: <Visibility />,
                        to: `/processes/${item.id}/versions/${item.publishedVersion}`,
                        tooltip: 'Prozess ansehen',
                        visible: item.draftedVersion == null && item.publishedVersion != null,
                    },
                ]}
                preSearchElements={[]}
            />
        </Box>
    );
}
