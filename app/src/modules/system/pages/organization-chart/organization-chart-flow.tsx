import ELK, {type ElkNode} from 'elkjs/lib/elk.bundled.js';
import {
    Background,
    BackgroundVariant,
    ControlButton,
    Controls,
    Handle,
    ReactFlow,
    ReactFlowProvider,
    type Edge as ReactFlowEdge,
    type Node as ReactFlowNode,
    type NodeProps,
    Position,
    useReactFlow,
    useStore,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import './organization-chart-flow.css';
import {
    Box,
    Button,
    Chip,
    CircularProgress,
    ListItem,
    ListItemAvatar,
    ListItemButton,
    ListItemText,
    Paper,
    Stack,
    Tooltip,
    Typography,
    useTheme,
} from '@mui/material';
import {alpha} from '@mui/material/styles';
import Add from '@aivot/mui-material-symbols-400-n25-outlined/Add';
import Remove from '@aivot/mui-material-symbols-400-n25-outlined/Remove';
import CropFree from '@aivot/mui-material-symbols-400-n25-outlined/CropFree';
import Groups from '@aivot/mui-material-symbols-400-n25-outlined/Groups';
import ViewRealSize from '@aivot/mui-material-symbols-400-n25-outlined/ViewRealSize';
import OpenInNew from '@aivot/mui-material-symbols-400-n25-outlined/OpenInNew';
import {memo, type CSSProperties, type ReactNode, useEffect, useState} from 'react';
import {Link as RouterLink} from 'react-router-dom';
import {StringAvatar} from '../../../../components/avatar/string-avatar';
import {DisabledTooltip} from '../../../../components/disabled-tooltip/disabled-tooltip';
import {getDepartmentTypeIcons, getDepartmentTypeLabel} from '../../../departments/utils/department-utils';
import {
    type OrganizationChartDepartmentItem,
    type OrganizationChartTeamItem,
    type OrganizationChartUserItem,
} from './organization-chart-types';
import {Permission} from '../../../../data/permissions/permission';
import {formatMissingPermissionTooltip} from '../../../permissions/utils/permission-utils';
import {getReactFlowBackgroundDotColor} from '../../../../theming/react-flow-theme';

interface OrganizationChartFlowProps {
    view: OrganizationChartFlowView;
    rootDepartments: OrganizationChartDepartmentItem[];
    teams: OrganizationChartTeamItem[];
    canReadUsers: boolean;
}

export type OrganizationChartFlowView = 'departments' | 'teams';

type OrganizationChartFlowNodeData =
    | {
        itemType: 'department';
        item: OrganizationChartDepartmentItem;
        height: number;
        canReadUsers: boolean;
        canReadMemberships: boolean;
    }
    | {
        itemType: 'team';
        item: OrganizationChartTeamItem;
        height: number;
        canReadUsers: boolean;
        canReadMemberships: boolean;
    };

type OrganizationChartFlowNode = ReactFlowNode<OrganizationChartFlowNodeData>;
type OrganizationChartFlowEdge = ReactFlowEdge;

interface OrganizationChartLayoutResult {
    nodes: OrganizationChartFlowNode[];
    edges: OrganizationChartFlowEdge[];
    groups: OrganizationChartTreeGroup[];
}

interface OrganizationChartDepartmentTreeLayoutInput {
    rootDepartment: OrganizationChartDepartmentItem;
    nodes: OrganizationChartFlowNode[];
    edges: OrganizationChartFlowEdge[];
}

interface OrganizationChartDepartmentTreeLayoutResult {
    nodes: OrganizationChartFlowNode[];
    edges: OrganizationChartFlowEdge[];
    group: OrganizationChartTreeGroup | null;
}

interface OrganizationChartFlowCanvasProps {
    view: OrganizationChartFlowView;
    rootDepartments: OrganizationChartDepartmentItem[];
    teams: OrganizationChartTeamItem[];
    canReadUsers: boolean;
}

interface OrganizationChartTreeGroup {
    id: string;
    label: string;
    color: string;
    x: number;
    y: number;
    width: number;
    height: number;
}

interface OrganizationChartFlowControlButtonProps {
    ariaLabel: string;
    children: ReactNode;
    disabled?: boolean;
    onClick: () => void;
    tooltip: string;
}

interface OrganizationChartMemberListProps {
    members: OrganizationChartUserItem[];
    compact?: boolean;
}

const elk = new ELK();
const FLOW_NODE_TYPE = 'organization-chart-node';
const FLOW_NODE_WIDTH = 420;
const FLOW_NODE_MIN_HEIGHT = 195;
const FLOW_MIN_ZOOM = 0.25;
const FLOW_MAX_ZOOM = 2;
const ZOOM_EPSILON = 0.001;
const MEMBER_PREVIEW_COUNT = 4;
const FLOW_NODE_BASE_HEIGHT = 100;
const FLOW_NODE_MEMBER_ROW_HEIGHT = 39;
const FLOW_NODE_MORE_LINK_HEIGHT = 34;
const FLOW_NODE_SHADOW = '0px 4px 20px rgba(0, 0, 0, 0.1)';
const EMPTY_MEMBER_PLACEHOLDER_ROWS = [
    {
        opacity: 0.28,
        width: '68%',
    },
    {
        opacity: 0.18,
        width: '54%',
    },
    {
        opacity: 0.1,
        width: '68%',
    },
] as const;
const TREE_GROUP_HORIZONTAL_PADDING = 96;
const TREE_GROUP_TOP_PADDING = 96;
const TREE_GROUP_BOTTOM_PADDING = 72;
const DEPARTMENT_TREE_GROUP_HORIZONTAL_SPACING = 160;
const DEPARTMENT_TREE_GROUP_START_X = 56;
const DEPARTMENT_TREE_GROUP_START_Y = 56;
const VIEW_SWITCH_OUT_DURATION = 140;
const VIEW_SWITCH_TOTAL_DURATION = 360;
const FLOW_FIT_VIEW_OPTIONS = {
    padding: 0.22,
    duration: 220,
};
const TEAM_LAYOUT_COLUMN_COUNT = 3;
const TEAM_LAYOUT_HORIZONTAL_SPACING = 88;
const TEAM_LAYOUT_VERTICAL_SPACING = 72;
const ORGANIZATION_CHART_LAYOUT_OPTIONS = {
    'elk.algorithm': 'layered',
    'elk.direction': 'DOWN',
    'elk.edgeRouting': 'ORTHOGONAL',
    'elk.padding': '[top=72,left=72,bottom=72,right=72]',
    'elk.spacing.nodeNode': '112',
    'org.eclipse.elk.spacing.edgeNode': '64',
    'org.eclipse.elk.spacing.componentComponent': '180',
    'elk.layered.spacing.nodeNodeBetweenLayers': '104',
    'elk.layered.nodePlacement.strategy': 'BRANDES_KOEPF',
    'elk.layered.nodePlacement.bk.fixedAlignment': 'BALANCED',
    'elk.layered.nodePlacement.favorStraightEdges': 'true',
} as const;

const NODE_TYPES = {
    [FLOW_NODE_TYPE]: memo(OrganizationChartFlowNodeComponent),
};

export function OrganizationChartFlow(props: OrganizationChartFlowProps): ReactNode {
    const {
        view,
        rootDepartments,
        teams,
        canReadUsers,
    } = props;

    const [renderedView, setRenderedView] = useState<OrganizationChartFlowView>(view);
    const [isSwitchingView, setIsSwitchingView] = useState(false);

    useEffect(() => {
        if (view === renderedView) {
            setIsSwitchingView(false);
            return;
        }

        setIsSwitchingView(true);

        const switchViewTimer = window.setTimeout(() => {
            setRenderedView(view);
        }, VIEW_SWITCH_OUT_DURATION);
        const finishTransitionTimer = window.setTimeout(() => {
            setIsSwitchingView(false);
        }, VIEW_SWITCH_TOTAL_DURATION);

        return () => {
            window.clearTimeout(switchViewTimer);
            window.clearTimeout(finishTransitionTimer);
        };
    }, [view]);

    return (
        <Box
            sx={{
                mt: 1.5,
                flex: 1,
                minHeight: 0,
                display: 'flex',
                flexDirection: 'column',
            }}
        >
            <Box
                sx={{
                    flex: 1,
                    minHeight: {xs: 640, md: 0},
                    overflow: 'hidden',
                    position: 'relative',
                    ml: {xs: -2, md: -4},
                    mr: {xs: -2, md: -4},
                    mb: -2,
                    bgcolor: 'background.default',
                }}
            >
                {
                    renderedView === 'departments' && rootDepartments.length === 0 ? (
                        <OrganizationChartFlowEmptyState message="Keine Organisationseinheiten vorhanden." />
                    ) : renderedView === 'teams' && teams.length === 0 ? (
                        <OrganizationChartFlowEmptyState message="Keine Teams vorhanden." />
                    ) : (
                        <ReactFlowProvider key={renderedView}>
                            <OrganizationChartFlowCanvas
                                view={renderedView}
                                rootDepartments={rootDepartments}
                                teams={teams}
                                canReadUsers={canReadUsers}
                            />
                        </ReactFlowProvider>
                    )
                }
                <OrganizationChartFlowSwitchOverlay
                    visible={isSwitchingView || renderedView !== view}
                />
            </Box>
        </Box>
    );
}

function OrganizationChartFlowSwitchOverlay(props: {
    visible: boolean;
}): ReactNode {
    const {
        visible,
    } = props;

    return (
        <Box
            sx={{
                position: 'absolute',
                inset: 0,
                zIndex: 6,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                bgcolor: (theme) => alpha(theme.palette.background.default, 0.72),
                opacity: visible ? 1 : 0,
                pointerEvents: visible ? 'auto' : 'none',
                transition: 'opacity 160ms ease-out',
            }}
        >
            <CircularProgress
                size={28}
                thickness={4}
            />
        </Box>
    );
}

function OrganizationChartFlowCanvas(props: OrganizationChartFlowCanvasProps): ReactNode {
    const {
        view,
        rootDepartments,
        teams,
        canReadUsers,
    } = props;
    const theme = useTheme();
    const {
        fitView,
    } = useReactFlow<OrganizationChartFlowNode, OrganizationChartFlowEdge>();

    const [layout, setLayout] = useState<OrganizationChartLayoutResult>({
        nodes: [],
        edges: [],
        groups: [],
    });
    const [isLayoutReady, setIsLayoutReady] = useState(false);

    useEffect(() => {
        let isActive = true;
        setIsLayoutReady(false);

        createLayout(view, rootDepartments, teams, canReadUsers)
            .then((nextLayout) => {
                if (!isActive) {
                    return;
                }

                setLayout(nextLayout);
                setIsLayoutReady(true);
            })
            .catch((error) => {
                console.error('Failed to layout organization chart', error);
                if (!isActive) {
                    return;
                }

                setLayout({
                    nodes: [],
                    edges: [],
                    groups: [],
                });
                setIsLayoutReady(true);
            });

        return () => {
            isActive = false;
        };
    }, [canReadUsers, rootDepartments, teams, view]);

    useEffect(() => {
        if (!isLayoutReady || layout.nodes.length === 0) {
            return;
        }

        const frameHandle = requestAnimationFrame(() => {
            void fitView(FLOW_FIT_VIEW_OPTIONS);
        });

        return () => {
            cancelAnimationFrame(frameHandle);
        };
    }, [fitView, isLayoutReady, layout.nodes.length, view]);

    return (
        <ReactFlow
            className="organization-chart-flow"
            style={{
                '--organization-chart-flow-top-fade-color-solid': alpha(theme.palette.background.default, 0.96),
                '--organization-chart-flow-top-fade-color-mid': alpha(theme.palette.background.default, 0.72),
                '--organization-chart-flow-top-fade-color-transparent': alpha(theme.palette.background.default, 0),
                '--organization-chart-flow-edge-color': theme.palette.mode === 'dark' ? theme.palette.grey[600] : theme.palette.grey[400],
                '--organization-chart-flow-surface': alpha(theme.palette.background.paper, 0.96),
                '--organization-chart-flow-border': theme.palette.divider,
                '--organization-chart-flow-text': theme.palette.text.secondary,
                '--organization-chart-flow-text-disabled': theme.palette.text.disabled,
                '--organization-chart-flow-hover': theme.palette.action.hover,
                '--organization-chart-flow-shadow': theme.shadows[3],
                opacity: isLayoutReady ? 1 : 0,
                transition: 'opacity 120ms ease-out',
            } as CSSProperties}
            nodes={layout.nodes}
            edges={layout.edges}
            nodeTypes={NODE_TYPES}
            onlyRenderVisibleElements
            nodesDraggable={false}
            nodesConnectable={false}
            elementsSelectable={false}
            nodesFocusable={false}
            edgesFocusable={false}
            edgesReconnectable={false}
            minZoom={FLOW_MIN_ZOOM}
            maxZoom={FLOW_MAX_ZOOM}
            panOnDrag={true}
            zoomOnScroll={true}
            zoomOnPinch={true}
            zoomOnDoubleClick={true}
            preventScrolling={true}
            proOptions={{
                hideAttribution: true,
            }}
        >
            <Background
                variant={BackgroundVariant.Dots}
                color={getReactFlowBackgroundDotColor(theme)}
            />
            <OrganizationChartTreeGroups
                groups={layout.groups}
            />
            <OrganizationChartFlowViewportControls />
        </ReactFlow>
    );
}

function OrganizationChartFlowViewportControls(): ReactNode {
    const {
        fitView,
        zoomIn,
        zoomOut,
        zoomTo,
    } = useReactFlow<OrganizationChartFlowNode, OrganizationChartFlowEdge>();
    const zoom = useStore((store) => store.transform[2]);
    const canZoomIn = zoom < FLOW_MAX_ZOOM - ZOOM_EPSILON;
    const canZoomOut = zoom > FLOW_MIN_ZOOM + ZOOM_EPSILON;

    return (
        <Controls
            className="organization-chart-flow-controls"
            position="bottom-left"
            showZoom={false}
            showFitView={false}
            showInteractive={false}
        >
            <OrganizationChartFlowControlButton
                disabled={!canZoomIn}
                onClick={() => {
                    void zoomIn();
                }}
                ariaLabel="Vergrößern"
                tooltip="Vergrößern"
            >
                <Add sx={{fontSize: 20}} />
            </OrganizationChartFlowControlButton>

            <OrganizationChartFlowControlButton
                disabled={!canZoomOut}
                onClick={() => {
                    void zoomOut();
                }}
                ariaLabel="Verkleinern"
                tooltip="Verkleinern"
            >
                <Remove sx={{fontSize: 20}} />
            </OrganizationChartFlowControlButton>

            <OrganizationChartFlowControlButton
                onClick={() => {
                    void fitView(FLOW_FIT_VIEW_OPTIONS);
                }}
                ariaLabel="Ansicht einpassen"
                tooltip="Ansicht einpassen"
            >
                <CropFree sx={{fontSize: 18}} />
            </OrganizationChartFlowControlButton>

            <OrganizationChartFlowControlButton
                onClick={() => {
                    void zoomTo(1);
                }}
                ariaLabel="Zoom auf Originalgröße (100 %)"
                tooltip="Zoom auf Originalgröße (100 %)"
            >
                <ViewRealSize sx={{fontSize: 18}} />
            </OrganizationChartFlowControlButton>
        </Controls>
    );
}

function OrganizationChartFlowControlButton(props: OrganizationChartFlowControlButtonProps): ReactNode {
    const {
        ariaLabel,
        children,
        disabled = false,
        onClick,
        tooltip,
    } = props;

    return (
        <Tooltip
            title={tooltip}
            arrow
            placement="right"
        >
            <span className="organization-chart-flow-control-tooltip-anchor">
                <ControlButton
                    className="organization-chart-flow-control-button"
                    disabled={disabled}
                    onClick={onClick}
                    aria-label={ariaLabel}
                >
                    {children}
                </ControlButton>
            </span>
        </Tooltip>
    );
}

function OrganizationChartTreeGroups(props: {
    groups: OrganizationChartTreeGroup[];
}): ReactNode {
    const {
        groups,
    } = props;
    const theme = useTheme();
    const transform = useStore((store) => store.transform);
    const [translateX, translateY, zoom] = transform;
    const groupBorderWidth = Math.max(0.5, Math.min(1, zoom));
    const groupBorderColor = alpha(theme.palette.text.primary, theme.palette.mode === 'dark' ? 0.2 : 0.12);

    if (groups.length === 0) {
        return null;
    }

    return (
        <Box
            sx={{
                position: 'absolute',
                inset: 0,
                zIndex: 1,
                pointerEvents: 'none',
            }}
        >
            {
                groups.map((group) => (
                    <Box
                        key={group.id}
                        sx={{
                            position: 'absolute',
                            left: translateX + (group.x * zoom),
                            top: translateY + (group.y * zoom),
                            width: group.width * zoom,
                            height: group.height * zoom,
                            border: `${groupBorderWidth}px solid ${groupBorderColor}`,
                            borderRadius: 2,
                            bgcolor: alpha(group.color, theme.palette.mode === 'dark' ? 0.045 : 0.025),
                            boxSizing: 'border-box',
                        }}
                    >
                        <Typography
                            variant="caption"
                            sx={{
                                position: 'absolute',
                                top: 10,
                                left: 14,
                                maxWidth: 'calc(100% - 28px)',
                                px: 0.75,
                                py: 0.25,
                                borderRadius: 1,
                                bgcolor: alpha(theme.palette.background.default, 0.9),
                                color: 'text.secondary',
                                fontSize: 11,
                                fontWeight: 700,
                                lineHeight: 1.35,
                                overflow: 'hidden',
                                textOverflow: 'ellipsis',
                                whiteSpace: 'nowrap',
                            }}
                        >
                            {group.label}
                        </Typography>
                    </Box>
                ))
            }
        </Box>
    );
}

function OrganizationChartFlowNodeComponent(props: NodeProps<OrganizationChartFlowNode>): ReactNode {
    const {
        data,
    } = props;

    return (
        <Box
            className="nodrag nopan"
            sx={{
                width: FLOW_NODE_WIDTH,
                height: data.height,
                position: 'relative',
            }}
        >
            <Handle
                id="target"
                type="target"
                position={Position.Top}
                isConnectable={false}
                style={{
                    opacity: 0,
                    pointerEvents: 'none',
                }}
            />
            <OrganizationChartNodeCard
                data={data}
            />
            <Handle
                id="source"
                type="source"
                position={Position.Bottom}
                isConnectable={false}
                style={{
                    opacity: 0,
                    pointerEvents: 'none',
                }}
            />
        </Box>
    );
}

function OrganizationChartNodeCard(props: {
    data: OrganizationChartFlowNodeData;
}): ReactNode {
    const {
        data,
    } = props;
    const item = data.item;
    const department = data.itemType === 'department' ? data.item : null;
    const members = item.members;
    const canReadDetails = item.canReadDetails;
    const canReadUsers = data.canReadUsers;
    const canReadMemberships = data.canReadMemberships;
    const isDepartment = department != null;
    const title = item.name;
    const detailLinkTo = isDepartment ? `/departments/${item.id}` : `/teams/${item.id}`;
    const managementLinkTo = isDepartment ? `/departments/${item.id}/members` : `/teams/${item.id}/members`;
    const typeLabel = department != null ? getDepartmentTypeLabel(department.depth) : 'Team';
    const membershipReadPermission = isDepartment ? Permission.DEPARTMENT_MEMBERSHIP_READ : Permission.TEAM_MEMBERSHIP_READ;

    return (
        <Paper
            elevation={0}
            sx={{
                width: '100%',
                height: '100%',
                boxSizing: 'border-box',
                px: 2,
                py: 1.75,
                borderRadius: '6px',
                bgcolor: 'background.paper',
                boxShadow: FLOW_NODE_SHADOW,
                overflow: 'hidden',
                display: 'flex',
                flexDirection: 'column',
            }}
        >
            <Box
                sx={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 1.25,
                    minWidth: 0,
                    pb: 1,
                    mb: 1,
                    borderBottom: 1,
                    borderColor: 'divider',
                }}
            >
                <StringAvatar
                    name={title}
                    backgroundMode="oklch"
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
                    {department != null ? getDepartmentTypeIcons(department.depth) : <Groups />}
                </StringAvatar>

                <Box sx={{display: 'flex', flexDirection: 'column', minWidth: 0, flex: 1}}>
                    {
                        canReadDetails ? (
                            <Typography
                                component={RouterLink}
                                to={detailLinkTo}
                                className="nodrag nopan"
                                variant="subtitle1"
                                title={title}
                                sx={{
                                    color: 'text.primary',
                                    textDecoration: 'none',
                                    fontWeight: 700,
                                    overflow: 'hidden',
                                    textOverflow: 'ellipsis',
                                    whiteSpace: 'nowrap',
                                    '&:hover': {
                                        textDecoration: 'underline',
                                    },
                                }}
                            >
                                {title}
                            </Typography>
                        ) : (
                            <Typography
                                variant="subtitle1"
                                title={title}
                                sx={{
                                    color: 'text.primary',
                                    fontWeight: 700,
                                    overflow: 'hidden',
                                    textOverflow: 'ellipsis',
                                    whiteSpace: 'nowrap',
                                }}
                            >
                                {title}
                            </Typography>
                        )
                    }
                    <Typography
                        variant="caption"
                        color="text.secondary"
                        sx={{
                            alignSelf: 'flex-start',
                            borderRadius: 1,
                            bgcolor: isDepartment ? alpha(item.color, 0.25) : undefined,
                            mt: -0.25,
                        }}
                    >
                        {typeLabel}
                    </Typography>
                </Box>

                <DisabledTooltip
                    disabled={!canReadMemberships}
                    title={formatMissingPermissionTooltip(membershipReadPermission)}
                    wrapperSx={{flexShrink: 0}}
                >
                    <Button
                        component={RouterLink}
                        to={managementLinkTo}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="nodrag nopan"
                        variant="outlined"
                        size="small"
                        disabled={!canReadMemberships}
                    >
                        Verwalten
                    </Button>
                </DisabledTooltip>
            </Box>

            {
                !canReadUsers ? (
                    <OrganizationChartMembersPermissionState
                        message="Die Berechtigung zum Einsehen der Mitarbeiter:innendaten fehlt"
                        permission={Permission.USER_READ}
                    />
                ) : !canReadMemberships ? (
                    <OrganizationChartMembersPermissionState
                        message="Die Berechtigung zum Einsehen der Zuordnungen fehlt"
                        permission={membershipReadPermission}
                    />
                ) : members.length > 0 ? (
                    <OrganizationChartMemberList
                        members={members.slice(0, MEMBER_PREVIEW_COUNT)}
                        compact
                    />
                ) : (
                    <OrganizationChartMembersEmptyState />
                )
            }

            {
                canReadUsers &&
                canReadMemberships &&
                members.length > MEMBER_PREVIEW_COUNT &&
                <Button
                    component={RouterLink}
                    to={managementLinkTo}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="nodrag nopan"
                    size="small"
                    variant="text"
                    endIcon={
                        <OpenInNew
                            sx={{
                                fontSize: '1rem!important',
                                color: 'text.disabled',
                            }}
                        />
                    }
                    sx={{alignSelf: 'flex-start', mt: 0.75, px: 0.75}}
                >
                    {`und ${members.length - MEMBER_PREVIEW_COUNT} weitere`}
                </Button>
            }
        </Paper>
    );
}

