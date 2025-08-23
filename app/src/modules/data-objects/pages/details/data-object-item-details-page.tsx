import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {Typography} from '@mui/material';
import {GenericDetailsPage} from '../../../../components/generic-details-page/generic-details-page';
import PaletteOutlinedIcon from '@mui/icons-material/PaletteOutlined';
import {DataObjectSchemasApiService} from '../../data-object-schemas-api-service';
import {useAdminGuard} from '../../../../hooks/use-admin-guard';
import {DataObjectSchema} from '../../models/data-object-schema';
import {useEffect, useState} from 'react';
import {useParams} from 'react-router-dom';
import {useApi} from '../../../../hooks/use-api';
import {LoadingPlaceholder} from '../../../../components/loading-placeholder/loading-placeholder';
import {DataObjectItemsApiService} from '../../data-object-items-api-service';
import {DataObjectItem} from '../../models/data-object-item';

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
                    icon: <PaletteOutlinedIcon />,
                    title: `Datenobjekt bearbeiten (${dataObjectSchema.name})`,
                    helpDialog: {
                        title: 'Hilfe zu Datenobjekten',
                        tooltip: 'Hilfe anzeigen',
                        content: (
                            <>
                                <Typography>
                                    Ein Farbschema ist eine Sammlung von Farben, die in der Benutzeroberfläche von Gover verwendet werden. Farbschemata können global oder für einzelne Formulare genutzt werden.
                                    So können Sie z. B. für verschiedene Fachbereiche oder Abteilungen unterschiedliche Farbschemata anlegen und nutzen.
                                </Typography>
                                <Typography sx={{mt: 2}}>
                                    Ein Farbschema besteht aus einem Namen und einer Liste von Farben. Bei der Auswahl der Farben sollte die Barrierfreiheit berücksichtigt werden.
                                </Typography>
                            </>
                        ),
                    },
                    actions: [
                        {
                            icon: <PaletteOutlinedIcon />,
                            tooltip: 'Datenobjektschema bearbeiten',
                            to: `/data-objects/${dataObjectSchema.key}`,
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