import {Avatar, Box, Breadcrumbs, Skeleton, Typography, useTheme} from '@mui/material';
import React, {type ReactNode, useMemo, useState} from 'react';
import {Link} from 'react-router-dom';
import Fuse from 'fuse.js';
import {type Action} from '../../../components/actions/actions-props';
import {Actions} from '../../../components/actions/actions';
import {AlertComponent} from '../../../components/alert/alert-component';
import {SearchInput} from '../../../components/search-input/search-input';
import {StringAvatar} from '../../../components/avatar/string-avatar';
import {type VDepartmentShadowedEntityWithChildren} from '../entities/v-department-shadowed-entity';
import {getDepartmentTypeIcons, getDepartmentTypeLabel} from '../utils/department-utils';

interface SearchableDepartment {
    id: number;
    name: string;
    address: string;
    type: string;
    path: string;
    pathParts: string[];
    department: VDepartmentShadowedEntityWithChildren;
}

interface DepartmentTreeSkeletonNode {
    id: string;
    nameWidth: number | string;
    subtitleWidth: number | string;
    addressWidth: number | string;
    children?: DepartmentTreeSkeletonNode[];
}

interface DepartmentBrowserProps {
    departments?: VDepartmentShadowedEntityWithChildren[];
    loadError?: boolean;
    emptyState?: ReactNode;
    getActions?: (department: VDepartmentShadowedEntityWithChildren) => Action[];
    getDepartmentHref?: (department: VDepartmentShadowedEntityWithChildren) => string | undefined;
    searchLabel?: string;
    searchPlaceholder?: string;
    loadErrorMessage?: string;
    noSearchResultsMessage?: string;
    selectedDepartmentId?: number | null;
}

interface DepartmentTreeItemProps {
    department: VDepartmentShadowedEntityWithChildren;
    getActions?: (department: VDepartmentShadowedEntityWithChildren) => Action[];
    getDepartmentHref?: (department: VDepartmentShadowedEntityWithChildren) => string | undefined;
    selectedDepartmentId?: number | null;
}

interface DepartmentSearchResultItemProps {
    result: SearchableDepartment;
    getActions?: (department: VDepartmentShadowedEntityWithChildren) => Action[];
    getDepartmentHref?: (department: VDepartmentShadowedEntityWithChildren) => string | undefined;
    selectedDepartmentId?: number | null;
}

interface DepartmentTreeSkeletonItemProps {
    node: DepartmentTreeSkeletonNode;
}

const TREE_CONNECTOR = {
    iconCenterX: 35,
    childrenIndent: 52,
    iconCenterY: 33,
    elbowSize: 14,
};

const DEPARTMENT_TREE_LOADING_SKELETON: DepartmentTreeSkeletonNode = {
    id: 'root',
    nameWidth: '44%',
    subtitleWidth: '18%',
    addressWidth: 250,
    children: [
        {
            id: 'level-2-a',
            nameWidth: '39%',
            subtitleWidth: '21%',
            addressWidth: 220,
            children: [
                {
                    id: 'level-3-a',
                    nameWidth: '36%',
                    subtitleWidth: '23%',
                    addressWidth: 190,
                },
            ],
        },
        {
            id: 'level-2-b',
            nameWidth: '42%',
            subtitleWidth: '20%',
            addressWidth: 210,
        },
    ],
};

