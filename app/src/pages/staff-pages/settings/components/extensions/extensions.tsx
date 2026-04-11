import React, {type ReactNode} from 'react';
import {Box, Button, Card, CardContent, Divider, Grid, Link, Skeleton, Stack, Typography} from '@mui/material';
import {alpha, type Theme} from '@mui/material/styles';
import ExtensionOutlinedIcon from '@mui/icons-material/ExtensionOutlined';
import HistoryOutlinedIcon from '@mui/icons-material/HistoryOutlined';
import WarningAmberOutlinedIcon from '@mui/icons-material/WarningAmberOutlined';
import WidgetsOutlinedIcon from '@mui/icons-material/WidgetsOutlined';
import {format} from 'date-fns';
import {
    type PluginComponentType,
    PluginComponentTypeDisplayNames,
    PluginComponentTypeOptions,
    type PluginDTO,
    PluginsApiService,
} from '../../../../../services/plugins-api-service';
import {AlertComponent} from '../../../../../components/alert/alert-component';
import {Badge} from '../../../../../components/badge/badge';
import Article from '@aivot/mui-material-symbols-400-outlined/dist/article/Article';
import {useConfirm} from '../../../../../providers/confirm-provider';
import {MarkdownContent} from '../../../../../components/markdown-content/markdown-content';
import {StatusTable} from '../../../../../components/status-table/status-table';
import {type StatusTablePropsItem} from '../../../../../components/status-table/status-table-props';
import {
    useGenericDetailsPageContext,
} from '../../../../../components/generic-details-page/generic-details-page-context';

export interface ExtensionsDetailsPageItem {
    plugins: PluginDTO[];
    loadingFailed: boolean;
}

type PluginComponentGroup = PluginDTO['components'][number];
type PluginComponentVersion = PluginComponentGroup[number];

function hasText(value?: string | null): value is string {
    return value != null && value.trim().length > 0;
}

function pluralize(count: number, singular: string, plural: string): string {
    return `${count} ${count === 1 ? singular : plural}`;
}

function getOrderedVersions(componentGroup: PluginComponentGroup): PluginComponentVersion[] {
    return [...componentGroup]
        .sort((a, b) => b.majorVersion - a.majorVersion);
}

function getComponentGroupsByType(plugin: PluginDTO, componentType: PluginComponentType): PluginComponentGroup[] {
    return plugin.components
        .filter((group) => group[0]?.componentType === componentType);
}

function countPluginLegacyVersions(plugin: PluginDTO): number {
    return plugin.components
        .reduce((total, componentGroup) => total + Math.max(componentGroup.length - 1, 0), 0);
}

function countLegacyVersions(plugins: PluginDTO[]): number {
    return plugins
        .reduce((total, plugin) => total + countPluginLegacyVersions(plugin), 0);
}

function countComponentGroups(plugins: PluginDTO[]): number {
    return plugins
        .reduce((total, plugin) => total + plugin.components.length, 0);
}

function countDeprecationNotices(plugins: PluginDTO[]): number {
    return plugins.reduce((total, plugin) => {
        const pluginDeprecationCount = hasText(plugin.deprecationNotice) ? 1 : 0;
        const componentDeprecationCount = plugin.components.reduce((componentTotal, componentGroup) => {
            return componentTotal + componentGroup.filter((version) => hasText(version.deprecationNotice)).length;
        }, 0);

        return total + pluginDeprecationCount + componentDeprecationCount;
    }, 0);
}

function getDeprecatedComponentVersions(plugin: PluginDTO): PluginComponentVersion[] {
    return plugin.components
        .flatMap((componentGroup) => componentGroup)
        .filter((version) => hasText(version.deprecationNotice));
}

function formatBuildDate(value: string): string {
    if (!hasText(value)) {
        return 'Nicht hinterlegt';
    }

    const parsedDate = new Date(value);
    if (Number.isNaN(parsedDate.getTime())) {
        return value;
    }

    return format(parsedDate, 'dd.MM.yyyy');
}

function MetadataItem(props: { label: string; children: ReactNode }) {
    return (
        <Box>
            <Typography
                component="dt"
                variant="overline"
                sx={{
                    display: 'block',
                    color: 'text.secondary',
                    lineHeight: 1.5,
                }}
            >
                {props.label}
            </Typography>

            <Box
                component="dd"
                sx={{
                    m: 0,
                    mt: 0.25,
                    typography: 'body2',
                    wordBreak: 'break-word',
                }}
            >
                {props.children}
            </Box>
        </Box>
    );
}

