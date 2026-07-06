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
import './organigram-flow.css';
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
import Add from '@mui/icons-material/Add';
import Remove from '@mui/icons-material/Remove';
import CropFree from '@mui/icons-material/CropFree';
import Lock from '@mui/icons-material/Lock';
import LockOpen from '@mui/icons-material/LockOpen';
import Groups from '@aivot/mui-material-symbols-400-outlined/dist/groups/Groups';
import ViewRealSize from '@aivot/mui-material-symbols-400-outlined/dist/view-real-size/ViewRealSize';
import OpenInNew from '@aivot/mui-material-symbols-400-outlined/dist/open-in-new/OpenInNew';
import {memo, type CSSProperties, type ReactNode, useCallback, useEffect, useState} from 'react';
import {Link as RouterLink} from 'react-router-dom';
import {StringAvatar} from '../../../../components/avatar/string-avatar';
import {getDepartmentTypeIcons, getDepartmentTypeLabel} from '../../../departments/utils/department-utils';
import {
    type OrganigramDepartmentItem,
    type OrganigramTeamItem,
    type OrganigramUserItem,
} from './organigram-types';

interface OrganigramFlowProps {
    view: OrganigramFlowView;
    rootDepartments: OrganigramDepartmentItem[];
    teams: OrganigramTeamItem[];
}

export type OrganigramFlowView = 'departments' | 'teams';

type OrganigramFlowNodeData =
    | {
        itemType: 'department';
        item: OrganigramDepartmentItem;
        height: number;
    }
    | {
        itemType: 'team';
        item: OrganigramTeamItem;
        height: number;
    };

type OrganigramFlowNode = ReactFlowNode<OrganigramFlowNodeData>;
type OrganigramFlowEdge = ReactFlowEdge;

interface OrganigramLayoutResult {
    nodes: OrganigramFlowNode[];
    edges: OrganigramFlowEdge[];
    groups: OrganigramTreeGroup[];
}

interface OrganigramDepartmentTreeLayoutInput {
    rootDepartment: OrganigramDepartmentItem;
    nodes: OrganigramFlowNode[];
    edges: OrganigramFlowEdge[];
}

interface OrganigramDepartmentTreeLayoutResult {
    nodes: OrganigramFlowNode[];
    edges: OrganigramFlowEdge[];
    group: OrganigramTreeGroup | null;
}

interface OrganigramFlowCanvasProps {
    view: OrganigramFlowView;
    rootDepartments: OrganigramDepartmentItem[];
    teams: OrganigramTeamItem[];
}

interface OrganigramTreeGroup {
    id: string;
    label: string;
    color: string;
    x: number;
    y: number;
    width: number;
    height: number;
}

interface OrganigramFlowControlButtonProps {
    ariaLabel: string;
    children: ReactNode;
    disabled?: boolean;
    onClick: () => void;
    tooltip: string;
}

interface OrganigramMemberListProps {
    members: OrganigramUserItem[];
    compact?: boolean;
}

const elk = new ELK();
const FLOW_NODE_TYPE = 'organigram-node';
const FLOW_NODE_WIDTH = 420;
const FLOW_NODE_MIN_HEIGHT = 195;
const FLOW_MIN_ZOOM = 0.25;
const FLOW_MAX_ZOOM = 2;
const ZOOM_EPSILON = 0.001;
const MEMBER_PREVIEW_COUNT = 4;
const FLOW_NODE_BASE_HEIGHT = 100;
const FLOW_NODE_MEMBER_ROW_HEIGHT = 39;
const FLOW_NODE_MORE_LINK_HEIGHT = 34;
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
const ORGANIGRAM_LAYOUT_OPTIONS = {
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
    [FLOW_NODE_TYPE]: memo(OrganigramFlowNodeComponent),
};

export function OrganigramFlow(props: OrganigramFlowProps): ReactNode {
    const {
        view,
        rootDepartments,
        teams,
    } = props;

    const [renderedView, setRenderedView] = useState<OrganigramFlowView>(view);
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
                        <OrganigramFlowEmptyState message="Keine Organisationseinheiten vorhanden." />
                    ) : renderedView === 'teams' && teams.length === 0 ? (
                        <OrganigramFlowEmptyState message="Keine Teams vorhanden." />
                    ) : (
                        <ReactFlowProvider key={renderedView}>
                            <OrganigramFlowCanvas
                                view={renderedView}
                                rootDepartments={rootDepartments}
                                teams={teams}
                            />
                        </ReactFlowProvider>
                    )
                }
                <OrganigramFlowSwitchOverlay
                    visible={isSwitchingView || renderedView !== view}
                />
            </Box>
        </Box>
    );
}