export function DepartmentBrowser(props: DepartmentBrowserProps): React.ReactElement {
    const {
        departments,
        loadError = false,
        emptyState,
        getActions,
        getDepartmentHref,
        searchLabel = 'Organisationseinheiten durchsuchen',
        searchPlaceholder = 'Name, Adresse oder Typ suchen…',
        loadErrorMessage = 'Die Organisationseinheiten konnten nicht geladen werden.',
        noSearchResultsMessage = 'Keine Organisationseinheiten für den Suchbegriff gefunden.',
        selectedDepartmentId,
    } = props;

    const [search, setSearch] = useState('');

    const searchableDepartments = useMemo<SearchableDepartment[]>(() => {
        if (departments == null) {
            return [];
        }

        const flattened: SearchableDepartment[] = [];
        const appendDepartments = (nodes: VDepartmentShadowedEntityWithChildren[], parentPath: string[]): void => {
            for (const department of nodes) {
                const pathSegments = [
                    ...parentPath,
                    department.name,
                ];

                flattened.push({
                    id: department.id,
                    name: department.name,
                    address: formatAddress(department.postalAddress),
                    type: getDepartmentTypeLabel(department.depth),
                    path: pathSegments.join(' > '),
                    pathParts: pathSegments,
                    department,
                });

                if (department.children.length > 0) {
                    appendDepartments(department.children, pathSegments);
                }
            }
        };

        appendDepartments(departments, []);
        return flattened;
    }, [departments]);

    const fuse = useMemo(() => {
        return new Fuse(searchableDepartments, {
            keys: [
                'name',
                'address',
                'type',
                'path',
            ],
            threshold: 0.3,
            shouldSort: true,
            minMatchCharLength: 2,
            ignoreLocation: true,
        });
    }, [searchableDepartments]);

    const cleanedSearch = search.trim();

    const searchResults = useMemo(() => {
        if (cleanedSearch.length === 0) {
            return [];
        }

        return fuse
            .search(cleanedSearch)
            .map((result) => result.item);
    }, [
        cleanedSearch,
        fuse,
    ]);

    return (
        <>
            {
                (departments == null || departments.length > 0) &&
                <SearchInput
                    value={search}
                    onChange={setSearch}
                    label={searchLabel}
                    placeholder={searchPlaceholder}
                    size="small"
                    debounce={200}
                    sx={{
                        mt: 1,
                        mb: 2,
                    }}
                />
            }

            {
                departments == null ?
                    <DepartmentTreeLoadingSkeleton /> :
                    loadError ?
                        <AlertComponent
                            color="error"
                            sx={{my: 1}}
                        >
                            {loadErrorMessage}
                        </AlertComponent> :
                        departments.length === 0 ?
                            (emptyState ?? (
                                <AlertComponent
                                    color="info"
                                    sx={{my: 1}}
                                >
                                    Es sind keine Organisationseinheiten vorhanden.
                                </AlertComponent>
                            )) :
                            cleanedSearch.length > 0 ?
                                <Box
                                    sx={{
                                        display: 'grid',
                                        gap: 2.25,
                                        minWidth: 0,
                                    }}
                                >
                                    {
                                        searchResults.map((result) => (
                                            <DepartmentSearchResultItem
                                                key={result.id}
                                                result={result}
                                                getActions={getActions}
                                                getDepartmentHref={getDepartmentHref}
                                                selectedDepartmentId={selectedDepartmentId}
                                            />
                                        ))
                                    }
                                </Box> :
                                <Box
                                    sx={{
                                        display: 'grid',
                                        gap: 2.25,
                                        minWidth: 0,
                                    }}
                                >
                                    {
                                        departments.map((department) => (
                                            <DepartmentTreeItem
                                                key={department.id}
                                                department={department}
                                                getActions={getActions}
                                                getDepartmentHref={getDepartmentHref}
                                                selectedDepartmentId={selectedDepartmentId}
                                            />
                                        ))
                                    }
                                </Box>
            }

            {
                !loadError &&
                departments != null &&
                departments.length > 0 &&
                cleanedSearch.length > 0 &&
                searchResults.length === 0 &&
                <AlertComponent
                    color="info"
                    sx={{my: 1}}
                >
                    {noSearchResultsMessage}
                </AlertComponent>
            }
        </>
    );
}

function formatAddress(value?: string | null): string {
    if (value == null) {
        return '';
    }

    return value
        .split(/[\n,]+/g)
        .map((part) => part.trim())
        .filter((part) => part.length > 0)
        .join(', ');
}

