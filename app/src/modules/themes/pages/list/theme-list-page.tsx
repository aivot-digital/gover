import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import {Box, Typography} from '@mui/material';
import {DescriptionOutlined, EditOutlined} from '@mui/icons-material';
import {useSelector} from 'react-redux';
import {selectUser} from '../../../../slices/user-slice';
import {useMemo} from 'react';
import {isAdmin} from '../../../../utils/is-admin';
import {CellLink} from '../../../../components/cell-link/cell-link';
import {type Theme} from '../../models/theme';
import {ThemesApiService} from '../../themes-api-service';
import PaletteOutlinedIcon from '@mui/icons-material/PaletteOutlined';
import Chip from '@mui/material/Chip';
import {useAppSelector} from '../../../../hooks/use-app-selector';
import {selectSystemConfigValue} from '../../../../slices/system-config-slice';
import {SystemConfigKeys} from '../../../../data/system-config-keys';
import {useAdminGuard} from '../../../../hooks/use-admin-guard';

export function ThemeListPage() {
    useAdminGuard();

    const user = useSelector(selectUser);
    const userIsAdmin = useMemo(() => isAdmin(user), [user]);
    const appThemeId = useAppSelector(selectSystemConfigValue(SystemConfigKeys.system.theme));

    return (
        <PageWrapper
            title="Farbschemata"
            fullWidth
            background
        >
            <GenericListPage<Theme>
                header={{
                    icon: <PaletteOutlinedIcon />,
                    title: 'Farbschemata',
                    actions: [
                        {
                            label: 'Neues Farbschema',
                            icon: <AddOutlinedIcon />,
                            disabled: !userIsAdmin,
                            tooltip: userIsAdmin ? undefined : 'Sie müssen globale Administrator:in sein, um diese Aktion durchführen zu können.',
                            to: '/themes/new',
                            variant: 'contained',
                        },
                    ],
                    helpDialog: {
                        title: 'Hilfe zu Farbschemata',
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
                searchLabel="Farbschema suchen"
                searchPlaceholder="Name des Farbschemas eingeben…"
                fetch={(options) => {
                    return new ThemesApiService(options.api)
                        .list(
                            options.page,
                            options.size,
                            options.sort,
                            options.order,
                            {
                                name: options.search,
                            },
                        );
                }}
                columnDefinitions={[
                    {
                        field: 'icon',
                        headerName: '',
                        renderCell: () => <PaletteOutlinedIcon />,
                        disableColumnMenu: true,
                        width: 24,
                        sortable: false,
                    },
                    {
                        field: 'name',
                        headerName: 'Name',
                        flex: 1,
                        renderCell: (params) => (
                            <CellLink
                                to={`/themes/${params.id}`}
                                title={`Farbschema bearbeiten`}
                            >
                                {String(params.value)}
                                {params.row.id === Number(appThemeId) && <Chip label="Standard" color="info" variant="outlined" size={"small"} title="Aktives Farbschema der Gover-Instanz" sx={{ml:1}}/>}
                            </CellLink>
                        ),
                    },
                    {
                        field: 'colors',
                        headerName: 'Farben',
                        flex: 1,
                        disableColumnMenu: true,
                        sortable: false,
                        renderCell: (params) => {
                            const colors = params.row;
                            const colorKeys = ['main', 'mainDark', 'accent', '|', 'error', 'warning', 'info', 'success'];

                            return (
                                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                    {colorKeys.map((key, index) => (
                                        key === '|' ? (
                                            <Box key={index} sx={{ width: 2, height: 16, backgroundColor: '#D4D4D4', mx: 0.5 }} />
                                        ) : (
                                            <Box
                                                key={index}
                                                sx={{
                                                    position: 'relative',
                                                    width: 18,
                                                    height: 18,
                                                    borderRadius: '50%',
                                                    backgroundColor: colors[key as keyof typeof colors] || '#ccc',
                                                    border: '2px solid white',
                                                    margin: '0 5px 0 5px',
                                                    '::before': {
                                                        content: '""',
                                                        position: 'absolute',
                                                        display: 'block',
                                                        width: 20,
                                                        height: 20,
                                                        left: '-3px',
                                                        top: '-3px',
                                                        borderRadius: '50%',
                                                        backgroundColor: '#C0C0C0',
                                                        zIndex: -1,
                                                    }
                                                }}
                                            />
                                        )
                                    ))}
                                </Box>
                            );
                        }
                    },
                ]}
                getRowIdentifier={row => row.id.toString()}
                noDataPlaceholder="Keine Farbschemata angelegt"
                noSearchResultsPlaceholder="Keine Farbschemata gefunden"
                rowActionsCount={2}
                rowActions={(item: Theme) => [
                    {
                        icon: <EditOutlined />,
                        to: `/themes/${item.id}`,
                        tooltip: 'Farbschema bearbeiten',
                    },
                    {
                        icon: <DescriptionOutlined />,
                        to: `/themes/${item.id}/forms`,
                        tooltip: 'Formulare mit diesem Schema ansehen',
                    }
                ]}
                defaultSortField="name"
                disableFullWidthToggle={true}
            />
        </PageWrapper>
    );
}