function OrganizationChartMembersPermissionState(props: {
    message: string;
    permission: Permission;
}): ReactNode {
    const {
        message,
        permission,
    } = props;

    return (
        <Box
            sx={{
                minHeight: 76,
                boxSizing: 'border-box',
                borderRadius: 1.5,
                border: (theme) => `1px solid ${theme.palette.action.hover}`,
                bgcolor: 'action.hover',
                display: 'flex',
                alignItems: 'center',
                px: 1.25,
                py: 1,
            }}
        >
            <Typography
                variant="body2"
                color="text.secondary"
                sx={{
                    fontWeight: 600,
                    lineHeight: 1.35,
                }}
            >
                {`${message} (${permission}).`}
            </Typography>
        </Box>
    );
}

function OrganizationChartMembersEmptyState(): ReactNode {
    return (
        <Stack
            aria-label="Keine Mitglieder"
            spacing={0.5}
            sx={{
                mt: 0,
            }}
        >
            <Box
                sx={{
                    minHeight: 38,
                    boxSizing: 'border-box',
                    borderRadius: 1.5,
                    border: (theme) => `1px solid ${theme.palette.action.hover}`,
                    bgcolor: 'action.hover',
                    display: 'flex',
                    alignItems: 'center',
                    px: 0.75,
                    py: 0.5,
                }}
            >
                <Box
                    sx={{
                        width: 28,
                        height: 28,
                        borderRadius: '50%',
                        bgcolor: 'background.paper',
                        border: 1,
                        borderColor: 'divider',
                        flexShrink: 0,
                    }}
                />
                <Typography
                    variant="body2"
                    color="text.secondary"
                    noWrap
                    sx={{
                        ml: 1,
                        fontWeight: 600,
                    }}
                >
                    Keine Mitarbeiter:innen zugeordnet
                </Typography>
            </Box>
            {
                EMPTY_MEMBER_PLACEHOLDER_ROWS.map((row, index) => (
                    <Box
                        key={index}
                        sx={{
                            minHeight: 38,
                            boxSizing: 'border-box',
                            borderRadius: 1.5,
                            border: (theme) => `1px solid ${theme.palette.action.hover}`,
                            bgcolor: 'action.hover',
                            display: 'flex',
                            alignItems: 'center',
                            px: 0.75,
                            py: 0.5,
                            opacity: row.opacity,
                        }}
                    >
                        <Box
                            sx={{
                                width: 28,
                                height: 28,
                                borderRadius: '50%',
                                bgcolor: 'background.paper',
                                border: 1,
                                borderColor: 'divider',
                                flexShrink: 0,
                            }}
                        />
                        <Box
                            sx={{
                                width: row.width,
                                height: 8,
                                ml: 1,
                                borderRadius: 999,
                                bgcolor: 'text.disabled',
                            }}
                        />
                    </Box>
                ))
            }
        </Stack>
    );
}

