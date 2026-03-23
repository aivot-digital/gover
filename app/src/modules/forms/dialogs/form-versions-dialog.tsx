import React, {useEffect, useMemo, useState} from 'react';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar, showErrorSnackbar} from '../../../slices/snackbar-slice';
import Box from '@mui/material/Box';
import Dialog from '@mui/material/Dialog';
import DialogContent from '@mui/material/DialogContent';
import Divider from '@mui/material/Divider';
import ListItem from '@mui/material/ListItem';
import Skeleton from '@mui/material/Skeleton';
import Typography from '@mui/material/Typography';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import List from '@mui/material/List';
import {FormStatus, FormVersionStatusIcons} from '../enums/form-status';
import {format} from 'date-fns/format';
import MoreVertOutlinedIcon from '@mui/icons-material/MoreVertOutlined';
import {FormStatusChip} from '../components/form-status-chip';
import {downloadObjectFile} from '../../../utils/download-utils';
import {useConfirm} from '../../../providers/confirm-provider';
import {FormVersionsDialogRowMenu} from '../components/form-versions-dialog-row-menu';
import {withDelay} from '../../../utils/with-delay';
import {ApiError, isApiError} from '../../../models/api-error';
import {Link} from 'react-router-dom';
import {Actions} from '../../../components/actions/actions';
import Edit from '@aivot/mui-material-symbols-400-outlined/dist/edit/Edit';
import Visibility from '@aivot/mui-material-symbols-400-outlined/dist/visibility/Visibility';
import {LoadingOverlay} from '../../../components/loading-overlay/loading-overlay';
import NewWindow from '@aivot/mui-material-symbols-400-outlined/dist/new-window/NewWindow';
import {FormApiService} from '../services/form-api-service';
import {FormEntity} from '../entities/form-entity';
import {FormVersionEntity} from '../entities/form-version-entity';
import {FormVersionApiService} from '../services/form-version-api-service';
import {setLoadingMessage} from '../../../slices/shell-slice';
import {ExportFormDialog} from './export-form-dialog';

interface FormVersionsDialogProps {
    form: FormEntity;
    onClose: () => void;
    onNewDraft: (basis: {
        form: FormEntity;
        version: FormVersionEntity;
    }) => void;
    onNewForm: (basis: {
        form: FormEntity;
        version: FormVersionEntity;
    }) => void;
    onShouldReload?: (form: FormEntity) => void;
}

interface FormVersionWithEditor extends FormVersionEntity {
    editorFullName?: string;
}

