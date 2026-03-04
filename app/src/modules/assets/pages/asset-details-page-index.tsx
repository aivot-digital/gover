import {Box, Button, Grid, Typography} from '@mui/material';
import React, {type FormEvent, useContext, useEffect, useMemo, useState} from 'react';
import {GenericDetailsPageContext, GenericDetailsPageContextType} from '../../../components/generic-details-page/generic-details-page-context';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';
import {useApi} from '../../../hooks/use-api';
import {useNavigate, useParams, useSearchParams} from 'react-router-dom';
import SaveOutlinedIcon from '@mui/icons-material/SaveOutlined';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../slices/snackbar-slice';
import {useChangeBlocker} from '../../../hooks/use-change-blocker';
import {useFormManager} from '../../../hooks/use-form-manager';
import {ConfirmDialog} from '../../../dialogs/confirm-dialog/confirm-dialog';
import * as yup from 'yup';
import {Asset} from '../models/asset';
import {AssetsApiService} from '../assets-api-service';
import ContentPasteOutlinedIcon from '@mui/icons-material/ContentPasteOutlined';
import {getFileTypeLabel} from '../../../utils/file-type-label';
import {FileUploadComponent} from '../../../components/file-upload-field/file-upload-component';
import {AppInfo} from '../../../app-info';
import {hideLoadingOverlayWithTimeout, showLoadingOverlay} from '../../../slices/loading-overlay-slice';
import {CheckboxFieldComponent} from '../../../components/checkbox-field/checkbox-field-component';
import {GenericDetailsSkeleton} from '../../../components/generic-details-page/generic-details-skeleton';
import {User} from '../../users/models/user';
import {UsersApiService} from '../../users/users-api-service';
import {resolveUserName} from '../../users/utils/resolve-user-name';
import {format} from 'date-fns';
import {StatusTable} from '../../../components/status-table/status-table';
import {BadgeOutlined} from '@mui/icons-material';
import {getFileTypeIcon} from '../../../utils/file-type-icon';
import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';
import {copyToClipboardText} from '../../../utils/copy-to-clipboard';
import {AssetDetailsPageAdditionalData} from './asset-details-page-additional-data';
import {StorageMetadataAttributesEditor} from '../../storage/components/storage-metadata-attributes-editor';

export const AssetSchema = yup.object({
    filename: yup.string()
        .trim()
        .min(1, 'Der Name der Datei muss mindestens ein Zeichen lang sein.')
        .max(255, 'Der Name der Datei darf maximal 255 Zeichen lang sein.')
        .required('Der Name der Datei ist ein Pflichtfeld.'),
    isPrivate: yup.boolean(),
});