function OrganizationChartMemberList(props: OrganizationChartMemberListProps): ReactNode {
    const {
        members,
        compact = true,
    } = props;

    return (
        <Stack spacing={compact ? 0.5 : 1}>
            {
                members.map((member) => (
                    <OrganizationChartMemberRow
                        key={member.id}
                        member={member}
                        compact={compact}
                    />
                ))
            }
        </Stack>
    );
}

function OrganizationChartMemberRow(props: {
    member: OrganizationChartUserItem;
    compact: boolean;
}): ReactNode {
    const {
        member,
        compact,
    } = props;
    const memberName = getMemberDisplayName(member);
    const avatarName = getMemberAvatarName(member);

    return (
        <ListItem
            disablePadding
            sx={{display: 'block'}}
        >
            <ListItemButton
                component={RouterLink}
                to={`/users/${member.id}`}
                target="_blank"
                rel="noopener noreferrer"
                className="nodrag nopan"
                sx={{
                    alignItems: 'center',
                    borderRadius: 1.5,
                    border: (theme) => `1px solid ${theme.palette.action.hover}`,
                    bgcolor: compact ? 'action.hover' : 'background.paper',
                    px: compact ? 0.75 : 1,
                    py: compact ? 0.5 : 0.75,
                    '&:hover': {
                        bgcolor: 'action.selected',
                        borderColor: 'action.selected',
                    },
                }}
            >
                <ListItemAvatar sx={{minWidth: compact ? 36 : 42}}>
                    <StringAvatar
                        name={avatarName}
                        sx={{
                            width: compact ? 28 : 32,
                            height: compact ? 28 : 32,
                            fontSize: compact ? 12 : 13,
                            opacity: member.enabled ? 1 : 0.72,
                        }}
                        backgroundMode="oklch"
                        showInitials
                    />
                </ListItemAvatar>
                <ListItemText
                    sx={{my: 0, minWidth: 0}}
                    primary={(
                        <Box
                            component="span"
                            sx={{
                                display: 'flex',
                                alignItems: 'center',
                                gap: 0.75,
                                minWidth: 0,
                            }}
                        >
                            <Typography
                                component="span"
                                title={memberName}
                                noWrap
                                sx={{
                                    display: 'block',
                                    fontWeight: 600,
                                    overflow: 'hidden',
                                    textOverflow: 'ellipsis',
                                    minWidth: 0,
                                }}
                            >
                                {memberName}
                            </Typography>
                            {
                                !member.enabled &&
                                <Chip
                                    label="Inaktiv"
                                    color="warning"
                                    variant="outlined"
                                    size="small"
                                    sx={{
                                        height: 20,
                                        flexShrink: 0,
                                        '& .MuiChip-label': {
                                            px: 0.75,
                                            fontSize: '0.6875rem',
                                        },
                                    }}
                                />
                            }
                        </Box>
                    )}
                    secondary={null}
                />
            </ListItemButton>
        </ListItem>
    );
}