export function FormVersionsDialog(props: FormVersionsDialogProps) {
    const {
        form,
        onClose,
        onNewDraft,
        onNewForm,
        onShouldReload,
    } = props;

    const dispatch = useAppDispatch();
    const showConfirm = useConfirm();

    const [isLoading, setIsLoading] = useState(false);
    const [isBusy, setIsBusy] = useState(false);

    const [versions, setVersions] = useState<FormVersionWithEditor[]>([]);

    const hasDraft = useMemo(() => {
        return versions.some(v => v.status === FormStatus.Drafted);
    }, [versions]);

    const publishedVersion = useMemo(() => {
        return versions.find(v => v.status === FormStatus.Published)?.version ?? null;
    }, [versions]);

    const latestVersion = useMemo(() => {
        return versions.length > 0 ? versions[0].version : null;
    }, [versions]);

    const [moreMenuAnchorEl, setMoreMenuAnchorEl] = useState<HTMLElement | null>(null);
    const [moreMenuForm, setMoreMenuForm] = useState<FormVersionEntity | null>(null);

    const handleCloseMoreMenu = () => {
        setMoreMenuAnchorEl(null);
        setMoreMenuForm(null);
    };

    const [versionToExport, setVersionToExport] = useState<number | null>(null);

    async function loadVersions(): Promise<void> {
        const formsApi = new FormApiService();
        const formVersionsApi = new FormVersionApiService();

        try {
            const [editors, formVersionsPage] = await Promise
                .all([
                    formsApi.listEditorsForForm(form.id),
                    formVersionsApi.listAllOrdered(
                        'version',
                        'DESC',
                        {
                            formId: form.id,
                        }),
                ]);
            const versionsWithEditors = formVersionsPage
                .content
                .map((version) => ({
                    ...version,
                    editorFullName: editors.find(e => e.formVersion === version.version)?.fullName,
                }));
            setVersions(versionsWithEditors);
        } catch (error) {
            dispatch(showApiErrorSnackbar(error, 'Fehler beim Laden der Formularversionen'));
        }
    }

    async function loadVersion(version: number): Promise<FormVersionWithEditor> {
        setIsBusy(true);
        await withDelay(loadVersions(), 500);
        setIsBusy(false);

        const item = versions
            .find(v => v.version === version);

        if (!item) {
            const error: ApiError = {
                status: 404,
                message: 'Die ausgewählte Formularversion wurde nicht gefunden.',
                details: null,
                displayableToUser: true,
            };
            throw error;
        }

        return item;
    }

    useEffect(() => {
        setIsLoading(true);
        withDelay(loadVersions(), 600)
            .finally(() => {
                setIsLoading(false);
            });
    }, [form]);

    async function handleUseAsNewDraft(version: number): Promise<void> {
        let item: FormVersionWithEditor;
        try {
            item = await loadVersion(version);
        } catch (error) {
            if (isApiError(error) && error.displayableToUser) {
                dispatch(showErrorSnackbar(error.message));
            } else {
                console.error(error);
                dispatch(showErrorSnackbar('Fehler beim Laden der Formularversion'));
            }
            return;
        }

        if (form.draftedVersion == null) {
            onNewDraft({
                form: form,
                version: item,
            });
            return;
        }

        const confirmed = await showConfirm({
            title: 'Bestehenden Entwurf überschreiben?',
            children: (
                <Typography>
                    Für dieses Formular existiert bereits eine Arbeitsversion (Version {form.draftedVersion}).
                    Möchten Sie dennoch einen neuen Entwurf auf Basis dieser Version erstellen?
                    Die bestehende Arbeitsversion wird dabei überschrieben.
                </Typography>
            ),
            isDestructive: false,
        });

        if (confirmed) {
            onNewDraft({
                form: form,
                version: item,
            });
        }
    }

    const handleNewDraft = (version: number) => {
        showConfirm({
            title: 'Neuen Entwurf anlegen?',
            confirmButtonText: 'Ja, Entwurf anlegen',
            children: (
                <Box>
                    Für dieses Formular existiert derzeit kein aktiver Entwurf.
                    Möchten Sie einen neuen Entwurf (Arbeitsversion) für dieses Formular anlegen um diesen zu bearbeiten?
                </Box>
            ),
        }).then((confirmed) => {
            if (confirmed) {
                handleUseAsNewDraft(version);
            }
        });
    };

    async function handleUseAsNewForm(version: number): Promise<void> {
        let item: FormVersionWithEditor;
        try {
            item = await loadVersion(version);
        } catch (error) {
            dispatch(showApiErrorSnackbar(error, 'Fehler beim Laden der Formularversion'));
            return;
        }

        onNewForm({
            form: form,
            version: item,
        });

        handleCloseMoreMenu();
    }

    const handleExportFormVersion = (version: number) => {
        dispatch(setLoadingMessage({
            message: 'Formular wird exportiert',
            blocking: true,
            estimatedTime: 1000,
        }));

        withDelay(new FormApiService()
            .export(form.id, version))
            .then((exp) => {
                downloadObjectFile(`${exp.form.slug} - ${exp.version.version}.json`, exp);
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Fehler beim Exportieren der Formularversion'));
            })
            .finally(() => {
                dispatch(setLoadingMessage(undefined));
            });

        handleCloseMoreMenu();
    };

    async function handleDeleteFormVersion(version: number): Promise<void> {
        let item: FormVersionWithEditor;
        try {
            item = await loadVersion(version);
        } catch (error) {
            if (isApiError(error) && error.displayableToUser) {
                dispatch(showErrorSnackbar(error.message));
            } else {
                console.error(error);
                dispatch(showErrorSnackbar('Fehler beim Laden der Formularversion'));
            }
            return;
        }

        if (form.publishedVersion === item.version) {
            dispatch(showErrorSnackbar('Die veröffentlichte Version kann nicht gelöscht werden.'));
            return;
        }

        if (item.status === FormStatus.Drafted && form.versionCount < 2) {
            showConfirm({
                title: 'Formularversion löschen',
                hideCancelButton: true,
                children: (
                    <Typography>
                        Die letzte Version eines Formulars kann nicht gelöscht werden.
                        <br />
                        Bitte löschen Sie stattdessen das gesamte Formular.
                    </Typography>
                ),
            });
            return;
        }

        showConfirm({
            title: 'Formularversion löschen',
            children: (
                <Typography>
                    Soll die Formularversion {item.version} wirklich gelöscht werden?
                </Typography>
            ),
            isDestructive: true,
        })
            .then((confirmed) => {
                if (!confirmed) {
                    return;
                }

                const formVersionsApi = new FormVersionApiService();

                formVersionsApi
                    .destroy({
                        formId: form.id,
                        version: item.version,
                    })
                    .then(() => {
                        loadVersions();
                        handleCloseMoreMenu();
                        if (onShouldReload != null) {
                            onShouldReload(form);
                        }
                    })
                    .catch(error => {
                        if (isApiError(error) && error.displayableToUser) {
                            dispatch(showErrorSnackbar(error.message));
                        } else {
                            dispatch(showErrorSnackbar('Fehler beim Löschen der Formularversion'));
                        }
                        console.error(error);
                    });
            });
    }

    return (
        <>
            <Dialog
                open={true}
                onClose={onClose}
                fullWidth={true}
            >
                <DialogTitleWithClose
                    onClose={onClose}
                    actions={[
                        {
                            label: 'Entwurf anlegen',
                            icon: <NewWindow />,
                            onClick: () => latestVersion != null && handleNewDraft(publishedVersion ?? latestVersion),
                            variant: 'text',
                            visible: !hasDraft && !isLoading,
                        },
                    ]}
                >
                    Versionen dieses Formulars
                </DialogTitleWithClose>
                <DialogContent>
                    <LoadingOverlay
                        isLoading={isBusy}
                        message="Liste der Versionen wird aktualisiert"
                    />

                    {
                        isLoading && (
                            <List
                                role="status"
                                aria-live="polite"
                                aria-busy="true"
                                sx={{'& .MuiDivider-root:last-of-type': {display: 'none'}}}
                            >
                                {Array.from({length: 4}).map((_, i) => (
                                    <React.Fragment key={i}>
                                        <VersionListItemSkeleton />
                                        <Divider sx={{my: 1.5}} />
                                    </React.Fragment>
                                ))}
                            </List>
                        )
                    }
                    {
                        !isLoading &&
                        <List sx={{'& .MuiDivider-root:last-of-type': {display: 'none'}}}>
                            {
                                versions.map(ver => (
                                    <React.Fragment key={ver.version}>
                                        <VersionListItem
                                            item={ver}
                                            onMoreClick={(target, item) => {
                                                setMoreMenuAnchorEl(target);
                                                setMoreMenuForm(item);
                                            }}
                                        />
                                        <Divider sx={{my: 1.5}} />
                                    </React.Fragment>
                                ))
                            }
                        </List>
                    }
                </DialogContent>
            </Dialog>

            {
                moreMenuAnchorEl != null &&
                moreMenuForm != null &&
                <FormVersionsDialogRowMenu
                    anchorEl={moreMenuAnchorEl}
                    form={form}
                    version={moreMenuForm}
                    onClose={handleCloseMoreMenu}
                    onReuseFormVersionAsDraft={handleUseAsNewDraft}
                    onReuseFormVersionAsNewForm={handleUseAsNewForm}
                    onExportFormVersion={(version) => {
                        setVersionToExport(version);
                    }}
                    onDeleteFormVersion={handleDeleteFormVersion}
                />
            }

            <ExportFormDialog
                open={versionToExport != null}
                onCancel={() => {
                    setVersionToExport(null);
                }}
                onExport={() => {
                    if (versionToExport != null) {
                        handleExportFormVersion(versionToExport);
                    }
                    setVersionToExport(null);
                }}
            />
        </>
    );
}