function subtleDividerColor(theme: Theme): string {
    return alpha(theme.palette.text.primary, 0.08);
}

type ConfirmFn = ReturnType<typeof useConfirm>;

function openDeprecationNotice(confirm: ConfirmFn, title: string, markdown: string) {
    confirm({
        title,
        children: (
            <MarkdownContent markdown={markdown}/>
        ),
        hideCancelButton: true,
        confirmButtonText: 'Schließen',
    });
}

function openDeprecatedComponentsDialog(confirm: ConfirmFn, plugin: PluginDTO) {
    const deprecatedComponentVersions = getDeprecatedComponentVersions(plugin);

    confirm({
        title: `Veraltete Komponenten: ${plugin.name}`,
        children: (
            <Stack spacing={2}>
                <Typography
                    variant="body2"
                    color="text.secondary"
                >
                    Diese Erweiterung enthält veraltete Komponenten oder Versionen. Prüfen Sie die folgenden Hinweise,
                    um notwendige Aktualisierungen oder Ablösungen planen zu können.
                </Typography>

                {
                    deprecatedComponentVersions.map((version, index) => (
                        <Box
                            key={`${version.key}-${version.componentVersion}-${index}`}
                            sx={{
                                pb: 2,
                                borderBottom: index < deprecatedComponentVersions.length - 1 ? '1px solid' : 'none',
                                borderColor: (theme) => subtleDividerColor(theme),
                            }}
                        >
                            <Typography variant="subtitle2">
                                {version.name}
                            </Typography>

                            <Typography
                                variant="body2"
                                color="text.secondary"
                                sx={{mt: 0.25}}
                            >
                                {PluginComponentTypeDisplayNames[version.componentType]} ·
                                                                                         Version {version.componentVersion}
                            </Typography>

                            <MarkdownContent
                                markdown={version.deprecationNotice!}
                                sx={{
                                    mt: 1,
                                    typography: 'body2',
                                }}
                            />
                        </Box>
                    ))
                }
            </Stack>
        ),
        hideCancelButton: true,
        confirmButtonText: 'Schließen',
    });
}

function DeprecationLink(props: {
    label?: string;
    onClick: () => void;
    compact?: boolean;
    inline?: boolean;
}) {
    const dense = props.compact || props.inline;

    return (
        <Box
            sx={{
                mt: props.inline ? 0 : props.compact ? 0.75 : 1,
                display: 'inline-flex',
                alignItems: 'center',
                gap: props.inline ? 0.5 : 0.75,
                flexWrap: 'wrap',
            }}
        >
            <WarningAmberOutlinedIcon
                sx={{
                    color: 'warning.main',
                    fontSize: props.inline ? 14 : props.compact ? 16 : 18,
                }}
            />

            <Typography
                variant={dense ? 'caption' : 'body2'}
                sx={{
                    color: 'warning.dark',
                    fontWeight: 600,
                }}
            >
                Veraltet
            </Typography>

            <Link
                component="button"
                type="button"
                underline="hover"
                onClick={props.onClick}
                sx={{
                    typography: dense ? 'caption' : 'body2',
                    color: 'warning.main',
                    cursor: 'pointer',
                    background: 'none',
                    border: 'none',
                    p: 0,
                }}
            >
                {props.label ?? 'Hinweis anzeigen'}
            </Link>
        </Box>
    );
}

function ExtensionsLoadingState() {
    return (
        <Stack spacing={3}>
            <Card variant="outlined">
                <CardContent sx={{
                    p: {
                        xs: 2.5,
                        md: 3,
                    },
                }}>
                    <Skeleton
                        variant="text"
                        width={240}
                        height={32}
                    />
                    <Skeleton
                        variant="text"
                        width="100%"
                        height={24}
                    />
                    <Skeleton
                        variant="text"
                        width="80%"
                        height={24}
                    />
                    <Skeleton
                        variant="rounded"
                        height={180}
                        sx={{mt: 2}}
                    />
                </CardContent>
            </Card>

            {
                Array.from({length: 2}).map((_, index) => (
                    <Card
                        key={index}
                        variant="outlined"
                    >
                        <CardContent sx={{
                            p: {
                                xs: 2.5,
                                md: 3,
                            },
                        }}>
                            <Skeleton
                                variant="text"
                                width={320}
                                height={34}
                            />
                            <Skeleton
                                variant="text"
                                width="55%"
                                height={22}
                            />
                            <Skeleton
                                variant="rounded"
                                height={320}
                                sx={{mt: 2}}
                            />
                        </CardContent>
                    </Card>
                ))
            }
        </Stack>
    );
}

