import React, {useCallback, useMemo, useRef, useState} from 'react';
import {Box, Typography} from '@mui/material';
import {Link} from 'react-router-dom';
import DescriptionOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Description';
import Edit from '@aivot/mui-material-symbols-400-n25-outlined/Edit';
import Visibility from '@aivot/mui-material-symbols-400-n25-outlined/Visibility';
import OpenInNew from '@aivot/mui-material-symbols-400-n25-outlined/OpenInNew';
import MoreVert from '@aivot/mui-material-symbols-400-n25-outlined/MoreVert';
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
    type FetchListFilterCounts,
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
import {showApiErrorSnackbar, showErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
import {clearLoadingMessage, setLoadingMessage} from '../../../../slices/shell-slice';
import {selectSystemConfigValue} from '../../../../slices/system-config-slice';
import {SystemConfigKeys} from '../../../../data/system-config-keys';
import {Permission} from '../../../../data/permissions/permission';
import {downloadBlobFile} from '../../../../utils/download-utils';
import {downloadQrCode} from '../../../../utils/download-qrcode';
import {FormsListRowMenu} from '../../components/forms-list-row-menu';
import {resolvePrintablePdfFilename} from '../../utils/printable-pdf-filename';
import {CellContentWrapper} from '../../../../components/cell-content-wrapper/cell-content-wrapper';

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
    read: Permission.PROCESS_DEFINITION_READ,
    update: Permission.PROCESS_DEFINITION_UPDATE,
};

