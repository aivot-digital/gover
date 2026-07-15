import React, {useCallback, useMemo} from 'react';
import {useNavigate} from 'react-router-dom';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import Visibility from '@aivot/mui-material-symbols-400-outlined/dist/visibility/Visibility';
import {Typography} from '@mui/material';
import {GridColDef} from '@mui/x-data-grid';
import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {EmptyDataListPlaceholder} from '../../../../components/empty-data-list-placeholder/empty-data-list-placeholder';
import {CellLink} from '../../../../components/cell-link/cell-link';
import {CellContentWrapper} from '../../../../components/cell-content-wrapper/cell-content-wrapper';
import {GenericListPropsFetchOptions} from '../../../../components/generic-list/generic-list-props';
import {useAccessGuard} from '../../../../hooks/use-admin-guard';
import {ModuleIcons} from '../../../../shells/staff/data/module-icons';
import {CodeListsApiService} from '../../code-lists-api-service';
import {CodeList} from '../../models/code-list';
import {CodeListSourceTypeLabels} from '../../enums/code-list-source-type';
import {CodeListStatusChip} from '../../components/code-list-status-chip';

export function CodeListsListPage() {
    const navigate = useNavigate();
    const hasAccess = useAccessGuard({
        onlyGlobalAdmin: true,
        messageType: 'snackbar',
    });

    const header = useMemo(() => ({
        icon: ModuleIcons.codeLists,
        title: 'Code-Listen',
        actions: [
            {
                label: 'Neue Code-Liste',
                icon: <AddOutlinedIcon />,
                to: '/code-lists/new',
                variant: 'contained' as const,
                disabled: !hasAccess,
            },
        ],
        helpDialog: {
            title: 'Hilfe zu Code-Listen',
            tooltip: 'Hilfe anzeigen',
            content: (
                <>
                    <Typography>
                        Code-Listen verwalten wiederverwendbare Werte und Beschriftungen für Formulare und Prozesse.
                    </Typography>
                    <Typography sx={{mt: 2}}>
                        Manuelle Listen werden direkt gepflegt. Listen aus XRepository oder CSV-Dateien werden aus ihrer Quelle synchronisiert.
                    </Typography>
                    <Typography sx={{mt: 2}}>
                        Alle Code-Listen sind öffentlich verfügbar und dürfen keine vertraulichen Informationen enthalten.
                    </Typography>
                </>
            ),
        },
    }), [hasAccess]);

    const fetchCodeLists = useCallback((options: GenericListPropsFetchOptions<CodeList>) => {
        return new CodeListsApiService()
            .list(
                options.page,
                options.size,
                options.sort,
                options.order,
                {
                    name: options.search,
                },
            );
    }, []);

    const columnIcon = useCallback(() => ModuleIcons.codeLists, []);

    const columnDefinitions = useMemo<Array<GridColDef<CodeList>>>(() => [
        {
            field: 'name',
            headerName: 'Name',
            flex: 1,
            renderCell: (params) => (
                <CellLink
                    to={`/code-lists/${params.row.id}`}
                    title={hasAccess ? 'Code-Liste bearbeiten' : 'Code-Liste ansehen'}
                >
                    {String(params.value)}
                </CellLink>
            ),
        },
        {
            field: 'sourceType',
            headerName: 'Quelle',
            flex: 1,
            valueGetter: (_: any, row) => CodeListSourceTypeLabels[row.sourceType],
        },
        {
            field: 'description',
            headerName: 'Beschreibung',
            flex: 2,
        },
        {
            field: 'status',
            headerName: 'Status',
            width: 210,
            renderCell: (params) => (
                <CellContentWrapper>
                    <CodeListStatusChip
                        status={params.row.status}
                        statusMessage={params.row.statusMessage}
                        lastSync={params.row.lastSync}
                    />
                </CellContentWrapper>
            ),
        },
    ], [hasAccess]);

    const getRowIdentifier = useCallback((row: CodeList) => row.id.toString(), []);

    const rowActions = useCallback((item: CodeList) => [
        {
            icon: hasAccess ? <EditOutlinedIcon /> : <Visibility />,
            to: `/code-lists/${item.id}`,
            tooltip: hasAccess ? 'Code-Liste bearbeiten' : 'Code-Liste ansehen',
        },
    ], [hasAccess]);

    return (
        <PageWrapper
            title="Code-Listen"
            fullWidth
            background
        >
            <GenericListPage<CodeList>
                header={header}
                searchLabel="Code-Liste suchen"
                searchPlaceholder="Name der Code-Liste eingeben..."
                fetch={fetchCodeLists}
                columnIcon={columnIcon}
                columnDefinitions={columnDefinitions}
                getRowIdentifier={getRowIdentifier}
                noDataPlaceholder={
                    <EmptyDataListPlaceholder
                        title="Noch keine Code-Listen angelegt"
                        description="Code-Listen stellen wiederverwendbare Werte für Auswahlfelder und Prozesse bereit."
                        addText={hasAccess ? 'Neue Code-Liste anlegen' : undefined}
                        onAdd={hasAccess ? () => navigate('/code-lists/new') : undefined}
                    />
                }
                noSearchResultsPlaceholder="Keine Code-Listen gefunden"
                rowActionsCount={1}
                rowActions={rowActions}
                defaultSortField="name"
                disableFullWidthToggle
            />
        </PageWrapper>
    );
}
