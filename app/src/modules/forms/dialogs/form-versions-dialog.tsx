import {useEffect, useMemo, useState} from 'react';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showErrorSnackbar} from '../../../slices/snackbar-slice';
import Box from '@mui/material/Box';
import Dialog from '@mui/material/Dialog';
import DialogContent from '@mui/material/DialogContent';
import Divider from '@mui/material/Divider';
import IconButton from '@mui/material/IconButton';
import ListItem from '@mui/material/ListItem';
import ListItemAvatar from '@mui/material/ListItemAvatar';
import ListItemText from '@mui/material/ListItemText';
import Skeleton from '@mui/material/Skeleton';
import Typography from '@mui/material/Typography';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import List from '@mui/material/List';
import {FormStatus, FormStatusIcons} from '../enums/form-status';
import {FormDetailsResponseDTO} from '../dtos/form-details-response-dto';
import {format} from 'date-fns/format';
import MoreVertOutlinedIcon from '@mui/icons-material/MoreVertOutlined';
import {FormStatusChip} from '../components/form-status-chip';
import {ExportApplicationDialog} from '../../../dialogs/application-dialogs/export-application-dialog/export-application-dialog';
import {downloadConfigFile} from '../../../utils/download-utils';
import {useConfirm} from '../../../providers/confirm-provider';
import {FormVersionsDialogRowMenu} from '../components/form-versions-dialog-row-menu';
import {FormVersionApiService} from '../form-versions-api-service';
import {withDelay} from '../../../utils/with-delay';
import {isApiError} from '../../../models/api-error';

interface FormVersionsDialogProps {
    formId: number;
    onClose: () => void;
    onNewDraft: (basis: FormDetailsResponseDTO) => void;
    onNewForm: (basis: FormDetailsResponseDTO) => void;
    onChange?: (formId: number) => void;
}

