import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {Typography} from '@mui/material';
import {GenericDetailsPage, NEW_ID_INDICATOR} from '../../../../components/generic-details-page/generic-details-page';
import DataArrayOutlinedIcon from '@mui/icons-material/DataArrayOutlined';
import {DataObjectSchemasApiService} from '../../data-object-schemas-api-service';
import {useAdminGuard} from '../../../../hooks/use-admin-guard';
import {DataObjectSchema} from '../../models/data-object-schema';
import {useParams} from 'react-router-dom';
import {useMemo, useRef} from 'react';
import CategoryOutlinedIcon from '@mui/icons-material/CategoryOutlined';
import {downloadObjectFile} from '../../../../utils/download-utils';
import CloudDownloadOutlinedIcon from '@mui/icons-material/CloudDownloadOutlined';
import {ServerEntityType} from '../../../../shells/staff/data/server-entity-type';
import { DataObject } from '@mui/icons-material';
import FolderData from '@aivot/mui-material-symbols-400-outlined/dist/folder-data/FolderData';

export function DataObjectSchemaDetailsPage() {
    useAdminGuard();

    const itemRef = useRef<DataObjectSchema | null>(null);
    const schemaKey = useParams().key;
    const isNew = useMemo(() => schemaKey === NEW_ID_INDICATOR, [schemaKey]);

    const handleExport = () => {
        const item = itemRef.current;

        if (item == null) {
            return;
        }

        downloadObjectFile(`datenmodell-${item.key}.json`, item);
    };

    return (
        <PageWrapper
            title="Datenmodell bearbeiten"
            fullWidth
            background
        >
            <GenericDetailsPage<DataObjectSchema, string, undefined>
                idParam="key"
                itemRef={itemRef}
                header={{
                    icon: <FolderData />,
                    title: 'Datenmodell bearbeiten',
                    helpDialog: {
                        title: 'Hilfe zu Datenmodelle',
                        tooltip: 'Hilfe anzeigen',
                        content: (
                            <>
                                <Typography>
                                    Ein Datenmodell beschreibt die Struktur eines Datenobjekts in Gover und legt fest, welche Datenfelder existieren, welche Datentypen sie haben, welche Standardwerte gelten und wie Werte geprüft werden. Es sorgt dafür, dass Daten aus Formularen, Workflows und Schnittstellen konsistent, valide und eindeutig interpretierbar sind.
                                </Typography>
                                <Typography sx={{ mt: 2 }}>
                                    Dazu können auch verschachtelte Objekte, Pflichtangaben, Wertebereiche oder Muster sowie Beschreibungen, Labels und optionale Sichtbarkeitsregeln gehören. Dasselbe Datenmodell kann in mehreren Prozessen und Komponenten wiederverwendet werden, sodass überall dieselbe Definition gilt. Bei der Ausgestaltung empfiehlt es sich, sprechende und langlebige Feldnamen zu verwenden, Weiterentwicklungen kompatibel vorzunehmen (zum Beispiel Felder hinzufügen statt umzubenennen oder zu entfernen) und Validierungen deutlich zu setzen.
                                </Typography>
                                <Typography sx={{ mt: 2 }}>
                                    Bei der Beziehung zwischen Datenmodell und Datenobjekt gilt: Das Datenmodell definiert die Form und das Datenobjekt füllt diese Form mit konkreten Werten. Änderungen am Datenmodell beeinflussen, wie neue oder geänderte Datenobjekte geprüft und gespeichert werden.
                                </Typography>
                            </>
                        ),
                    },
                    actions: isNew ? [] : [
                        {
                            icon: <CloudDownloadOutlinedIcon />,
                            onClick: handleExport,
                            variant: 'outlined',
                            label: 'Modell exportieren',
                        },
                        {
                            label: 'Datenobjekte anzeigen',
                            to: `/data-objects/${schemaKey}`,
                            variant: 'contained',
                            icon: <DataObject />,
                        },
                    ],
                }}
                tabs={[
                    {
                        path: '/data-objects/:key',
                        label: 'Allgemeine Angaben',
                    },
                ]}
                initializeItem={(api) => {
                    return new DataObjectSchemasApiService(api)
                        .initialize();
                }}
                fetchData={(api, key: string) => {
                    return new DataObjectSchemasApiService(api).retrieve(key);
                }}
                getTabTitle={(item: DataObjectSchema) => {
                    if (item.key === 'new') {
                        return 'Neues Datenmodell';
                    } else {
                        return item.name;
                    }
                }}
                getHeaderTitle={(item, isNewItem, notFound) => {
                    if (notFound) return 'Datenmodell nicht gefunden';
                    if (isNewItem) return 'Neues Datenmodell anlegen';
                    return `Datenmodell: ${item?.name ?? 'Unbenannt'}`;
                }}
                parentLink={{
                    label: 'Liste der Datenmodelle',
                    to: '/data-models',
                }}
                entityType={ServerEntityType.DataObjectSchemas}
            />
        </PageWrapper>
    );
}