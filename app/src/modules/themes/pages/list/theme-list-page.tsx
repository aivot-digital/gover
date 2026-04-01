import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import {Box, Typography} from '@mui/material';
import {DescriptionOutlined, EditOutlined} from '@mui/icons-material';
import {CellLink} from '../../../../components/cell-link/cell-link';
import {type Theme} from '../../models/theme';
import {ThemesApiService} from '../../themes-api-service';
import PaletteOutlinedIcon from '@mui/icons-material/PaletteOutlined';
import Chip from '@mui/material/Chip';
import {useAppSelector} from '../../../../hooks/use-app-selector';
import {selectSystemConfigValue} from '../../../../slices/system-config-slice';
import {SystemConfigKeys} from '../../../../data/system-config-keys';
import {CellContentWrapper} from '../../../../components/cell-content-wrapper/cell-content-wrapper';
import {useAccessGuard} from '../../../../hooks/use-admin-guard';
import Visibility from '@aivot/mui-material-symbols-400-outlined/dist/visibility/Visibility';

const activeThemeChip = (
    <Chip
        label="Standard"
        color="info"
        variant="outlined"
        size="small"
        title="Aktives Erscheinungsbild der Gover-Instanz"
        sx={{
            ml: 1,
        }}
    />
);

export function ThemeListPage() {
    const appThemeId = useAppSelector(selectSystemConfigValue(SystemConfigKeys.system.theme));

    const hasAccess = useAccessGuard({
        onlyGlobalAdmin: true,
        messageType: 'snackbar',
    });

    return (
        <PageWrapper
            title="Erscheinungsbilder"
            fullWidth
            background
        >
            <GenericListPage<Theme>
                header={{
                    icon: <PaletteOutlinedIcon />,
                    title: 'Erscheinungsbilder',
                    actions: [
                        {
                            label: 'Neues Erscheinungsbild',
                            icon: <AddOutlinedIcon />,
                            to: '/themes/new',
                            variant: 'contained',
                            disabled: !hasAccess,
                        },
                    ],
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
                searchLabel="Erscheinungsbild suchen"
                searchPlaceholder="Name des Erscheinungsbildes eingeben…"
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
                        renderCell: () => <CellContentWrapper><PaletteOutlinedIcon /></CellContentWrapper>,
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
                                title={hasAccess ? 'Erscheinungsbild bearbeiten' : 'Erscheinungsbild ansehen'}
                            >
                                {String(params.value)}
                                {params.row.id === Number(appThemeId) && activeThemeChip}
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
                                <CellContentWrapper sx={{gap: 1, position: 'relative', zIndex: 2}}>
                                    {colorKeys.map((key, index) => (
                                        key === '|' ? (
                                            <Box
                                                key={index}
                                                sx={{width: 2, height: 16, backgroundColor: '#D4D4D4', mx: 0.5}}
                                            />
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
                                                    },
                                                }}
                                            />
                                        )
                                    ))}
                                </CellContentWrapper>
                            );
                        },
                    },
                ]}
                getRowIdentifier={row => row.id.toString()}
                noDataPlaceholder="Keine Erscheinungsbilder angelegt"
                noSearchResultsPlaceholder="Keine Erscheinungsbilder gefunden"
                rowActionsCount={2}
                rowActions={(item: Theme) => [
                    {
                        icon: hasAccess ? <EditOutlined /> : <Visibility/>,
                        to: `/themes/${item.id}`,
                        tooltip: hasAccess ? 'Erscheinungsbild bearbeiten' : 'Erscheinungsbild ansehen',
                    },
                    {
                        icon: <DescriptionOutlined />,
                        to: `/themes/${item.id}/forms`,
                        tooltip: 'Formulare mit diesem Erscheinungsbild ansehen',
                    },
                ]}
                defaultSortField="name"
                disableFullWidthToggle={true}
            />
        </PageWrapper>
    );
}