function OrganizationChartFlowEmptyState(props: {
    message: string;
}): ReactNode {
    const {
        message,
    } = props;

    return (
        <Box
            sx={{
                height: '100%',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                p: 2,
            }}
        >
            <Typography
                variant="body2"
                color="text.secondary"
            >
                {message}
            </Typography>
        </Box>
    );
}

async function createLayout(
    view: OrganizationChartFlowView,
    rootDepartments: OrganizationChartDepartmentItem[],
    teams: OrganizationChartTeamItem[],
    canReadUsers: boolean,
): Promise<OrganizationChartLayoutResult> {
    if (view === 'teams') {
        return createTeamLayout(teams, canReadUsers);
    }

    return await createDepartmentLayout(rootDepartments, canReadUsers);
}

async function createDepartmentLayout(rootDepartments: OrganizationChartDepartmentItem[], canReadUsers: boolean): Promise<OrganizationChartLayoutResult> {
    const treeLayoutInputs = rootDepartments.map((rootDepartment) => createDepartmentTreeLayoutInput(rootDepartment, canReadUsers));
    const normalizedNodesById = new Map(
        normalizeFlowNodeHeights(treeLayoutInputs.flatMap((tree) => tree.nodes))
            .map((node) => [node.id, node])
    );

    if (normalizedNodesById.size === 0) {
        return {
            nodes: [],
            edges: [],
            groups: [],
        };
    }

    const treeLayouts: OrganizationChartDepartmentTreeLayoutResult[] = [];
    let nextTreeGroupX = DEPARTMENT_TREE_GROUP_START_X;

    for (const treeLayoutInput of treeLayoutInputs) {
        const treeLayout = await createPositionedDepartmentTreeLayout(
            {
                ...treeLayoutInput,
                nodes: treeLayoutInput.nodes
                    .map((node) => normalizedNodesById.get(node.id))
                    .filter((node): node is OrganizationChartFlowNode => node != null),
            },
            nextTreeGroupX,
        );
        treeLayouts.push(treeLayout);

        if (treeLayout.group != null) {
            nextTreeGroupX = treeLayout.group.x + treeLayout.group.width + DEPARTMENT_TREE_GROUP_HORIZONTAL_SPACING;
        }
    }

    return {
        nodes: treeLayouts.flatMap((treeLayout) => treeLayout.nodes),
        edges: treeLayouts.flatMap((treeLayout) => treeLayout.edges),
        groups: treeLayouts
            .map((treeLayout) => treeLayout.group)
            .filter((group): group is OrganizationChartTreeGroup => group != null),
    };
}