function ExtensionsLoadErrorState() {
    return (
        <AlertComponent
            title="Erweiterungen konnten nicht geladen werden"
            color="error"
            text="Die installierten Erweiterungen konnten aktuell nicht geladen werden. Bitte laden Sie die Seite erneut oder prüfen Sie die Systeminformationen."
            sx={{
                mb: 0,
            }}
        />
    );
}

export async function loadExtensionsDetailsPageItem(): Promise<ExtensionsDetailsPageItem> {
    try {
        const plugins = await new PluginsApiService()
            .getPlugins();

        return {
            plugins,
            loadingFailed: false,
        };
    } catch (err) {
        console.error(err);

        return {
            plugins: [],
            loadingFailed: true,
        };
    }
}

function ExtensionComponentTypeSection(props: { plugin: PluginDTO; componentType: PluginComponentType }) {
    const {
        plugin,
        componentType,
    } = props;

    const confirm = useConfirm();
    const componentGroups = getComponentGroupsByType(plugin, componentType);
    const deprecatedComponentCount = componentGroups.reduce((total, componentGroup) => {
        return total + componentGroup.filter((version) => hasText(version.deprecationNotice)).length;
    }, 0);

    return (
        <Box
            sx={{
                border: '1px solid',
                borderColor: (theme) => subtleDividerColor(theme),
                borderRadius: 2,
                overflow: 'hidden',
                backgroundColor: 'background.paper',
            }}
        >
            <Box
                sx={{
                    px: 1.75,
                    py: 0.75,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    gap: 0.75,
                    flexWrap: 'wrap',
                    borderBottom: '1px solid',
                    borderColor: (theme) => subtleDividerColor(theme),
                    backgroundColor: (theme) => alpha(theme.palette.text.primary, 0.015),
                }}
            >
                <Box>
                    <Typography variant="subtitle2">
                        {PluginComponentTypeDisplayNames[componentType]}
                    </Typography>

                    <Typography
                        variant="caption"
                        color="text.secondary"
                        sx={{
                            display: 'block',
                            mt: -0.125,
                        }}
                    >
                        {pluralize(componentGroups.length, 'registrierte Komponente', 'registrierte Komponenten')}
                    </Typography>
                </Box>

                <Stack
                    direction="row"
                    spacing={1}
                    useFlexGap
                    flexWrap="wrap"
                    sx={{
                        justifyContent: 'flex-end',
                    }}
                >
                    {
                        deprecatedComponentCount > 0 &&
                        <Badge
                            label={`${deprecatedComponentCount} veraltet`}
                            color="warning"
                        />
                    }

                    <Badge
                        label={componentGroups.length}
                        color={componentGroups.length > 0 ? 'primary' : 'default'}
                    />
                </Stack>
            </Box>

            {
                componentGroups.length > 0 &&
                <Stack
                    spacing={0}
                    divider={
                        <Divider
                            sx={{
                                borderColor: (theme) => subtleDividerColor(theme),
                            }}
                        />
                    }
                >
                    {
                        componentGroups.map((componentGroup) => {
                            const orderedVersions = getOrderedVersions(componentGroup);
                            const currentVersion = orderedVersions[0];

                            if (currentVersion == null) {
                                return null;
                            }

                            return (
                                <Box
                                    key={`${currentVersion.key}-${currentVersion.componentVersion}`}
                                    sx={{
                                        px: 1.75,
                                        py: 0.5,
                                    }}
                                >
                                    <Box
                                        sx={{
                                            display: 'grid',
                                            gridTemplateColumns: 'minmax(0, 1fr) auto',
                                            columnGap: 0.5,
                                            rowGap: 0.125,
                                            py: 0.75,
                                            alignItems: 'start',
                                        }}
                                    >
                                        <Typography
                                            variant="body2"
                                            sx={{
                                                fontWeight: 600,
                                                minWidth: 0,
                                                lineHeight: 1.25,
                                            }}
                                        >
                                            {currentVersion.name}
                                        </Typography>

                                        <Box
                                            sx={{
                                                display: 'flex',
                                                alignItems: 'center',
                                                gap: 0.75,
                                                flexWrap: 'wrap',
                                                justifyContent: 'flex-end',
                                            }}
                                        >
                                            {
                                                hasText(currentVersion.deprecationNotice) &&
                                                <Badge
                                                    label="Veraltet"
                                                    color="warning"
                                                />
                                            }

                                            <Badge
                                                label={currentVersion.componentVersion}
                                                color="default"
                                            />

                                            {
                                                hasText(currentVersion.deprecationNotice) &&
                                                <DeprecationLink
                                                    compact
                                                    inline
                                                    label="Hinweis"
                                                    onClick={() => {
                                                        openDeprecationNotice(
                                                            confirm,
                                                            `Veraltete Komponente: ${currentVersion.name}`,
                                                            currentVersion.deprecationNotice!,
                                                        );
                                                    }}
                                                />
                                            }
                                        </Box>

                                        {
                                            hasText(currentVersion.description) &&
                                            <MarkdownContent
                                                markdown={currentVersion.description}
                                                sx={{
                                                    gridColumn: '1 / -1',
                                                    typography: 'body2',
                                                    color: 'text.secondary',
                                                    fontSize: (theme) => theme.typography.pxToRem(13),
                                                    '& p': {
                                                        my: 0,
                                                        lineHeight: 1.25,
                                                    },
                                                    '& ul, & ol': {
                                                        my: 0.2,
                                                        pl: 2.25,
                                                    },
                                                    maxWidth: 600,
                                                }}
                                            />
                                        }

                                        {
                                            orderedVersions.length > 1 &&
                                            <Box
                                                sx={{
                                                    gridColumn: '1 / -1',
                                                    display: 'flex',
                                                    alignItems: 'center',
                                                    flexWrap: 'wrap',
                                                    gap: 0.5,
                                                    pt: hasText(currentVersion.description) ? 0.125 : 0,
                                                }}
                                            >
                                                <Typography
                                                    variant="caption"
                                                    sx={{
                                                        color: 'text.secondary',
                                                        mr: 0.25,
                                                    }}
                                                >
                                                    Weitere Versionen:
                                                </Typography>

                                                {
                                                    orderedVersions
                                                        .slice(1)
                                                        .map((version) => (
                                                            <Box
                                                                key={`${version.key}-${version.componentVersion}`}
                                                                sx={{
                                                                    display: 'inline-flex',
                                                                    alignItems: 'center',
                                                                    gap: 0.5,
                                                                    flexWrap: 'wrap',
                                                                }}
                                                            >
                                                                <Typography
                                                                    variant="caption"
                                                                    sx={{
                                                                        fontWeight: 600,
                                                                        lineHeight: 1.3,
                                                                    }}
                                                                >
                                                                    {version.name}
                                                                </Typography>

                                                                <Badge
                                                                    label={version.componentVersion}
                                                                    color="default"
                                                                />

                                                                {
                                                                    hasText(version.deprecationNotice) &&
                                                                    <DeprecationLink
                                                                        compact
                                                                        inline
                                                                        label="Hinweis"
                                                                        onClick={() => {
                                                                            openDeprecationNotice(
                                                                                confirm,
                                                                                `Veraltete Komponente: ${version.name}`,
                                                                                version.deprecationNotice!,
                                                                            );
                                                                        }}
                                                                    />
                                                                }
                                                            </Box>
                                                        ))
                                                }
                                            </Box>
                                        }
                                    </Box>
                                </Box>
                            );
                        })
                    }
                </Stack>
            }
            {
                componentGroups.length === 0 &&
                <Box
                    sx={{
                        px: 1.75,
                        py: 0.875,
                    }}
                >
                    <Typography
                        variant="body2"
                        color="text.secondary"
                    >
                        Keine Komponenten dieses Typs vorhanden.
                    </Typography>
                </Box>
            }
        </Box>
    );
}