export function FormVersionsDialog(props: FormVersionsDialogProps) {
    const {
        formId,
        onClose,
        onNewDraft,
        onNewForm,
        onChange,
    } = props;

    const dispatch = useAppDispatch();

    const showConfirm = useConfirm();

    const [isLoading, setIsLoading] = useState(false);
    const [versions, setVersions] = useState<FormDetailsResponseDTO[]>([]);

    const [moreMenuAnchorEl, setMoreMenuAnchorEl] = useState<HTMLElement | null>(null);
    const [moreMenuForm, setMoreMenuForm] = useState<FormDetailsResponseDTO | null>(null);

    const handleCloseMoreMenu = () => {
        setMoreMenuAnchorEl(null);
        setMoreMenuForm(null);
    };

    const [versionToExport, setVersionToExport] = useState<FormDetailsResponseDTO | null>(null);

    useEffect(() => {
        setIsLoading(true);

        const formVersionsApi = new FormVersionApiService(formId);

        withDelay(
            formVersionsApi.listAllOrdered(
                'version',
                'DESC',
                {
                    formId: formId,
                },
            ),
            500,
        )
            .then(forms => {
                setVersions(forms.content);
            })
            .catch(error => {
                if (isApiError(error) && error.displayableToUser) {
                    dispatch(showErrorSnackbar(error.message));
                } else {
                    dispatch(showErrorSnackbar('Fehler beim Laden der Formular-Versionen'));
                }
                console.error(error);
            })
            .finally(() => {
                setIsLoading(false);
            });
    }, [formId]);

    const handleUseAsNewDraft = (item: FormDetailsResponseDTO) => {
        if (item.draftedVersion == null) {
            onNewDraft(item);
            return;
        }

        showConfirm({
            title: 'Als Entwurf verwenden',
            children: (
                <Typography>
                    Dieses Formular hat bereits eine Version in Bearbeitung (Version {item.draftedVersion}).
                    <br />
                    Möchten Sie trotzdem eine neue Entwurf-Version auf Basis dieser Version erstellen?
                    Bitte beachten Sie, die existierende Entwurf-Version wird mit dieser Version überschrieben.
                </Typography>
            ),
            isDestructive: false,
        }).then((confirmed) => {
            if (confirmed) {
                onNewDraft(item);
            }
        });
    };

    const handleUseAsNewForm = (item: FormDetailsResponseDTO) => {
        onNewForm(item);
        handleCloseMoreMenu();
    };

    const handleExportFormVersion = (item: FormDetailsResponseDTO) => {
        setVersionToExport(item);
        handleCloseMoreMenu();
    };

    const handleDeleteFormVersion = (item: FormDetailsResponseDTO) => {
        if (item.publishedVersion === item.version) {
            dispatch(showErrorSnackbar('Die veröffentlichte Version kann nicht gelöscht werden.'));
            return;
        }

        if (item.status === FormStatus.Drafted && item.versionCount < 2) {
            showConfirm({
                title: 'Formular-Version löschen',
                confirmButtonText: 'Ok',
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
            title: 'Formular-Version löschen',
            children: (
                <Typography>
                    Soll die Formular-Version {item.version} wirklich gelöscht werden?
                </Typography>
            ),
            isDestructive: true,
        })
            .then((confirmed) => {
                if (!confirmed) {
                    return;
                }

                const formVersionsApi = new FormVersionApiService(formId);

                formVersionsApi.destroy(item.version)
                    .then(() => {
                        setVersions((prev) => prev.filter(v => v.version !== item.version));
                        handleCloseMoreMenu();
                        if (onChange) {
                            onChange(formId);
                        }
                    })
                    .catch(error => {
                        if (isApiError(error) && error.displayableToUser) {
                            dispatch(showErrorSnackbar(error.message));
                        } else {
                            dispatch(showErrorSnackbar('Fehler beim Löschen der Formular-Version'));
                        }
                        console.error(error);
                    });
            });
    };

    return (
        <>
            <Dialog
                open={true}
                onClose={onClose}
                fullWidth={true}
            >
                <DialogTitleWithClose onClose={onClose}>
                    Versionen dieses Formulars
                </DialogTitleWithClose>
                <DialogContent>
                    {
                        isLoading &&
                        <>
                            <Skeleton
                                height={128}
                                width="100%"
                            />
                            <Divider />
                            <Skeleton
                                height={128}
                                width="100%"
                            />
                            <Divider />
                            <Skeleton
                                height={128}
                                width="100%"
                            />
                        </>
                    }
                    {
                        !isLoading &&
                        <List>
                            {
                                versions.map(ver => (
                                    <VersionListItem
                                        key={ver.version}
                                        item={ver}
                                        onMoreClick={(target, item) => {
                                            setMoreMenuAnchorEl(target);
                                            setMoreMenuForm(item);
                                        }}
                                    />
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
                    form={moreMenuForm}
                    onClose={handleCloseMoreMenu}
                    onReuseFormVersionAsDraft={handleUseAsNewDraft}
                    onReuseFormVersionAsNewForm={handleUseAsNewForm}
                    onExportFormVersion={handleExportFormVersion}
                    onDeleteFormVersion={handleDeleteFormVersion}
                />
            }

            <ExportApplicationDialog
                open={versionToExport != null}
                onCancel={() => {
                    setVersionToExport(null);
                }}
                onExport={() => {
                    if (versionToExport != null) {
                        downloadConfigFile(versionToExport);
                    }
                    setVersionToExport(null);
                }}
            />
        </>
    );
}

interface VersionListItemProps {
    item: FormDetailsResponseDTO;
    onMoreClick: (target: HTMLButtonElement, item: FormDetailsResponseDTO) => void;
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
    } = item;

    const subtext = useMemo(() => {
        const _format = (val: string | null | undefined) => {
            const fallback = updated != null ? new Date(updated) : new Date();
            return format(val ?? fallback, 'dd.MM.yyyy HH:mm') + ' Uhr';
        };

        switch (status) {
            case FormStatus.Drafted:
                return `Zuletzt bearbeitet: ${_format(updated)}`;
            case FormStatus.Published:
                return `Veröffentlicht: ${_format(published)}`;
            case FormStatus.Revoked:
                return `Formular zurückgezogen: ${_format(revoked)}`;
            default:
                return '';
        }
    }, [status, updated, revoked, published]);

    return (
        <ListItem
            secondaryAction={
                <IconButton
                    edge="end"
                    onClick={(event) => {
                        onMoreClick(event.currentTarget, item);
                    }}
                >
                    <MoreVertOutlinedIcon />
                </IconButton>
            }
        >
            <ListItemAvatar>
                {FormStatusIcons[status]}
            </ListItemAvatar>
            <ListItemText
                primary={
                    <Box
                        sx={{
                            display: 'flex',
                            alignItems: 'baseline',
                        }}
                    >
                        <Typography
                            variant="h5"
                            component="span"
                            sx={{
                                mr: 'auto',
                            }}
                        >
                            Version {version}
                        </Typography>

                        <FormStatusChip
                            status={status}
                            size="small"
                            variant="outlined"
                        />
                    </Box>
                }
                secondary={subtext}
            />
        </ListItem>
    );
}
