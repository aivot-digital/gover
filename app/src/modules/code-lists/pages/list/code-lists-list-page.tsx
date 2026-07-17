import React, {useCallback, useMemo} from 'react';
import {useNavigate} from 'react-router-dom';
import Add from '@aivot/mui-material-symbols-400-n25-outlined/Add';
import Edit from '@aivot/mui-material-symbols-400-n25-outlined/Edit';
import Visibility from '@aivot/mui-material-symbols-400-n25-outlined/Visibility';
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
        title: 'Codelisten',
        actions: [
            {
                label: 'Neue Codeliste',
                icon: <Add />,
                to: '/code-lists/new',
                variant: 'contained' as const,
                disabled: !hasAccess,
            },
        ],
        helpDialog: {
            title: 'Hilfe zu Codelisten',
            tooltip: 'Hilfe anzeigen',
            content: (
                <>
                    <Typography>
                        Codelisten verwalten wiederverwendbare Werte und Beschriftungen für Formulare und Prozesse.
                    </Typography>
                    <Typography sx={{mt: 2}}>
                        Manuelle Listen werden direkt gepflegt. Listen aus XRepository oder CSV-Dateien werden aus ihrer Quelle synchronisiert.
                    </Typography>
                    <Typography sx={{mt: 2}}>
                        Auswahlwerte von Codelisten können in öffentlichen Formularen verwendet und über die
                        öffentliche Codelisten-API ohne Anmeldung abgerufen werden. Hinterlegen Sie daher keine
                        vertraulichen Informationen.
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
                    title={hasAccess ? 'Codeliste bearbeiten' : 'Codeliste ansehen'}
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
            width: 280,
            renderCell: (params) => (
                <CellContentWrapper>
                    <CodeListStatusChip
                        status={params.row.status}
                        sourceType={params.row.sourceType}
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
            icon: hasAccess ? <Edit /> : <Visibility />,
            to: `/code-lists/${item.id}`,
            tooltip: hasAccess ? 'Codeliste bearbeiten' : 'Codeliste ansehen',
        },
    ], [hasAccess]);

    return (
        <PageWrapper
            title="Codelisten"
            fullWidth
            background
        >
            <GenericListPage<CodeList>
                header={header}
                searchLabel="Codeliste suchen"
                searchPlaceholder="Name der Codeliste eingeben..."
                fetch={fetchCodeLists}
                columnIcon={columnIcon}
                columnDefinitions={columnDefinitions}
                getRowIdentifier={getRowIdentifier}
                noDataPlaceholder={
                    <EmptyDataListPlaceholder
                        title="Noch keine Codelisten angelegt"
                        description="Codelisten stellen wiederverwendbare Werte für Auswahlfelder und Prozesse bereit."
                        addText={hasAccess ? 'Neue Codeliste anlegen' : undefined}
                        onAdd={hasAccess ? () => navigate('/code-lists/new') : undefined}
                    />
                }
                noSearchResultsPlaceholder="Keine Codelisten gefunden"
                rowActionsCount={1}
                rowActions={rowActions}
                defaultSortField="name"
                disableFullWidthToggle
            />
        </PageWrapper>
    );
}