function createDepartmentTreeLayoutInput(rootDepartment: OrganizationChartDepartmentItem, canReadUsers: boolean): OrganizationChartDepartmentTreeLayoutInput {
    const nodes: OrganizationChartFlowNode[] = [];
    const edges: OrganizationChartFlowEdge[] = [];

    function appendDepartment(department: OrganizationChartDepartmentItem): void {
        nodes.push(createFlowNode('department', department, canReadUsers));

        for (const child of department.children) {
            edges.push(createFlowEdge(
                `department-edge-${department.id}-${child.id}`,
                getNodeId('department', department.id),
                getNodeId('department', child.id),
            ));
            appendDepartment(child);
        }
    }

    appendDepartment(rootDepartment);

    return {
        rootDepartment,
        nodes,
        edges,
    };
}

async function createPositionedDepartmentTreeLayout(
    treeLayoutInput: OrganizationChartDepartmentTreeLayoutInput,
    treeGroupX: number,
): Promise<OrganizationChartDepartmentTreeLayoutResult> {
    const elkGraph: ElkNode = {
        id: `organization-chart-departments-${treeLayoutInput.rootDepartment.id}`,
        layoutOptions: ORGANIZATION_CHART_LAYOUT_OPTIONS,
        children: treeLayoutInput.nodes.map((node) => ({
            id: node.id,
            width: FLOW_NODE_WIDTH,
            height: node.data.height,
        })),
        edges: treeLayoutInput.edges.map((edge) => ({
            id: edge.id,
            sources: [edge.source],
            targets: [edge.target],
        })),
    };
    const laidOutGraph = await elk.layout(elkGraph);
    const positionsByNodeId = new Map((laidOutGraph.children ?? []).map((child) => [
        child.id,
        {
            x: child.x ?? 0,
            y: child.y ?? 0,
        },
    ]));
    const positionedNodes = treeLayoutInput.nodes.map((node) => ({
        ...node,
        position: positionsByNodeId.get(node.id) ?? node.position,
    }));
    const positionedNodesById = new Map(positionedNodes.map((node) => [node.id, node]));
    const unshiftedGroup = createDepartmentTreeGroup(treeLayoutInput.rootDepartment, positionedNodesById);

    if (unshiftedGroup == null) {
        return {
            nodes: positionedNodes,
            edges: treeLayoutInput.edges,
            group: null,
        };
    }

    const offsetX = treeGroupX - unshiftedGroup.x;
    const offsetY = DEPARTMENT_TREE_GROUP_START_Y - unshiftedGroup.y;
    const shiftedNodes = positionedNodes.map((node) => ({
        ...node,
        position: {
            x: node.position.x + offsetX,
            y: node.position.y + offsetY,
        },
    }));

    return {
        nodes: shiftedNodes,
        edges: treeLayoutInput.edges,
        group: {
            ...unshiftedGroup,
            x: unshiftedGroup.x + offsetX,
            y: unshiftedGroup.y + offsetY,
        },
    };
}