function DepartmentName(props: {
    department: VDepartmentShadowedEntityWithChildren;
    subtitle: string;
    href?: string;
}): React.ReactElement {
    const {
        department,
        subtitle,
        href,
    } = props;

    if (href != null) {
        return (
            <Box
                component={Link}
                to={href}
                sx={{
                    display: 'flex',
                    flexDirection: 'column',
                    textDecoration: 'none',
                    minWidth: 0,
                    flex: 1,
                    mr: 1,
                }}
            >
                <Typography
                    variant="subtitle1"
                    color="text.primary"
                    title={department.name}
                    sx={{
                        'fontWeight': 700,
                        'overflow': 'hidden',
                        'textOverflow': 'ellipsis',
                        'whiteSpace': 'nowrap',
                        'textDecoration': 'none',
                        '&:hover': {
                            textDecoration: 'underline',
                        },
                    }}
                >
                    {department.name}
                </Typography>

                <Typography
                    variant="caption"
                    color="text.secondary"
                    sx={{
                        alignSelf: 'flex-start',
                        mt: -0.25,
                    }}
                >
                    {subtitle}
                </Typography>
            </Box>
        );
    }

    return (
        <Box
            sx={{
                display: 'flex',
                flexDirection: 'column',
                minWidth: 0,
                flex: 1,
                mr: 1,
            }}
        >
            <Typography
                variant="subtitle1"
                color="text.primary"
                title={department.name}
                sx={{
                    fontWeight: 700,
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap',
                }}
            >
                {department.name}
            </Typography>

            <Typography
                variant="caption"
                color="text.secondary"
                sx={{
                    alignSelf: 'flex-start',
                    mt: -0.25,
                }}
            >
                {subtitle}
            </Typography>
        </Box>
    );
}

function DepartmentSearchResultItem(props: DepartmentSearchResultItemProps): React.ReactElement {
    const theme = useTheme();
    const {
        result,
        getActions,
        getDepartmentHref,
        selectedDepartmentId,
    } = props;

    const {
        department,
    } = result;

    const actions = getActions?.(department) ?? [];
    const addressText = result.address.length > 0 ? result.address : 'Keine Adresse hinterlegt';
    const isSelected = selectedDepartmentId === department.id;

    return (
        <Box sx={{minWidth: 0}}>
            <Breadcrumbs
                separator="›"
                maxItems={5}
                itemsBeforeCollapse={2}
                itemsAfterCollapse={2}
                sx={{
                    'ml': 1,
                    'mb': 0.75,
                    'color': 'text.secondary',
                    '& .MuiBreadcrumbs-ol': {
                        flexWrap: 'nowrap',
                        overflow: 'hidden',
                    },
                }}
            >
                {
                    result.pathParts.map((segment, index) => (
                        <Typography
                            key={`${result.id}-${index}`}
                            variant="caption"
                            color="text.secondary"
                            sx={{
                                maxWidth: 220,
                                overflow: 'hidden',
                                textOverflow: 'ellipsis',
                                whiteSpace: 'nowrap',
                            }}
                            title={segment}
                        >
                            {segment}
                        </Typography>
                    ))
                }
            </Breadcrumbs>

            <Box
                sx={{
                    'display': 'flex',
                    'alignItems': 'center',
                    'gap': 1.5,
                    'minWidth': 0,
                    'py': 1.75,
                    'px': 2,
                    'border': '1px solid',
                    'borderColor': isSelected ? 'primary.main' : 'divider',
                    'borderRadius': 2,
                    'bgcolor': isSelected ? 'action.selected' : 'background.paper',
                    'transition': 'background-color .2s ease',
                    '&:hover': {
                        bgcolor: isSelected ? 'action.selected' : 'action.hover',
                    },
                }}
            >
                <StringAvatar
                    name={department.name}
                    backgroundMode={'oklch'}
                    showInitials={false}
                    sx={{
                        width: 38,
                        height: 38,
                        border: '1px solid',
                        borderColor: 'divider',
                        '& svg': {
                            fontSize: 22,
                        },
                    }}
                >
                    {getDepartmentTypeIcons(department.depth)}
                </StringAvatar>

                <Box
                    sx={{
                        display: 'flex',
                        alignItems: 'flex-start',
                        minWidth: 0,
                        flex: 1,
                    }}
                >
                    <DepartmentName
                        department={department}
                        subtitle={result.type}
                        href={getDepartmentHref?.(department)}
                    />
                </Box>

                <Typography
                    sx={{
                        ml: 1,
                        display: {
                            xs: 'none',
                            md: 'block',
                        },
                        minWidth: 0,
                        flexShrink: 1,
                        whiteSpace: 'nowrap',
                        maxWidth: {
                            md: 220,
                            lg: 320,
                        },
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                    }}
                    variant="body2"
                    color="text.secondary"
                    title={addressText}
                >
                    {addressText}
                </Typography>

                {
                    actions.length > 0 &&
                    <Actions
                        size="small"
                        dense={true}
                        actions={actions}
                        sx={{
                            ml: 2,
                            flexShrink: 0,
                        }}
                    />
                }
            </Box>
        </Box>
    );
}