function ExtensionCard(props: { plugin: PluginDTO; onShowChangelog: (plugin: PluginDTO) => void }) {
    const {
        plugin,
        onShowChangelog,
    } = props;

    const confirm = useConfirm();
    const legacyVersions = countPluginLegacyVersions(plugin);
    const deprecatedComponentVersions = getDeprecatedComponentVersions(plugin);

    return (
        <Card
            variant="outlined"
            sx={{
                overflow: 'hidden',
                borderColor: 'divider',
                backgroundColor: 'background.paper',
            }}
        >
            <Box
                sx={{
                    p: {
                        xs: 2.5,
                        md: 3,
                    },
                    display: 'flex',
                    alignItems: 'flex-start',
                    justifyContent: 'space-between',
                    gap: 2,
                    flexWrap: 'wrap',
                    borderBottom: '1px solid',
                    borderColor: 'divider',
                    backgroundColor: (theme) => alpha(theme.palette.text.primary, 0.015),
                }}
            >
                <Box
                    sx={{
                        minWidth: 0,
                        flex: 1,
                    }}
                >
                    <Box
                        sx={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: 1,
                            flexWrap: 'wrap',
                        }}
                    >
                        <Typography variant="h5">
                            {plugin.name}
                        </Typography>

                        <Badge
                            label={`Version ${plugin.version}`}
                            color="default"
                        />

                        {
                            hasText(plugin.deprecationNotice) &&
                            <Badge
                                label="Erweiterung veraltet"
                                color="warning"
                            />
                        }

                        {
                            deprecatedComponentVersions.length > 0 &&
                            <Badge
                                label={pluralize(
                                    deprecatedComponentVersions.length,
                                    'veraltete Komponente',
                                    'veraltete Komponenten',
                                )}
                                color="warning"
                            />
                        }
                    </Box>

                    <Box
                        sx={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: 1.5,
                            flexWrap: 'wrap',
                            mt: 1,
                        }}
                    >
                        <Typography
                            variant="body2"
                            color="text.secondary"
                        >
                            von {plugin.vendorName}
                        </Typography>

                        {
                            hasText(plugin.vendorWebsite) &&
                            <>
                                &bull;
                                <Link
                                    href={plugin.vendorWebsite}
                                    target="_blank"
                                    rel="noopener noreferrer"
                                    underline="hover"
                                    sx={{
                                        typography: 'body2',
                                        wordBreak: 'break-word',
                                    }}
                                >
                                    {plugin.vendorWebsite}
                                </Link>
                            </>
                        }
                    </Box>
                </Box>

                <Stack
                    direction="row"
                    spacing={1}
                    useFlexGap
                    flexWrap="wrap"
                    sx={{
                        justifyContent: 'flex-end',
                    }}
                >
                    {
                        hasText(plugin.deprecationNotice) &&
                        <Button
                            variant="text"
                            size="small"
                            color="warning"
                            startIcon={<WarningAmberOutlinedIcon/>}
                            onClick={() => {
                                openDeprecationNotice(
                                    confirm,
                                    `Veraltete Erweiterung: ${plugin.name}`,
                                    plugin.deprecationNotice!,
                                );
                            }}
                        >
                            Hinweis anzeigen
                        </Button>
                    }

                    {
                        deprecatedComponentVersions.length > 0 &&
                        <Button
                            variant="text"
                            size="small"
                            color="warning"
                            startIcon={<WarningAmberOutlinedIcon/>}
                            onClick={() => {
                                openDeprecatedComponentsDialog(confirm, plugin);
                            }}
                        >
                            Komponenten-Hinweise
                        </Button>
                    }

                    <Button
                        variant="outlined"
                        size="small"
                        startIcon={<Article/>}
                        onClick={() => {
                            onShowChangelog(plugin);
                        }}
                    >
                        Changelog anzeigen
                    </Button>
                </Stack>
            </Box>

            <Box sx={{p: 0}}>
                <Grid container>
                    <Grid
                        size={{
                            xs: 12,
                            md: 4,
                        }}
                        sx={{
                            p: {
                                xs: 2.5,
                                md: 3,
                            },
                            borderBottom: {
                                xs: '1px solid',
                                md: 'none',
                            },
                            borderBottomColor: {
                                xs: (theme) => subtleDividerColor(theme),
                                md: 'transparent',
                            },
                            position: {
                                md: 'relative',
                            },
                            '&::after': {
                                content: '""',
                                display: {
                                    xs: 'none',
                                    md: 'block',
                                },
                                position: 'absolute',
                                top: 0,
                                right: 0,
                                bottom: 0,
                                width: '1px',
                                backgroundColor: (theme) => subtleDividerColor(theme),
                            },
                        }}
                    >
                        <Typography variant="subtitle1">
                            Überblick
                        </Typography>

                        <MarkdownContent
                            markdown={plugin.description}
                            sx={{
                                mt: 1.5,
                                typography: 'body2',
                                color: 'text.secondary',
                            }}
                        />

                        <Box
                            component="dl"
                            sx={{
                                m: 0,
                                mt: 3,
                                display: 'grid',
                                gridTemplateColumns: {
                                    xs: '1fr',
                                    sm: 'repeat(2, minmax(0, 1fr))',
                                    md: '1fr',
                                },
                                gap: 2,
                            }}
                        >
                            <MetadataItem label="Version">
                                {plugin.version}
                            </MetadataItem>

                            <MetadataItem label="Build-Datum">
                                {formatBuildDate(plugin.buildDate)}
                            </MetadataItem>

                            <MetadataItem label="Anbieter">
                                {plugin.vendorName}
                            </MetadataItem>

                            <MetadataItem label="Website">
                                {
                                    hasText(plugin.vendorWebsite) ?
                                        (
                                            <Link
                                                href={plugin.vendorWebsite}
                                                target="_blank"
                                                rel="noopener noreferrer"
                                                underline="hover"
                                            >
                                                {plugin.vendorWebsite}
                                            </Link>
                                        ) :
                                        'Nicht hinterlegt'
                                }
                            </MetadataItem>

                            <MetadataItem label="Komponenten">
                                {pluralize(plugin.components.length, 'Komponente', 'Komponenten')}
                            </MetadataItem>

                            <MetadataItem label="Weitere Versionen">
                                {
                                    legacyVersions > 0 ?
                                        pluralize(legacyVersions, 'Version', 'Versionen') :
                                        'Keine'
                                }
                            </MetadataItem>

                            <MetadataItem label="Veraltete Komponenten">
                                {
                                    deprecatedComponentVersions.length > 0 ?
                                        pluralize(
                                            deprecatedComponentVersions.length,
                                            'Komponente betroffen',
                                            'Komponenten betroffen',
                                        ) :
                                        'Keine'
                                }
                            </MetadataItem>
                        </Box>
                    </Grid>

                    <Grid
                        size={{
                            xs: 12,
                            md: 8,
                        }}
                        sx={{
                            p: {
                                xs: 2.5,
                                md: 3,
                            },
                        }}
                    >
                        <Typography variant="subtitle1">
                            Registrierte Komponenten
                        </Typography>

                        <Typography
                            variant="body2"
                            color="text.secondary"
                            sx={{
                                mt: 1,
                                mb: 2,
                                maxWidth: 900,
                            }}
                        >
                            Alle installierten Komponenten werden nach Typ gruppiert angezeigt. Bei mehreren Versionen
                            wird die aktuellste Fassung zuerst dargestellt.
                        </Typography>

                        <Stack spacing={1.25}>
                            {
                                PluginComponentTypeOptions.map((componentType) => (
                                    <Box
                                        key={componentType}
                                    >
                                        <ExtensionComponentTypeSection
                                            plugin={plugin}
                                            componentType={componentType}
                                        />
                                    </Box>
                                ))
                            }
                        </Stack>
                    </Grid>
                </Grid>
            </Box>
        </Card>
    );
}