interface VersionListItemProps {
    item: FormVersionWithEditor;
    onMoreClick: (target: HTMLButtonElement, item: FormVersionWithEditor) => void;
}


function VersionListItem(props: VersionListItemProps) {
    const {
        item,
        onMoreClick,
    } = props;

    const {
        version,
        status,
        updated,
        published,
        revoked,
        formId,
        editorFullName,
    } = item;

    const subtext = useMemo(() => {
        const _format = (val: string | null | undefined) => {
            const fallback = updated != null ? new Date(updated) : new Date();
            return format(val ?? fallback, 'dd.MM.yyyy – HH:mm') + ' Uhr';
        };

        switch (status) {
            case FormStatus.Drafted:
                return `Zuletzt bearbeitet: ${_format(updated)}\nBearbeitet von: ${editorFullName ?? 'Unbekannte Nutzer:in'}`;
            case FormStatus.Published:
                return `Veröffentlicht am: ${_format(published)}\nVeröffentlicht von: ${editorFullName ?? 'Unbekannte Nutzer:in'}`;
            case FormStatus.Revoked:
                return `Zurückgezogen am: ${_format(revoked)}\nZurückgezogen von: ${editorFullName ?? 'Unbekannte Nutzer:in'}`;
            default:
                return '';
        }
    }, [status, updated, revoked, published, editorFullName]);

    return (
        <ListItem
            sx={{
                px: 0,
                display: 'flex',
                alignItems: 'start',
            }}
        >
            <Box sx={{width: 20, textAlign: 'center', mr: 2.5}}>
                {FormVersionStatusIcons[status]}
            </Box>
            <Box sx={{flexGrow: '1'}}>
                <Box sx={{display: 'flex', alignItems: 'center', gap: 2, flexWrap: 'nowrap'}}>
                    <Typography
                        variant="h5"
                    >
                        <Link
                            style={{color: 'inherit', textDecoration: 'none'}}
                            to={`/forms/${formId}/${version}`}
                            title={'Formular bearbeiten'}
                        >
                            Version {version}
                        </Link>
                    </Typography>
                    {(status === FormStatus.Drafted || status === FormStatus.Published) && (
                        <Box sx={{ml: 'auto', mr: 0.5}}>
                            <FormStatusChip
                                status={status}
                                size="small"
                                variant="soft"
                            />
                        </Box>
                    )}
                </Box>
                <Typography
                    color="text.secondary"
                    sx={{mt: 0.5}}
                >
                    {subtext}
                </Typography>
            </Box>

            <Box sx={{ml: 2, display: 'flex', alignItems: 'center', gap: 2, transform: 'translateY(-4px)'}}>
                <Actions
                    actions={[
                        {
                            icon: <Edit />,
                            to: `/forms/${formId}/${version}`,
                            tooltip: 'Version bearbeiten',
                            visible: !item.published && !revoked,
                        },
                        {
                            icon: <Visibility />,
                            to: `/forms/${formId}/${version}`,
                            tooltip: 'Version ansehen',
                            visible: !!(published || revoked),
                        },
                        {
                            icon: <MoreVertOutlinedIcon />,
                            onClick: (event) => {
                                onMoreClick(event.currentTarget as HTMLButtonElement, item);
                            },
                            tooltip: 'Optionen',
                        },
                    ]}
                    sx={{
                        justifyContent: 'end',
                    }}
                    dense
                />
            </Box>

        </ListItem>
    );
}