function createTeamLayout(teams: OrganizationChartTeamItem[], canReadUsers: boolean): OrganizationChartLayoutResult {
    const nodes = normalizeFlowNodeHeights(teams.map((team) => createFlowNode('team', team, canReadUsers)));
    const rowHeights: number[] = [];

    nodes.forEach((node, index) => {
        const row = Math.floor(index / TEAM_LAYOUT_COLUMN_COUNT);
        rowHeights[row] = Math.max(rowHeights[row] ?? 0, node.data.height);
    });

    const rowOffsets = rowHeights.reduce<number[]>((offsets, rowHeight, index) => {
        const previousOffset = index === 0 ? 56 : offsets[index - 1] + rowHeights[index - 1] + TEAM_LAYOUT_VERTICAL_SPACING;
        offsets.push(previousOffset);
        return offsets;
    }, []);

    return {
        nodes: nodes.map((node, index) => {
            const column = index % TEAM_LAYOUT_COLUMN_COUNT;
            const row = Math.floor(index / TEAM_LAYOUT_COLUMN_COUNT);

            return {
                ...node,
                position: {
                    x: 56 + (column * (FLOW_NODE_WIDTH + TEAM_LAYOUT_HORIZONTAL_SPACING)),
                    y: rowOffsets[row] ?? 56,
                },
            };
        }),
        edges: [],
        groups: [],
    };
}