export function ExtensionsSummary() {
    const {
        item,
    } = useGenericDetailsPageContext<ExtensionsDetailsPageItem, undefined>();

    if (item == null) {
        return (
            <ExtensionsLoadingState/>
        );
    }

    if (item.loadingFailed) {
        return (
            <ExtensionsLoadErrorState/>
        );
    }

    const loadedPlugins = item.plugins;
    const totalComponentGroups = countComponentGroups(loadedPlugins);
    const legacyVersionCount = countLegacyVersions(loadedPlugins);
    const deprecationNoticeCount = countDeprecationNotices(loadedPlugins);

    const summaryItems: StatusTablePropsItem[] = [
        {
            label: 'Erweiterungen',
            icon: <ExtensionOutlinedIcon/>,
            children: pluralize(loadedPlugins.length, 'installierte Erweiterung', 'installierte Erweiterungen'),
        },
        {
            label: 'Komponenten',
            icon: <WidgetsOutlinedIcon/>,
            children: pluralize(totalComponentGroups, 'registrierte Komponente', 'registrierte Komponenten'),
        },
        {
            label: 'Weitere Versionen',
            icon: <HistoryOutlinedIcon/>,
            children: legacyVersionCount > 0 ?
                pluralize(legacyVersionCount, 'zusätzliche Version', 'zusätzliche Versionen') :
                'Keine zusätzlichen Versionen hinterlegt',
        },
        {
            label: 'Veraltete Einträge',
            icon: <WarningAmberOutlinedIcon color={deprecationNoticeCount > 0 ? 'warning' : 'disabled'}/>,
            children: deprecationNoticeCount > 0 ?
                pluralize(deprecationNoticeCount, 'Eintrag markiert', 'Einträge markiert') :
                'Keine veralteten Einträge',
        },
    ];

    return (
        <Box>
            <Typography
                variant="subtitle1"
                component="h2"
            >
                Installierte Erweiterungen
            </Typography>

            <Typography
                sx={{
                    mt: 1,
                    maxWidth: 900,
                }}
            >
                Die Übersicht bündelt den aktuellen Erweiterungsbestand Ihrer Gover-Instanz. Sie zeigt auf einen Blick,
                wie groß der installierte Funktionsumfang ist, ob neben aktuellen Komponenten auch ältere Fassungen
                vorhanden sind und welche Erweiterungen oder Komponenten als veraltet markiert sind.
            </Typography>

            <StatusTable
                cardSx={{
                    mt: 2.5,
                }}
                sx={{mt: 0}}
                cardVariant="outlined"
                items={summaryItems}
            />

            <Typography
                variant="subtitle1"
                component="h2"
                sx={{mt: 4}}
            >
                Einordnung der Kennzahlen
            </Typography>

            <Typography
                sx={{
                    mt: 1,
                    maxWidth: 900,
                }}
            >
                Erweiterungen und Komponenten zeigen den zusätzlich installierten Funktionsumfang.
                Weitere Versionen weist auf parallel vorhandene ältere
                Versionen von Komponenten hin. Veraltete Einträge markieren Erweiterungen oder
                Komponenten, die bei Updates oder Bereinigungen geprüft werden sollten.
            </Typography>

            <Typography
                variant="body2"
                color="text.secondary"
                sx={{
                    mt: 1.5,
                    lineHeight: 1.6,
                }}
            >
                Die vollständigen Metadaten, Changelogs und nach Typ gruppierten Komponenten finden Sie im Reiter
                &bdquo;Liste der Erweiterungen&ldquo;.
            </Typography>

        </Box>
    );
}

