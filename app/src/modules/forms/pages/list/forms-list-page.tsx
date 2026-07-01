import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
import {EmptyDataListPlaceholder} from '../../../../components/empty-data-list-placeholder/empty-data-list-placeholder';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {Box, type SxProps, type Theme} from '@mui/material';
import React, {useCallback, useMemo, useRef} from 'react';
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import {useAppSelector} from '../../../../hooks/use-app-selector';
import {selectMemberships} from '../../../../slices/user-slice';
import Typography from '@mui/material/Typography';
import {FormsListPageHelp} from '../../components/forms-list-page-help';
import {
    GenericListColDef,
    GenericListPropsFetchOptions,
    ListControlRef,
} from '../../../../components/generic-list/generic-list-props';
import Edit from '@aivot/mui-material-symbols-400-outlined/dist/edit/Edit';
import {GenericPageHeaderProps} from '../../../../components/generic-page-header/generic-page-header-props';
import {FormTriggerApiService, FormTriggerListItem} from '../../services/form-trigger-api-service';
import {ModuleIcons} from '../../../../shells/staff/data/module-icons';
import {CellLink} from '../../../../components/cell-link/cell-link';
import {ProcessStatusChip} from '../../../process/components/process-status/process-status-chip';
import {Action} from '../../../../components/actions/actions-props';
import {ProcessStatus} from '../../../process/enums/process-status';
import ArrowForward from '@aivot/mui-material-symbols-400-outlined/dist/arrow-forward/ArrowForward';
import {AlertComponent} from '../../../../components/alert/alert-component';

const shrinkableCellLinkSx: SxProps<Theme> = {
    '& > span': {
        minWidth: 0,
    },
    '& .cell-link-text::after': {
        bottom: -2,
    },
};

const ellipsizedCellTextSx: SxProps<Theme> = {
    display: 'inline-block',
    maxWidth: '100%',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
    verticalAlign: 'middle',
};

const columns: GenericListColDef<FormTriggerListItem>[] = [
    {
        field: 'name',
        headerName: 'Name des Prozesselementes',
        flex: 1.4,
        valueGetter: (_, row) => {
            return row.node.name ?? 'Formulareingang';
        },
        renderCell: (params) => {
            const nodeName = params.row.node.name ?? 'Formulareingang';

            return (
                <CellLink
                    to={`/form-triggers/${params.row.node.id}`}
                    title={nodeName}
                    sx={shrinkableCellLinkSx}
                >
                    <Box
                        component="span"
                        sx={ellipsizedCellTextSx}
                    >
                        {nodeName}
                    </Box>
                </CellLink>
            );
        },
    },
    {
        field: 'processId',
        headerName: 'Prozess',
        flex: 1.4,
        sortable: false,
        valueGetter: (_, row) => {
            return row.process.internalTitle;
        },
        renderCell: (params) => {
            const processTitle = params.row.process.internalTitle;
            const processVersion = params.row.version.processVersion;

            return (
                <CellLink
                    to={`/processes/${params.row.process.id}/versions/${processVersion}`}
                    title={`${processTitle} (Version ${processVersion})`}
                    sx={shrinkableCellLinkSx}
                >
                    <Box
                        component="span"
                        sx={ellipsizedCellTextSx}
                    >
                        {processTitle}
                        <Box
                            component="span"
                            sx={{
                                color: 'text.secondary',
                                ml: 0.5,
                            }}
                        >
                            (Version {processVersion})
                        </Box>
                    </Box>
                </CellLink>
            );
        },
    },
    {
        field: 'status',
        headerName: 'Status',
        flex: 0.75,
        sortable: false,
        valueGetter: (_, row) => {
            return row.version.status;
        },
        renderCell: (params) => (
            <ProcessStatusChip
                status={params.row.version.status}
                size="small"
                variant="soft"
            />
        ),
    },
];

