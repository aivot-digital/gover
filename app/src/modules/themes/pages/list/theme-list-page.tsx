import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
import {useNavigate} from 'react-router-dom';
import {EmptyDataListPlaceholder} from '../../../../components/empty-data-list-placeholder/empty-data-list-placeholder';
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
import Visibility from '@aivot/mui-material-symbols-400-outlined/dist/visibility/Visibility';
import React, {useCallback, useMemo} from 'react';
import {GenericListPropsFetchOptions} from '../../../../components/generic-list/generic-list-props';
import {selectPermissions} from '../../../../slices/user-slice';
import {Permission} from '../../../../data/permissions/permission';
import {checkSystemPermission, formatMissingPermissionTooltip} from '../../../permissions/utils/permission-utils';
import {useHasSystemPermission} from '../../../permissions/hooks/use-permissions';

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
    const navigate = useNavigate();
    useHasSystemPermission(Permission.THEME_READ);
    const appThemeId = useAppSelector(selectSystemConfigValue(SystemConfigKeys.system.theme));
    const permissions = useAppSelector(selectPermissions);
    const canCreateTheme = checkSystemPermission(permissions, Permission.THEME_CREATE);
    const canUpdateThemes = checkSystemPermission(permissions, Permission.THEME_UPDATE);

    const header = useMemo(() => ({
        icon: <PaletteOutlinedIcon />,
        title: 'Erscheinungsbilder',
        actions: [
            {
                label: 'Neues Erscheinungsbild',
                icon: <AddOutlinedIcon />,
                to: '/themes/new',
                variant: 'contained' as const,
                disabled: !canCreateTheme,
                disabledTooltip: formatMissingPermissionTooltip(Permission.THEME_CREATE),
            },
        ],
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
    }), [canCreateTheme]);

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

    const columnDefinitions = useMemo(() => [
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
                        title={canUpdateThemes ? 'Erscheinungsbild bearbeiten' : 'Erscheinungsbild ansehen'}
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
    ], [appThemeId, canUpdateThemes]);

    const getRowIdentifier = useCallback((row: Theme) => row.id.toString(), []);

    const rowActions = useCallback((item: Theme) => [
        {
            icon: canUpdateThemes ? <EditOutlined /> : <Visibility/>,
            to: `/themes/${item.id}`,
            tooltip: canUpdateThemes ? 'Erscheinungsbild bearbeiten' : 'Erscheinungsbild ansehen',
        },
        {
            icon: <DescriptionOutlined />,
            to: `/themes/${item.id}/forms`,
            tooltip: 'Formulare mit diesem Erscheinungsbild ansehen',
        },
    ], [canUpdateThemes]);

    return (
        <PageWrapper
            title="Erscheinungsbilder"
            fullWidth
            background
        >
            <GenericListPage<Theme>
                header={header}
                searchLabel="Erscheinungsbild suchen"
                searchPlaceholder="Name des Erscheinungsbildes eingeben…"
                fetch={fetchThemes}
                columnDefinitions={columnDefinitions}
                getRowIdentifier={getRowIdentifier}
                noDataPlaceholder={
                    <EmptyDataListPlaceholder
                        title="Keine Erscheinungsbilder vorhanden"
                        description="Es wurden noch keine Erscheinungsbilder angelegt."
                        addText="Neues Erscheinungsbild anlegen"
                        onAdd={() => navigate('/themes/new')}
                        addDisabled={!canCreateTheme}
                        addDisabledTooltip={formatMissingPermissionTooltip(Permission.THEME_CREATE)}
                    />
                }
                noSearchResultsPlaceholder="Keine Erscheinungsbilder gefunden, die zu Ihrer Suche passen"
                rowActionsCount={2}
                rowActions={rowActions}
                defaultSortField="name"
                disableFullWidthToggle={true}
            />
        </PageWrapper>
    );
}