function OrganigramFlowSwitchOverlay(props: {
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

function OrganigramFlowCanvas(props: OrganigramFlowCanvasProps): ReactNode {
    const {
        view,
        rootDepartments,
        teams,
    } = props;
    const theme = useTheme();
    const {
        fitView,
    } = useReactFlow<OrganigramFlowNode, OrganigramFlowEdge>();

    const [layout, setLayout] = useState<OrganigramLayoutResult>({
        nodes: [],
        edges: [],
        groups: [],
    });
    const [isLayoutReady, setIsLayoutReady] = useState(false);
    const [isViewportLocked, setIsViewportLocked] = useState(false);

    useEffect(() => {
        let isActive = true;
        setIsLayoutReady(false);

        createLayout(view, rootDepartments, teams)
            .then((nextLayout) => {
                if (!isActive) {
                    return;
                }

                setLayout(nextLayout);
                setIsLayoutReady(true);
            })
            .catch((error) => {
                console.error('Failed to layout organigram', error);
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
    }, [rootDepartments, teams, view]);

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

    const handleToggleViewportLock = useCallback(() => {
        setIsViewportLocked((current) => !current);
    }, []);

    return (
        <ReactFlow
            className="organigram-flow"
            style={{
                '--organigram-flow-top-fade-color-solid': alpha(theme.palette.background.default, 0.96),
                '--organigram-flow-top-fade-color-mid': alpha(theme.palette.background.default, 0.72),
                '--organigram-flow-top-fade-color-transparent': alpha(theme.palette.background.default, 0),
                '--organigram-flow-edge-color': theme.palette.mode === 'dark' ? theme.palette.grey[600] : theme.palette.grey[400],
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
            panOnDrag={!isViewportLocked}
            zoomOnScroll={!isViewportLocked}
            zoomOnPinch={!isViewportLocked}
            zoomOnDoubleClick={!isViewportLocked}
            preventScrolling={!isViewportLocked}
            proOptions={{
                hideAttribution: true,
            }}
        >
            <Background
                variant={BackgroundVariant.Dots}
            />
            <OrganigramTreeGroups
                groups={layout.groups}
            />
            <OrganigramFlowViewportControls
                isViewportLocked={isViewportLocked}
                onToggleViewportLock={handleToggleViewportLock}
            />
        </ReactFlow>
    );
}

function OrganigramFlowViewportControls(props: {
    isViewportLocked: boolean;
    onToggleViewportLock: () => void;
}): ReactNode {
    const {
        isViewportLocked,
        onToggleViewportLock,
    } = props;
    const {
        fitView,
        zoomIn,
        zoomOut,
        zoomTo,
    } = useReactFlow<OrganigramFlowNode, OrganigramFlowEdge>();
    const zoom = useStore((store) => store.transform[2]);
    const canZoomIn = zoom < FLOW_MAX_ZOOM - ZOOM_EPSILON;
    const canZoomOut = zoom > FLOW_MIN_ZOOM + ZOOM_EPSILON;

    return (
        <Controls
            className="organigram-flow-controls"
            position="bottom-left"
            showZoom={false}
            showFitView={false}
            showInteractive={false}
        >
            <OrganigramFlowControlButton
                disabled={!canZoomIn}
                onClick={() => {
                    void zoomIn();
                }}
                ariaLabel="Vergrößern"
                tooltip="Vergrößern"
            >
                <Add sx={{fontSize: 20}} />
            </OrganigramFlowControlButton>

            <OrganigramFlowControlButton
                disabled={!canZoomOut}
                onClick={() => {
                    void zoomOut();
                }}
                ariaLabel="Verkleinern"
                tooltip="Verkleinern"
            >
                <Remove sx={{fontSize: 20}} />
            </OrganigramFlowControlButton>

            <OrganigramFlowControlButton
                onClick={() => {
                    void fitView(FLOW_FIT_VIEW_OPTIONS);
                }}
                ariaLabel="Ansicht einpassen"
                tooltip="Ansicht einpassen"
            >
                <CropFree sx={{fontSize: 18}} />
            </OrganigramFlowControlButton>

            <OrganigramFlowControlButton
                onClick={() => {
                    void zoomTo(1);
                }}
                ariaLabel="Zoom auf Originalgröße (100 %)"
                tooltip="Zoom auf Originalgröße (100 %)"
            >
                <ViewRealSize sx={{fontSize: 18}} />
            </OrganigramFlowControlButton>

            <OrganigramFlowControlButton
                onClick={onToggleViewportLock}
                ariaLabel={isViewportLocked ? 'Viewport entsperren' : 'Viewport sperren'}
                tooltip={isViewportLocked ? 'Viewport entsperren' : 'Viewport sperren'}
            >
                {
                    isViewportLocked ?
                        <Lock sx={{fontSize: 18}} /> :
                        <LockOpen sx={{fontSize: 18}} />
                }
            </OrganigramFlowControlButton>
        </Controls>
    );
}

function OrganigramFlowControlButton(props: OrganigramFlowControlButtonProps): ReactNode {
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
            <span className="organigram-flow-control-tooltip-anchor">
                <ControlButton
                    className="organigram-flow-control-button"
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

function OrganigramTreeGroups(props: {
    groups: OrganigramTreeGroup[];
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

function OrganigramFlowNodeComponent(props: NodeProps<OrganigramFlowNode>): ReactNode {
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
            <OrganigramNodeCard
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

function OrganigramNodeCard(props: {
    data: OrganigramFlowNodeData;
}): ReactNode {
    const {
        data,
    } = props;
    const item = data.item;
    const department = data.itemType === 'department' ? data.item : null;
    const members = item.members;
    const isDepartment = department != null;
    const title = item.name;
    const detailLinkTo = isDepartment ? `/departments/${item.id}` : `/teams/${item.id}`;
    const managementLinkTo = isDepartment ? `/departments/${item.id}/members` : `/teams/${item.id}/members`;
    const typeLabel = department != null ? getDepartmentTypeLabel(department.depth) : 'Team';

    return (
        <Paper
            variant="outlined"
            sx={{
                width: '100%',
                height: '100%',
                boxSizing: 'border-box',
                px: 2,
                py: 1.75,
                borderRadius: 2,
                borderColor: 'divider',
                bgcolor: 'background.paper',
                boxShadow: 1,
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

                <Button
                    component={RouterLink}
                    to={managementLinkTo}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="nodrag nopan"
                    variant="outlined"
                    size="small"
                    sx={{flexShrink: 0}}
                >
                    Verwalten
                </Button>
            </Box>

            {
                members.length > 0 ? (
                    <OrganigramMemberList
                        members={members.slice(0, MEMBER_PREVIEW_COUNT)}
                        compact
                    />
                ) : (
                    <OrganigramMembersEmptyState />
                )
            }

            {
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

function OrganigramMembersEmptyState(): ReactNode {
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

function OrganigramMemberList(props: OrganigramMemberListProps): ReactNode {
    const {
        members,
        compact = true,
    } = props;

    return (
        <Stack spacing={compact ? 0.5 : 1}>
            {
                members.map((member) => (
                    <OrganigramMemberRow
                        key={member.id}
                        member={member}
                        compact={compact}
                    />
                ))
            }
        </Stack>
    );
}

function OrganigramMemberRow(props: {
    member: OrganigramUserItem;
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

function OrganigramFlowEmptyState(props: {
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
    view: OrganigramFlowView,
    rootDepartments: OrganigramDepartmentItem[],
    teams: OrganigramTeamItem[],
): Promise<OrganigramLayoutResult> {
    if (view === 'teams') {
        return createTeamLayout(teams);
    }

    return await createDepartmentLayout(rootDepartments);
}

async function createDepartmentLayout(rootDepartments: OrganigramDepartmentItem[]): Promise<OrganigramLayoutResult> {
    const treeLayoutInputs = rootDepartments.map(createDepartmentTreeLayoutInput);
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

    const treeLayouts: OrganigramDepartmentTreeLayoutResult[] = [];
    let nextTreeGroupX = DEPARTMENT_TREE_GROUP_START_X;

    for (const treeLayoutInput of treeLayoutInputs) {
        const treeLayout = await createPositionedDepartmentTreeLayout(
            {
                ...treeLayoutInput,
                nodes: treeLayoutInput.nodes
                    .map((node) => normalizedNodesById.get(node.id))
                    .filter((node): node is OrganigramFlowNode => node != null),
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
            .filter((group): group is OrganigramTreeGroup => group != null),
    };
}

function createDepartmentTreeLayoutInput(rootDepartment: OrganigramDepartmentItem): OrganigramDepartmentTreeLayoutInput {
    const nodes: OrganigramFlowNode[] = [];
    const edges: OrganigramFlowEdge[] = [];

    function appendDepartment(department: OrganigramDepartmentItem): void {
        nodes.push(createFlowNode('department', department));

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
    treeLayoutInput: OrganigramDepartmentTreeLayoutInput,
    treeGroupX: number,
): Promise<OrganigramDepartmentTreeLayoutResult> {
    const elkGraph: ElkNode = {
        id: `organigram-departments-${treeLayoutInput.rootDepartment.id}`,
        layoutOptions: ORGANIGRAM_LAYOUT_OPTIONS,
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

function createTeamLayout(teams: OrganigramTeamItem[]): OrganigramLayoutResult {
    const nodes = normalizeFlowNodeHeights(teams.map((team) => createFlowNode('team', team)));
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
    item: OrganigramDepartmentItem,
): OrganigramFlowNode;
function createFlowNode(
    itemType: 'team',
    item: OrganigramTeamItem,
): OrganigramFlowNode;
function createFlowNode(
    itemType: 'department' | 'team',
    item: OrganigramDepartmentItem | OrganigramTeamItem,
): OrganigramFlowNode {
    const id = getNodeId(itemType, item.id);
    const height = getFlowNodeHeight(item);

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
            item: item as OrganigramDepartmentItem,
            height,
        } : {
            itemType,
            item: item as OrganigramTeamItem,
            height,
        },
    };
}

function normalizeFlowNodeHeights(nodes: OrganigramFlowNode[]): OrganigramFlowNode[] {
    const height = Math.max(
        FLOW_NODE_MIN_HEIGHT,
        ...nodes.map((node) => node.data.height),
    );

    return nodes.map((node) => setFlowNodeHeight(node, height));
}

function setFlowNodeHeight(node: OrganigramFlowNode, height: number): OrganigramFlowNode {
    return {
        ...node,
        style: {
            ...node.style,
            height,
        },
        data: {
            ...node.data,
            height,
        } as OrganigramFlowNodeData,
    };
}

function createDepartmentTreeGroup(
    rootDepartment: OrganigramDepartmentItem,
    nodesById: Map<string, OrganigramFlowNode>,
): OrganigramTreeGroup | null {
    const treeNodes = collectDepartmentNodeIds(rootDepartment)
        .map((nodeId) => nodesById.get(nodeId))
        .filter((node): node is OrganigramFlowNode => node != null);

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

function collectDepartmentNodeIds(department: OrganigramDepartmentItem): string[] {
    return [
        getNodeId('department', department.id),
        ...department.children.flatMap(collectDepartmentNodeIds),
    ];
}

function getFlowNodeHeight(item: OrganigramDepartmentItem | OrganigramTeamItem): number {
    const visibleMemberCount = Math.min(item.members.length, MEMBER_PREVIEW_COUNT);
    const hasMoreMembersLink = item.members.length > MEMBER_PREVIEW_COUNT;

    return Math.max(
        FLOW_NODE_MIN_HEIGHT,
        FLOW_NODE_BASE_HEIGHT +
        (visibleMemberCount * FLOW_NODE_MEMBER_ROW_HEIGHT) +
        (hasMoreMembersLink ? FLOW_NODE_MORE_LINK_HEIGHT : 0),
    );
}

function createFlowEdge(id: string, source: string, target: string): OrganigramFlowEdge {
    return {
        id,
        source,
        target,
        sourceHandle: 'source',
        targetHandle: 'target',
        type: 'smoothstep',
        interactionWidth: 16,
        style: {
            stroke: 'var(--organigram-flow-edge-color)',
            strokeWidth: 2,
        },
    };
}

function getNodeId(itemType: 'department' | 'team', id: number): string {
    return `${itemType}-${id}`;
}

function getMemberAvatarName(member: OrganigramUserItem): string {
    return getMemberDisplayName(member);
}

function getMemberDisplayName(member: OrganigramUserItem): string {
    const fullName = `${member.firstName ?? ''} ${member.lastName ?? ''}`.trim();
    if (fullName.length > 0) {
        return fullName;
    }

    if (member.fullName != null && member.fullName.trim().length > 0) {
        return member.fullName;
    }

    return member.email ?? member.id;
}