function createFlowNode(
    itemType: 'department',
    item: OrganizationChartDepartmentItem,
    canReadUsers: boolean,
): OrganizationChartFlowNode;
function createFlowNode(
    itemType: 'team',
    item: OrganizationChartTeamItem,
    canReadUsers: boolean,
): OrganizationChartFlowNode;
function createFlowNode(
    itemType: 'department' | 'team',
    item: OrganizationChartDepartmentItem | OrganizationChartTeamItem,
    canReadUsers: boolean,
): OrganizationChartFlowNode {
    const id = getNodeId(itemType, item.id);
    const canReadMembers = canReadUsers && item.canReadMemberships;
    const height = getFlowNodeHeight(item, canReadMembers);

    return {
        id,
        type: FLOW_NODE_TYPE,
        position: {
            x: 0,
            y: 0,
        },
        sourcePosition: Position.Bottom,
        targetPosition: Position.Top,
        style: {
            width: FLOW_NODE_WIDTH,
            height,
        },
        data: itemType === 'department' ? {
            itemType,
            item: item as OrganizationChartDepartmentItem,
            height,
            canReadUsers,
            canReadMemberships: item.canReadMemberships,
        } : {
            itemType,
            item: item as OrganizationChartTeamItem,
            height,
            canReadUsers,
            canReadMemberships: item.canReadMemberships,
        },
    };
}

