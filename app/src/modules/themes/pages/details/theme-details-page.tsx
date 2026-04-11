import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {Typography} from '@mui/material';
import {GenericDetailsPage} from '../../../../components/generic-details-page/generic-details-page';
import PaletteOutlinedIcon from '@mui/icons-material/PaletteOutlined';
import {type Theme} from '../../models/theme';
import {ThemesApiService} from '../../themes-api-service';
import {ServerEntityType} from '../../../../shells/staff/data/server-entity-type';
import {useUserIsAdmin} from '../../../../hooks/use-admin-guard';

export function ThemeDetailsPage() {
    const userIsAdmin = useUserIsAdmin();

    return (
        <PageWrapper
            title="Erscheinungsbild bearbeiten"
            fullWidth
            background
        >
            <GenericDetailsPage<Theme, number, undefined>
                isEditable={() => userIsAdmin}
                header={{
                    icon: <PaletteOutlinedIcon />,
                    title: 'Erscheinungsbild bearbeiten',
                    helpDialog: {
                        title: 'Hilfe zu Erscheinungsbildern',
                        tooltip: 'Hilfe anzeigen',
                        content: (
                            <>
                                <Typography>
                                    Ein Erscheinungsbild legt Farben, Logo und Favicon für die Benutzeroberfläche von Gover fest. Erscheinungsbilder können global oder für einzelne Formulare verwendet werden.
                                    So können Sie z. B. für verschiedene Fachbereiche oder Abteilungen unterschiedliche Erscheinungsbilder anlegen und nutzen.
                                </Typography>
                                <Typography sx={{mt: 2}}>
                                    Ein Erscheinungsbild besteht aus einem Namen, Farben sowie optional einem Logo und Favicon. Bei der Auswahl der Farben sollte die Barrierefreiheit berücksichtigt werden.
                                </Typography>
                            </>
                        ),
                    },
                }}
                tabs={[
                    {
                        path: '/themes/:id',
                        label: 'Allgemeine Angaben',
                    },
                    {
                        path: '/themes/:id/forms',
                        label: 'Formulare',
                        isDisabled: (item) => !item?.id,
                    },
                    {
                        path: '/themes/:id/departments',
                        label: 'Fachbereiche',
                        isDisabled: (item) => !item?.id,
                    },
                ]}
                initializeItem={(api) => new ThemesApiService(api).initialize()}
                fetchData={(api, id: number) => new ThemesApiService(api).retrieve(id)}
                getTabTitle={(item: Theme) => {
                    if (item.id === 0) {
                        return 'Neues Erscheinungsbild';
                    } else {
                        return item.name;
                    }
                }}
                getHeaderTitle={(item, isNewItem, notFound) => {
                    if (notFound) return 'Erscheinungsbild nicht gefunden';
                    if (isNewItem) return 'Neues Erscheinungsbild anlegen';
                    return `Erscheinungsbild: ${item?.name ?? 'Unbenannt'}`;
                }}
                parentLink={{
                    label: 'Liste der Erscheinungsbilder',
                    to: '/themes',
                }}
                entityType={ServerEntityType.Themes}
            />
        </PageWrapper>
    );
}
