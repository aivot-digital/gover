import {useApi} from '../../../hooks/use-api';
import React, {type FormEvent, useEffect, useState} from 'react';
import {useNavigate, useParams} from 'react-router-dom';
import {Box} from '@mui/material';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../slices/snackbar-slice';
import {useAssetLinkOfAsset, useAssetsApi} from '../../../hooks/use-assets-api';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';
import {Asset} from '../../../models/entities/asset';
import {FormPageWrapper} from '../../../components/form-page-wrapper/form-page-wrapper';
import {useChangeBlocker} from '../../../hooks/use-change-blocker';
import {ConfirmDialog} from '../../../dialogs/confirm-dialog/confirm-dialog';
import ContentPasteOutlinedIcon from '@mui/icons-material/ContentPasteOutlined';
import {FileUploadComponent} from '../../../components/file-upload-field/file-upload-component';
import {AppConfig} from '../../../app-config';

export function AssetEditPage(): JSX.Element {
    const api = useApi();

    const dispatch = useAppDispatch();
    const navigate = useNavigate();

    const {key} = useParams();

    const [file, setFile] = useState<File[]>();
    const [uploadError, setUploadError] = useState<string>();

    const [originalAsset, setOriginalAsset] = useState<Asset>();
    const [editedAsset, setEditedAsset] = useState<Asset>();

    const [isBusy, setIsBusy] = useState(false);
    const [isNotFound, setIsNotFound] = useState(false);
    const [confirmDelete, setConfirmDelete] = useState<() => void>();

    const hasChanged = useChangeBlocker(originalAsset, editedAsset);

    useEffect(() => {
        if (key != null && key !== 'new') {
            setIsBusy(true);
            setIsNotFound(false);
            useAssetsApi(api)
                .retrieve(key)
                .then((asset) => {
                    setOriginalAsset(asset);
                    setEditedAsset(asset);
                    setIsBusy(false);
                })
                .catch((err) => {
                    console.error(err);
                    setIsBusy(false);
                    setIsNotFound(true);
                });
        }
    }, [key]);

    const handleSubmit = (event: FormEvent): void => {
        event.preventDefault();

        if (file == null || file.length === 0) {
            return;
        }

        setIsBusy(true);
        useAssetsApi(api)
            .create(file[0])
            .then((asset) => {
                setOriginalAsset(asset);
                setEditedAsset(asset);
                setFile([]);
            })
            .catch((err) => {
                if (err.status === 406) {
                    setUploadError('Die von Ihnen hochgeladene Datei hat einen ungültigen Dateinamen. Bitte benennen Sie die Datei um und versuchen Sie es erneut. Es sind nur Groß- und Kleinbuchstaben, Zahlen, Bindestriche, Unterstriche und Punkte erlaubt.');
                } else if (err.status === 409) {
                    setUploadError('Die von Ihnen hochgeladene Datei weist die Signatur eines Virus auf und wurde abgelehnt. Probieren Sie eine andere Datei.');
                } else if (err.status === 413) {
                    setUploadError(`Die von Ihnen hochgeladene Datei überschreitet das Limit von ${AppConfig.maxFileSizeMB}. Probieren Sie eine andere Datei.`);
                } else {
                    console.error(err);
                    setUploadError('Die Datei konnte nicht hochgeladen werden. Probieren Sie eine andere Datei.');
                }
            })
            .finally(() => {
                setIsBusy(false);
            });
    };

    const handleSave = (): void => {
        if (editedAsset != null) {
            setIsBusy(true);
            useAssetsApi(api)
                .update(editedAsset.key, editedAsset)
                .then((updatedAsset) => {
                    setOriginalAsset(updatedAsset);
                    setEditedAsset(updatedAsset);
                })
                .catch((err) => {
                    if (err.stat === 409) {
                        dispatch(showErrorSnackbar('Die Datei konnte nicht gespeichert werden, da sie die Signatur eines Virus aufweist.'));
                    } else {
                        console.error(err);
                        dispatch(showErrorSnackbar('Datei konnte nicht gespeichert werden.'));
                    }
                })
                .finally(() => {
                    setIsBusy(false);
                });
        }
    };

    const handleDelete = (): void => {
        if (originalAsset != null) {
            setIsBusy(true);
            useAssetsApi(api)
                .destroy(originalAsset.key)
                .then(() => {
                    navigate('/assets');
                })
                .catch((err) => {
                    console.error(err);
                    dispatch(showErrorSnackbar('Datei konnte nicht gelöscht werden.'));
                    setIsBusy(false);
                });
        }
    };

    const handleReset = (): void => {
        if (originalAsset != null) {
            setEditedAsset(originalAsset);
        }
    };

    return (
        <>
            <FormPageWrapper
                title="Dokument / Medieninhalt bearbeiten"
                isLoading={isBusy}
                is404={isNotFound}
                hasChanged={file != null && file.length > 0 || hasChanged}
                onSave={originalAsset != null ? handleSave : handleSubmit}
                onReset={editedAsset != null ? handleReset : undefined}
                onDelete={editedAsset != null ? () => setConfirmDelete(() => handleDelete) : undefined}
            >
                {
                    originalAsset == null &&
                    <FileUploadComponent
                        id="asset-upload"
                        value={file}
                        onChange={file => {
                            setFile(file);
                            setUploadError(undefined);
                        }}
                        label="Datei"
                        isMultifile={false}
                        minFiles={1}
                        maxFiles={1}
                        required={true}
                        error={uploadError}
                    />
                }

                {
                    originalAsset != null &&
                    <Box>
                        <TextFieldComponent
                            label="Dateiname"
                            value={editedAsset?.filename}
                            minCharacters={1}
                            maxCharacters={255}
                            onChange={val => {
                                if (editedAsset != null) {
                                    setEditedAsset({
                                        ...editedAsset,
                                        filename: val ?? '',
                                    });
                                }
                            }}
                            hint="Der Dateiname wird als Titel des Dokuments / Medieninhalts verwendet."
                        />

                        <TextFieldComponent
                            label="Internet Media Type"
                            value={originalAsset.contentType ?? 'application/octet-stream'}
                            onChange={() => {
                            }}
                            hint="Der Internet Media Type (auch MIME-Typ genannt) gibt an, um welchen Medientyp es sich handelt. Dieser Wert wird vom Browser verwendet, um das Dokument / den Medieninhalt korrekt darzustellen. Bitte beachten Sie, dass der Internet Media Type nicht verändert werden kann."
                        />

                        <TextFieldComponent
                            label="Link zum Dokument / Medieninhalt"
                            value={useAssetLinkOfAsset(originalAsset)}
                            disabled
                            onChange={() => {
                            }}
                            endAction={{
                                icon: <ContentPasteOutlinedIcon />,
                                onClick: () => {
                                    navigator.clipboard.writeText(useAssetLinkOfAsset(originalAsset));
                                    dispatch(showSuccessSnackbar('Link in die Zwischenablage kopiert.'));
                                },
                            }}
                            hint="Kopieren Sie diesen Link um das Dokument / den Medieninhalt in Ihren Formularen zu referenzieren."
                        />
                    </Box>
                }
            </FormPageWrapper>

            <ConfirmDialog
                title="Dokument / Medieninhalt löschen"
                onCancel={() => {
                    setConfirmDelete(undefined);
                }}
                onConfirm={confirmDelete}
            >
                Sind Sie sicher, dass Sie dieses Dokument / diesen Medieninhalt wirklich löschen wollen?
                Bitte beachten Sie, dass Sie dies nicht rückgängig machen können.
            </ConfirmDialog>
        </>
    );
}