function normalizeFlowNodeHeights(nodes: OrganizationChartFlowNode[]): OrganizationChartFlowNode[] {
    const height = Math.max(
        FLOW_NODE_MIN_HEIGHT,
        ...nodes.map((node) => node.data.height),
    );

    return nodes.map((node) => setFlowNodeHeight(node, height));
}

function setFlowNodeHeight(node: OrganizationChartFlowNode, height: number): OrganizationChartFlowNode {
    return {
        ...node,
        style: {
            ...node.style,
            height,
        },
        data: {
            ...node.data,
            height,
        } as OrganizationChartFlowNodeData,
    };
}

function createDepartmentTreeGroup(
    rootDepartment: OrganizationChartDepartmentItem,
    nodesById: Map<string, OrganizationChartFlowNode>,
): OrganizationChartTreeGroup | null {
    const treeNodes = collectDepartmentNodeIds(rootDepartment)
        .map((nodeId) => nodesById.get(nodeId))
        .filter((node): node is OrganizationChartFlowNode => node != null);

    if (treeNodes.length === 0) {
        return null;
    }

    const minX = Math.min(...treeNodes.map((node) => node.position.x));
    const minY = Math.min(...treeNodes.map((node) => node.position.y));
    const maxX = Math.max(...treeNodes.map((node) => node.position.x + FLOW_NODE_WIDTH));
    const maxY = Math.max(...treeNodes.map((node) => node.position.y + node.data.height));

    return {
        id: `department-tree-group-${rootDepartment.id}`,
        label: rootDepartment.name,
        color: rootDepartment.color,
        x: minX - TREE_GROUP_HORIZONTAL_PADDING,
        y: minY - TREE_GROUP_TOP_PADDING,
        width: (maxX - minX) + (TREE_GROUP_HORIZONTAL_PADDING * 2),
        height: (maxY - minY) + TREE_GROUP_TOP_PADDING + TREE_GROUP_BOTTOM_PADDING,
    };
}

function collectDepartmentNodeIds(department: OrganizationChartDepartmentItem): string[] {
    return [
        getNodeId('department', department.id),
        ...department.children.flatMap(collectDepartmentNodeIds),
    ];
}

function getFlowNodeHeight(item: OrganizationChartDepartmentItem | OrganizationChartTeamItem, canReadMembers: boolean): number {
    if (!canReadMembers) {
        return FLOW_NODE_MIN_HEIGHT;
    }

    const visibleMemberCount = Math.min(item.members.length, MEMBER_PREVIEW_COUNT);
    const hasMoreMembersLink = item.members.length > MEMBER_PREVIEW_COUNT;

    return Math.max(
        FLOW_NODE_MIN_HEIGHT,
        FLOW_NODE_BASE_HEIGHT +
        (visibleMemberCount * FLOW_NODE_MEMBER_ROW_HEIGHT) +
        (hasMoreMembersLink ? FLOW_NODE_MORE_LINK_HEIGHT : 0),
    );
}

function createFlowEdge(id: string, source: string, target: string): OrganizationChartFlowEdge {
    return {
        id,
        source,
        target,
        sourceHandle: 'source',
        targetHandle: 'target',
        type: 'smoothstep',
        interactionWidth: 16,
        style: {
            stroke: 'var(--organization-chart-flow-edge-color)',
            strokeWidth: 2,
        },
    };
}

function getNodeId(itemType: 'department' | 'team', id: number): string {
    return `${itemType}-${id}`;
}

function getMemberAvatarName(member: OrganizationChartUserItem): string {
    return getMemberDisplayName(member);
}

function getMemberDisplayName(member: OrganizationChartUserItem): string {
    const fullName = `${member.firstName ?? ''} ${member.lastName ?? ''}`.trim();
    if (fullName.length > 0) {
        return fullName;
    }

    if (member.fullName != null && member.fullName.trim().length > 0) {
        return member.fullName;
    }

    return member.email ?? member.id;
}
