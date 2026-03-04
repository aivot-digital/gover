import {Avatar, Box, Container, Paper, Skeleton, Typography, useTheme} from '@mui/material';
import React, {useEffect, useMemo, useState} from 'react';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {GenericPageHeader} from '../../../../components/generic-page-header/generic-page-header';
import {ModuleIcons} from '../../../../shells/staff/data/module-icons';
import {Link} from 'react-router-dom';
import Add from '@aivot/mui-material-symbols-400-outlined/dist/add/Add';
import {Actions} from '../../../../components/actions/actions';
import {NewParentIdQueryParam} from '../details/departments-details-page';
import {VDepartmentShadowedApiService} from '../../services/v-department-shadowed-api-service';
import {type VDepartmentShadowedEntityWithChildren} from '../../entities/v-department-shadowed-entity';
import {getDepartmentTypeIcons, getDepartmentTypeLabel} from '../../utils/department-utils';
import {AlertComponent} from '../../../../components/alert/alert-component';
import {SearchInput} from '../../../../components/search-input/search-input';
import Fuse from 'fuse.js';
import EditOutlined from '@mui/icons-material/EditOutlined';
import GroupOutlined from '@mui/icons-material/GroupOutlined';
import DescriptionOutlined from '@mui/icons-material/DescriptionOutlined';

interface SearchableDepartment {
    id: number;
    name: string;
    address: string;
    type: string;
}

