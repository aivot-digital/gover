import React, {useCallback, useMemo, useRef} from 'react';
import {Box, Chip, type SxProps, type Theme, Typography} from '@mui/material';
import {Link} from 'react-router-dom';
import DescriptionOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Description';
import Edit from '@aivot/mui-material-symbols-400-n25-outlined/Edit';
import Visibility from '@aivot/mui-material-symbols-400-n25-outlined/Visibility';
import OpenInNew from '@aivot/mui-material-symbols-400-n25-outlined/OpenInNew';
import ContentCopy from '@aivot/mui-material-symbols-400-n25-outlined/ContentCopy';
import {
    GenericListPage,
    type GenericListPagePermissionConfig,
    type GenericListPagePermissionState,
} from '../../../../components/generic-list-page/generic-list-page';
import {EmptyDataListPlaceholder} from '../../../../components/empty-data-list-placeholder/empty-data-list-placeholder';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {useAppSelector} from '../../../../hooks/use-app-selector';
import {selectMemberships} from '../../../../slices/user-slice';
import {FormsListPageHelp} from '../../components/forms-list-page-help';
import {
    type GenericListColDef,
    type GenericListPropsFetchOptions,
    type ListControlRef,
} from '../../../../components/generic-list/generic-list-props';
import {type GenericPageHeaderProps} from '../../../../components/generic-page-header/generic-page-header-props';
import {
    type FormOverviewItem,
    type FormOverviewMode,
    type FormTriggerSortField,
    FormTriggerApiService,
} from '../../services/form-trigger-api-service';
import {ModuleIcons} from '../../../../shells/staff/data/module-icons';
import {type Action} from '../../../../components/actions/actions-props';
import {formatInstantInApplicationTimeZone} from '../../../../utils/temporal-utils';
import {copyToClipboardText} from '../../../../utils/copy-to-clipboard';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
import {selectSystemConfigValue} from '../../../../slices/system-config-slice';
import {SystemConfigKeys} from '../../../../data/system-config-keys';
import {Permission} from '../../../../data/permissions/permission';

const ellipsizedTextSx: SxProps<Theme> = {
    display: 'block',
    maxWidth: '100%',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
};

const stackedCellSx: SxProps<Theme> = {
    display: 'flex',
    flexDirection: 'column',
    justifyContent: 'center',
    minWidth: 0,
    height: '100%',
};

const secondaryCellTextSx: SxProps<Theme> = {
    ...ellipsizedTextSx,
    lineHeight: 1.35,
    mt: 0.25,
};

const overviewFilters = [
    {
        label: 'Veröffentlicht',
        value: 'Published',
    },
    {
        label: 'In Bearbeitung',
        value: 'Drafted',
    },
];

const permissionCheck: GenericListPagePermissionConfig<FormOverviewItem> = {
    scope: {
        type: 'process',
        getResourceId: (item) => item.processId,
    },
    update: Permission.PROCESS_DEFINITION_UPDATE,
};

