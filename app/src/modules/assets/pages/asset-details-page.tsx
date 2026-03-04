import {PageWrapper} from '../../../components/page-wrapper/page-wrapper';
import {Typography} from '@mui/material';
import {GenericDetailsPage} from '../../../components/generic-details-page/generic-details-page';
import {Asset} from '../models/asset';
import {AssetsApiService} from '../assets-api-service';
import InsertDriveFileOutlinedIcon from '@mui/icons-material/InsertDriveFileOutlined';
import React, {useEffect, useMemo, useState} from 'react';
import {ServerEntityType} from '../../../shells/staff/data/server-entity-type';
import {useParams} from 'react-router-dom';
import {StorageProvidersApiService} from '../../storage/storage-providers-api-service';
import {showApiErrorSnackbar} from '../../../slices/snackbar-slice';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';

export function AssetDetailsPage() {
    const dispatch = useAppDispatch();
    const {storageProviderId} = useParams<{ storageProviderId: string }>();
    const [storageProviderReadOnly, setStorageProviderReadOnly] = useState(false);

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

    useEffect(() => {
        if (parsedStorageProviderId == null) {
            setStorageProviderReadOnly(false);
            return;
        }

        new StorageProvidersApiService()
            .retrieve(parsedStorageProviderId)
            .then((provider) => {
                setStorageProviderReadOnly(provider.readOnlyStorage);
            })
            .catch((err) => {
                setStorageProviderReadOnly(false);
                dispatch(showApiErrorSnackbar(err, 'Der Speicheranbieter konnte nicht geladen werden.'));
            });
    }, [dispatch, parsedStorageProviderId]);

    const parentRoute = `/assets/providers/${storageProviderId}`;
    const detailsPath = `/assets/providers/${storageProviderId}/files/*`;

    return (
        <PageWrapper
            title={storageProviderReadOnly ? 'Datei ansehen' : 'Datei bearbeiten'}
            fullWidth
            background
        >
            <GenericDetailsPage<Asset, string, undefined>
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
                        ? `Datei ansehen: ${item?.filename ?? "Unbenannt"}`
                        : `Datei: ${item?.filename ?? "Unbenannt"}`;
                }}
                idParam="*"
                parentLink={{
                    label: "Liste der Dateien",
                    to: parentRoute,
                }}
                entityType={ServerEntityType.Assets}
                isEditable={() => !storageProviderReadOnly}
            />
        </PageWrapper>
    );
}