export function FormsListPage() {
    const memberships = useAppSelector(selectMemberships);

    const listControlRef = useRef<ListControlRef>(null);

    const header: GenericPageHeaderProps = useMemo(() => ({
        icon: <DescriptionOutlinedIcon/>,
        title: 'Formulare',
        helpDialog: {
            title: 'Hilfe zu Formularen',
            tooltip: 'Hilfe anzeigen',
            content: <FormsListPageHelp/>,
        },
    }), []);

    const fetch = useCallback(async (options: GenericListPropsFetchOptions<FormTriggerListItem>) => {
        return await new FormTriggerApiService()
            .list(options.page, options.size, options.sort as any, options.order, {
                name: options.search,
            });
    }, []);

    const columnIcon = useMemo(() => <DescriptionOutlinedIcon/>, []);

    const listContextElements = useMemo(() => [
        <AlertComponent
            color="info"
            text="Diese Übersicht zeigt alle Formulareingänge, die in Prozessen verwendet werden. Jeder Eintrag entspricht einem Formulareingang-Prozesselement in einer konkreten Prozessversion und führt zum dort verwendeten Formular."
        />,
    ], []);

    const noDataPlaceholder = useMemo(() => (
        <Box
            sx={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                textAlign: 'center',
                p: 4,
            }}
        >
            {
                (memberships == null || memberships.length === 0) &&
                <>
                    <Typography
                        variant="h5"
                        component="h2"
                    >
                        Noch keiner Organisationseinheit zugeordnet
                    </Typography>
                    <Typography>
                        Eine Administrator:in muss Sie einer Organisationseinheit zuordnen und Ihnen
                        eine Domänenrolle zuweisen.
                        Erst dann können Sie mit der Entwicklung von Formularen beginnen. Nach der
                        Zuweisung müssen Sie diese Seite ggf. einmal neu laden.
                    </Typography>
                </>
            }
            {
                memberships != null &&
                memberships.length > 0 &&
                <EmptyDataListPlaceholder
                    title="Keine Formulareingänge vorhanden"
                    description="Formulareingänge binden Formulare in Prozesse ein und übernehmen eingereichte Daten in den jeweiligen Vorgang."
                />
            }
        </Box>
    ), [memberships]);

    const rowActions = useCallback((item: FormTriggerListItem): Action[] => [
        {
            icon: <Edit/>,
            to: `/form-triggers/${item.node.id}`,
            tooltip: 'Formular bearbeiten',
            visible: item.version.status === ProcessStatus.Drafted,
        },
        {
            icon: <ArrowForward/>,
            to: `/form-triggers/${item.node.id}`,
            tooltip: 'Formular ansehen',
            visible: item.version.status !== ProcessStatus.Drafted,
        },
        {
            icon: ModuleIcons.processes,
            to: `/processes/${item.process.id}/versions/${item.version.processVersion}`,
            tooltip: 'Prozess ansehen',
        },
    ], []);

    return (
        <>
            <PageWrapper
                title="Formulare"
                fullWidth
                background
            >
                <GenericListPage<FormTriggerListItem>
                    controlRef={listControlRef}
                    defaultFilter="all"
                    header={header}
                    searchLabel="Prozesselemente vom Typ „Formulareingang“ suchen"
                    searchPlaceholder="Titel des Prozesselementes eingeben…"
                    listContextElements={listContextElements}
                    fetch={fetch}
                    columnIcon={columnIcon}
                    columnDefinitions={columns}
                    getRowIdentifier={getRowIdentifier}
                    noDataPlaceholder={noDataPlaceholder}
                    noSearchResultsPlaceholder="Keine Formulareingänge gefunden"
                    rowActionsCount={2}
                    rowActions={rowActions}
                    defaultSortField={'id' as any}
                    disableFullWidthToggle={true}
                />
            </PageWrapper>
        </>
    );
}

function getRowIdentifier(row: FormTriggerListItem): string {
    return row.node.id.toString();
}
