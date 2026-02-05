import React, {type ReactNode, useEffect, useState} from 'react';
import {Box, Breadcrumbs, Button, Grid, Typography} from '@mui/material';
import {StorageProvidersApiService} from '../storage-providers-api-service';
import {type StorageIndexItem} from '../entities/storage-index-item-entity';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar} from '../../../slices/snackbar-slice';
import {getFileTypeIcon} from '../../../utils/file-type-icon';
import {type StorageProviderEntity} from '../entities/storage-provider-entity';
import {downloadBlobFile} from '../../../utils/download-utils';

interface StorageExplorerProps {
    providerId: number;
    filterMimeTypes?: string[];
}

export function StorageExplorer(props: StorageExplorerProps): ReactNode {
    const {
        providerId,
    } = props;

    const dispatch = useAppDispatch();

    const [provider, setProvider] = useState<StorageProviderEntity>();

    const [currentPath, setCurrentPath] = useState<string>('/');
    const [currentFolder, setCurrentFolder] = useState<StorageIndexItem[]>([]);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        new StorageProvidersApiService()
            .retrieve(providerId)
            .then(setProvider)
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Der Speicheranbieter konnte nicht geladen werden.'));
            });
    }, [providerId]);

    useEffect(() => {
        setIsLoading(true);

        new StorageProvidersApiService()
            .getFolder(providerId, currentPath)
            .then(setCurrentFolder)
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Beim Abrufen des Ordners ist ein Fehler aufgetreten'));
            })
            .finally(() => {
                setIsLoading(false);
            });
    }, [currentPath]);

    if (provider == null) {
        return null;
    }

    return (
        <Box
            sx={{
                position: 'relative',
            }}
        >
            <Breadcrumbs separator="/">
                <Button
                    onClick={() => {
                        setCurrentPath('/');
                    }}
                >
                    {provider.name}
                </Button>

                {
                    currentPath
                        .split('/')
                        .map((part, index, all) => {
                            if (part === '') {
                                return null;
                            }

                            const pathUntilHere = all
                                .slice(0, index + 1)
                                .join('/');

                            return (
                                <Button
                                    key={part}
                                    onClick={() => {
                                        setCurrentPath(pathUntilHere);
                                    }}
                                >
                                    {part}
                                </Button>
                            );
                        })
                }
            </Breadcrumbs>

            <Box
                sx={{
                    mt: 2,
                }}
            >
                {
                    currentFolder.length === 0 && (
                        <Typography
                            color="textSecondary"
                        >
                            Dieser Ordner ist leer.
                        </Typography>
                    )
                }

                <Grid
                    container={true}
                    spacing={2}
                >

                    {
                        currentFolder
                            .map((item) => (
                                <Grid
                                    key={item.pathFromRoot}
                                    size={{
                                        xs: 12,
                                        sm: 8,
                                        md: 4,
                                        lg: 2,
                                        xl: 1,
                                    }}
                                >
                                    <Button
                                        sx={{
                                            width: '100%',
                                            height: '100%',
                                            display: 'flex',
                                            flexDirection: 'column',
                                            alignItems: 'center',
                                            justifyContent: 'flex-start',
                                            wordBreak: 'break-all',
                                            p: 2,
                                        }}
                                        onClick={() => {
                                            if (item.isDirectory) {
                                                setCurrentPath(item.pathFromRoot);
                                            } else {
                                                new StorageProvidersApiService()
                                                    .downloadFile(providerId, item.pathFromRoot)
                                                    .then((blob) => {
                                                        downloadBlobFile(item.filename, blob);
                                                    })
                                                    .catch((err) => {
                                                        dispatch(showApiErrorSnackbar(err, 'Die Datei konnte nicht heruntergeladen werden.'));
                                                    });
                                            }
                                        }}
                                    >
                                        {getFileTypeIcon(item.mimeType, {
                                            fontSize: 'large',
                                        })}

                                        <Typography
                                            textAlign="center"
                                            sx={{
                                                mt: 1,
                                            }}
                                        >
                                            {item.filename}
                                        </Typography>
                                    </Button>
                                </Grid>
                            ))
                    }
                </Grid>

            </Box>

            <Box
                sx={{
                    display: isLoading ? 'block' : 'none',
                    position: 'absolute',
                    width: '100%',
                    height: '100%',
                    background: 'rgba(255, 255, 255, 0.85)',
                    backdropFilter: 'blur(4px)',
                }}
            >
            </Box>
        </Box>
    );
}