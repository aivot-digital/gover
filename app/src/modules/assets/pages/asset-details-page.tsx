import {PageWrapper} from '../../../components/page-wrapper/page-wrapper';
import {Typography} from '@mui/material';
import {GenericDetailsPage} from '../../../components/generic-details-page/generic-details-page';
import {Asset} from '../models/asset';
import {AssetsApiService} from '../assets-api-service';
import InsertDriveFileOutlinedIcon from '@mui/icons-material/InsertDriveFileOutlined';
import React from 'react';

export function AssetDetailsPage() {
    return (
        <PageWrapper
            title="Datei bearbeiten"
            fullWidth
            background
        >
            <GenericDetailsPage<Asset, string, undefined>
                header={{
                    icon: <InsertDriveFileOutlinedIcon />,
                    title: 'Datei bearbeiten',
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
                        path: '/assets/:key',
                        label: '',
                    },
                ]}
                initializeItem={(api) => new AssetsApiService(api).initialize()}
                fetchData={(api, key: string) => new AssetsApiService(api).retrieve(key)}
                getTabTitle={(item: Asset) => {
                    if (item.key === "") {
                        return 'Neue Datei';
                    } else {
                        return item.filename;
                    }
                }}
                getHeaderTitle={(item, isNewItem, notFound) => {
                    if (notFound) return "Datei nicht gefunden";
                    if (isNewItem) return "Neue Datei hochladen";
                    return `Datei: ${item?.filename ?? "Unbenannt"}`;
                }}
                idParam={'key'}
                parentLink={{
                    label: "Liste der Dateien",
                    to: "/assets",
                }}
            />
        </PageWrapper>
    );
}