export function AssetDetailsPageIndex() {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const {storageProviderId} = useParams<{ storageProviderId?: string; key?: string }>();

    const api = useApi();
    const {
        item,
        setItem,
        additionalData,
        isBusy,
        setIsBusy,
        isEditable,
    } = useContext(GenericDetailsPageContext) as GenericDetailsPageContextType<Asset, AssetDetailsPageAdditionalData>;

    const {
        currentItem,
        errors,
        hasNotChanged,
        handleInputBlur,
        handleInputChange,
        validate,
        reset,
    } = useFormManager<Asset>(item, AssetSchema as any);

    const apiService = useMemo(() => new AssetsApiService(api), [api]);
    const asset = currentItem;
    const [file, setFile] = useState<File[]>();
    const [uploadError, setUploadError] = useState<string>();
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
    const parentRoute = parsedStorageProviderId != null
        ? `/assets/providers/${parsedStorageProviderId}?path=${encodeURIComponent(AssetsApiService.normalizeFolderPath(searchParams.get('path') ?? '/'))}`
        : '/assets';
    const isReadOnlyStorageProvider = !isEditable;
    const readOnlyHint = 'Der ausgewählte Speicheranbieter ist schreibgeschützt. Änderungen sind nicht möglich.';

    const combinedEditedState = {
        ...currentItem,
        file,
        hasFileSelected: !!file?.length,
    };

    const combinedOriginalState = {
        ...item,
        file: file ?? undefined,
        hasFileSelected: false,
    };

    const changeBlocker = useChangeBlocker(combinedOriginalState, combinedEditedState);

    const [confirmDeleteAction, setConfirmDeleteAction] = useState<(() => void) | undefined>(undefined);

    const [uploader, setUploader] = useState<User>();
    useEffect(() => {
        if (item == null) {
            return;
        }

        if(asset?.uploaderId){
            new UsersApiService()
                .retrieve(item.uploaderId)
                .then(setUploader);
        }
    }, [item]);


    const uploadedDate = useMemo(() => {
        if (item == null) {
            return new Date();
        }
        return new Date(item.created);
    }, [item]);
    const storageProvider = additionalData?.storageProvider;
    const assetMetadata = (asset?.metadata ?? {}) as Record<string, unknown>;
    const hasSelectedFile = file != null && file.length > 0;
    const isNewAsset = asset?.filename === '';

    if (asset == null) {
        return (
            <GenericDetailsSkeleton />
        );
    }

    const handleSubmit = (event: FormEvent): void => {
        event.preventDefault();

        if (isReadOnlyStorageProvider) {
            dispatch(showErrorSnackbar(readOnlyHint));
            return;
        }

        if (file == null || file.length === 0 || parsedStorageProviderId == null) {
            return;
        }

        const filename = file[0].name;
        const targetFolderPath = AssetsApiService.normalizeFolderPath(searchParams.get('path') ?? '/');
        const storagePathFromRoot = `${targetFolderPath}${filename}`;

        setIsBusy(true);
        dispatch(showLoadingOverlay('Datei wird hochgeladen…'));

        new AssetsApiService(api)
            .upload(file[0], parsedStorageProviderId, storagePathFromRoot, asset)
            .then((newAsset) => {
                setItem(newAsset);
                reset();
                setFile([]);

                dispatch(showSuccessSnackbar('Neue Datei erfolgreich angelegt.'));

                // use setTimeout instead of useEffect to prevent unnecessary rerender
                setTimeout(() => {
                    const detailsRoute = parsedStorageProviderId != null
                        ? `/assets/providers/${parsedStorageProviderId}/files/${AssetsApiService.encodeStoragePathForRoute(newAsset.storagePathFromRoot)}?path=${encodeURIComponent(targetFolderPath)}`
                        : '/assets';
                    navigate(detailsRoute, {replace: true});
                }, 0);

            })
            .catch((err) => {
                dispatch(showErrorSnackbar('Speichern fehlgeschlagen. Bitte überprüfen Sie Ihre Eingaben.'));
                if (err.status === 406) {
                    setUploadError('Die von Ihnen hochgeladene Datei hat einen ungültigen Dateinamen. Bitte benennen Sie die Datei um und versuchen Sie es erneut. Es sind nur Groß- und Kleinbuchstaben, Zahlen, Bindestriche, Unterstriche und Punkte erlaubt.');
                } else if (err.status === 409) {
                    setUploadError('Die von Ihnen hochgeladene Datei weist die Signatur eines Virus auf und wurde abgelehnt. Probieren Sie eine andere Datei.');
                } else if (err.status === 413) {
                    setUploadError(`Die von Ihnen hochgeladene Datei überschreitet das Limit von ${AppInfo.maxFileSizeMB}MB. Probieren Sie eine andere Datei.`);
                } else {
                    console.error(err);
                    setUploadError('Die Datei konnte nicht hochgeladen werden. Probieren Sie eine andere Datei.');
                }
            })
            .finally(() => {
                setIsBusy(false);
                dispatch(hideLoadingOverlayWithTimeout(1000));
            });
    };

    const handleSave = () => {
        if (asset != null) {
            if (isReadOnlyStorageProvider) {
                dispatch(showErrorSnackbar(readOnlyHint));
                return;
            }

            const validationResult = validate();

            if (!validationResult) {
                dispatch(showErrorSnackbar('Bitte überprüfen Sie Ihre Eingaben.'));
                return;
            }

            setIsBusy(true);

            if (parsedStorageProviderId == null) {
                dispatch(showErrorSnackbar('Es wurde kein Speicheranbieter ausgewählt.'));
                setIsBusy(false);
                return;
            }

            if (file == null || file.length === 0) {
                dispatch(showErrorSnackbar('Für das Aktualisieren muss eine neue Datei ausgewählt werden.'));
                setIsBusy(false);
                return;
            }

            apiService
                .updateInStorageProvider(asset.storagePathFromRoot, file[0], asset, parsedStorageProviderId)
                .then((updatedAsset) => {
                    setItem(updatedAsset);
                    reset();
                    setFile([]);

                    dispatch(showSuccessSnackbar('Änderungen an Datei erfolgreich gespeichert.'));
                })
                .catch(err => {
                    console.error(err);
                    dispatch(showErrorSnackbar('Speichern fehlgeschlagen. Bitte überprüfen Sie Ihre Eingaben.'));
                })
                .finally(() => {
                    setIsBusy(false);
                });
        }
    };

    const confirmDelete = () => {
        if (isReadOnlyStorageProvider) {
            dispatch(showErrorSnackbar(readOnlyHint));
            return;
        }

        if (asset.key === '') return;

        setIsBusy(true);
        if (parsedStorageProviderId == null) {
            dispatch(showErrorSnackbar('Es wurde kein Speicheranbieter ausgewählt.'));
            setIsBusy(false);
            return;
        }

        apiService.destroyInStorageProvider(asset.storagePathFromRoot, parsedStorageProviderId)
            .then(() => {
                reset(); // prevent change blocker by resetting unsaved changes
                navigate(parentRoute, {
                    replace: true,
                });
                dispatch(showSuccessSnackbar('Die Datei wurde erfolgreich gelöscht.'));
            })
            .catch(() => dispatch(showErrorSnackbar('Beim Löschen ist ein Fehler aufgetreten.')))
            .finally(() => setIsBusy(false));
    };

    return (
        <Box>
            {
                !asset?.contentType &&
                <>
                    <Typography
                        variant="h5"
                        sx={{mb: 1}}
                    >
                        Datei hochladen
                    </Typography>

                    <Typography sx={{mb: 2, maxWidth: 900}}>
                        {isReadOnlyStorageProvider
                            ? readOnlyHint
                            : 'Wählen Sie eine einzelne Datei zum Hochladen aus oder ziehen Sie die Datei in das Feld.'}
                    </Typography>

                    <FileUploadComponent
                        id="asset-upload"
                        value={file}
                        onChange={file => {
                            if (isReadOnlyStorageProvider) {
                                return;
                            }
                            setFile(file);
                            setUploadError(undefined);
                        }}
                        label="Datei"
                        isMultifile={false}
                        minFiles={1}
                        maxFiles={1}
                        required={true}
                        error={uploadError}
                        disabled={isReadOnlyStorageProvider}
                        hint={isReadOnlyStorageProvider ? readOnlyHint : undefined}
                    />
                </>
            }
            {
                asset?.contentType &&
                <>
                    <Typography
                        variant="h5"
                        sx={{mb: 1}}
                    >
                        Dateiinformationen
                    </Typography>

                    <Typography sx={{mb: 2, maxWidth: 900}}>
                        {isReadOnlyStorageProvider
                            ? 'Der Speicheranbieter ist schreibgeschützt. Sie können Dateiinformationen einsehen, aber nicht bearbeiten.'
                            : 'Ersetzen Sie die Datei, bearbeiten Sie Datenschutz- und Metadatenangaben und sehen Sie ergänzende Informationen ein.'}
                    </Typography>

                    <FileUploadComponent
                        id="asset-upload-replace"
                        value={file}
                        onChange={nextFile => {
                            if (isReadOnlyStorageProvider) {
                                return;
                            }
                            setFile(nextFile);
                            setUploadError(undefined);
                        }}
                        label="Datei ersetzen"
                        isMultifile={false}
                        minFiles={1}
                        maxFiles={1}
                        required={!isReadOnlyStorageProvider}
                        error={uploadError}
                        disabled={isReadOnlyStorageProvider}
                        hint={isReadOnlyStorageProvider
                            ? readOnlyHint
                            : 'Wählen Sie die neue Datei aus, die den aktuellen Inhalt ersetzen soll.'}
                    />

                    <Grid
                        container
                        columnSpacing={4}
                    >
                        <Grid
                            size={{
                                xs: 12,
                                lg: 12
                            }}>
                            <TextFieldComponent
                                label="Dateiname"
                                value={asset?.filename}
                                onChange={handleInputChange('filename')}
                                onBlur={handleInputBlur('filename')}
                                disabled
                                required
                                maxCharacters={255}
                                minCharacters={1}
                                error={errors.filename}
                                hint="Der Dateiname wird aus dem Speicherindex abgeleitet und kann in diesem Schritt nicht direkt geändert werden."
                            />
                        </Grid>
                    </Grid>

                    <StatusTable
                        sx={{ mt: 3 }}
                        cardVariant="outlined"
                        items={[
                            {
                                label: 'Dateityp',
                                icon: getFileTypeIcon(asset?.contentType ?? 'application/octet-stream'),
                                children: `${getFileTypeLabel(asset?.contentType ?? 'application/octet-stream')} (${asset?.contentType})`,
                            },
                            {
                                label: 'Hochgeladen von',
                                icon: <BadgeOutlined />,
                                children: resolveUserName(uploader) + ' am ' + format(uploadedDate, 'dd.MM.yyyy') + ' – ' + format(uploadedDate, 'HH:mm') + ' Uhr',
                            },
                        ]}
                    />

                    <Box sx={{mt: 2, maxWidth: 900}}>
                        <CheckboxFieldComponent
                            label="Öffentlichen (nicht authentifizierten) Zugriff zulassen"
                            value={!asset.isPrivate}
                            onChange={(val) => handleInputChange('isPrivate')(!val)}
                            disabled={isReadOnlyStorageProvider}
                            variant="switch"
                            hint="Wenn diese Option aktiviert ist, kann die Datei über einen öffentlichen Link ohne Authentifizierung abgerufen werden.
                                    Nutzen Sie diese Option nur für Dateien, die öffentlich sein müssen und niemals für sicherheitsrelevante
                                    Dateien wie Zertifikate. Änderungen werden erst nach dem Speichern wirksam."
                        />
                    </Box>

                    {
                        storageProvider != null && storageProvider.metadataAttributes.length > 0 && (
                            <Box sx={{mt: 3, maxWidth: 900}}>
                                <Typography variant="h6" sx={{mb: 1}}>
                                    Metadaten
                                </Typography>
                                <Typography sx={{mb: 2}}>
                                    Hinterlegen Sie optional zusätzliche Metadaten für die Datei entsprechend den
                                    konfigurierten Attributen des Speicheranbieters.
                                </Typography>
                                <StorageMetadataAttributesEditor
                                    storageProvider={storageProvider}
                                    metadata={assetMetadata}
                                    disabled={isReadOnlyStorageProvider}
                                    onChange={(metadata) => handleInputChange('metadata')(metadata as any)}
                                />
                            </Box>
                        )
                    }

                    {(!asset.isPrivate && item?.isPrivate === false) && (
                        <Box sx={{mt: 1}}>
                            <TextFieldComponent
                                label="Link zum Dokument / Medieninhalt"
                                value={AssetsApiService.useAssetLinkOfAsset(asset)}
                                disabled
                                onChange={() => {
                                }}
                                endAction={{
                                    icon: <ContentPasteOutlinedIcon />,
                                    onClick: async () => {
                                        const success = await copyToClipboardText(AssetsApiService.useAssetLinkOfAsset(asset));
                                        if (success) {
                                            dispatch(showSuccessSnackbar('Link in Zwischenablage kopiert!'));
                                        } else {
                                            dispatch(showErrorSnackbar('Fehler beim Kopieren des Links!'));
                                        }
                                    },
                                }}
                                hint="Kopieren Sie diesen Link um das Dokument / den Medieninhalt in Ihren Formularen zu referenzieren."
                            />
                        </Box>
                    )}
                </>
            }
            <Box
                sx={{
                    display: 'flex',
                    marginTop: 4,
                    gap: 2,
                }}
            >
                <Button
                    onClick={asset?.filename !== '' ? handleSave : handleSubmit}
                    disabled={isBusy || isReadOnlyStorageProvider || !hasSelectedFile}
                    variant="contained"
                    color="primary"
                    startIcon={<SaveOutlinedIcon />}
                >
                    {isReadOnlyStorageProvider ? 'Nur Ansicht' : 'Speichern'}
                </Button>

                {
                    asset.key !== '' &&
                    <Button
                        onClick={() => {
                            reset();
                        }}
                        disabled={isBusy || isReadOnlyStorageProvider || hasNotChanged}
                        color="error"
                    >
                        Zurücksetzen
                    </Button>
                }

                {
                    asset.key !== '' &&
                    <Button
                        variant={'outlined'}
                        onClick={() => setConfirmDeleteAction(() => confirmDelete)}
                        disabled={isBusy || isReadOnlyStorageProvider}
                        color="error"
                        sx={{
                            marginLeft: 'auto',
                        }}
                        startIcon={<Delete />}
                    >
                        Löschen
                    </Button>
                }
            </Box>
            {changeBlocker.dialog}
            <ConfirmDialog
                title="Datei löschen"
                onCancel={() => setConfirmDeleteAction(undefined)}
                onConfirm={confirmDeleteAction}
                isDestructive
                confirmButtonText="Ja, endgültig löschen"
            >
                <Typography>
                    Möchten Sie diese Datei wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden.
                </Typography>
            </ConfirmDialog>
        </Box>
    );
}