export function FormsListPage(): React.ReactElement {
    const dispatch = useAppDispatch();
    const memberships = useAppSelector(selectMemberships);
    const publicListingDisabled = useAppSelector(selectSystemConfigValue(
        SystemConfigKeys.provider.listingPage.disableProsunaListingPage,
    )) === 'true';
    const listControlRef = useRef<ListControlRef>(null);

    const header: GenericPageHeaderProps = useMemo(() => ({
        icon: <DescriptionOutlinedIcon />,
        title: 'Formulare',
        helpDialog: {
            title: 'Hilfe zu Formularen',
            tooltip: 'Hilfe anzeigen',
            content: <FormsListPageHelp />,
        },
    }), []);

    const fetch = useCallback(async (options: GenericListPropsFetchOptions<FormOverviewItem>) => {
        const view: FormOverviewMode = options.filter === 'Drafted' ? 'Drafted' : 'Published';

        return await new FormTriggerApiService().listOverview(
            options.page,
            options.size,
            view,
            options.search,
            options.sort as FormTriggerSortField | undefined,
            options.order,
        );
    }, []);

    const columns = useMemo<Array<GenericListColDef<FormOverviewItem>>>(() => [
        {
            field: 'formTitle',
            headerName: 'Formular',
            flex: 1.5,
            minWidth: 220,
            sortable: false,
            renderCell: (params) => (
                <Box sx={stackedCellSx}>
                    <Typography
                        component={Link}
                        to={`/form-triggers/${params.row.id}`}
                        title={params.row.formTitle}
                        variant="body2"
                        sx={{
                            ...ellipsizedTextSx,
                            'color': 'text.primary',
                            'textDecoration': 'none',
                            'fontWeight': 500,
                            'lineHeight': 1.35,
                            '&:hover': {
                                textDecoration: 'underline',
                                textDecorationColor: 'divider',
                                textUnderlineOffset: 3,
                            },
                        }}
                    >
                        {params.row.formTitle}
                    </Typography>
                    <Typography
                        variant="caption"
                        title={params.row.nodeName}
                        sx={[{
                            color: "text.secondary"
                        }, ...(Array.isArray(secondaryCellTextSx) ? secondaryCellTextSx : [secondaryCellTextSx])]}>
                        {params.row.nodeName}
                    </Typography>
                </Box>
            ),
        },
        {
            field: 'processTitle',
            headerName: 'Prozess',
            flex: 1.35,
            minWidth: 200,
            sortable: false,
            renderCell: (params) => (
                <Box sx={stackedCellSx}>
                    <Typography
                        component={Link}
                        to={`/processes/${params.row.processId}/versions/${params.row.processVersion}`}
                        title={params.row.processTitle}
                        variant="body2"
                        sx={{
                            ...ellipsizedTextSx,
                            'color': 'text.primary',
                            'textDecoration': 'none',
                            'lineHeight': 1.35,
                            '&:hover': {
                                textDecoration: 'underline',
                                textDecorationColor: 'divider',
                                textUnderlineOffset: 3,
                            },
                        }}
                    >
                        {params.row.processTitle}
                    </Typography>
                    <Typography
                        variant="caption"
                        sx={[{
                            color: "text.secondary"
                        }, ...(Array.isArray(secondaryCellTextSx) ? secondaryCellTextSx : [secondaryCellTextSx])]}>
                        Version {params.row.processVersion}
                    </Typography>
                </Box>
            ),
        },
        {
            field: 'availability',
            headerName: 'Bereitstellung',
            flex: 0.9,
            minWidth: 170,
            sortable: false,
            renderCell: (params) => {
                if (params.row.status === 'Drafted') {
                    return (
                        <Chip
                            label="In Bearbeitung"
                            title="In Bearbeitung"
                            size="small"
                            variant="outlined"
                        />
                    );
                }

                const visibleInListing = params.row.showOnFormIndexPage && !publicListingDisabled;
                const availabilityLabel = visibleInListing ? 'Im Formularverzeichnis' : 'Nur per Direktlink';
                return (
                    <Chip
                        label={availabilityLabel}
                        title={availabilityLabel}
                        size="small"
                        variant="outlined"
                    />
                );
            },
        },
        {
            field: 'updated',
            headerName: 'Stand',
            flex: 1,
            minWidth: 190,
            sortable: false,
            renderCell: (params) => {
                const isPublished = params.row.status === 'Published';
                const date = formatInstantInApplicationTimeZone(
                    isPublished ? params.row.published : params.row.updated,
                    'dd.MM.yyyy – HH:mm',
                );

                return (
                    <Box sx={stackedCellSx}>
                        <Typography variant="body2" noWrap sx={{lineHeight: 1.35}}>
                            {date != null ? `${date} Uhr` : 'Keine Angabe'}
                        </Typography>
                        <Typography
                            variant="caption"
                            noWrap
                            sx={{
                                color: "text.secondary",
                                lineHeight: 1.35,
                                mt: 0.25
                            }}>
                            {isPublished ? 'Veröffentlicht' : 'Zuletzt bearbeitet'}
                        </Typography>
                    </Box>
                );
            },
        },
    ], [publicListingDisabled]);

    const listContextElements = useMemo(() => [
        <Typography
            key="description"
            variant="body2"
            sx={{
                color: "text.secondary",
                maxWidth: 900
            }}>
            Formulare werden als Einstiegspunkte innerhalb von Prozessen erstellt.
            <br/>
            Diese Übersicht bündelt die
            aktuell veröffentlichten Formulare und Formulare in Bearbeitung.
        </Typography>,
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
                <EmptyDataListPlaceholder
                    title="Noch keiner Organisationseinheit zugeordnet"
                    description="Eine Administrator:in muss Sie einer Organisationseinheit zuordnen und Ihnen eine passende Domänenrolle zuweisen. Danach werden hier die Formulare Ihrer zugänglichen Prozesse angezeigt."
                />
            }
            {
                memberships != null && memberships.length > 0 &&
                <EmptyDataListPlaceholder
                    title="Keine Formulare in diesem Bereich"
                    description="Formulare entstehen als Formulareingänge innerhalb eines Prozesses und werden gemeinsam mit der jeweiligen Prozessversion veröffentlicht."
                />
            }
        </Box>
    ), [memberships]);

    const rowActions = useCallback((
        item: FormOverviewItem,
        permissions: GenericListPagePermissionState<FormOverviewItem>,
    ): Action[] => {
        const isPublished = item.status === 'Published';
        const canUpdate = permissions.canUpdate(item);

        return [
            {
                icon: <OpenInNew />,
                href: item.publicUrl ?? '',
                tooltip: 'Formular öffnen (in neuem Tab)',
                ariaLabel: 'Formular öffnen (in neuem Tab)',
                visible: isPublished && item.publicUrl != null,
            },
            {
                icon: <ContentCopy />,
                tooltip: 'Öffentlichen Link kopieren',
                ariaLabel: 'Öffentlichen Link kopieren',
                visible: isPublished && item.publicUrl != null,
                onClick: () => {
                    if (item.publicUrl == null) {
                        return;
                    }

                    void copyToClipboardText(item.publicUrl).then((copied) => {
                        if (copied) {
                            dispatch(showSuccessSnackbar('Der öffentliche Link wurde kopiert.'));
                        } else {
                            dispatch(showErrorSnackbar('Der öffentliche Link konnte nicht kopiert werden.'));
                        }
                    });
                },
            },
            {
                icon: !isPublished && canUpdate ? <Edit /> : <Visibility />,
                to: `/form-triggers/${item.id}`,
                tooltip: !isPublished && canUpdate ? 'Formular bearbeiten' : 'Formular im Editor ansehen',
                ariaLabel: !isPublished && canUpdate ? 'Formular bearbeiten' : 'Formular im Editor ansehen',
            },
            {
                icon: ModuleIcons.processes,
                to: `/processes/${item.processId}/versions/${item.processVersion}`,
                tooltip: 'Prozess ansehen',
                ariaLabel: 'Prozess ansehen',
            },
        ];
    }, [dispatch]);

    return (
        <PageWrapper
            title="Formulare"
            fullWidth
            background
        >
            <GenericListPage<FormOverviewItem>
                controlRef={listControlRef}
                defaultFilter="Published"
                filters={overviewFilters}
                header={header}
                searchLabel="Formulare suchen"
                searchPlaceholder="Formular, Prozess oder URL-Segment eingeben…"
                listContextElements={listContextElements}
                fetch={fetch}
                columnIcon={<DescriptionOutlinedIcon />}
                columnDefinitions={columns}
                getRowIdentifier={(row) => row.id.toString()}
                noDataPlaceholder={noDataPlaceholder}
                noSearchResultsPlaceholder="Keine Formulare gefunden"
                rowActionsCount={4}
                rowActions={rowActions}
                permissionCheck={permissionCheck}
                defaultSortField="id"
                disableFullWidthToggle
                rowHeight={68}
            />
        </PageWrapper>
    );
}
