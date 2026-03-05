import {PageWrapper} from '../../../components/page-wrapper/page-wrapper';
import {Typography} from '@mui/material';
import {GenericDetailsPage} from '../../../components/generic-details-page/generic-details-page';
import {Asset} from '../models/asset';
import {AssetsApiService} from '../assets-api-service';
import InsertDriveFileOutlinedIcon from '@mui/icons-material/InsertDriveFileOutlined';
import DriveFileMoveOutlinedIcon from '@mui/icons-material/DriveFileMoveOutlined';
import React, {useMemo, useState} from 'react';
import {ServerEntityType} from '../../../shells/staff/data/server-entity-type';
import {useNavigate, useParams, useSearchParams} from 'react-router-dom';
import {StorageProvidersApiService} from '../../storage/storage-providers-api-service';
import {showApiErrorSnackbar, showSuccessSnackbar} from '../../../slices/snackbar-slice';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {AssetDetailsPageAdditionalData} from './asset-details-page-additional-data';
import {useApi} from '../../../hooks/use-api';
import {PromptDialog} from '../../../dialogs/prompt-dialog/prompt-dialog';

export function AssetDetailsPage() {
    const dispatch = useAppDispatch();
    const api = useApi();
    const navigate = useNavigate();
    const {storageProviderId, '*': storagePathFromRoute} = useParams<{ storageProviderId: string; '*': string }>();
    const [searchParams] = useSearchParams();
    const [storageProviderReadOnly, setStorageProviderReadOnly] = useState(false);
    const [isMoveDialogOpen, setIsMoveDialogOpen] = useState(false);

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
    const canMoveAsset = parsedStorageProviderId != null && normalizedCurrentStoragePath != null && !storageProviderReadOnly;

    const parentRoute = `/assets/providers/${storageProviderId}?path=${encodeURIComponent(AssetsApiService.normalizeFolderPath(searchParams.get('path') ?? '/'))}`;
    const detailsPath = `/assets/providers/${storageProviderId}/files/*`;
    const headerActions = useMemo(() => {
        return [
            {
                icon: <DriveFileMoveOutlinedIcon />,
                tooltip: 'Datei verschieben',
                onClick: () => setIsMoveDialogOpen(true),
                disabled: !canMoveAsset,
                disabledTooltip: storageProviderReadOnly
                    ? 'Der Speicheranbieter ist schreibgeschützt. Dateien können nicht verschoben werden.'
                    : 'Die Datei muss zuerst gespeichert werden, bevor sie verschoben werden kann.',
            },
        ];
    }, [canMoveAsset, storageProviderReadOnly]);

    const handleConfirmMove = async (targetPathInput: string) => {
        setIsMoveDialogOpen(false);

        if (!canMoveAsset || parsedStorageProviderId == null || normalizedCurrentStoragePath == null) {
            return;
        }

        const normalizedTargetPath = AssetsApiService.normalizeStoragePath(targetPathInput);
        if (normalizedTargetPath === normalizedCurrentStoragePath) {
            return;
        }

        try {
            const movedAsset = await new AssetsApiService(api).moveInStorageProvider(
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
        }
    };

    return (
        <PageWrapper
            title={storageProviderReadOnly ? 'Datei ansehen' : 'Datei bearbeiten'}
            fullWidth
            background
        >
            <GenericDetailsPage<Asset, string, AssetDetailsPageAdditionalData>
                header={{
                    icon: <InsertDriveFileOutlinedIcon />,
                    title: storageProviderReadOnly ? 'Datei ansehen' : 'Datei bearbeiten',
                    helpDialog: {
                        title: 'Hilfe zu Dokumenten & Medieninhalten',
                        tooltip: 'Hilfe anzeigen',
                        content: (
                            <>
                                <Typography>
                                    Dokumente und Medieninhalte sind Dateien, die in der Anwendung hochgeladen und verwaltet werden können.
                                    In dieser Oberfläche können Sie die im System verfügbaren Dateien einsehen und bearbeiten.
                                </Typography>
                                <Typography sx={{mt: 2}}>
                                    Sie können die hochgeladenen Dateien u.A. in Formularen verwenden, um z.B. Bilder oder PDFs einzubinden.
                                    Darüber hinaus können Systemdateien (wie Zertifikate oder Templates) z.B. für die Konfiguration von
                                    Zahlungsdienstleistern oder der Dokumentengenerierung genutzt werden.
                                </Typography>
                            </>
                        ),
                    },
                    actions: headerActions
                }}
                tabs={[
                    {
                        path: detailsPath,
                        label: 'Allgemein',
                    },
                ]}
                initializeItem={(api) => new AssetsApiService(api).initialize()}
                fetchData={(api, key: string) => {
                    if (parsedStorageProviderId == null) {
                        return Promise.reject(new Error('No storage provider selected'));
                    }

                    return new AssetsApiService(api).retrieveInStorageProvider(
                        AssetsApiService.decodeStoragePathFromRoute(key),
                        parsedStorageProviderId,
                    );
                }}
                fetchAdditionalData={{
                    storageProvider: async () => {
                        if (parsedStorageProviderId == null) {
                            throw new Error('No storage provider selected');
                        }

                        try {
                            const provider = await new StorageProvidersApiService().retrieve(parsedStorageProviderId);
                            setStorageProviderReadOnly(provider.readOnlyStorage);
                            return provider;
                        } catch (err) {
                            setStorageProviderReadOnly(false);
                            dispatch(showApiErrorSnackbar(err, 'Der Speicheranbieter konnte nicht geladen werden.'));
                            throw err;
                        }
                    }
                }}
                getTabTitle={(item: Asset) => {
                    if (item.key === "") {
                        return 'Neue Datei';
                    } else {
                        return item.filename;
                    }
                }}
                getHeaderTitle={(item, isNewItem, notFound) => {
                    if (notFound) return "Datei nicht gefunden";
                    if (isNewItem) {
                        return storageProviderReadOnly
                            ? "Datei hochladen nicht möglich (schreibgeschützt)"
                            : "Neue Datei hochladen";
                    }
                    return storageProviderReadOnly
                        ? `Datei bearbeiten (eingeschränkt): ${item?.filename ?? "Unbenannt"}`
                        : `Datei: ${item?.storagePathFromRoot ?? "Unbenannt"}`;
                }}
                idParam="*"
                parentLink={{
                    label: "Liste der Dateien",
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
        </PageWrapper>
    );
}