interface DepartmentTreeSkeletonNode {
    id: string;
    nameWidth: number | string;
    subtitleWidth: number | string;
    addressWidth: number | string;
    children?: DepartmentTreeSkeletonNode[];
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

function flattenDepartments(nodes: VDepartmentShadowedEntityWithChildren[]): VDepartmentShadowedEntityWithChildren[] {
    const flattened: VDepartmentShadowedEntityWithChildren[] = [];

    for (const node of nodes) {
        flattened.push(node);
        if (node.children != null && node.children.length > 0) {
            flattened.push(...flattenDepartments(node.children));
        }
    }

    return flattened;
}

function filterDepartmentTree(
    nodes: VDepartmentShadowedEntityWithChildren[],
    matchedIds: Set<number>,
): VDepartmentShadowedEntityWithChildren[] {
    const filteredNodes: VDepartmentShadowedEntityWithChildren[] = [];

    for (const node of nodes) {
        const filteredChildren = filterDepartmentTree(node.children ?? [], matchedIds);

        if (matchedIds.has(node.id) || filteredChildren.length > 0) {
            filteredNodes.push({
                ...node,
                children: filteredChildren,
            });
        }
    }

    return filteredNodes;
}

export function DepartmentTree(): React.ReactElement {
    const [
        rootOrgs,
        setRootOrgs,
    ] = useState<VDepartmentShadowedEntityWithChildren[]>();
    const [
        search,
        setSearch,
    ] = useState('');
    const [
        loadError,
        setLoadError,
    ] = useState(false);

    useEffect(() => {
        void new VDepartmentShadowedApiService()
            .retrieveOrgTree()
            .then((items) => {
                setRootOrgs(items);
                setLoadError(false);
            })
            .catch((err) => {
                console.error(err);
                setRootOrgs([]);
                setLoadError(true);
            });
    }, []);

    const searchableDepartments = useMemo<SearchableDepartment[]>(() => {
        if (rootOrgs == null) {
            return [];
        }

        return flattenDepartments(rootOrgs)
            .map((department) => ({
                id: department.id,
                name: department.name,
                address: formatAddress(department.address),
                type: getDepartmentTypeLabel(department.depth),
            }));
    }, [rootOrgs]);

    const fuse = useMemo(() => {
        return new Fuse(searchableDepartments, {
            keys: [
                'name',
                'address',
                'type',
            ],
            threshold: 0.3,
            shouldSort: true,
            minMatchCharLength: 2,
            ignoreLocation: true,
        });
    }, [searchableDepartments]);

    const filteredRootOrgs = useMemo(() => {
        if (rootOrgs == null) {
            return undefined;
        }

        const cleanedSearch = search.trim();
        if (cleanedSearch.length === 0) {
            return rootOrgs;
        }

        const matchedIds = new Set(
            fuse
                .search(cleanedSearch)
                .map((result) => result.item.id),
        );

        return filterDepartmentTree(rootOrgs, matchedIds);
    }, [
        rootOrgs,
        search,
        fuse,
    ]);

    return (
        <PageWrapper
            title="Organisationseinheiten"
            fullWidth
            background
        >
            <Container>
                <GenericPageHeader
                    icon={ModuleIcons.departments}
                    title="Organisationseinheiten"
                    actions={[
                        {
                            label: 'Neue Organisation',
                            icon: <Add />,
                            to: '/departments/new',
                            variant: 'contained',
                        },
                    ]}
                    helpDialog={{
                        title: 'Hilfe zu Organisationseinheiten',
                        tooltip: 'Hilfe anzeigen',
                        content: (
                            <>
                                <Typography>
                                    Organisationseinheiten bilden die fachliche Struktur in Gover ab.
                                    In dieser Baumansicht sehen Sie die Hierarchie vom übergeordneten Bereich bis zu den untergeordneten Einheiten.
                                </Typography>
                                <Typography sx={{mt: 2}}>
                                    Jede Organisationseinheit kann Stammdaten wie Name, Typ und Adresse enthalten.
                                    Diese Informationen werden in Fachprozessen wiederverwendet und erleichtern eine konsistente Verwaltung.
                                </Typography>
                                <Typography sx={{mt: 2}}>
                                    Über die Aktionsbuttons können Sie direkt Untereinheiten anlegen, Einheiten bearbeiten,
                                    Mitarbeitende verwalten und die zugehörigen Formulare einsehen.
                                    Mit der Suche finden Sie Einheiten nach Name, Adresse oder Typ.
                                </Typography>
                            </>
                        ),
                    }}
                />

                <Paper
                    sx={{
                        mt: 3.5,
                        mb: 4,
                        p: 2,
                        pb: 3.5,
                    }}
                >
                    <SearchInput
                        value={search}
                        onChange={setSearch}
                        label="Organisationseinheiten durchsuchen"
                        placeholder="Name, Adresse oder Typ suchen…"
                        size="small"
                        debounce={200}
                        sx={{mb: 2}}
                    />

                    {
                        filteredRootOrgs != null ?
                            <Box
                                sx={{
                                    display: 'grid',
                                    gap: 2.5,
                                }}
                            >
                                {
                                    filteredRootOrgs.map((orgUnit) => (
                                        <DepartmentTreeItem
                                            key={orgUnit.id}
                                            department={orgUnit}
                                        />
                                    ))
                                }
                            </Box> :
                            <DepartmentTreeLoadingSkeleton />
                    }

                    {
                        filteredRootOrgs != null &&
                        filteredRootOrgs.length === 0 &&
                        <AlertComponent
                            color="info"
                            sx={{my: 1}}
                        >
                            Keine Organisationseinheiten für den Suchbegriff gefunden.
                        </AlertComponent>
                    }
                    {
                        loadError &&
                        <Typography
                            color="error"
                            sx={{py: 1}}
                        >
                            Die Organisationseinheiten konnten nicht geladen werden.
                        </Typography>
                    }
                </Paper>

            </Container>
        </PageWrapper>
    );
}

interface DepartmentTreeItemProps {
    department: VDepartmentShadowedEntityWithChildren;
}

interface DepartmentTreeSkeletonItemProps {
    node: DepartmentTreeSkeletonNode;
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
        <Box>
            <Box
                sx={{
                    'display': 'flex',
                    'alignItems': 'center',
                    'gap': 1.5,
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
                        Array.from({length: 4}).map((_, index) => (
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
    } = props;

    const typeLabel = getDepartmentTypeLabel(department.depth);
    const formattedAddress = formatAddress(department.address);
    const addressText = formattedAddress.length > 0 ? formattedAddress : 'Keine Adresse hinterlegt';
    const connectorColor = theme.palette.mode === 'dark' ?
        theme.palette.grey[600] :
        theme.palette.grey[400];
    const connectorY = TREE_CONNECTOR.iconCenterY + 5;
    const elbowTop = connectorY - TREE_CONNECTOR.elbowSize + 2;
    const elbowLeft = TREE_CONNECTOR.iconCenterX - TREE_CONNECTOR.childrenIndent;
    const horizontalExtensionLeft = elbowLeft + TREE_CONNECTOR.elbowSize - 1;
    const horizontalExtensionWidth = TREE_CONNECTOR.childrenIndent - TREE_CONNECTOR.iconCenterX - TREE_CONNECTOR.elbowSize + 3;

    return (
        <Box>
            <Box
                sx={{
                    'display': 'flex',
                    'alignItems': 'center',
                    'gap': 1.5,
                    'py': 1.75,
                    'px': 2,
                    'border': '1px solid',
                    'borderColor': 'divider',
                    'borderRadius': 2,
                    'bgcolor': 'background.paper',
                    'transition': 'background-color .2s ease',
                    '&:hover': {
                        bgcolor: 'action.hover',
                    },
                }}
            >
                <Avatar
                    sx={{
                        width: 38,
                        height: 38,
                        bgcolor: theme.palette.mode === 'dark' ? theme.palette.grey[700] : theme.palette.grey[200],
                        color: 'text.primary',
                        border: '1px solid',
                        borderColor: 'divider',
                    }}
                >
                    {getDepartmentTypeIcons(department.depth)}
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
                        component={Link}
                        to={`/departments/${department.id}`}
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
                            {typeLabel}
                        </Typography>
                    </Box>
                </Box>

                <Typography
                    sx={{
                        ml: 1,
                        display: {
                            xs: 'none',
                            md: 'flex',
                        },
                        alignItems: 'center',
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

                <Actions
                    size="small"
                    dense={true}
                    actions={[
                        {
                            tooltip: `${getDepartmentTypeLabel(department.depth + 1)} hinzufügen`,
                            icon: <Add />,
                            to: `/departments/new?${NewParentIdQueryParam}=${department.id}`,
                            variant: 'contained',
                        },
                        {
                            tooltip: 'Bearbeiten',
                            icon: <EditOutlined />,
                            to: `/departments/${department.id}`,
                            variant: 'contained',
                        },
                        {
                            tooltip: 'Mitarbeiter:innen verwalten',
                            icon: <GroupOutlined />,
                            to: `/departments/${department.id}/members`,
                        },
                        {
                            tooltip: 'Formulare des Fachbereichs ansehen',
                            icon: <DescriptionOutlined />,
                            to: `/departments/${department.id}/forms`,
                        },
                    ]}
                    sx={{
                        ml: 2,
                        flexShrink: 0,
                    }}
                />
            </Box>

            {
                department.children != null &&
                department.children.length > 0 &&
                <Box
                    sx={{
                        'display': 'grid',
                        'gap': 1.25,
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
                        department
                            .children
                            .map((child, index) => (
                                <Box
                                    key={child.id}
                                    sx={{
                                        'position': 'relative',
                                        'zIndex': 1,
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
                                    />
                                </Box>
                            ))
                    }
                </Box>
            }
        </Box>
    );
}
