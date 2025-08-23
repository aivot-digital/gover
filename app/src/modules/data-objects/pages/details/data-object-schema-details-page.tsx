import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {Typography} from '@mui/material';
import {GenericDetailsPage} from '../../../../components/generic-details-page/generic-details-page';
import PaletteOutlinedIcon from '@mui/icons-material/PaletteOutlined';
import {DataObjectSchemasApiService} from '../../data-object-schemas-api-service';
import {useAdminGuard} from '../../../../hooks/use-admin-guard';
import {DataObjectSchema} from '../../models/data-object-schema';

export function DataObjectSchemaDetailsPage() {
    useAdminGuard();

    return (
        <PageWrapper
            title="Datenobjektschema bearbeiten"
            fullWidth
            background
        >
            <GenericDetailsPage<DataObjectSchema, string, undefined>
                idParam="key"
                header={{
                    icon: <PaletteOutlinedIcon />,
                    title: 'Datenobjektschema bearbeiten',
                    helpDialog: {
                        title: 'Hilfe zu Datenobjektschemata',
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
                }}
                tabs={[
                    {
                        path: '/data-objects/:key',
                        label: 'Allgemeine Angaben',
                    },
                    {
                        path: '/data-objects/:key/items',
                        label: 'Datenobjekte',
                        isDisabled: (item) => item?.key === '',
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