export function ExtensionsList() {
    const {
        item,
    } = useGenericDetailsPageContext<ExtensionsDetailsPageItem, undefined>();
    const confirm = useConfirm();

    if (item == null) {
        return (
            <ExtensionsLoadingState/>
        );
    }

    if (item.loadingFailed) {
        return (
            <ExtensionsLoadErrorState/>
        );
    }

    const loadedPlugins = item.plugins;

    const handleShowChangelog = (plugin: PluginDTO) => {
        confirm({
            title: `Changelog: ${plugin.name}`,
            children: (
                <MarkdownContent
                    markdown={hasText(plugin.changelog) ? plugin.changelog : 'Kein Changelog hinterlegt.'}
                />
            ),
            hideCancelButton: true,
            confirmButtonText: 'Schließen',
        });
    };

    return (
        <Box>
            <Typography
                variant="subtitle1"
                component="h2"
            >
                Erweiterungen im Detail
            </Typography>

            <Typography
                sx={{
                    mt: 1,
                    maxWidth: 900,
                }}
            >
                Jede Erweiterung bündelt die zentralen Metadaten, Versionsinformationen und die zugehörigen
                Komponenten in einer strukturierten Detailansicht. Die aktuellste Version einer Komponente wird zuerst
                dargestellt; zusätzliche Versionen bleiben darunter nachvollziehbar erhalten.
            </Typography>

            <Box
                sx={{
                    my: 3,
                }}
            />

            {
                loadedPlugins.length === 0 ?
                    (
                        <AlertComponent
                            title="Keine Erweiterungen installiert"
                            color="info"
                            text="Auf dieser Instanz sind derzeit keine Erweiterungen registriert."
                            sx={{
                                mt: 0,
                                mb: 0,
                            }}
                        />
                    ) :
                    (
                        <Stack spacing={3}>
                            {
                                loadedPlugins.map((plugin) => (
                                    <ExtensionCard
                                        key={plugin.key}
                                        plugin={plugin}
                                        onShowChangelog={handleShowChangelog}
                                    />
                                ))
                            }
                        </Stack>
                    )
            }
        </Box>
    );
}
