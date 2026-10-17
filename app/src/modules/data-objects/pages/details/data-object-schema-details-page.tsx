import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {Typography} from '@mui/material';
import {GenericDetailsPage, NEW_ID_INDICATOR} from '../../../../components/generic-details-page/generic-details-page';
import {DataObjectSchemasApiService} from '../../data-object-schemas-api-service';
import {DataObjectSchema} from '../../models/data-object-schema';
import {useParams} from 'react-router-dom';
import {useMemo, useRef} from 'react';
import {downloadObjectFile} from '../../../../utils/download-utils';
import CloudDownloadOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/CloudDownload';
import {ServerEntityType} from '../../../../shells/staff/data/server-entity-type';
import DataObject from '@aivot/mui-material-symbols-400-n25-outlined/DataObject';
import FolderData from '@aivot/mui-material-symbols-400-n25-outlined/FolderData';
import {Permission} from '../../../../data/permissions/permission';
import {useHasSystemPermission} from '../../../permissions/hooks/use-permissions';
import {formatMissingPermissionTooltip} from '../../../permissions/utils/permission-utils';

export function DataObjectSchemaDetailsPage() {
    const itemRef = useRef<DataObjectSchema | null>(null);
    const schemaKey = useParams().key;
    const isNew = useMemo(() => schemaKey === NEW_ID_INDICATOR, [schemaKey]);
    const canReadDataObjectItems = useHasSystemPermission(Permission.OBJECT_ITEM_READ);

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
                permissionCheck={{
                    create: Permission.OBJECT_SCHEMA_CREATE,
                    read: Permission.OBJECT_SCHEMA_READ,
                    update: Permission.OBJECT_SCHEMA_UPDATE,
                    scope: {
                        type: 'system',
                    },
                }}
                header={{
                    icon: <FolderData />,
                    title: 'Datenmodell bearbeiten',
                    helpDialog: {
                        title: 'Hilfe zu Datenmodellen',
                        tooltip: 'Hilfe anzeigen',
                        content: (
                            <>
                                <Typography>
                                    Ein Datenmodell beschreibt die Struktur eines Datenobjekts in Prosuna und legt fest, welche Datenfelder existieren, welche Datentypen sie haben, welche Standardwerte gelten und wie Werte geprüft werden. Es
                                    sorgt dafür, dass Daten aus Formularen, Prozessen und Schnittstellen konsistent, valide und eindeutig interpretierbar sind.
                                </Typography>
                                <Typography sx={{mt: 2}}>
                                    Dazu können auch verschachtelte Objekte, Pflichtangaben, Wertebereiche oder Muster sowie Beschreibungen, Labels und optionale Sichtbarkeitsregeln gehören. Dasselbe Datenmodell kann in mehreren Prozessen
                                    und Komponenten wiederverwendet werden, sodass überall dieselbe Definition gilt. Bei der Ausgestaltung empfiehlt es sich, sprechende und langlebige Feldnamen zu verwenden, Weiterentwicklungen kompatibel
                                    vorzunehmen (zum Beispiel Felder hinzufügen statt umzubenennen oder zu entfernen) und Validierungen deutlich zu setzen.
                                </Typography>
                                <Typography sx={{mt: 2}}>
                                    Bei der Beziehung zwischen Datenmodell und Datenobjekt gilt: Das Datenmodell definiert die Form und das Datenobjekt füllt diese Form mit konkreten Werten. Änderungen am Datenmodell beeinflussen, wie neue
                                    oder geänderte Datenobjekte geprüft und gespeichert werden.
                                </Typography>
                            </>
                        ),
                    },
                    actions: isNew ? [] : [
                        {
                            icon: <CloudDownloadOutlinedIcon />,
                            onClick: handleExport,
                            variant: 'text',
                            label: 'Exportieren',
                        },
                        {
                            label: 'Datenobjekte anzeigen',
                            to: `/data-objects/${schemaKey}`,
                            variant: 'text',
                            icon: <DataObject />,
                            disabled: !canReadDataObjectItems,
                            disabledTooltip: formatMissingPermissionTooltip(Permission.OBJECT_ITEM_READ),
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