function DepartmentTreeLoadingSkeleton(): React.ReactElement {
    return (
        <Box
            sx={{
                display: 'grid',
                gap: 2.5,
            }}
        >
            <DepartmentTreeSkeletonItem node={DEPARTMENT_TREE_LOADING_SKELETON} />
        </Box>
    );
}

function DepartmentTreeSkeletonItem(props: DepartmentTreeSkeletonItemProps): React.ReactElement {
    const theme = useTheme();
    const {
        node,
    } = props;

    const connectorColor = theme.palette.mode === 'dark' ?
        theme.palette.grey[600] :
        theme.palette.grey[400];
    const connectorY = TREE_CONNECTOR.iconCenterY + 5;
    const elbowTop = connectorY - TREE_CONNECTOR.elbowSize + 2;
    const elbowLeft = TREE_CONNECTOR.iconCenterX - TREE_CONNECTOR.childrenIndent;
    const horizontalExtensionLeft = elbowLeft + TREE_CONNECTOR.elbowSize - 1;
    const horizontalExtensionWidth = TREE_CONNECTOR.childrenIndent - TREE_CONNECTOR.iconCenterX - TREE_CONNECTOR.elbowSize + 3;
    const childNodes = node.children ?? [];

    return (
        <Box sx={{minWidth: 0}}>
            <Box
                sx={{
                    'display': 'flex',
                    'alignItems': 'center',
                    'gap': 1.5,
                    'minWidth': 0,
                    'py': 1.75,
                    'px': 2,
                    'border': '1px solid',
                    'borderColor': 'divider',
                    'borderRadius': 2,
                    'bgcolor': 'background.paper',
                }}
            >
                <Avatar
                    sx={{
                        width: 38,
                        height: 38,
                        bgcolor: theme.palette.mode === 'dark' ? theme.palette.grey[700] : theme.palette.grey[200],
                        border: '1px solid',
                        borderColor: 'divider',
                    }}
                >
                    <Skeleton
                        variant="circular"
                        width={18}
                        height={18}
                    />
                </Avatar>

                <Box
                    sx={{
                        display: 'flex',
                        alignItems: 'flex-start',
                        minWidth: 0,
                        flex: 1,
                    }}
                >
                    <Box
                        sx={{
                            display: 'flex',
                            flexDirection: 'column',
                            minWidth: 0,
                            flex: 1,
                            mr: 1,
                        }}
                    >
                        <Skeleton
                            variant="text"
                            width={node.nameWidth}
                            height={30}
                        />
                        <Skeleton
                            variant="text"
                            width={node.subtitleWidth}
                            height={20}
                            sx={{mt: -0.5}}
                        />
                    </Box>
                </Box>

                <Box
                    sx={{
                        ml: 1,
                        display: {
                            xs: 'none',
                            md: 'flex',
                        },
                        alignItems: 'center',
                    }}
                >
                    <Skeleton
                        variant="text"
                        width={node.addressWidth}
                        height={24}
                    />
                </Box>

                <Box
                    sx={{
                        ml: 2,
                        display: 'flex',
                        gap: 1,
                        flexShrink: 0,
                    }}
                >
                    {
                        Array.from({length: 3}).map((_, index) => (
                            <Skeleton
                                key={index}
                                variant="circular"
                                width={28}
                                height={28}
                            />
                        ))
                    }
                </Box>
            </Box>

            {
                childNodes.length > 0 &&
                <Box
                    sx={{
                        'display': 'grid',
                        'gap': 1.25,
                        'minWidth': 0,
                        'mt': 0,
                        'pt': 1.5,
                        'pl': `${TREE_CONNECTOR.childrenIndent}px`,
                        'position': 'relative',
                        '&::before': {
                            content: '""',
                            position: 'absolute',
                            top: 0,
                            bottom: 0,
                            left: `${TREE_CONNECTOR.iconCenterX}px`,
                            width: 2,
                            bgcolor: connectorColor,
                            zIndex: 0,
                        },
                    }}
                >
                    {
                        childNodes.map((child, index) => (
                            <Box
                                key={child.id}
                                sx={{
                                    'position': 'relative',
                                    'zIndex': 1,
                                    'minWidth': 0,
                                    '&::before': {
                                        content: '""',
                                        position: 'absolute',
                                        left: `${elbowLeft}px`,
                                        top: `${elbowTop}px`,
                                        width: `${TREE_CONNECTOR.elbowSize}px`,
                                        height: `${TREE_CONNECTOR.elbowSize}px`,
                                        borderLeft: `2px solid ${connectorColor}`,
                                        borderBottom: `2px solid ${connectorColor}`,
                                        borderBottomLeftRadius: 10,
                                        zIndex: 2,
                                    },
                                    '&::after': {
                                        content: '""',
                                        position: 'absolute',
                                        left: `${horizontalExtensionLeft}px`,
                                        top: `${connectorY}px`,
                                        width: `${horizontalExtensionWidth}px`,
                                        height: 2,
                                        borderRadius: 999,
                                        bgcolor: connectorColor,
                                        zIndex: 2,
                                    },
                                }}
                            >
                                {
                                    index === childNodes.length - 1 &&
                                    <Box
                                        sx={{
                                            position: 'absolute',
                                            left: `${TREE_CONNECTOR.iconCenterX - TREE_CONNECTOR.childrenIndent}px`,
                                            top: `${TREE_CONNECTOR.iconCenterY}px`,
                                            width: 2,
                                            bottom: -4,
                                            bgcolor: 'background.paper',
                                            zIndex: 1,
                                        }}
                                    />
                                }
                                <DepartmentTreeSkeletonItem node={child} />
                            </Box>
                        ))
                    }
                </Box>
            }
        </Box>
    );
}

