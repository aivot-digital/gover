import {
    GenericListPage,
    type GenericListPagePermissionConfig,
    type GenericListPagePermissionState,
} from '../../../../components/generic-list-page/generic-list-page';
import {useNavigate} from 'react-router-dom';
import {EmptyDataListPlaceholder} from '../../../../components/empty-data-list-placeholder/empty-data-list-placeholder';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import AddOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Add';
import {Box, Typography} from '@mui/material';
import DescriptionOutlined from '@aivot/mui-material-symbols-400-n25-outlined/Description';
import EditOutlined from '@aivot/mui-material-symbols-400-n25-outlined/Edit';
import {CellLink} from '../../../../components/cell-link/cell-link';
import {type Theme} from '../../models/theme';
import {ThemesApiService} from '../../themes-api-service';
import PaletteOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Palette';
import Chip from '@mui/material/Chip';
import {useAppSelector} from '../../../../hooks/use-app-selector';
import {selectSystemConfigValue} from '../../../../slices/system-config-slice';
import {SystemConfigKeys} from '../../../../data/system-config-keys';
import {CellContentWrapper} from '../../../../components/cell-content-wrapper/cell-content-wrapper';
import Visibility from '@aivot/mui-material-symbols-400-n25-outlined/Visibility';
import React, {useCallback} from 'react';
import {GenericListPropsFetchOptions} from '../../../../components/generic-list/generic-list-props';
import {Permission} from '../../../../data/permissions/permission';

const themeListPermissionCheck: GenericListPagePermissionConfig<Theme> = {
    scope: {
        type: 'system',
    },
    read: Permission.THEME_READ,
    create: Permission.THEME_CREATE,
    update: Permission.THEME_UPDATE,
};

const activeThemeChip = (
    <Chip
        label="Standard"
        color="info"
        variant="outlined"
        size="small"
        title="Aktives Erscheinungsbild der Prosuna-Instanz"
        sx={{
            ml: 1,
        }}
    />
);

function ThemeColorSwatch({color, title}: {color: string; title: string}) {
    return (
        <Box
            role="img"
            aria-label={`${title}: ${color}`}
            title={title}
            sx={{
                position: 'relative',
                width: 18,
                height: 18,
                flexShrink: 0,
                borderRadius: '50%',
                backgroundColor: color,
                border: '2px solid',
                borderColor: 'background.paper',
                margin: '0 5px',
                '::before': {
                    content: '""',
                    position: 'absolute',
                    display: 'block',
                    width: 20,
                    height: 20,
                    left: '-3px',
                    top: '-3px',
                    borderRadius: '50%',
                    backgroundColor: 'divider',
                    zIndex: -1,
                },
            }}
        />
    );
}

