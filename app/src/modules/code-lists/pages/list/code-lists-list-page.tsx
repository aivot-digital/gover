import React, {useCallback} from 'react';
import {useNavigate} from 'react-router-dom';
import Add from '@aivot/mui-material-symbols-400-n25-outlined/Add';
import Edit from '@aivot/mui-material-symbols-400-n25-outlined/Edit';
import Visibility from '@aivot/mui-material-symbols-400-n25-outlined/Visibility';
import FormatListBulleted from '@aivot/mui-material-symbols-400-n25-outlined/FormatListBulleted';
import {Typography} from '@mui/material';
import {
    GenericListPage,
    type GenericListPagePermissionConfig,
    type GenericListPagePermissionState,
} from '../../../../components/generic-list-page/generic-list-page';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {EmptyDataListPlaceholder} from '../../../../components/empty-data-list-placeholder/empty-data-list-placeholder';
import {CellLink} from '../../../../components/cell-link/cell-link';
import {CellContentWrapper} from '../../../../components/cell-content-wrapper/cell-content-wrapper';
import {GenericListColDef, GenericListPropsFetchOptions} from '../../../../components/generic-list/generic-list-props';
import {ModuleIcons} from '../../../../shells/staff/data/module-icons';
import {CodeListsApiService} from '../../code-lists-api-service';
import {CodeList} from '../../models/code-list';
import {CodeListSourceTypeLabels} from '../../enums/code-list-source-type';
import {CodeListStatusChip} from '../../components/code-list-status-chip';
import {Permission} from '../../../../data/permissions/permission';

const codeListsListPermissionCheck: GenericListPagePermissionConfig<CodeList> = {
    scope: {
        type: 'system',
    },
    read: Permission.CODE_LIST_READ,
    create: Permission.CODE_LIST_CREATE,
    update: Permission.CODE_LIST_UPDATE,
};

export function CodeListsListPage() {
    const navigate = useNavigate();

    const header = useCallback((permissions: GenericListPagePermissionState<CodeList>) => ({
        icon: ModuleIcons.codeLists,
        title: 'Codelisten',
        actions: [
            {
                label: 'Neue Codeliste',
                icon: <Add />,
                to: '/code-lists/new',
                variant: 'contained' as const,
                disabled: !permissions.canCreate,
                disabledTooltip: permissions.createDisabledTooltip,
            },
        ],
        helpDialog: {
            title: 'Hilfe zu Codelisten',
            tooltip: 'Hilfe anzeigen',
            content: (
                <>
                    <Typography>
                        Codelisten bündeln wiederverwendbare Auswahlwerte für Formulare, Prozesse und Schnittstellen.
                    </Typography>
                    <Typography sx={{mt: 2}}>
                        Jede Codeliste legt fest, welche Beschriftung Benutzer:innen angezeigt wird und welcher technische Wert gespeichert oder weitergegeben wird.
                    </Typography>
                    <Typography sx={{mt: 2}}>
                        Einträge können manuell gepflegt oder aus XRepository bzw. einer CSV-Datei synchronisiert werden.
                    </Typography>
                    <Typography sx={{mt: 2}}>
                        Da Codelisten in öffentlichen Formularen genutzt und über die öffentliche Codelisten-API ohne Anmeldung abgerufen werden können,
                        dürfen sie keine vertraulichen Informationen enthalten.
                    </Typography>
                </>
            ),
        },
    }), []);

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

    const columnDefinitions = useCallback((permissions: GenericListPagePermissionState<CodeList>): Array<GenericListColDef<CodeList>> => [
        {
            field: 'name',
            headerName: 'Name',
            flex: 1,
            renderCell: (params: any) => (
                <CellLink
                    to={`/code-lists/${encodeURIComponent(params.row.key)}`}
                    title={permissions.canUpdate(params.row) ? 'Codeliste bearbeiten' : 'Codeliste ansehen'}
                >
                    {String(params.value)}
                </CellLink>
            ),
        },
        {
            field: 'key',
            headerName: 'Schlüssel',
            flex: 1,
        },
        {
            field: 'sourceType',
            headerName: 'Quelle',
            flex: 1,
            valueGetter: (_: any, row: CodeList) => CodeListSourceTypeLabels[row.sourceType],
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
            renderCell: (params: any) => (
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
    ], []);

    const getRowIdentifier = useCallback((row: CodeList) => row.key, []);

    const rowActions = useCallback((item: CodeList, permissions: GenericListPagePermissionState<CodeList>) => {
        const canUpdateCodeList = permissions.canUpdate(item);

        return [
            {
                icon: canUpdateCodeList ? <Edit /> : <Visibility />,
                to: `/code-lists/${encodeURIComponent(item.key)}`,
                tooltip: canUpdateCodeList ? 'Codeliste bearbeiten' : 'Codeliste ansehen',
            },
            {
                icon: <FormatListBulleted />,
                to: `/code-lists/${encodeURIComponent(item.key)}/items`,
                tooltip: 'Einträge anzeigen',
            },
        ];
    }, []);

    const noDataPlaceholder = useCallback((permissions: GenericListPagePermissionState<CodeList>) => (
        <EmptyDataListPlaceholder
            title="Keine Codelisten vorhanden"
            description="Codelisten stellen wiederverwendbare Werte für Auswahlfelder und Prozesse bereit."
            addText="Neue Codeliste anlegen"
            onAdd={() => navigate('/code-lists/new')}
            addDisabled={!permissions.canCreate}
            addDisabledTooltip={permissions.createDisabledTooltip}
        />
    ), [navigate]);

    return (
        <PageWrapper
            title="Codelisten"
            fullWidth
            background
        >
            <GenericListPage<CodeList>
                header={header}
                permissionCheck={codeListsListPermissionCheck}
                searchLabel="Codeliste suchen"
                searchPlaceholder="Name der Codeliste eingeben..."
                fetch={fetchCodeLists}
                columnIcon={columnIcon}
                columnDefinitions={columnDefinitions}
                getRowIdentifier={getRowIdentifier}
                noDataPlaceholder={noDataPlaceholder}
                noSearchResultsPlaceholder="Keine Codelisten gefunden"
                rowActionsCount={2}
                rowActions={rowActions}
                defaultSortField="name"
                disableFullWidthToggle
            />
        </PageWrapper>
    );
}
