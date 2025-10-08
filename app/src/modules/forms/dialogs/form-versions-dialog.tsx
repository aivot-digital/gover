import {useEffect, useMemo, useState} from 'react';
import {useApi} from '../../../hooks/use-api';
import {FormsApiService} from '../forms-api-service';
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
import Menu from '@mui/material/Menu';
import MenuItem from '@mui/material/MenuItem';
import Skeleton from '@mui/material/Skeleton';
import Typography from '@mui/material/Typography';
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
import {useConfirm} from '../../../providers/confirm-provider';

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

    const showConfirm = useConfirm();

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

    const handleUseAsNewDraft = (item: FormDetailsResponseDTO) => {
        if (item.draftedVersion == null) {
            onNewDraft(item);
            return;
        }

        showConfirm({
            title: 'Als Entwurf verwenden',
            children: (
                <Typography>
                    Dieses Formular hat bereits eine Version in Bearbeitung (Version {item.draftedVersion}).<br />
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
    }

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
                        moreMenu?.item.status === FormStatus.Drafted ? 'Diese Version Bearbeiten' : 'Diese Version Anzeigen'
                    }
                </MenuItem>

                <MenuItem
                    onClick={() => {
                        if (moreMenu == null) {
                            return;
                        }
                        handleUseAsNewDraft(moreMenu.item);
                        setMoreMenu(undefined);
                    }}
                    disabled={moreMenu?.item.status === FormStatus.Drafted}
                >
                    Neuen Entwurf auf Basis dieser Version erzeugen
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
