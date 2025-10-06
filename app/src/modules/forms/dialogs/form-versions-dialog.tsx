import React, {useEffect, useMemo, useState} from 'react';
import {useApi} from '../../../hooks/use-api';
import {FormsApiService} from '../forms-api-service';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../slices/snackbar-slice';
import {Box, Dialog, DialogContent, Divider, IconButton, ListItem, ListItemAvatar, ListItemText, Menu, MenuItem, Skeleton, Typography} from '@mui/material';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import List from '@mui/material/List';
import {withAsyncWrapper} from '../../../utils/with-async-wrapper';
import {Page} from '../../../models/dtos/page';
import {FormStatus, FormStatusIcons} from '../enums/form-status';
import {FormDetailsResponseDTO} from '../dtos/form-details-response-dto';
import {format} from 'date-fns/format';
import MoreVertOutlinedIcon from '@mui/icons-material/MoreVertOutlined';
import {Link} from 'react-router-dom';
import {Link as MuiLink} from '@mui/material';
import {FormStatusChip} from '../components/form-status-chip';
import {ExportApplicationDialog} from '../../../dialogs/application-dialogs/export-application-dialog/export-application-dialog';
import {downloadConfigFile} from '../../../utils/download-utils';

interface FormVersionsDialogProps {
    formId: number;
    onClose: () => void;
    onNewDraft: (basis: FormDetailsResponseDTO) => void;
}

export function FormVersionsDialog(props: FormVersionsDialogProps) {
    const {
        formId,
        onClose,
        onNewDraft,
    } = props;

    const api = useApi();
    const dispatch = useAppDispatch();

    const [isLoading, setIsLoading] = useState(false);
    const [versions, setVersions] = useState<FormDetailsResponseDTO[]>([]);
    const [moreMenu, setMoreMenu] = useState<{
        anchorEl: HTMLElement;
        item: FormDetailsResponseDTO;
    } | undefined>();

    const [versionToExport, setVersionToExport] = useState<FormDetailsResponseDTO | null>(null);

    useEffect(() => {
        setIsLoading(true);

        const formsApi = new FormsApiService(api);
        withAsyncWrapper<void, Page<FormDetailsResponseDTO>>({
            desiredMinRuntime: 500,
            main: () => formsApi.listVersions(
                0,
                999,
                'version',
                'DESC',
                {
                    formId: formId,
                },
            ),
        })
            .then(forms => {
                setVersions(forms.content);
            })
            .catch(error => {
                console.error(error);
                dispatch(showErrorSnackbar('Fehler beim Laden der Formular-Versionen'));
            })
            .finally(() => {
                setIsLoading(false);
            });
    }, [formId]);

    const handleFormDelete = (id: number, version: number) => {
        const originalVersions = [...versions];

        const versionsWithoutDeleted = versions.filter(v => !(v.id === id && v.version === version));
        setVersions(versionsWithoutDeleted);

        new FormsApiService(api)
            .destroy({
                id: id,
                version: version,
            })
            .then(() => {
                dispatch(showSuccessSnackbar('Formular-Version wurde gelöscht'));
            })
            .catch((error) => {
                console.error(error);
                dispatch(showErrorSnackbar('Fehler beim Löschen der Formular-Version'));
                setVersions(originalVersions);
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
                                            setMoreMenu({
                                                anchorEl: target,
                                                item: item,
                                            });
                                        }}
                                    />
                                ))
                            }
                        </List>
                    }
                </DialogContent>
            </Dialog>

            <Menu
                anchorEl={moreMenu?.anchorEl}
                open={moreMenu != null}
                onClose={() => setMoreMenu(undefined)}
            >
                <MenuItem
                    component={Link}
                    to={`/forms/${moreMenu?.item.id}/${moreMenu?.item.version}`}
                >
                    {
                        moreMenu?.item.status === FormStatus.Drafted ? 'Bearbeiten' : 'Anzeigen'
                    }
                </MenuItem>

                <MenuItem
                    onClick={() => {
                        if (moreMenu == null) {
                            return;
                        }
                        onNewDraft(moreMenu.item);
                        setMoreMenu(undefined);
                    }}
                >
                    Als Entwurf verwenden
                </MenuItem>

                <MenuItem
                    onClick={() => {
                        if (moreMenu == null) {
                            return;
                        }
                        setVersionToExport(moreMenu.item);
                        setMoreMenu(undefined);
                    }}
                >
                    Version exportieren
                </MenuItem>

                <MenuItem
                    component={MuiLink}
                    href={`/${moreMenu?.item.slug}/${moreMenu?.item.version}`}
                    target={'_blank'}
                >
                    Version im Vorschau-Modus aufrufen
                </MenuItem>

                <Divider />

                <MenuItem
                    disabled={moreMenu?.item.status !== FormStatus.Revoked}
                    onClick={() => {
                        if (moreMenu == null) {
                            return;
                        }
                        handleFormDelete(moreMenu.item.id, moreMenu.item.version);
                        setMoreMenu(undefined);
                    }}
                >
                    Version löschen
                </MenuItem>
            </Menu>

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