function VersionListItemSkeleton() {
    return (
        <ListItem
            sx={{
                px: 0,
                display: 'flex',
                alignItems: 'start',
            }}
        >
            <Box sx={{width: 24, textAlign: 'center', mr: 2}}>
                <Skeleton
                    variant="circular"
                    width={24}
                    height={24}
                    sx={{mt: 0.5}}
                />
            </Box>

            <Box sx={{flex: '1 1 auto', minWidth: 0}}>
                <Box sx={{display: 'flex', alignItems: 'center', gap: 2, flexWrap: 'nowrap'}}>
                    <Skeleton
                        variant="text"
                        width={'40%'}
                        height={28}
                    />
                    <Box sx={{ml: 'auto'}}>
                        <Skeleton
                            variant="rectangular"
                            width={92}
                            height={24}
                            sx={{borderRadius: 999}}
                        />
                    </Box>
                </Box>

                <Skeleton
                    variant="text"
                    width={'35%'}
                    height={20}
                    sx={{mt: 0.5, mr: 0.5}}
                />
            </Box>

            <Box sx={{ml: 2, display: 'flex', alignItems: 'center', gap: 1.5, flexShrink: 0}}>
                <Skeleton
                    variant="circular"
                    width={28}
                    height={28}
                />
                <Skeleton
                    variant="circular"
                    width={28}
                    height={28}
                />
            </Box>
        </ListItem>
    );
}
