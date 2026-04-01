import {PageWrapper} from '../../../components/page-wrapper/page-wrapper';
import {Button, Dialog, DialogActions, DialogContent, Typography} from '@mui/material';
import {GenericDetailsPage} from '../../../components/generic-details-page/generic-details-page';
import {Asset} from '../models/asset';
import {AssetsApiService} from '../assets-api-service';
import InsertDriveFileOutlinedIcon from '@mui/icons-material/InsertDriveFileOutlined';
import DriveFileMoveOutlinedIcon from '@aivot/mui-material-symbols-400-outlined/dist/drive-file-move/DriveFileMove';
import ContentCopyOutlinedIcon from '@aivot/mui-material-symbols-400-outlined/dist/content-copy/ContentCopy';
import React, {useMemo, useState} from 'react';
import {ServerEntityType} from '../../../shells/staff/data/server-entity-type';
import {useNavigate, useParams, useSearchParams} from 'react-router-dom';
import {StorageProvidersApiService} from '../../storage/storage-providers-api-service';
import {showApiErrorSnackbar, showSuccessSnackbar} from '../../../slices/snackbar-slice';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {AssetDetailsPageAdditionalData} from './asset-details-page-additional-data';
import {PromptDialog} from '../../../dialogs/prompt-dialog/prompt-dialog';
import SaveAs from '@aivot/mui-material-symbols-400-outlined/dist/save-as/SaveAs';
import {downloadBlobFile} from '../../../utils/download-utils';
import Download from '@aivot/mui-material-symbols-400-outlined/dist/download/Download';
import {Action} from '../../../components/actions/actions-props';
import {FileUploadComponent} from '../../../components/file-upload-field/file-upload-component';
import SwapHoriz from '@aivot/mui-material-symbols-400-outlined/dist/swap-horiz/SwapHoriz';
import {StorageProviderEntity} from '../../storage/entities/storage-provider-entity';
import {GenericDetailsSkeleton} from '../../../components/generic-details-page/generic-details-skeleton';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {
    addSnackbarMessage, clearLoadingMessage,
    removeSnackbarMessage,
    setLoadingMessage,
    SnackbarSeverity,
    SnackbarType,
} from '../../../slices/shell-slice';
import {Breadcrumbs} from '../../../components/breadcrumbs/breadcrumbs';
import {showExperimentalFeatures} from '../../../hooks/use-show-experimental-features';
import {useNotImplemented} from '../../../hooks/use-not-implemented';

type UrlParamsType = {
    storageProviderId: string;
    '*': string;
};