export function ThemeListPage() {
    const navigate = useNavigate();
    const appThemeId = useAppSelector(selectSystemConfigValue(SystemConfigKeys.system.theme));

    const header = useCallback((permissions: GenericListPagePermissionState<Theme>) => ({
        icon: <PaletteOutlinedIcon />,
        title: 'Erscheinungsbilder',
        actions: [
            {
                label: 'Neues Erscheinungsbild',
                icon: <AddOutlinedIcon />,
                to: '/themes/new',
                variant: 'contained' as const,
                disabled: !permissions.canCreate,
                disabledTooltip: permissions.createDisabledTooltip,
            },
        ],
        helpDialog: {
            title: 'Hilfe zu Erscheinungsbildern',
            tooltip: 'Hilfe anzeigen',
            content: (
                <>
                    <Typography>
                        Ein Erscheinungsbild legt Farben, Logo und Favicon für die Benutzeroberfläche von Prosuna fest. Erscheinungsbilder können global oder für einzelne Formulare verwendet werden.
                        So können Sie z. B. für verschiedene Organisationen oder Abteilungen unterschiedliche Erscheinungsbilder anlegen und nutzen.
                    </Typography>
                    <Typography sx={{mt: 2}}>
                        Ein Erscheinungsbild besteht aus einem Namen, Farben sowie optional einem Logo und Favicon. Bei der Auswahl der Farben sollte die Barrierefreiheit berücksichtigt werden.
                    </Typography>
                </>
            ),
        },
    }), []);

    const fetchThemes = useCallback((options: GenericListPropsFetchOptions<Theme>) => {
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
    }, []);

    const columnDefinitions = useCallback((permissions: GenericListPagePermissionState<Theme>) => [
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
            renderCell: (params: any) => {
                const content = (
                    <>
                        {String(params.value)}
                        {params.row.id === Number(appThemeId) && activeThemeChip}
                    </>
                );

                return (
                    <CellLink
                        to={`/themes/${params.id}`}
                        title={permissions.canUpdate(params.row) ? 'Erscheinungsbild bearbeiten' : 'Erscheinungsbild ansehen'}
                    >
                        {content}
                    </CellLink>
                );
            },
        },
        {
            field: 'colors',
            headerName: 'Farben',
            flex: 1,
            disableColumnMenu: true,
            sortable: false,
            renderCell: (params: any) => {
                const theme = params.row as Theme;
                const lightColors = [
                    {color: theme.primaryColor, title: 'Primärfarbe'},
                    {color: theme.secondaryColor, title: 'Sekundärfarbe'},
                ];
                const darkColors = [
                    theme.primaryColorDark == null
                        ? null
                        : {color: theme.primaryColorDark, title: 'Primärfarbe im dunklen Farbschema'},
                    theme.secondaryColorDark == null
                        ? null
                        : {color: theme.secondaryColorDark, title: 'Sekundärfarbe im dunklen Farbschema'},
                ].filter((color): color is {color: string; title: string} => color != null);

                return (
                    <CellContentWrapper sx={{gap: 1, position: 'relative', zIndex: 2}}>
                        {lightColors.map(({color, title}) => (
                            <ThemeColorSwatch
                                key={title}
                                color={color}
                                title={title}
                            />
                        ))}
                        {darkColors.length > 0 && (
                            <>
                                <Box
                                    aria-hidden
                                    sx={{
                                        flex: '0 0 1px',
                                        width: '1px',
                                        minWidth: '1px',
                                        maxWidth: '1px',
                                        height: 18,
                                        backgroundColor: 'divider',
                                        mx: 0.5,
                                    }}
                                />
                                {darkColors.map(({color, title}) => (
                                    <ThemeColorSwatch
                                        key={title}
                                        color={color}
                                        title={title}
                                    />
                                ))}
                            </>
                        )}
                    </CellContentWrapper>
                );
            },
        },
    ], [appThemeId]);

    const getRowIdentifier = useCallback((row: Theme) => row.id.toString(), []);

    const rowActions = useCallback((item: Theme, permissions: GenericListPagePermissionState<Theme>) => {
        const canUpdateTheme = permissions.canUpdate(item);

        return [
            {
                icon: canUpdateTheme ? <EditOutlined /> : <Visibility/>,
                to: `/themes/${item.id}`,
                tooltip: canUpdateTheme ? 'Erscheinungsbild bearbeiten' : 'Erscheinungsbild ansehen',
            },
            {
                icon: <DescriptionOutlined />,
                to: `/themes/${item.id}/forms`,
                tooltip: 'Formulare mit diesem Erscheinungsbild ansehen',
            },
        ];
    }, []);

    const noDataPlaceholder = useCallback((permissions: GenericListPagePermissionState<Theme>) => (
        <EmptyDataListPlaceholder
            title="Keine Erscheinungsbilder vorhanden"
            description="Es wurden noch keine Erscheinungsbilder angelegt."
            addText="Neues Erscheinungsbild anlegen"
            onAdd={() => navigate('/themes/new')}
            addDisabled={!permissions.canCreate}
            addDisabledTooltip={permissions.createDisabledTooltip}
        />
    ), [navigate]);

    return (
        <PageWrapper
            title="Erscheinungsbilder"
            fullWidth
            background
        >
            <GenericListPage<Theme>
                header={header}
                permissionCheck={themeListPermissionCheck}
                searchLabel="Erscheinungsbild suchen"
                searchPlaceholder="Name des Erscheinungsbildes eingeben…"
                fetch={fetchThemes}
                columnDefinitions={columnDefinitions}
                getRowIdentifier={getRowIdentifier}
                noDataPlaceholder={noDataPlaceholder}
                noSearchResultsPlaceholder="Keine Erscheinungsbilder gefunden, die zu Ihrer Suche passen"
                rowActionsCount={2}
                rowActions={rowActions}
                defaultSortField="name"
                disableFullWidthToggle={true}
            />
        </PageWrapper>
    );
}