function DepartmentTreeItem(props: DepartmentTreeItemProps): React.ReactElement {
    const theme = useTheme();
    const {
        department,
        getActions,
        getDepartmentHref,
        selectedDepartmentId,
    } = props;

    const actions = getActions?.(department) ?? [];
    const typeLabel = getDepartmentTypeLabel(department.depth);
    const formattedAddress = formatAddress(department.postalAddress);
    const addressText = formattedAddress.length > 0 ? formattedAddress : 'Keine Adresse hinterlegt';
    const connectorColor = theme.palette.mode === 'dark' ?
        theme.palette.grey[600] :
        theme.palette.grey[400];
    const connectorY = TREE_CONNECTOR.iconCenterY + 5;
    const elbowTop = connectorY - TREE_CONNECTOR.elbowSize + 2;
    const elbowLeft = TREE_CONNECTOR.iconCenterX - TREE_CONNECTOR.childrenIndent;
    const horizontalExtensionLeft = elbowLeft + TREE_CONNECTOR.elbowSize - 1;
    const horizontalExtensionWidth = TREE_CONNECTOR.childrenIndent - TREE_CONNECTOR.iconCenterX - TREE_CONNECTOR.elbowSize + 3;
    const isSelected = selectedDepartmentId === department.id;

    return (
        <Box sx={{minWidth: 0}}>
            <Box
                sx={{
                    'display': 'flex',
                    'alignItems': 'center',
                    'gap': 1.5,
                    'minWidth': 0,
                    'py': 1.75,
                    'px': 2,
                    'border': '1px solid',
                    'borderColor': isSelected ? 'primary.main' : 'divider',
                    'borderRadius': 2,
                    'bgcolor': isSelected ? 'action.selected' : 'background.paper',
                    'transition': 'background-color .2s ease',
                    '&:hover': {
                        bgcolor: isSelected ? 'action.selected' : 'action.hover',
                    },
                }}
            >
                <StringAvatar
                    name={department.name}
                    backgroundMode={'oklch'}
                    showInitials={false}
                    sx={{
                        width: 38,
                        height: 38,
                        border: '1px solid',
                        borderColor: 'divider',
                        '& svg': {
                            fontSize: 22,
                        },
                    }}
                >
                    {getDepartmentTypeIcons(department.depth)}
                </StringAvatar>

                <Box
                    sx={{
                        display: 'flex',
                        alignItems: 'flex-start',
                        minWidth: 0,
                        flex: 1,
                    }}
                >
                    <DepartmentName
                        department={department}
                        subtitle={typeLabel}
                        href={getDepartmentHref?.(department)}
                    />
                </Box>

                <Typography
                    sx={{
                        ml: 1,
                        display: {
                            xs: 'none',
                            md: 'block',
                        },
                        minWidth: 0,
                        flexShrink: 1,
                        whiteSpace: 'nowrap',
                        maxWidth: {
                            md: 220,
                            lg: 320,
                        },
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                    }}
                    variant="body2"
                    color="text.secondary"
                    title={addressText}
                >
                    {addressText}
                </Typography>

                {
                    actions.length > 0 &&
                    <Actions
                        size="small"
                        dense={true}
                        actions={actions}
                        sx={{
                            ml: 2,
                            flexShrink: 0,
                        }}
                    />
                }
            </Box>

            {
                department.children.length > 0 &&
                <Box
                    sx={{
                        'display': 'grid',
                        'gap': 1.25,
                        'minWidth': 0,
                        'mt': 0,
                        'pt': 1.5,
                        'pl': `${TREE_CONNECTOR.childrenIndent}px`,
                        'position': 'relative',
                        '&::before': {
                            content: '""',
                            position: 'absolute',
                            top: 0,
                            bottom: 0,
                            left: `${TREE_CONNECTOR.iconCenterX}px`,
                            width: 2,
                            bgcolor: connectorColor,
                            zIndex: 0,
                        },
                    }}
                >
                    {
                        department.children.map((child, index) => (
                            <Box
                                key={child.id}
                                sx={{
                                    'position': 'relative',
                                    'zIndex': 1,
                                    'minWidth': 0,
                                    '&::before': {
                                        content: '""',
                                        position: 'absolute',
                                        left: `${elbowLeft}px`,
                                        top: `${elbowTop}px`,
                                        width: `${TREE_CONNECTOR.elbowSize}px`,
                                        height: `${TREE_CONNECTOR.elbowSize}px`,
                                        borderLeft: `2px solid ${connectorColor}`,
                                        borderBottom: `2px solid ${connectorColor}`,
                                        borderBottomLeftRadius: 10,
                                        zIndex: 2,
                                    },
                                    '&::after': {
                                        content: '""',
                                        position: 'absolute',
                                        left: `${horizontalExtensionLeft}px`,
                                        top: `${connectorY}px`,
                                        width: `${horizontalExtensionWidth}px`,
                                        height: 2,
                                        borderRadius: 999,
                                        bgcolor: connectorColor,
                                        zIndex: 2,
                                    },
                                }}
                            >
                                {
                                    index === department.children.length - 1 &&
                                    <Box
                                        sx={{
                                            position: 'absolute',
                                            left: `${TREE_CONNECTOR.iconCenterX - TREE_CONNECTOR.childrenIndent}px`,
                                            top: `${TREE_CONNECTOR.iconCenterY}px`,
                                            width: 2,
                                            bottom: -4,
                                            bgcolor: 'background.paper',
                                            zIndex: 1,
                                        }}
                                    />
                                }
                                <DepartmentTreeItem
                                    department={child}
                                    getActions={getActions}
                                    getDepartmentHref={getDepartmentHref}
                                    selectedDepartmentId={selectedDepartmentId}
                                />
                            </Box>
                        ))
                    }
                </Box>
            }
        </Box>
    );
}
