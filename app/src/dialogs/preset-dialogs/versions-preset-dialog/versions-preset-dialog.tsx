import {type VersionsPresetDialogProps} from './versions-preset-dialog-props';
import {type DialogProps} from '@mui/material/Dialog';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {Box, Dialog, DialogContent, List, ListItem, ListItemButton, ListItemText, Skeleton, Typography} from '@mui/material';
import React, {useEffect, useMemo, useState} from 'react';
import {type PresetVersion} from '../../../models/entities/preset-version';
import {Link} from 'react-router-dom';
import {format, parseISO} from 'date-fns';
import {determinePresetVersionDescriptor} from '../../../utils/determine-preset-version-descriptor';
import {useApi} from '../../../hooks/use-api';
import {PresetVersionApiService} from '../../../modules/presets/preset-version-api-service';
import {FormStatus, FormVersionStatusIcons} from '../../../modules/forms/enums/form-status';
import Divider from '@mui/material/Divider';
import { FormStatusChip } from '../../../modules/forms/components/form-status-chip';
import type {Preset} from '../../../models/entities/preset';
import { Edit } from '@mui/icons-material';
import Visibility from '@aivot/mui-material-symbols-400-outlined/dist/visibility/Visibility';
import {Actions} from '../../../components/actions/actions';
import {withDelay} from '../../../utils/with-delay';
import {isApiError} from '../../../models/api-error';
import {showErrorSnackbar} from '../../../slices/snackbar-slice';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';

export function VersionsPresetDialog(props: DialogProps & VersionsPresetDialogProps) {
    const api = useApi();
    const dispatch = useAppDispatch();

    const [isLoading, setIsLoading] = useState(false);

    const {
        onClose,
        preset,

        ...passThroughProps
    } = props;

    const [versions, setVersions] = useState<PresetVersion[]>([]);

    useEffect(() => {
        setIsLoading(true);
        const presetVersionApiService = new PresetVersionApiService(api, preset.key);

        withDelay(
            presetVersionApiService
                .listAllOrdered('version', 'DESC'),
            500,
        )
            .then(page => {
                setVersions(page.content);
            })
            .catch(error => {
                setVersions([]);
                if (isApiError(error) && error.displayableToUser) {
                    dispatch(showErrorSnackbar(error.message));
                } else {
                    dispatch(showErrorSnackbar('Fehler beim Laden der Vorlagen-Versionen'));
                }
                console.error(error);
            })
            .finally(() => {
                setIsLoading(false);
            });
    }, [preset, passThroughProps.open]);

    return (
        <Dialog
            {...passThroughProps}
            fullWidth
            onClose={onClose}
        >
            <DialogTitleWithClose
                onClose={onClose}
                closeTooltip="Schließen"
            >
                Versionen dieser Vorlage
            </DialogTitleWithClose>
            <DialogContent tabIndex={0}>
                {
                    isLoading && (
                        <List
                            role="status"
                            aria-live="polite"
                            aria-busy="true"
                            sx={{ '& .MuiDivider-root:last-of-type': { display: 'none' } }}
                        >
                            {Array.from({ length: 4 }).map((_, i) => (
                                <React.Fragment key={i}>
                                    <VersionListItemSkeleton />
                                    <Divider sx={{ my: 1.5 }} />
                                </React.Fragment>
                            ))}
                        </List>
                    )
                }
                {
                    !isLoading &&
                    <List  sx={{'& .MuiDivider-root:last-of-type': {display: 'none'}}}>
                        {
                            versions.map(ver => (
                                <>
                                    <VersionListItem
                                        key={ver.version}
                                        item={ver}
                                    />
                                    <Divider sx={{my: 1.5}}/>
                                </>
                            ))
                        }
                    </List>
                }
            </DialogContent>
        </Dialog>
    );
}



interface VersionListItemProps {
    item: PresetVersion;
}


function VersionListItem(props: VersionListItemProps) {
    const {
        item,
    } = props;

    const {
        version,
        status,
        updated,
        published,
        revoked,
        presetKey,
    } = item;

    const subtext = useMemo(() => {
        const _format = (val: string | null | undefined) => {
            const fallback = updated != null ? new Date(updated) : new Date();
            return format(val ?? fallback, 'dd.MM.yyyy – HH:mm') + ' Uhr';
        };

        switch (status) {
            case FormStatus.Drafted:
                return `Zuletzt bearbeitet: ${_format(updated)}`;
            case FormStatus.Published:
                return `Veröffentlicht am: ${_format(published)}`;
            case FormStatus.Revoked:
                return `Zurückgezogen am: ${_format(revoked)}`;
            default:
                return '';
        }
    }, [status, updated, revoked, published]);

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
                            to={`/presets/edit/${item.presetKey}/${item.version}`}
                            title={'Vorlage bearbeiten'}
                        >
                            Version {version}
                        </Link>
                    </Typography>
                    {!revoked && (
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
                            to: `/presets/edit/${item.presetKey}/${item.version}`,
                            tooltip: 'Version bearbeiten',
                            visible: !item.published && !item.revoked,
                        },
                        {
                            icon: <Visibility />,
                            to: `/presets/edit/${item.presetKey}/${item.version}`,
                            tooltip: 'Version ansehen',
                            visible: !!(item.published || item.revoked),
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
            <Box sx={{ width: 24, textAlign: 'center', mr: 2 }}>
                <Skeleton variant="circular" width={24} height={24} sx={{mt: 0.5}}/>
            </Box>

            <Box sx={{ flex: '1 1 auto', minWidth: 0 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, flexWrap: 'nowrap' }}>
                    <Skeleton
                        variant="text"
                        width={'40%'}
                        height={28}
                    />
                    <Box sx={{ ml: 'auto' }}>
                        <Skeleton
                            variant="rectangular"
                            width={92}
                            height={24}
                            sx={{ borderRadius: 999 }}
                        />
                    </Box>
                </Box>

                <Skeleton
                    variant="text"
                    width={'35%'}
                    height={20}
                    sx={{ mt: 0.5, mr: 0.5}}
                />
            </Box>

            <Box sx={{ ml: 2, display: 'flex', alignItems: 'center', gap: 1.5, flexShrink: 0 }}>
                <Skeleton variant="circular" width={28} height={28} />
                <Skeleton variant="circular" width={28} height={28} />
            </Box>
        </ListItem>
    );
}
