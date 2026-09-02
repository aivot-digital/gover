import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {Typography} from '@mui/material';
import {GenericDetailsPage} from '../../../../components/generic-details-page/generic-details-page';
import PaletteOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Palette';
import {type Theme} from '../../models/theme';
import {ThemesApiService} from '../../themes-api-service';
import {ServerEntityType} from '../../../../shells/staff/data/server-entity-type';
import {Permission} from '../../../../data/permissions/permission';

export function ThemeDetailsPage() {
    return (
        <PageWrapper
            title="Erscheinungsbild bearbeiten"
            fullWidth
            background
        >
            <GenericDetailsPage<Theme, number, undefined>
                permissionCheck={{
                    create: Permission.THEME_CREATE,
                    read: Permission.THEME_READ,
                    update: Permission.THEME_UPDATE,
                    scope: {
                        type: 'system',
                    },
                }}
                header={{
                    icon: <PaletteOutlinedIcon />,
                    title: 'Erscheinungsbild bearbeiten',
                    helpDialog: {
                        title: 'Hilfe zu Erscheinungsbildern',
                        tooltip: 'Hilfe anzeigen',
                        content: (
                            <>
                                <Typography>
                                    Ein Erscheinungsbild legt Farben, Logo und Favicon für die Benutzeroberfläche von
                                    Prosuna fest. Es kann als Standard der Prosuna-Instanz oder für einzelne
                                    Organisationseinheiten verwendet werden.
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
                        path: '/themes/:id/departments',
                        label: 'Organisationseinheiten',
                        onlyExisting: true,
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
