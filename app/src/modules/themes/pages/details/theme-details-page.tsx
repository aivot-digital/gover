import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {Typography} from '@mui/material';
import {GenericDetailsPage} from '../../../../components/generic-details-page/generic-details-page';
import PaletteOutlinedIcon from '@mui/icons-material/PaletteOutlined';
import {type Theme} from '../../models/theme';
import {ThemesApiService} from '../../themes-api-service';
import {ServerEntityType} from '../../../../shells/staff/data/server-entity-type';
import {Permission} from '../../../../data/permissions/permission';
import {useCallback} from 'react';
import {useAppSelector} from '../../../../hooks/use-app-selector';
import {selectPermissions} from '../../../../slices/user-slice';
import {checkSystemPermission, hasSystemPermission} from '../../../permissions/utils/permission-utils';

export function ThemeDetailsPage() {
    const permissions = useAppSelector(selectPermissions);
    const canCreateTheme = checkSystemPermission(permissions, Permission.THEME_CREATE);
    const canUpdateTheme = checkSystemPermission(permissions, Permission.THEME_UPDATE);
    const isEditable = useCallback((item: Theme | undefined) => {
        if (item == null) {
            return false;
        }

        return item.id === 0 ? canCreateTheme : canUpdateTheme;
    }, [canCreateTheme, canUpdateTheme]);
    const hasAccess = useCallback((item: Theme | undefined) => {
        if (item == null) {
            return;
        }

        hasSystemPermission(permissions, item.id === 0
            ? Permission.THEME_CREATE
            : Permission.THEME_READ);
    }, [permissions]);

    return (
        <PageWrapper
            title="Erscheinungsbild bearbeiten"
            fullWidth
            background
        >
            <GenericDetailsPage<Theme, number, undefined>
                hasAccess={hasAccess}
                isEditable={isEditable}
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
                                    So können Sie z. B. für verschiedene Organisationen oder Abteilungen unterschiedliche Erscheinungsbilder anlegen und nutzen.
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
