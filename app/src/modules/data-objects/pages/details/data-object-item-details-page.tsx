import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {Typography} from '@mui/material';
import {GenericDetailsPage} from '../../../../components/generic-details-page/generic-details-page';
import CategoryOutlinedIcon from '@mui/icons-material/CategoryOutlined';
import {DataObjectSchemasApiService} from '../../data-object-schemas-api-service';
import {useAdminGuard} from '../../../../hooks/use-admin-guard';
import {DataObjectSchema} from '../../models/data-object-schema';
import {useEffect, useState} from 'react';
import {useParams} from 'react-router-dom';
import {useApi} from '../../../../hooks/use-api';
import {LoadingPlaceholder} from '../../../../components/loading-placeholder/loading-placeholder';
import {DataObjectItemsApiService} from '../../data-object-items-api-service';
import {DataObjectItem} from '../../models/data-object-item';
import DataArrayOutlinedIcon from '@mui/icons-material/DataArrayOutlined';
import {ServerEntityType} from '../../../../shells/staff/data/server-entity-type';
import DataObject from '@aivot/mui-material-symbols-400-outlined/dist/data-object/DataObject';
import FolderData from '@aivot/mui-material-symbols-400-outlined/dist/folder-data/FolderData';

export function DataObjectItemDetailsPage() {
    useAdminGuard();

    const schemaKey = useParams().schemaKey;

    const api = useApi();

    const [dataObjectSchema, setDataObjectSchema] = useState<DataObjectSchema>();
    useEffect(() => {
        if (schemaKey == null) {
            return;
        }

        new DataObjectSchemasApiService(api)
            .retrieve(schemaKey)
            .then((dataObject) => {
                setDataObjectSchema(dataObject);
            })
            .catch((error) => {
                console.error('Error fetching data object:', error);
            });
    }, [schemaKey]);

    if (dataObjectSchema == null) {
        return (
            <LoadingPlaceholder />
        );
    }

    return (
        <PageWrapper
            title={`Datenobjekt bearbeiten: ${dataObjectSchema.name}`}
            fullWidth
            background
        >
            <GenericDetailsPage<DataObjectItem, string, void>
                header={{
                    icon: <DataObject />,
                    title: `Datenobjekt bearbeiten: ${dataObjectSchema.name}`,
                    helpDialog: {
                        title: 'Hilfe zu Datenobjekten',
                        tooltip: 'Hilfe anzeigen',
                        content: (
                            <>
                                <Typography>
                                    Ein Datenobjekt ist eine konkrete Instanz eines Datenmodells. Es enthält die tatsächlichen Werte zu den im Datenmodell definierten Feldern und bildet damit die „laufenden“ Fachinformationen im System ab. Datenobjekte fließen durch Prozesse, Komponenten und Schnittstellen. Ihre Struktur, Datentypen und Prüfregeln ergeben sich immer aus dem verknüpften Datenmodell.
                                </Typography>
                                <Typography sx={{ mt: 2 }}>
                                    Typischerweise enthält ein Datenobjekt Werte für Text, Zahlen, Datums- oder Wahrheitsfelder sowie gegebenenfalls verschachtelte Strukturen. Neben den Nutzdaten können Metadaten wie Erstell- und Änderungszeitpunkte, Quelle oder Status sowie Referenzen auf andere Objekte vorhanden sein. Beim Anlegen werden Standardwerte aus dem Datenmodell übernommen; Validierungen stellen sicher, dass nur erlaubte, vollständige und konsistente Inhalte gespeichert werden. Änderungen an der Struktur erfolgen nicht am Datenobjekt selbst, sondern am zugrunde liegenden Datenmodell, das dann die Prüfung neuer oder geänderter Objekte steuert.
                                </Typography>
                                <Typography sx={{ mt: 2 }}>
                                    Ein einfaches Beispiel: Das Datenmodell „Bauvorhaben“ definiert Felder und Regeln, und das Datenobjekt „Erweiterungsbau Grundschule #2025-123“ füllt diese Felder mit konkreten Angaben.
                                </Typography>
                            </>
                        ),
                    },
                    actions: [
                        {
                            icon: <FolderData />,
                            to: `/data-models/${dataObjectSchema.key}`,
                            variant: 'outlined',
                            label: 'Datenmodell bearbeiten',
                        },
                    ],
                }}
                tabs={[
                    {
                        path: `/data-objects/${dataObjectSchema.key}/:id`,
                        label: 'Allgemeine Angaben',
                    },
                ]}
                initializeItem={(api) => {
                    return new DataObjectItemsApiService(api, dataObjectSchema.key)
                        .initialize();
                }}
                fetchData={(api, key: string) => {
                    return new DataObjectItemsApiService(api, dataObjectSchema.key)
                        .retrieve(key);
                }}
                getTabTitle={(item: DataObjectItem) => {
                    if (item.id === 'new') {
                        return 'Neues Datenobjekt';
                    } else {
                        return item.id;
                    }
                }}
                getHeaderTitle={(item, isNewItem, notFound) => {
                    if (notFound) return 'Datenobjekt nicht gefunden';
                    if (isNewItem) return 'Neues Datenobjekt anlegen';
                    return `Datenobjekt: ${item?.id ?? 'Unbenannt'}`;
                }}
                parentLink={{
                    label: 'Liste der Datenmodelle',
                    to: `/data-objects/${dataObjectSchema.key}/items`,
                }}
                entityType={ServerEntityType.DataObjectItems}
            />
        </PageWrapper>
    );
}