export function AssetDetailsPage() {
    const {storageProviderId, '*': storagePathFromRoute} = useParams<UrlParamsType>();
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const dispatch = useAppDispatch();
    const notImplemented = useNotImplemented();

    const [asset, setAsset] = useState<Asset | null>(null);
    const [storageProvider, setStorageProvider] = useState<StorageProviderEntity | null>(null);

    const [file, setFile] = useState<File[] | null>(null);
    const [uploadError, setUploadError] = useState<string>();

    const [isMoveDialogOpen, setIsMoveDialogOpen] = useState(false);
    const [isCopyDialogOpen, setIsCopyDialogOpen] = useState(false);
    const [isRenameDialogOpen, setIsRenameDialogOpen] = useState(false);
    const [isReplaceDialogOpen, setIsReplaceDialogOpen] = useState(false);

    const storageProviderReadOnly = useMemo(() => {
        return storageProvider?.readOnlyStorage ?? false;
    }, [storageProvider]);

    const parsedStorageProviderId = useMemo(() => {
        if (storageProviderId == null) {
            return undefined;
        }

        const parsed = Number.parseInt(storageProviderId, 10);
        if (Number.isNaN(parsed) || parsed <= 0) {
            return undefined;
        }

        return parsed;
    }, [storageProviderId]);

    const normalizedCurrentStoragePath = useMemo(() => {
        if (storagePathFromRoute == null || storagePathFromRoute.length === 0 || storagePathFromRoute === 'new') {
            return undefined;
        }

        try {
            return AssetsApiService.decodeStoragePathFromRoute(storagePathFromRoute);
        } catch (err) {
            return undefined;
        }
    }, [storagePathFromRoute]);

    const defaultMoveTargetPath = useMemo(() => {
        if (normalizedCurrentStoragePath == null) {
            return '';
        }
        return normalizedCurrentStoragePath;
    }, [normalizedCurrentStoragePath]);

    const defaultCopyTargetPath = useMemo(() => {
        if (normalizedCurrentStoragePath == null) {
            return '';
        }
        return normalizedCurrentStoragePath;
    }, [normalizedCurrentStoragePath]);

    const canMoveAsset = parsedStorageProviderId != null && normalizedCurrentStoragePath != null && !storageProviderReadOnly;
    const canCopyAsset = parsedStorageProviderId != null && normalizedCurrentStoragePath != null && !storageProviderReadOnly;
    const canRenameAsset = parsedStorageProviderId != null && normalizedCurrentStoragePath != null && !storageProviderReadOnly;

    const parentRoute = `/assets/providers/${storageProviderId}?path=${encodeURIComponent(AssetsApiService.normalizeFolderPath(searchParams.get('path') ?? '/'))}`;
    const detailsPath = `/assets/providers/${storageProviderId}/files/*`;

    const handleConfirmMove = async (targetPathInput: string) => {
        setIsMoveDialogOpen(false);

        if (!canMoveAsset || parsedStorageProviderId == null || normalizedCurrentStoragePath == null) {
            return;
        }

        const normalizedTargetPath = AssetsApiService.normalizeStoragePath(targetPathInput);
        if (normalizedTargetPath === normalizedCurrentStoragePath) {
            return;
        }

        dispatch(setLoadingMessage({
            message: 'Verschiebe Datei',
            blocking: true,
            estimatedTime: 500,
        }));
        try {
            const movedAsset = await new AssetsApiService()
                .moveInStorageProvider(
                    parsedStorageProviderId,
                    normalizedCurrentStoragePath,
                    normalizedTargetPath,
                );

            const parentFolder = movedAsset.storagePathFromRoot.includes('/')
                ? AssetsApiService.normalizeFolderPath(movedAsset.storagePathFromRoot.substring(0, movedAsset.storagePathFromRoot.lastIndexOf('/')))
                : '/';

            dispatch(showSuccessSnackbar('Datei erfolgreich verschoben.'));
            navigate(
                `/assets/providers/${parsedStorageProviderId}/files/${AssetsApiService.encodeStoragePathForRoute(movedAsset.storagePathFromRoot)}?path=${encodeURIComponent(parentFolder)}`,
                {replace: true},
            );
        } catch (err) {
            dispatch(showApiErrorSnackbar(err, 'Die Datei konnte nicht verschoben werden.'));
        } finally {
            dispatch(clearLoadingMessage());
        }
    };

    const handleConfirmCopy = async (targetPathInput: string) => {
        setIsCopyDialogOpen(false);

        if (!canCopyAsset || parsedStorageProviderId == null || normalizedCurrentStoragePath == null) {
            return;
        }

        const normalizedTargetPath = AssetsApiService.normalizeStoragePath(targetPathInput);
        if (normalizedTargetPath === normalizedCurrentStoragePath) {
            return;
        }

        dispatch(setLoadingMessage({
            message: 'Kopiere Datei',
            blocking: true,
            estimatedTime: 500,
        }));
        try {
            const copiedAsset = await new AssetsApiService()
                .copyInStorageProvider(
                    parsedStorageProviderId,
                    normalizedCurrentStoragePath,
                    normalizedTargetPath,
                );

            const parentFolder = copiedAsset.storagePathFromRoot.includes('/')
                ? AssetsApiService.normalizeFolderPath(copiedAsset.storagePathFromRoot.substring(0, copiedAsset.storagePathFromRoot.lastIndexOf('/')))
                : '/';

            dispatch(showSuccessSnackbar('Datei erfolgreich kopiert. Navigiere zur Kopie.'));
            navigate(
                `/assets/providers/${parsedStorageProviderId}/files/${AssetsApiService.encodeStoragePathForRoute(copiedAsset.storagePathFromRoot)}?path=${encodeURIComponent(parentFolder)}`,
                {replace: true},
            );
        } catch (err) {
            dispatch(showApiErrorSnackbar(err, 'Die Datei konnte nicht kopiert werden.'));
        } finally {
            dispatch(clearLoadingMessage());
        }
    };

    const handleConfirmRename = async (targetName: string) => {
        setIsRenameDialogOpen(false);

        if (asset == null || storageProvider == null) {
            return;
        }

        const normalizedCurrentStoragePath = AssetsApiService
            .normalizeStoragePath(asset.storagePathFromRoot);

        const normalizedTargetPath = AssetsApiService
            .normalizeStoragePath(asset.storagePathFromRoot)
            .replace(/[^\/]*$/, targetName);

        await new AssetsApiService()
            .moveInStorageProvider(
                storageProvider.id,
                normalizedCurrentStoragePath,
                normalizedTargetPath,
            );

        dispatch(showSuccessSnackbar('Datei erfolgreich umbenannt.'));

        const encodedRoutePath = AssetsApiService
            .encodeStoragePathForRoute(normalizedTargetPath);

        navigate(`/assets/providers/${storageProvider.id}/files/${encodedRoutePath}`, {
            replace: true,
        });
    };

    const handleDownload = () => {
        if (asset == null || storageProvider == null) {
            return;
        }

        new AssetsApiService()
            .downloadContentInStorageProvider(asset.storagePathFromRoot, storageProvider.id, true)
            .then((blob) => {
                return downloadBlobFile(asset.filename, blob);
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Beim Herunterladen ist ein Fehler aufgetreten.'));
            });
    };

    const handleReplace = (file: File) => {
        if (asset == null || storageProvider == null) {
            return;
        }

        const snackbarKey = 'replace_asset';

        dispatch(addSnackbarMessage({
            key: snackbarKey,
            type: SnackbarType.Loading,
            severity: SnackbarSeverity.Info,
            message: 'Datei wird ersetzt',
        }));

        new AssetsApiService()
            .updateInStorageProvider(
                asset.storagePathFromRoot,
                asset,
                storageProvider.id,
                file,
            )
            .then((updatedAsset) => {
                dispatch(showSuccessSnackbar('Datei erfolgreich ersetzt.'));
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Fehler beim Ersetzen der Datei.'));
            })
            .finally(() => {
                dispatch(removeSnackbarMessage(snackbarKey));
            });
    };

    const headerActions: Action[] = [
        {
            icon: <Download/>,
            tooltip: 'Datei herunterladen',
            onClick: handleDownload,
        },
        {
            icon: <SaveAs/>,
            tooltip: 'Datei umbenennen',
            onClick: () => setIsRenameDialogOpen(true),
            disabled: !canRenameAsset,
            disabledTooltip: storageProviderReadOnly
                ? 'Der Speicheranbieter ist schreibgeschützt. Dateien können nicht umbenannt werden.'
                : 'Die Datei muss zuerst gespeichert werden, bevor sie umbenannt werden kann.',
        },
        {
            icon: <SwapHoriz/>,
            tooltip: 'Datei ersetzen',
            onClick: () => setIsReplaceDialogOpen(true),
        },
        'separator',
        {
            icon: <DriveFileMoveOutlinedIcon/>,
            tooltip: 'Datei verschieben',
            onClick: () => {
                if (showExperimentalFeatures()) {
                    setIsMoveDialogOpen(true)
                } else {
                    notImplemented();
                }
            },
            disabled: !canMoveAsset,
            disabledTooltip: storageProviderReadOnly
                ? 'Der Speicheranbieter ist schreibgeschützt. Dateien können nicht verschoben werden.'
                : 'Die Datei muss zuerst gespeichert werden, bevor sie verschoben werden kann.',
        },
        {
            icon: <ContentCopyOutlinedIcon/>,
            tooltip: 'Datei kopieren',
            onClick: () => {
                if (showExperimentalFeatures()) {
                    setIsCopyDialogOpen(true)
                } else {
                    notImplemented();
                }
            },
            disabled: !canCopyAsset,
            disabledTooltip: storageProviderReadOnly
                ? 'Der Speicheranbieter ist schreibgeschützt. Dateien können nicht kopiert werden.'
                : 'Die Datei muss zuerst gespeichert werden, bevor sie kopiert werden kann.',
        },
    ];

    if (parsedStorageProviderId == null) {
        return <GenericDetailsSkeleton/>;
    }

    return (
        <PageWrapper
            title={storageProviderReadOnly ? 'Datei ansehen' : 'Datei bearbeiten'}
            fullWidth
            background
        >
            <GenericDetailsPage<Asset, string, AssetDetailsPageAdditionalData>
                onItemChange={setAsset}
                onAdditionalDataChange={(res) => {
                    if (res?.storageProvider != null) {
                        setStorageProvider(res.storageProvider);
                    }
                }}
                header={{
                    icon: <InsertDriveFileOutlinedIcon/>,
                    title: storageProviderReadOnly ? 'Datei ansehen' : 'Datei bearbeiten',
                    helpDialog: {
                        title: 'Hilfe zu Dokumenten & Medieninhalten',
                        tooltip: 'Hilfe anzeigen',
                        content: (
                            <>
                                <Typography>
                                    Dokumente und Medieninhalte sind Dateien, die in der Anwendung hochgeladen und
                                    verwaltet werden können.
                                    In dieser Oberfläche können Sie die im System verfügbaren Dateien einsehen und
                                    bearbeiten.
                                </Typography>
                                <Typography sx={{mt: 2}}>
                                    Sie können die hochgeladenen Dateien u.A. in Formularen verwenden, um z.B. Bilder
                                    oder PDFs einzubinden.
                                    Darüber hinaus können Systemdateien (wie Zertifikate oder Templates) z.B. für die
                                    Konfiguration von
                                    Zahlungsdienstleistern oder der Dokumentengenerierung genutzt werden.
                                </Typography>
                            </>
                        ),
                    },
                    actions: headerActions,
                }}
                tabs={[
                    {
                        path: detailsPath,
                        label: 'Allgemein',
                    },
                ]}
                initializeItem={(api) => new AssetsApiService(api).initialize()}
                fetchData={(api, key: string) => {
                    return new AssetsApiService().retrieveInStorageProvider(
                        AssetsApiService.decodeStoragePathFromRoute(key),
                        parsedStorageProviderId,
                    );
                }}
                fetchAdditionalData={{
                    storageProvider: () => {
                        return new StorageProvidersApiService()
                            .retrieve(parsedStorageProviderId);
                    },
                }}
                getTabTitle={(item: Asset) => {
                    if (item.key === '') {
                        return 'Neue Datei';
                    } else {
                        return item.filename;
                    }
                }}
                getHeaderTitle={(item, isNewItem, notFound) => {
                    if (notFound || item == null || storageProvider == null) {
                        return 'Datei nicht gefunden';
                    }
                    return storageProvider.name + item.storagePathFromRoot;
                }}
                idParam="*"
                parentLink={{
                    label: 'Liste der Dateien',
                    to: parentRoute,
                }}
                entityType={ServerEntityType.Assets}
                isEditable={() => !storageProviderReadOnly}
            />

            {
                isMoveDialogOpen &&
                <PromptDialog
                    title="Datei verschieben"
                    message="Geben Sie den Zielpfad der Datei innerhalb des Speicheranbieters an."
                    inputLabel="Zielpfad"
                    inputPlaceholder="/ordner/datei.ext"
                    defaultValue={defaultMoveTargetPath}
                    confirmButtonText="Verschieben"
                    cancelButtonText="Abbrechen"
                    onConfirm={handleConfirmMove}
                    onCancel={() => setIsMoveDialogOpen(false)}
                />
            }

            {
                isCopyDialogOpen &&
                <PromptDialog
                    title="Datei kopieren"
                    message="Geben Sie den Zielpfad der Datei innerhalb des Speicheranbieters an."
                    inputLabel="Zielpfad"
                    inputPlaceholder="/ordner/datei.ext"
                    defaultValue={defaultCopyTargetPath}
                    confirmButtonText="Kopieren"
                    cancelButtonText="Abbrechen"
                    onConfirm={handleConfirmCopy}
                    onCancel={() => setIsCopyDialogOpen(false)}
                />
            }

            {
                isRenameDialogOpen &&
                <PromptDialog
                    title="Datei umbenennen"
                    inputLabel="Neuer Name"
                    inputPlaceholder="datei.ext"
                    defaultValue={asset?.filename ?? ''}
                    confirmButtonText="Umbenennen"
                    cancelButtonText="Abbrechen"
                    onConfirm={handleConfirmRename}
                    onCancel={() => setIsRenameDialogOpen(false)}
                >
                    <Typography>
                        Geben Sie den neuen Namen für die Datei an.
                        Bitte beachten Sie, dass dies die Datei im zugehörigen Speicheranbieter umbenennt.

                    </Typography>
                </PromptDialog>
            }

            <Dialog
                open={isReplaceDialogOpen}
                onClose={() => setIsReplaceDialogOpen(false)}
            >
                <DialogTitleWithClose onClose={() => setIsReplaceDialogOpen(false)}>
                    Datei ersetzen
                </DialogTitleWithClose>

                <DialogContent>
                    <Typography sx={{mb: 2, maxWidth: 900}}>
                        {storageProvider?.readOnlyStorage
                            ? 'Der Speicheranbieter ist schreibgeschützt. Sie können den Dateiinhalt nicht ersetzen, aber den öffentlichen Zugriff (Privatsphäre) weiterhin anpassen.'
                            : 'Ersetzen Sie optional die Datei und bearbeiten Sie Datenschutzangaben. Metadaten können nur zusammen mit einem Datei-Upload geändert werden.'}
                    </Typography>

                    <FileUploadComponent
                        id="asset-upload-replace"
                        value={file ?? undefined}
                        onChange={nextFile => {
                            setFile(nextFile ?? null);
                            setUploadError(undefined);
                        }}
                        label="Datei ersetzen"
                        isMultifile={false}
                        minFiles={1}
                        maxFiles={1}
                        required={false}
                        error={uploadError}
                        hint="Wählen Sie die neue Datei aus, die den aktuellen Inhalt ersetzen soll."
                    />
                </DialogContent>
                <DialogActions>
                    <Button
                        onClick={() => {
                            if (file != null && file.length > 0) {
                                handleReplace(file[0]);
                            }
                            setFile(null);
                            setIsReplaceDialogOpen(false);
                        }}
                    >
                        Ersetzen
                    </Button>
                </DialogActions>
            </Dialog>
        </PageWrapper>
    );
}