export function FormsListPage(): React.ReactElement {
    const dispatch = useAppDispatch();
    const memberships = useAppSelector(selectMemberships);
    const publicListingDisabled = useAppSelector(selectSystemConfigValue(
        SystemConfigKeys.provider.listingPage.disableProsunaListingPage,
    )) === 'true';
    const listControlRef = useRef<ListControlRef>(null);
    const [rowMenu, setRowMenu] = useState<{
        anchorEl: HTMLElement;
        form: FormOverviewItem;
        canDownloadPrintablePdf: boolean;
    } | null>(null);

    const copyPublicLink = useCallback((form: FormOverviewItem) => {
        if (form.publicUrl == null) {
            return;
        }

        void copyToClipboardText(form.publicUrl).then((copied) => {
            dispatch(copied
                ? showSuccessSnackbar('Der öffentliche Link wurde kopiert.')
                : showErrorSnackbar('Der öffentliche Link konnte nicht kopiert werden.'));
        }).catch(() => {
            dispatch(showErrorSnackbar('Der öffentliche Link konnte nicht kopiert werden.'));
        });
    }, [dispatch]);

    const downloadPublicQrCode = useCallback(async (form: FormOverviewItem) => {
        if (form.publicUrl == null) {
            return;
        }

        try {
            await downloadQrCode(form.publicUrl, `qr-code-${form.id}.png`);
            dispatch(showSuccessSnackbar('Der QR-Code wurde heruntergeladen.'));
        } catch (error) {
            dispatch(showErrorSnackbar('Der QR-Code konnte nicht heruntergeladen werden.'));
        }
    }, [dispatch]);

    const downloadPrintablePdf = useCallback(async (form: FormOverviewItem) => {
        dispatch(setLoadingMessage({
            blocking: false,
            estimatedTime: 1500,
            message: 'Vordruck wird generiert',
        }));

        try {
            const blob = await new FormTriggerApiService().downloadPrintablePdf(form.id);
            downloadBlobFile(resolvePrintablePdfFilename(form.formTitle, form.nodeName), blob);
            dispatch(showSuccessSnackbar('Der Vordruck wurde erstellt und der Download gestartet.'));
        } catch (error) {
            dispatch(showApiErrorSnackbar(error, 'Der Vordruck konnte nicht erstellt werden.'));
        } finally {
            dispatch(clearLoadingMessage());
        }
    }, [dispatch]);

    const header: GenericPageHeaderProps = useMemo(() => ({
        icon: <DescriptionOutlinedIcon />,
        title: 'Formulare',
        helpDialog: {
            title: 'Hilfe zu Formularen',
            tooltip: 'Hilfe anzeigen',
            content: <FormsListPageHelp />,
        },
    }), []);

    const fetchFilterCounts = useCallback<FetchListFilterCounts>(
        ({signal}) => new FormTriggerApiService().overviewCounts(signal),
        [],
    );

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
            field: 'icon',
            headerName: '',
            width: 24,
            sortable: false,
            disableColumnMenu: true,
            renderCell: () => (
                <CellContentWrapper sx={{alignItems: 'start', py: 1}}>
                    <DescriptionOutlinedIcon />
                </CellContentWrapper>
            ),
        },
        {
            field: 'formTitle',
            headerName: 'Formular',
            flex: 1.5,
            minWidth: 220,
            sortable: false,
            renderCell: (params) => {
                const secondaryLabel = params.row.formSlug == null
                    ? params.row.nodeName
                    : `${params.row.nodeName} · /${params.row.formSlug}`;

                return (
                    <Box sx={{py: 1, minWidth: 0, width: '100%'}}>
                        <Typography
                            variant="h5"
                            noWrap
                            sx={{mb: 0.5, fontSize: '1rem'}}
                        >
                            <Link
                                style={{color: 'inherit', textDecoration: 'none'}}
                                to={`/form-triggers/${params.row.id}`}
                                title={params.row.formTitle}
                            >
                                {params.row.formTitle}
                            </Link>
                        </Typography>
                        <Typography
                            variant="body2"
                            noWrap
                            title={secondaryLabel}
                            color="textSecondary"
                            sx={{
                                mt: -0.75,
                                fontSize: '0.875rem',
                                lineHeight: '1.5rem',
                            }}
                        >
                            {secondaryLabel}
                        </Typography>
                    </Box>
                );
            },
        },
        {
            field: 'processTitle',
            headerName: 'Prozess',
            flex: 1.35,
            minWidth: 200,
            sortable: false,
            renderCell: (params) => (
                <Box sx={{py: 1, minWidth: 0, width: '100%'}}>
                    <Typography
                        variant="h5"
                        noWrap
                        sx={{mb: 0.5, fontSize: '1rem'}}
                    >
                        <Link
                            style={{color: 'inherit', textDecoration: 'none'}}
                            to={`/processes/${params.row.processId}/versions/${params.row.processVersion}`}
                            title={params.row.processTitle}
                        >
                            {params.row.processTitle}
                        </Link>
                    </Typography>
                    <Typography
                        variant="body2"
                        noWrap
                        title={`Version ${params.row.processVersion}`}
                        color="textSecondary"
                        sx={{mt: -0.75, fontSize: '0.875rem', lineHeight: '1.5rem'}}
                    >
                        Version {params.row.processVersion}
                    </Typography>
                </Box>
            ),
        },
        {
            field: 'status',
            headerName: 'Status',
            flex: 0.9,
            minWidth: 190,
            sortable: false,
            renderCell: (params) => {
                const isPublished = params.row.status === 'Published';
                const availabilityLabel = params.row.showOnFormIndexPage && !publicListingDisabled
                    ? 'Im Formularverzeichnis'
                    : 'Nur per Direktlink';

                return (
                    <Box sx={{py: 1, display: 'flex', flexDirection: 'column', minWidth: 0, width: '100%'}}>
                        <Typography
                            noWrap
                            title={isPublished ? 'Veröffentlicht' : 'In Bearbeitung'}
                            sx={{fontSize: '0.875rem'}}
                        >
                            {isPublished ? 'Veröffentlicht' : 'In Bearbeitung'}
                        </Typography>
                        {isPublished && (
                            <Typography
                                noWrap
                                color="textSecondary"
                                title={availabilityLabel}
                                sx={{fontSize: '0.875rem'}}
                            >
                                {availabilityLabel}
                            </Typography>
                        )}
                    </Box>
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
                    <Box sx={{py: 1, display: 'flex', flexDirection: 'column', minWidth: 0, width: '100%'}}>
                        <Typography noWrap title={date != null ? `${date} Uhr` : 'Keine Angabe'} sx={{fontSize: '0.875rem'}}>
                            {date != null ? `${date} Uhr` : 'Keine Angabe'}
                        </Typography>
                        <Typography
                            noWrap
                            color="textSecondary"
                            title={isPublished ? 'Veröffentlicht am' : 'Zuletzt bearbeitet'}
                            sx={{fontSize: '0.875rem'}}
                        >
                            {isPublished ? 'Veröffentlicht am' : 'Zuletzt bearbeitet'}
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
            Diese zentrale Übersicht zeigt veröffentlichte Formulare und Formularentwürfe, auf die Sie Zugriff haben.
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
                icon: !isPublished && canUpdate ? <Edit /> : <Visibility />,
                to: `/form-triggers/${item.id}`,
                tooltip: !isPublished && canUpdate ? 'Formular im Editor bearbeiten' : 'Formular im Editor ansehen',
                ariaLabel: !isPublished && canUpdate ? 'Formular im Editor bearbeiten' : 'Formular im Editor ansehen',
            },
            {
                icon: ModuleIcons.processes,
                to: `/processes/${item.processId}/versions/${item.processVersion}`,
                tooltip: 'Prozess ansehen',
                ariaLabel: 'Prozess ansehen',
            },
            {
                icon: <MoreVert />,
                tooltip: 'Weitere Optionen',
                ariaLabel: 'Weitere Optionen',
                visible: (isPublished && item.publicUrl != null) || permissions.canRead(item),
                onClick: (event) => {
                    setRowMenu({
                        anchorEl: event.currentTarget as HTMLElement,
                        form: item,
                        canDownloadPrintablePdf: permissions.canRead(item),
                    });
                },
            },
        ];
    }, []);

    return (
        <>
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
                    fetchFilterCounts={fetchFilterCounts}
                    columnDefinitions={columns}
                    getRowIdentifier={(row) => row.id.toString()}
                    noDataPlaceholder={noDataPlaceholder}
                    noSearchResultsPlaceholder="Keine Formulare gefunden"
                    rowActionsCount={4}
                    rowActions={rowActions}
                    permissionCheck={permissionCheck}
                    defaultSortField="id"
                    disableFullWidthToggle
                    dynamicRowHeight
                />
            </PageWrapper>
            {rowMenu != null && (
                <FormsListRowMenu
                    anchorEl={rowMenu.anchorEl}
                    form={rowMenu.form}
                    canDownloadPrintablePdf={rowMenu.canDownloadPrintablePdf}
                    onClose={() => setRowMenu(null)}
                    onCopyPublicLink={copyPublicLink}
                    onDownloadQrCode={(form) => { void downloadPublicQrCode(form); }}
                    onDownloadPrintablePdf={(form) => { void downloadPrintablePdf(form); }}
                />
            )}
        </>
    );
}
