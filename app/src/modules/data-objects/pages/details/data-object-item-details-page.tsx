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
            title={`Datenobjekt bearbeiten (${dataObjectSchema.name})`}
            fullWidth
            background
        >
            <GenericDetailsPage<DataObjectItem, string, void>
                header={{
                    icon: <CategoryOutlinedIcon />,
                    title: `Datenobjekt bearbeiten (${dataObjectSchema.name})`,
                    helpDialog: {
                        title: 'Hilfe zu Datenobjekten',
                        tooltip: 'Hilfe anzeigen',
                        content: (
                            <>
                                <Typography>
                                    Ein Datenobjekt ist eine konkrete Instanz eines Datenmodells in Gover: Es enthält die tatsächlichen Werte zu den im Schema definierten Feldern. Datenobjekte werden in Prozessen, Komponenten und Schnittstellen weiterverarbeitet und stellen damit die „laufenden“ Fachinformationen dar. Sie sind stets an ein Schema gebunden, das Struktur, Datentypen und Prüfregeln vorgibt.
                                </Typography>
                                <Typography sx={{ mt: 2 }}>
                                    Typischerweise umfasst ein Datenobjekt Werte für Text-, Zahlen-, Datums- oder Wahrheitsfelder sowie ggf. verschachtelte Strukturen. Neben Nutzdaten können Metadaten enthalten sein, etwa Erstell- und Änderungszeitpunkte, Quelle/Ersteller:in, Status oder Referenzen auf verknüpfte Objekte. Standardwerte aus dem Schema werden beim Anlegen übernommen und Validierungen sorgen dafür, dass nur erlaubte, vollständige und konsistente Inhalte gespeichert werden.
                                </Typography>
                            </>
                        ),
                    },
                    actions: [
                        {
                            icon: <DataArrayOutlinedIcon />,
                            tooltip: 'Datenobjektschema bearbeiten',
                            to: `/data-objects/${dataObjectSchema.key}`,
                            variant: 'outlined',
                            label: 'Datenobjektschema bearbeiten',
                        },
                    ],
                }}
                tabs={[
                    {
                        path: `/data-objects/${dataObjectSchema.key}/items/:id`,
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
                    label: 'Liste der Datenobjektschemata',
                    to: `/data-objects/${dataObjectSchema.key}/items`,
                }}
            />
        </PageWrapper>
    );
}