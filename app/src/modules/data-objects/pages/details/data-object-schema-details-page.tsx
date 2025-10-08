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
import Download from '@aivot/mui-material-symbols-400-outlined/dist/download/Download';
import {downloadObjectFile} from '../../../../utils/download-utils';

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

        downloadObjectFile(`datenobjektschema-${item.key}.json`, item);
    };

    return (
        <PageWrapper
            title="Datenobjektschema bearbeiten"
            fullWidth
            background
        >
            <GenericDetailsPage<DataObjectSchema, string, undefined>
                idParam="key"
                itemRef={itemRef}
                header={{
                    icon: <DataArrayOutlinedIcon />,
                    title: 'Datenobjektschema bearbeiten',
                    helpDialog: {
                        title: 'Hilfe zu Datenobjektschemata',
                        tooltip: 'Hilfe anzeigen',
                        content: (
                            <>
                                <Typography>
                                    Ein Datenobjektschema beschreibt die Struktur eines Datenobjekts in Gover und legt fest, welche Datenfelder existieren, welche Datentypen diese haben und wie Werte geprüft werden. Es sorgt dafür, dass Daten aus Formularen, Workflows und Schnittstellen konsistent, valide und eindeutig interpretierbar sind.
                                </Typography>
                                <Typography sx={{ mt: 2 }}>
                                    Typischerweise umfasst ein Schema Felder mit Schlüsseln, Datentypen wie Text, Zahl, Datum oder Wahrheitswerte, Validierungen für Pflichtangaben, Wertebereiche oder Muster, sowie die Möglichkeit verschachtelte Objekte abzubilden. Ergänzend können Metadaten wie Beschreibungen, Labels oder Sichtbarkeitsregeln hinterlegt und sinnvolle Standardwerte definiert werden.
                                </Typography>
                                <Typography sx={{ mt: 2 }}>
                                    Das Schema unterstützt verschiedene Komponenten bei der einheitlichen Nutzung desselben Datenmodells. Bei der Ausgestaltung empfehlen sich sprechende, langlebige Feldnamen, kompatible Weiterentwicklungen (hinzufügen statt umbenennen/entfernen) und deutliche Validierungen.
                                </Typography>
                            </>
                        ),
                    },
                    actions: isNew ? [] : [
                        {
                            label: 'Datenobjekte anzeigen',
                            to: `/data-objects/${schemaKey}/items`,
                            variant: 'outlined',
                            icon: <CategoryOutlinedIcon />,
                            tooltip: 'Zu den Datenobjekten dieses Schemas wechseln',
                        },
                        {
                            onClick: handleExport,
                            tooltip: 'Datenobjektschema exportieren',
                            icon: <Download />,
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
                        return 'Neues Datenobjektschema';
                    } else {
                        return item.name;
                    }
                }}
                getHeaderTitle={(item, isNewItem, notFound) => {
                    if (notFound) return 'Datenobjektschema nicht gefunden';
                    if (isNewItem) return 'Neues Datenobjektschema anlegen';
                    return `Datenobjektschema: ${item?.name ?? 'Unbenannt'}`;
                }}
                parentLink={{
                    label: 'Liste der Datenobjektschemata',
                    to: '/data-objects',
                }}
            />
        </PageWrapper>
    );
}