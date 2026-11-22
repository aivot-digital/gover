import {NoCodeEditorNotes} from './no-code-editor-notes';
import {NoCodeEditorMetadata} from '../../../models/functions/no-code-editor-metadata';
import ArrowUpward from '@aivot/mui-material-symbols-400-n25-outlined/ArrowUpward';
import ArrowDownward from '@aivot/mui-material-symbols-400-n25-outlined/ArrowDownward';
import OpenWith from '@aivot/mui-material-symbols-400-n25-outlined/OpenWith';
import AccountTree from '@aivot/mui-material-symbols-400-n25-outlined/AccountTree';
import Info from '@aivot/mui-material-symbols-400-n25-outlined/Info';
import Delete from '@aivot/mui-material-symbols-400-n25-outlined/Delete';
import {createPortal} from 'react-dom';
import Undo from '@aivot/mui-material-symbols-400-n25-outlined/Undo';
import Redo from '@aivot/mui-material-symbols-400-n25-outlined/Redo';
import Science from '@aivot/mui-material-symbols-400-n25-outlined/Science';
import Add from '@aivot/mui-material-symbols-400-n25-outlined/Add';
import Remove from '@aivot/mui-material-symbols-400-n25-outlined/Remove';
import KeyboardArrowDown from '@aivot/mui-material-symbols-400-n25-outlined/KeyboardArrowDown';
import ChevronRight from '@aivot/mui-material-symbols-400-n25-outlined/ChevronRight';
import {NoCodeOperatorDeleteDialog} from './no-code-operator-delete-dialog';
import {NoCodeTestResult} from './no-code-test-result';
import {formatNoCodeValue, formatNoCodeWhitespaceText} from '../utils/format-no-code-value';
import {resolveNoCodeExpressionPresentation} from '../utils/no-code-parameter-presentation';
import {useNoCodePresentationContext} from '../hooks/use-no-code-presentation-context';
import {NoCodePresentationContext} from '../utils/humanize-no-code';
import {useNoCodeParameterSuggestions} from '../hooks/use-no-code-parameter-suggestions';
import {
    type KeyboardEvent,
    type ReactElement,
    type ReactNode,
    type RefObject,
    useEffect,
    useMemo,
    useLayoutEffect,
    useRef,
    useState,
} from 'react';
import {
    Box,
    Button,
    ButtonBase,
    Divider,
    IconButton,
    Link,
    ListItemIcon,
    ListItemText,
    Menu,
    MenuItem,
    Popover,
    Tooltip,
    Typography,
} from '@mui/material';
import MoreVert from '@aivot/mui-material-symbols-400-n25-outlined/MoreVert';
import {useDrop, VisuallyHidden} from 'react-aria-components';
import {v4 as uuid4} from 'uuid';
import {
    NoCodeOperand,
    NoCodeOperandError,
} from '../../../models/functions/no-code-expression';
import {
    findMatchingNoCodeSignature,
    getNoCodeVariadicInputKind,
    NoCodeOperatorDetailsDTO,
    NoCodeParameter,
    NoCodeParameterOption,
    resolveNoCodeSignature,
} from '../../../models/dtos/no-code-operator-details-dto';
import {NoCodeDataType, NoCodeDataTypeLabels} from '../../../data/no-code-data-type';
import {ElementWithParents} from '../../../utils/flatten-elements';
import {generateComponentTitle} from '../../../utils/generate-component-title';
import {isStringNullOrEmpty} from '../../../utils/string-utils';
import {useNormalizedReactId} from '../../../hooks/use-normalized-react-id';
import {SelectOperatorDialog} from '../../../dialogs/select-operator-dialog/select-operator-dialog';
import {ConfirmDialog} from '../../../dialogs/confirm-dialog/confirm-dialog';
import {InfoDialog} from '../../../dialogs/info-dialog/info-dialog';
import {MarkdownContent} from '../../../components/markdown-content/markdown-content';
import {HintTooltip} from '../../../components/hint-tooltip/hint-tooltip';
import {NoCodeDryRunResultDTO, NoCodeDryRunStatus} from '../../../models/dtos/no-code-dry-run-dto';
import {NoCodeDataTypeMap} from '../data/no-code-data-type-map';
import {humanizeNoCode} from '../utils/humanize-no-code';
import {NoCodeSummary, summarizeNoCode} from '../utils/no-code-summary';
import {NoCodeExpressionSummary} from './no-code-expression-summary';
import {NoCodeOperandKindHelp} from '../data/no-code-operand-kind-help';
import {useNoCodeEditorStore} from '../hooks/use-no-code-editor-store';
import {
    findNoCodeEditorNode,
    findNoCodeEditorSlot,
    NoCodeEditorAction,
    NoCodeEditorExpressionNode,
    NoCodeEditorLeafNode,
    NoCodeEditorNode,
    noCodeEditorReducer,
    NoCodeEditorSlot,
    toNoCodeOperand,
    toNoCodeEditorNode,
} from '../utils/no-code-editor-store';
import {validateNoCode} from '../utils/validate-no-code';
import {getNoCodeEditorTypeConflict} from '../utils/no-code-editor-types';
import {NoCodeDataTypeIcons, NoCodePill} from './no-code-pill';
import {NoCodeOperatorChangeDialog} from './no-code-operator-change-dialog';
import {NoCodeDryRunPanel} from './no-code-dry-run-panel';
import {NoCodeOperandEditor, NoCodeOperandEditorContextType} from './no-code-operand-editor';
import {NoCodeOperandEditorSelector} from './no-code-operand-editor-selector';
import {BOOL_DEFAULT_OPTIONS} from './no-code-operand-editor-static-value';

interface NoCodeEditorProps {
    parameter: NoCodeParameter;
    operand: NoCodeOperand | undefined | null;
    onChange: (operand: NoCodeOperand | undefined | null) => void;
    allOperators: NoCodeOperatorDetailsDTO[];
    allElements: ElementWithParents[];
    contextType?: NoCodeOperandEditorContextType;
    operandError?: NoCodeOperandError;
    readOnly?: boolean;
    busy?: boolean;
    validationRequest?: number;
    focusOnValidation?: boolean;
    toolbarContainer?: HTMLElement | null;
    emptyDescription?: string;
    scrollContainerRef?: RefObject<HTMLDivElement | null>;
}

/** A node picked up for moving, with the keys of the tree items it can be dropped on in DOM order. */
interface GrabState {
    nodeId: string;
    name: string;
    targets: string[];
    targetIndex: number;
}

interface MenuState {
    open: boolean;
    details: NodeDetails | null;
    nodeId: string;
    name: string;
    anchor: HTMLElement;
    up: NoCodeEditorSlot | null;
    down: NoCodeEditorSlot | null;
    canMoveElsewhere: boolean;
}

interface EditorContext {
    readOnly: boolean;
    emptyDescription?: string;
    collapsed: Set<string>;
    toggleCollapsed: (id: string) => void;
    validated: Set<string>;
    touched: Set<string>;
    touch: (id: string) => void;
    allOperators: NoCodeOperatorDetailsDTO[];
    allElements: ElementWithParents[];
    contextType: NoCodeOperandEditorContextType;
    dispatch: (action: NoCodeEditorAction) => void;
    insert: (parentId: string | null, index: number, operand: NoCodeOperand, editAfterInsert?: boolean) => void;
    /** Removes a node and moves focus to the socket it leaves behind. */
    remove: (nodeId: string) => void;
    appendOperand: (parentId: string) => void;
    removeOperand: (parentId: string, index: number, label: string) => void;
    /** An incomplete leaf inserted into an empty slot opens its value editor right away. */
    autoEditId: string | null;
    /** Key of the single tree item in the tab sequence (roving tab index). */
    activeKey: string | null;
    grab: GrabState | null;
    startGrab: (nodeId: string, name: string) => void;
    openMenu: (nodeId: string, name: string, anchor: HTMLElement) => void;
    /** Opens the description of a node; focus returns to its tree item afterwards. */
    showDescription: (nodeId: string, details: NodeDetails) => void;
    startPointerDrag: (nodeId: string, name: string) => void;
    endPointerDrag: () => void;
    canDropPointerDrag: (slot: NoCodeEditorSlot) => boolean;
    dropPointerDrag: (slot: NoCodeEditorSlot) => void;
    /** Dry run results by node ID while the dry run panel is open. */
    results: Map<string, NoCodeDryRunResultDTO> | null;
    presentation: NoCodePresentationContext;
}

/** The full description of a node, shown in a dialog. */
interface NodeDetails {
    title: string;
    details: ReactNode;
}

interface SlotProps {
    depth?: number;
    suggestions?: NoCodeParameterOption[];
    node: NoCodeEditorNode | null;
    parentId: string | null;
    index: number;
    parameter: NoCodeParameter;
    error: NoCodeOperandError | null | undefined;
    onRemoveOperand?: () => void;
    ctx: EditorContext;
}

const TREE_ITEM_SELECTOR = '[role="treeitem"]';
const MIN_VARIADIC_OPERANDS = 2;
const DRAG_TYPE = 'application/x-prosuna-no-code-node';

const focusRing = {
    outline: '2px solid',
    outlineColor: 'primary.main',
    outlineOffset: 2,
};

function socketKey(parentId: string | null, index: number): string {
    return `socket:${parentId ?? 'root'}:${index}`;
}

function slotOf(item: HTMLElement): NoCodeEditorSlot {
    return {
        parentId: item.dataset.parentId ?? null,
        index: Number(item.dataset.index),
    };
}

function getTreeItems(tree: HTMLElement | null): HTMLElement[] {
    return tree == null ? [] : Array.from(tree.querySelectorAll<HTMLElement>(TREE_ITEM_SELECTOR));
}

function describeTarget(item: HTMLElement): string {
    const swap = item.dataset.socket == null ? ', die Werte werden getauscht' : '';
    return `Ziel: ${item.getAttribute('aria-label')}${swap}.`;
}

/**
 * Shared No-Code editor for inline and dialog editing, including optional authoring metadata.
 *
 * Keyboard moving is handled here instead of by react-aria, whose Enter/Tab scheme would collide with the tree
 * navigation; react-aria provides the pointer drag and drop.
 */
export function NoCodeEditor(props: NoCodeEditorProps) {
    const {
        parameter,
        operand,
        onChange,
        allOperators,
        allElements,
        contextType = 'BOTH',
        operandError,
        readOnly = false,
        busy = false,
    } = props;

    const [testOpen, setTestOpen] = useState(false);
    const testPanelId = `${useNormalizedReactId()}-test-panel`;
    const scrollRef = useRef<HTMLDivElement>(null);
    const testPanelRef = useRef<HTMLDivElement>(null);
    useLayoutEffect(() => {
        if (testOpen && testPanelRef.current != null) {
            const container = props.scrollContainerRef?.current ?? scrollRef.current;
            if (container == null) return;
            const reducedMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
            const top = testPanelRef.current.getBoundingClientRect().top - container.getBoundingClientRect().top
                + container.scrollTop - container.clientTop;
            container.scrollTo?.({
                top,
                behavior: reducedMotion ? 'instant' : 'smooth',
            });
        }
    }, [testOpen, props.scrollContainerRef]);
    const locked = readOnly || busy;
    const [root, dispatch, history] = useNoCodeEditorStore(operand, onChange, !locked);
    const updateEditorMetadata = (patch: Partial<NoCodeEditorMetadata>) => {
        const metadata = {...root?.editorMetadata, ...patch};
        dispatch({
            type: 'setEditorMetadata',
            metadata: metadata.notes?.trim() || metadata.testData != null ? metadata : undefined,
        });
    };
    const [collapsed, setCollapsed] = useState<Set<string>>(new Set());
    const [touched, setTouched] = useState<Set<string>>(new Set());
    const [validated, setValidated] = useState<Set<string>>(new Set());
    const [focusError, setFocusError] = useState(false);
    const [removeTarget, setRemoveTarget] = useState<{ node: NoCodeEditorNode; hasNotes: boolean; open: boolean } | null>(null);
    const removeFocus = useRef<string | null>(null);
    const [operandRemoval, setOperandRemoval] = useState<{
        parentId: string;
        index: number;
        label: string;
        nodeId: string;
        open: boolean;
        focusKey: string;
    } | null>(null);
    const validation = operandError ?? validateNoCode(toNoCodeOperand(root), allOperators, allElements, true, parameter);
    const [autoEditId, setAutoEditId] = useState<string | null>(null);
    const [activeKey, setActiveKey] = useState<string | null>(null);
    const [focusRequest, setFocusRequest] = useState<{ key: string } | null>(null);
    // A new id per message, so repeating the same text is announced again.
    const [announcement, setAnnouncement] = useState<{ text: string; id: number } | null>(null);
    const [grab, setGrab] = useState<GrabState | null>(null);
    const [menu, setMenu] = useState<MenuState | null>(null);
    // Keep the menu's snapshot until onExited, even when an action replaces or removes its node.
    const closeMenu = () => setMenu((current) => current == null ? null : {
        ...current,
        open: false,
    });
    const [pointerDrag, setPointerDrag] = useState<{ nodeId: string; name: string } | null>(null);
    const previewOperand = useMemo(() => toNoCodeOperand(root), [root]);
    const presentation = useNoCodePresentationContext(previewOperand, allElements);
    const [results, setResults] = useState<Map<string, NoCodeDryRunResultDTO> | null>(null);
    const [wrapTarget, setWrapTarget] = useState<{
        nodeId: string;
        name: string;
        returnType: NoCodeDataType
    } | null>(null);
    // Kept until the dialog has closed, so its content stays during the transition and focus can return.
    const [description, setDescription] = useState<NodeDetails & { nodeId: string; open: boolean } | null>(null);
    const treeRef = useRef<HTMLDivElement>(null);
    // Blur and Tab handlers can run before the next render, so they read the grab state from here.
    const grabRef = useRef<GrabState | null>(null);
    // What happens once the menu or the operator dialog has finished closing; focusing earlier races their focus trap.
    const afterMenuRef = useRef<{
        nodeId: string;
        name: string;
        wrap: boolean;
        remove?: boolean;
        describe?: NodeDetails
    } | null>(null);
    const focusAfterDialogRef = useRef<string | null>(null);
    const helpId = `${useNormalizedReactId()}-help`;

    const markValidated = () => {
        const keys = new Set<string>();
        const visit = (node: NoCodeEditorNode | null, parentId: string | null, index: number) => {
            keys.add(node?.id ?? socketKey(parentId, index));
            if (node?.type === 'NoCodeExpression') node.operands.forEach((child, i) => visit(child, node.id, i));
        };
        visit(root, null, 0);
        setValidated(keys);
        setCollapsed(new Set());
    };
    const requestValidation = () => {
        markValidated();
        setFocusError(true);
    };
    // Validate the current draft once per explicit request; later insertions start untouched.
    useEffect(() => {
        if (props.validationRequest) {
            markValidated();
            if (props.focusOnValidation !== false) setFocusError(true);
        } else setValidated(new Set());
    }, [props.validationRequest]);
    useLayoutEffect(() => {
        if (!focusError) return;
        const first = treeRef.current?.querySelector<HTMLElement>('[role="treeitem"][aria-invalid="true"]');
        if (first) {
            setActiveKey(first.dataset.key ?? null);
            first.focus();
        }
        setFocusError(false);
    }, [focusError]);

    const requestFocus = (key: string) => {
        setActiveKey(key);
        setFocusRequest({key});
    };
    // A menu or dialog hides the rest of the page from screen readers until it has closed, live region included.
    // Messages caused from inside them wait until then, otherwise they would not be read.
    const deferredAnnouncementRef = useRef<{ text: string | null } | null>(null);
    const showAnnouncement = (text: string) => setAnnouncement((current) => ({
        text,
        id: (current?.id ?? 0) + 1,
    }));
    const announce = (text: string) => {
        if (deferredAnnouncementRef.current != null) {
            deferredAnnouncementRef.current.text = text;
        } else {
            showAnnouncement(text);
        }
    };
    const deferAnnouncements = () => {
        deferredAnnouncementRef.current = {text: null};
    };
    const flushAnnouncements = () => {
        const text = deferredAnnouncementRef.current?.text;
        deferredAnnouncementRef.current = null;
        if (text != null) {
            showAnnouncement(text);
        }
    };
    const updateGrab = (next: GrabState | null) => {
        grabRef.current = next;
        setGrab(next);
    };

    // Keeps exactly one tree item tabbable after inserts and removals, and moves focus once it is rendered.
    useLayoutEffect(() => {
        const items = getTreeItems(treeRef.current);
        const active = items.find((item) => item.dataset.key === activeKey) ?? items[0];
        if (active == null) {
            return;
        }

        if (active.dataset.key !== activeKey) {
            setActiveKey(active.dataset.key ?? null);
        }
        if (focusRequest != null) {
            setFocusRequest(null);
            active.focus();
        }
    });

    const getSlotParameter = (slot: NoCodeEditorSlot): Pick<NoCodeParameter, 'type' | 'dateInput'> => {
        if (slot.parentId == null) {
            return parameter;
        }
        const parent = findNoCodeEditorNode(root, slot.parentId);
        const operator = parent?.type === 'NoCodeExpression'
            ? allOperators.find((op) => op.identifier === parent.operatorIdentifier)
            : undefined;
        if (parent?.type !== 'NoCodeExpression' || operator == null) {
            return {type: NoCodeDataType.Runtime};
        }
        return resolveNoCodeSignature(operator, {operandCount: parent.operands.length || undefined})
            .parameters[slot.index] ?? {type: NoCodeDataType.Runtime};
    };

    const fitsSlot = (node: NoCodeEditorNode | null, slot: NoCodeEditorSlot) => (
        node == null || getNoCodeEditorTypeConflict(node, getSlotParameter(slot), allElements, allOperators) == null
    );

    // Incompatible types are rejected like structurally impossible moves, for every way of moving.
    const canMove = (nodeId: string, slot: NoCodeEditorSlot) => {
        if (noCodeEditorReducer(root, {
            type: 'moveNode',
            nodeId, ...slot,
        }) === root) {
            return false;
        }
        const source = findNoCodeEditorSlot(root, nodeId);
        const parent = slot.parentId == null ? null : findNoCodeEditorNode(root, slot.parentId);
        const occupant = slot.parentId == null
            ? root
            : parent?.type === 'NoCodeExpression' ? parent.operands[slot.index] ?? null : null;
        // A swap moves the occupant into the source slot, so it has to fit there as well.
        return fitsSlot(findNoCodeEditorNode(root, nodeId), slot) && (source == null || fitsSlot(occupant, source));
    };

    const move = (nodeId: string, slot: NoCodeEditorSlot, name: string) => {
        dispatch({
            type: 'moveNode',
            nodeId, ...slot,
        });
        announce(`„${name}“ wurde verschoben.`);
    };

    const findItem = (key: string) => getTreeItems(treeRef.current).find((item) => item.dataset.key === key);

    const getMoveTargets = (nodeId: string) => getTreeItems(treeRef.current)
        .filter((item) => item.dataset.key !== nodeId && canMove(nodeId, slotOf(item)));

    const startGrab = (nodeId: string, name: string) => {
        if (locked) return;
        const grabbed = findItem(nodeId);
        const targets = getMoveTargets(nodeId);
        if (grabbed == null || targets.length === 0) {
            announce(`Für „${name}“ ist kein Ziel zum Verschieben verfügbar.`);
            return;
        }

        // Start at the first target after the grabbed item, which is usually where the user wants to go.
        const following = targets.findIndex((item) => grabbed.compareDocumentPosition(item) & Node.DOCUMENT_POSITION_FOLLOWING);
        const targetIndex = Math.max(following, 0);
        updateGrab({
            nodeId,
            name,
            targets: targets.map((item) => item.dataset.key!),
            targetIndex,
        });
        announce(
            `„${name}“ aufgenommen. ${describeTarget(targets[targetIndex])} ` +
            'Wählen Sie mit den Pfeiltasten ein anderes Ziel, legen Sie mit der Leertaste ab oder brechen Sie mit Escape ab.',
        );
    };

    const moveGrabTarget = (targetIndex: number) => {
        const current = grabRef.current;
        if (current == null) {
            return;
        }
        const clamped = Math.min(Math.max(targetIndex, 0), current.targets.length - 1);
        const target = findItem(current.targets[clamped]);
        updateGrab({
            ...current,
            targetIndex: clamped,
        });
        if (target != null) {
            announce(describeTarget(target));
        }
    };

    const dropGrab = (targetKey: string) => {
        const current = grabRef.current;
        const target = findItem(targetKey);
        if (current == null || target == null) {
            return;
        }
        updateGrab(null);
        move(current.nodeId, slotOf(target), current.name);
        requestFocus(current.nodeId);
    };

    const cancelGrab = () => {
        const current = grabRef.current;
        if (current != null) {
            updateGrab(null);
            announce(`Verschieben von „${current.name}“ abgebrochen.`);
        }
    };

    useEffect(() => {
        if (locked) {
            updateGrab(null);
            setPointerDrag(null);
            setWrapTarget(null);
            setAutoEditId(null);
            setOperandRemoval(current => current?.open ? {...current, open: false} : current);
            afterMenuRef.current = null;
            closeMenu();
        }
    }, [locked]);

    const removeImmediately = (nodeId: string) => {
        const slot = findNoCodeEditorSlot(root, nodeId);
        dispatch({
            type: 'removeNode',
            nodeId,
        });
        return slot == null ? nodeId : socketKey(slot.parentId, slot.index);
    };

    const findEditableVariadicParent = (parentId: string): NoCodeEditorExpressionNode | null => {
        if (locked || grabRef.current != null || pointerDrag != null) return null;
        const parent = findNoCodeEditorNode(root, parentId);
        if (parent?.type !== 'NoCodeExpression') return null;
        const operator = allOperators.find(op => op.identifier === parent.operatorIdentifier);
        return operator != null && getNoCodeVariadicInputKind(operator) != null ? parent : null;
    };

    const removeOperandAt = (parent: NoCodeEditorExpressionNode, index: number): string => {
        const operands = parent.operands.filter((_, i) => i !== index);
        dispatch({type: 'replaceNode', nodeId: parent.id, node: {...parent, operands}});
        // Empty slots are identified by position. Keep their validation state with the remaining slots.
        setValidated(current => {
            const next = new Set(current);
            for (let i = index; i < parent.operands.length; i++) {
                const key = socketKey(parent.id, i);
                if (i < operands.length && current.has(socketKey(parent.id, i + 1))) next.add(key);
                else next.delete(key);
            }
            return next;
        });
        const adjacentIndex = Math.min(index, operands.length - 1);
        return operands[adjacentIndex]?.id ?? socketKey(parent.id, adjacentIndex);
    };

    const ctx: EditorContext = {
        collapsed,
        toggleCollapsed: (id) => {
            setCollapsed((current) => {
                const next = new Set(current);
                if (next.has(id)) next.delete(id);
                else next.add(id);
                return next;
            });
            requestFocus(id);
        },
        validated,
        touched,
        touch: (id) => setTouched((current) => new Set(current).add(id)),
        readOnly: locked,
        emptyDescription: props.emptyDescription,
        allOperators,
        allElements,
        contextType,
        dispatch,
        autoEditId,
        activeKey,
        grab,
        startGrab,
        results,
        presentation,
        showDescription: (nodeId, details) => setDescription({
            ...details,
            nodeId,
            open: true,
        }),
        insert: (parentId, index, inserted, editAfterInsert = true) => {
            const node = toNoCodeEditorNode(inserted);
            if (node == null) {
                return;
            }
            dispatch({
                type: 'insertNode',
                parentId,
                index,
                node,
            });
            setAutoEditId(editAfterInsert && node.type !== 'NoCodeExpression' ? node.id : null);
            requestFocus(node.id);
        },
        remove: (nodeId) => {
            if (locked) return;
            const node = findNoCodeEditorNode(root, nodeId);
            if (node == null) return;
            const hasChildren = node.type === 'NoCodeExpression' && node.operands.some((child) => child != null);
            const hasNotes = node.id === root?.id && Boolean(root?.editorMetadata?.notes?.trim());
            if (hasChildren || hasNotes) {
                removeFocus.current = nodeId;
                setRemoveTarget({
                    node,
                    hasNotes,
                    open: true,
                });
            } else requestFocus(removeImmediately(nodeId));
        },
        appendOperand: (parentId) => {
            const parent = findEditableVariadicParent(parentId);
            if (parent == null) return;
            const index = Math.max(MIN_VARIADIC_OPERANDS, parent.operands.length);
            const operands = Array.from({length: index + 1}, (_, i) => parent.operands[i] ?? null);
            dispatch({type: 'replaceNode', nodeId: parentId, node: {...parent, operands}});
            const key = socketKey(parentId, index);
            setValidated(current => {
                if (!current.has(key)) return current;
                const next = new Set(current);
                next.delete(key);
                return next;
            });
            requestFocus(key);
        },
        removeOperand: (parentId, index, label) => {
            const parent = findEditableVariadicParent(parentId);
            if (parent == null || index < MIN_VARIADIC_OPERANDS || index >= parent.operands.length) return;
            const child = parent.operands[index];
            if (child == null) requestFocus(removeOperandAt(parent, index));
            else setOperandRemoval({parentId, index, label, nodeId: child.id, open: true, focusKey: child.id});
        },
        openMenu: (nodeId, name, anchor) => {
            const item = findItem(nodeId);
            // Siblings in the same group are the neighbouring parameters of the same operator.
            const sibling = (element: Element | null | undefined) => {
                if (!(element instanceof HTMLElement) || !element.matches(TREE_ITEM_SELECTOR)) {
                    return null;
                }
                const slot = slotOf(element);
                return canMove(nodeId, slot) ? slot : null;
            };
            afterMenuRef.current = {
                nodeId,
                name,
                wrap: false,
            };
            const node = findNoCodeEditorNode(root, nodeId);
            setMenu({
                open: true,
                details: node == null ? null : getNodeDetails(node, allOperators),
                nodeId,
                name,
                anchor,
                up: sibling(item?.previousElementSibling),
                down: sibling(item?.nextElementSibling),
                canMoveElsewhere: getMoveTargets(nodeId).length > 0,
            });
        },
        startPointerDrag: (nodeId, name) => setPointerDrag({
            nodeId,
            name,
        }),
        endPointerDrag: () => setPointerDrag(null),
        canDropPointerDrag: (slot) => !locked && pointerDrag != null && canMove(pointerDrag.nodeId, slot),
        dropPointerDrag: (slot) => {
            if (pointerDrag != null && canMove(pointerDrag.nodeId, slot)) {
                move(pointerDrag.nodeId, slot, pointerDrag.name);
            }
        },
    };

    const restoreHistory = (redo: boolean) => {
        if (locked) return;
        cancelGrab();
        setAutoEditId(null);
        closeMenu();
        afterMenuRef.current = null;
        setWrapTarget(null);
        if (redo) history.redo();
        else history.undo();
    };

    const handleKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
        const tree = treeRef.current;
        const target = event.target as HTMLElement;
        // Events from popovers and dialogs bubble through the React tree although they render in a portal. A key
        // already handled by a tree item, such as the Space that starts moving, must not be handled twice.
        if (tree == null || !tree.contains(target) || event.defaultPrevented) {
            return;
        }
        // Summary controls and input actions handle their own keys outside tree navigation.
        if (target.closest('[data-no-code-summary], [data-no-code-tree-action]') != null) {
            return;
        }

        const currentGrab = grabRef.current;
        if (currentGrab != null) {
            switch (event.key) {
                case 'ArrowDown':
                case 'ArrowRight':
                    moveGrabTarget(currentGrab.targetIndex + 1);
                    break;
                case 'ArrowUp':
                case 'ArrowLeft':
                    moveGrabTarget(currentGrab.targetIndex - 1);
                    break;
                case 'Home':
                    moveGrabTarget(0);
                    break;
                case 'End':
                    moveGrabTarget(currentGrab.targets.length - 1);
                    break;
                case ' ':
                case 'Enter':
                    dropGrab(currentGrab.targets[currentGrab.targetIndex]);
                    break;
                case 'Escape':
                    cancelGrab();
                    break;
                case 'Tab':
                    cancelGrab();
                    return;
                default:
                    return;
            }
            event.preventDefault();
            return;
        }

        const item = target.closest<HTMLElement>(TREE_ITEM_SELECTOR);
        if (item == null) {
            return;
        }

        const items = getTreeItems(tree);
        const index = items.indexOf(item);
        let next: HTMLElement | null | undefined;
        switch (event.key) {
            case 'ArrowDown':
                next = items[index + 1];
                break;
            case 'ArrowUp':
                next = items[index - 1];
                break;
            case 'Home':
                next = items[0];
                break;
            case 'End':
                next = items[items.length - 1];
                break;
            case 'ArrowRight':
                if (item.getAttribute('aria-expanded') === 'false') {
                    ctx.toggleCollapsed(item.dataset.key!);
                    break;
                }
                next = item.querySelector<HTMLElement>(`:scope > [role="group"] > ${TREE_ITEM_SELECTOR}`);
                break;
            case 'ArrowLeft':
                if (item.getAttribute('aria-expanded') === 'true') {
                    ctx.toggleCollapsed(item.dataset.key!);
                    break;
                }
                next = item.parentElement?.closest<HTMLElement>(TREE_ITEM_SELECTOR);
                break;
            default:
                return;
        }

        event.preventDefault();
        if (next != null) {
            setActiveKey(next.dataset.key ?? null);
            next.focus();
        }
    };

    const menuDetails = menu?.details;

    const closeDescription = () => setDescription((current) => current == null ? null : {
        ...current,
        open: false,
    });

    const runMenuAction = (action: (menu: MenuState) => void) => {
        if (menu?.open) {
            deferAnnouncements();
            action(menu);
        }
        closeMenu();
    };

    const historyToolbar = <Box sx={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'flex-end',
        gap: 0.5,
        flexWrap: 'wrap',
        mb: props.toolbarContainer ? 0 : 1.5,
    }}>
        {!props.toolbarContainer && <Typography variant="h6"
                                                component="div"
                                                sx={{mr: 'auto'}}>{parameter.label || 'Ausdruck'}</Typography>}
        <NoCodeEditorNotes value={root?.editorMetadata?.notes}
                           hasExpression={root != null}
                           disabled={locked || root == null}
                           onChange={(notes) => updateEditorMetadata({notes: notes?.trim() ? notes : undefined})}/>
        <Button size="small"
                startIcon={<Science/>}
                variant={testOpen ? 'outlined' : 'text'}
                aria-expanded={testOpen}
                aria-controls={testPanelId}
                onClick={() => setTestOpen(!testOpen)}>
            Testlauf
        </Button>
        {!readOnly && <>
            <Tooltip arrow
                     title="Rückgängig"><span><IconButton aria-label="Rückgängig"
                                                          disabled={locked || !history.canUndo}
                                                          onClick={() => restoreHistory(false)}><Undo/></IconButton></span></Tooltip>
            <Tooltip arrow
                     title="Wiederholen"><span><IconButton aria-label="Wiederholen"
                                                           disabled={locked || !history.canRedo}
                                                           onClick={() => restoreHistory(true)}><Redo/></IconButton></span></Tooltip>
        </>}
    </Box>;
    const removeExpression = removeTarget?.node.type === 'NoCodeExpression' ? removeTarget.node : null;
    const removeOperands = removeExpression?.operands ?? [];
    const removeOperator = removeExpression == null ? undefined : allOperators.find((op) => op.identifier === removeExpression.operatorIdentifier);
    const removeSlot = removeTarget == null ? null : findNoCodeEditorSlot(root, removeTarget.node.id);
    const removeParameters = removeOperator == null ? [] : resolveNoCodeSignature(removeOperator, {operandCount: removeOperands.length}).parameters;

    const expressionTree = <Box
        sx={{
            minWidth: 0,
            minHeight: 0,
            overflowX: 'auto',
            p: 0.5,
        }}
        ref={treeRef}
        role="tree"
        aria-label={isStringNullOrEmpty(parameter.label) ? 'No-Code-Ausdruck' : parameter.label}
        aria-describedby={helpId}
        aria-readonly={locked}
        onKeyDown={handleKeyDown}
        onFocus={(event) => {
            // Clicking a pill focuses its inner button; the surrounding item becomes the tab stop.
            const target = event.target as HTMLElement;
            const item = treeRef.current?.contains(target) ? target.closest<HTMLElement>(TREE_ITEM_SELECTOR) : null;
            if (item?.dataset.key != null) {
                setActiveKey(item.dataset.key);
            }
        }}
        onBlur={(event) => {
            if (!treeRef.current?.contains(event.relatedTarget as Node | null)) {
                cancelGrab();
            }
        }}
        onClickCapture={(event) => {
            // While moving, a click picks the drop target, so a pointer can move nodes without dragging.
            const current = grabRef.current;
            const target = event.target as HTMLElement;
            if (current == null || !treeRef.current?.contains(target)) {
                return;
            }
            event.preventDefault();
            event.stopPropagation();
            const item = target.closest<HTMLElement>(TREE_ITEM_SELECTOR);
            if (item?.dataset.key == null || item.dataset.key === current.nodeId) {
                cancelGrab();
            } else if (current.targets.includes(item.dataset.key)) {
                dropGrab(item.dataset.key);
            } else {
                announce(`„${current.name}“ kann hier nicht abgelegt werden.`);
            }
        }}
    >
        <NoCodeEditorSlotView
            node={root}
            parentId={null}
            index={0}
            parameter={parameter}
            error={validation}
            ctx={ctx}
        />
    </Box>;

    return (
        <Box aria-busy={busy || undefined}
             sx={{
                 minWidth: 0,
                 minHeight: 0,
                 display: 'flex',
                 flexDirection: 'column',
             }}>
            {props.toolbarContainer ? createPortal(historyToolbar, props.toolbarContainer) : historyToolbar}
            {
                grab != null &&
                <Box sx={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 1,
                    mb: 1,
                }}>
                    <Typography variant="caption"
                                color="text.secondary">
                        „{grab.name}“ wird verschoben. Wählen Sie ein markiertes Ziel aus.
                    </Typography>
                    <Button
                        size="small"
                        // Keeps focus on the grabbed item, so cancelling does not drop focus to the page.
                        onMouseDown={(event) => event.preventDefault()}
                        onClick={cancelGrab}
                    >
                        Abbrechen
                    </Button>
                </Box>
            }

            <Box ref={scrollRef}
                 sx={{
                     minHeight: 0,
                     minWidth: 0, ...(props.scrollContainerRef == null ? {
                         overflowY: 'auto',
                         maxHeight: 'calc(100dvh - 260px)',
                     } : {}),
                 }}>
                {expressionTree}
                <Box ref={testPanelRef}
                     id={testPanelId}
                     hidden={!testOpen}
                     sx={{
                         mt: 3,
                         pt: 3,
                         borderTop: 1,
                         borderColor: 'divider',
                     }}>
                    <NoCodeDryRunPanel embedded
                                       open={testOpen}
                                       readOnly={locked}
                                       onTestDataChange={(testData) => updateEditorMetadata({testData})}
                                       presentation={presentation}
                                       onValidate={requestValidation}
                                       canEvaluate={validation.isValid && root != null}
                                       root={root}
                                       allElements={allElements}
                                       onResults={setResults}/>
                </Box>
            </Box>
            {operandRemoval != null && <ConfirmDialog
                title="Eingabe entfernen?"
                isDestructive
                disableRestoreFocus
                confirmButtonText="Eingabe entfernen"
                onCancel={() => setOperandRemoval({...operandRemoval, open: false})}
                onConfirm={operandRemoval.open && !locked ? () => {
                    const {parentId, index, nodeId} = operandRemoval;
                    const parent = findEditableVariadicParent(parentId);
                    const focusKey = parent?.operands[index]?.id === nodeId
                        ? removeOperandAt(parent, index)
                        : parentId;
                    setOperandRemoval({...operandRemoval, focusKey, open: false});
                } : undefined}
                onExited={() => {
                    requestFocus(operandRemoval.focusKey);
                    setOperandRemoval(null);
                }}
            >
                <Typography>Die Eingabe „{operandRemoval.label}“ wird mit ihrem Inhalt aus dem Ausdruck entfernt.</Typography>
            </ConfirmDialog>}
            {removeTarget != null && <NoCodeOperatorDeleteDialog key={removeTarget.node.id}
                                                                 open={removeTarget.open && !locked}
                                                                 name={removeOperator?.label ?? 'Unbekannte Funktion'}
                                                                 hasNotes={removeTarget.hasNotes}
                                                                 childrenOptions={removeOperands.flatMap((child, index) => child == null ? [] : [{
                                                                     value: child.id,
                                                                     label: `${removeParameters[index]?.label ?? `Parameter ${index + 1}`}: ${humanizeNoCode(toNoCodeOperand(child), allElements, allOperators, presentation)}`,
                                                                     warning: getNoCodeEditorTypeConflict(child, removeSlot == null ? parameter : getSlotParameter(removeSlot), allElements, allOperators),
                                                                 }])}
                                                                 onClose={() => setRemoveTarget({
                                                                     ...removeTarget,
                                                                     open: false,
                                                                 })}
                                                                 onExited={() => {
                                                                     if (removeFocus.current) requestFocus(removeFocus.current);
                                                                     setRemoveTarget(null);
                                                                 }}
                                                                 onDelete={() => {
                                                                     if (!locked) removeFocus.current = removeImmediately(removeTarget.node.id);
                                                                     setRemoveTarget({
                                                                         ...removeTarget,
                                                                         open: false,
                                                                     });
                                                                 }}
                                                                 onKeep={(id) => {
                                                                     const child = removeOperands.find((node) => node?.id === id);
                                                                     if (!locked && child != null) {
                                                                         dispatch({
                                                                             type: 'replaceNode',
                                                                             nodeId: removeTarget.node.id,
                                                                             node: child,
                                                                         });
                                                                         removeFocus.current = removeTarget.node.id;
                                                                     }
                                                                     setRemoveTarget({
                                                                         ...removeTarget,
                                                                         open: false,
                                                                     });
                                                                 }}
            />}

            <VisuallyHidden id={helpId}>
                {locked
                    ? 'Die Pfeiltasten wechseln zwischen den Einträgen. Die Eingabetaste öffnet die Beschreibung. Der Ausdruck kann nicht bearbeitet werden.'
                    : 'Die Pfeiltasten wechseln zwischen den Einträgen. Die Eingabetaste bearbeitet, die Leertaste nimmt zum Verschieben auf, Entfernen löscht und Umschalt+F10 öffnet weitere Aktionen.'}
            </VisuallyHidden>
            <VisuallyHidden role="status">
                {announcement != null && <span key={announcement.id}>{announcement.text}</span>}
            </VisuallyHidden>

            <Menu
                open={menu?.open ?? false}
                anchorEl={menu?.anchor}
                onClose={closeMenu}
                // The tree item takes focus once the menu is gone; focusing it earlier would race the focus trap.
                disableRestoreFocus
                slotProps={{
                    transition: {
                        onExited: () => {
                            setMenu(null);
                            flushAnnouncements();
                            const after = afterMenuRef.current;
                            afterMenuRef.current = null;
                            const slot = after == null ? null : findNoCodeEditorSlot(root, after.nodeId);
                            if (after?.remove) {
                                ctx.remove(after.nodeId);
                            } else if (after?.describe != null) {
                                setDescription({
                                    ...after.describe,
                                    nodeId: after.nodeId,
                                    open: true,
                                });
                            } else if (after?.wrap && slot != null) {
                                setWrapTarget({
                                    nodeId: after.nodeId,
                                    name: after.name,
                                    returnType: getSlotParameter(slot).type,
                                });
                            } else if (after != null) {
                                requestFocus(after.nodeId);
                            }
                        },
                    },
                }}
            >
                <MenuItem
                    disabled={locked || menu?.up == null}
                    onClick={() => runMenuAction((current) => current.up != null && move(current.nodeId, current.up, current.name))}
                >
                    <ListItemIcon><ArrowUpward/></ListItemIcon>
                    <ListItemText>Nach oben verschieben</ListItemText>
                </MenuItem>
                <MenuItem
                    disabled={locked || menu?.down == null}
                    onClick={() => runMenuAction((current) => current.down != null && move(current.nodeId, current.down, current.name))}
                >
                    <ListItemIcon><ArrowDownward/></ListItemIcon>
                    <ListItemText>Nach unten verschieben</ListItemText>
                </MenuItem>
                <MenuItem disabled={locked || !menu?.canMoveElsewhere}
                          onClick={() => runMenuAction((current) => startGrab(current.nodeId, current.name))}>
                    <ListItemIcon><OpenWith/></ListItemIcon>
                    <ListItemText>An andere Stelle verschieben</ListItemText>
                </MenuItem>
                <MenuItem
                    disabled={locked}
                    onClick={() => runMenuAction((current) => {
                        afterMenuRef.current = {
                            nodeId: current.nodeId,
                            name: current.name,
                            wrap: true,
                        };
                    })}
                >
                    <ListItemIcon><AccountTree/></ListItemIcon>
                    <ListItemText>Übergeordnete Funktion hinzufügen</ListItemText>
                </MenuItem>
                {
                    menuDetails != null &&
                    <MenuItem
                        onClick={() => runMenuAction((current) => {
                            afterMenuRef.current = {
                                nodeId: current.nodeId,
                                name: current.name,
                                wrap: false,
                                describe: menuDetails,
                            };
                        })}
                    >
                        <ListItemIcon><Info/></ListItemIcon>
                        <ListItemText>Beschreibung anzeigen</ListItemText>
                    </MenuItem>
                }
                <Divider/>
                <MenuItem disabled={locked}
                          onClick={() => runMenuAction((current) => {
                              afterMenuRef.current = {
                                  nodeId: current.nodeId,
                                  name: current.name,
                                  wrap: false,
                                  remove: true,
                              };
                          })}>
                    <ListItemIcon><Delete color="error"/></ListItemIcon>
                    <ListItemText sx={{color: 'error.main'}}>Löschen</ListItemText>
                </MenuItem>
            </Menu>

            <InfoDialog
                open={description?.open ?? false}
                title={description?.title ?? ''}
                severity="info"
                closeButtonLabel="Schließen"
                onClose={closeDescription}
                // The tree item takes focus once the dialog is gone, also when it was opened from a closed tooltip.
                disableRestoreFocus
                slotProps={{
                    transition: {
                        onExited: () => {
                            if (description != null) {
                                requestFocus(description.nodeId);
                            }
                            setDescription(null);
                        },
                    },
                }}
            >
                {description?.details}
            </InfoDialog>

            <SelectOperatorDialog
                open={!locked && wrapTarget != null}
                // The node becomes the first parameter, so operators without parameters would hide it.
                operators={allOperators.filter((op) => op.signatures.some((signature) => signature.parameters.length > 0))}
                desiredReturnType={wrapTarget?.returnType ?? NoCodeDataType.Runtime}
                onSelect={(op) => {
                    if (wrapTarget == null) {
                        return;
                    }
                    const wrapperId = uuid4();
                    deferAnnouncements();
                    dispatch({
                        type: 'wrapNode',
                        nodeId: wrapTarget.nodeId,
                        wrapperId,
                        operatorIdentifier: op.identifier,
                        parameterCount: resolveNoCodeSignature(op, {operandCount: 1}).parameters.length,
                    });
                    announce(`Funktion „${op.label}“ hinzugefügt. „${wrapTarget.name}“ ist jetzt ihr erster Parameter.`);
                    focusAfterDialogRef.current = wrapperId;
                    setWrapTarget(null);
                }}
                onClose={() => {
                    focusAfterDialogRef.current = wrapTarget?.nodeId ?? null;
                    setWrapTarget(null);
                }}
                onExited={() => {
                    flushAnnouncements();
                    if (focusAfterDialogRef.current != null) {
                        requestFocus(focusAfterDialogRef.current);
                        focusAfterDialogRef.current = null;
                    }
                }}
            />
        </Box>
    );
}

interface NoCodeEditorTreeItemProps {
    itemKey: string;
    slot: NoCodeEditorSlot;
    /** Name in announcements and the action menu. Sockets have none and cannot be moved. */
    name?: string;
    /** Sentence wording is visual; the accessible name retains the full parameter label. */
    parameter: NoCodeParameter;
    /** Only the first three levels add horizontal indentation. */
    depth?: number;
    summary?: NoCodeSummary;
    lead?: string | null;
    resultType?: NoCodeDataType;
    invalid?: boolean;
    ariaLabel: string;
    /** IDs of the error and warning texts that describe the item. */
    describedBy?: (string | undefined)[];
    /** Dry run results are shown for values that are not literally written in the expression. */
    showResult?: boolean;
    ctx: EditorContext;
    onActivate: () => void;
    onRemove?: () => void;
    onRemoveOperand?: () => void;
    /** The pill or socket shown next to the parameter label. */
    children: ReactNode;
    /** Child tree items of an operator, rendered as an indented group. */
    group?: ReactNode;
    groupAction?: ReactNode;
}

function NoCodeEditorTreeItem(props: NoCodeEditorTreeItemProps) {
    const {
        itemKey,
        slot,
        name,
        parameter,
        ariaLabel,
        describedBy = [],
        showResult = false,
        ctx,
        onActivate,
        onRemove,
        onRemoveOperand,
        children,
        group,
        groupAction,
    } = props;
    const parameterLabel = parameter.label;
    const phrase = parameter.sentencePhrase ?? parameterLabel;
    const hasPhrase = parameter.sentencePhrase != null || !isStringNullOrEmpty(parameterLabel);
    const suffix = parameter.sentenceSuffix;
    const isCollapsed = ctx.collapsed.has(itemKey);
    const isOperator = props.resultType != null;
    const rowRef = useRef<HTMLDivElement>(null);
    const isNode = name != null;
    const result = showResult ? ctx.results?.get(itemKey) : undefined;
    const resultId = result == null ? undefined : `no-code-editor-result-${itemKey}`;
    const summaryId = isCollapsed && props.summary != null ? `no-code-editor-summary-${itemKey}` : undefined;
    const descriptionIds = [...describedBy, resultId, summaryId].filter((id) => id != null).join(' ');

    const {
        dropProps,
        isDropTarget,
    } = useDrop({
        ref: rowRef,
        hasDropButton: true,
        getDropOperation: (types) => types.has(DRAG_TYPE) && ctx.canDropPointerDrag(slot) ? 'move' : 'cancel',
        onDrop: () => ctx.dropPointerDrag(slot),
    });

    const openMenu = (anchor: HTMLElement) => {
        if (name != null) {
            ctx.openMenu(itemKey, name, anchor);
        }
    };

    const isGrabbed = ctx.grab?.nodeId === itemKey;
    const isGrabCandidate = ctx.grab?.targets.includes(itemKey) ?? false;
    const isGrabTarget = ctx.grab != null && ctx.grab.targets[ctx.grab.targetIndex] === itemKey;

    return (
        <Box
            role="treeitem"
            aria-expanded={group != null ? !isCollapsed : undefined}
            aria-invalid={props.invalid || undefined}
            data-key={itemKey}
            data-parent-id={slot.parentId ?? undefined}
            data-index={slot.index}
            data-socket={isNode ? undefined : ''}
            tabIndex={ctx.activeKey === itemKey ? 0 : -1}
            aria-label={isStringNullOrEmpty(parameterLabel) ? ariaLabel : `${parameterLabel}: ${ariaLabel}`}
            aria-describedby={descriptionIds || undefined}
            onKeyDown={(event) => {
                // Nested items share this handler through bubbling; only the focused item reacts. While moving,
                // the tree handles every key.
                if (event.target !== event.currentTarget || ctx.grab != null) {
                    return;
                }
                if (event.key === 'Enter') {
                    event.preventDefault();
                    onActivate();
                } else if (!ctx.readOnly && (event.key === 'Delete' || event.key === 'Backspace') && onRemove != null) {
                    event.preventDefault();
                    onRemove();
                } else if (event.key === ' ') {
                    event.preventDefault();
                    if (!ctx.readOnly && name != null) {
                        ctx.startGrab(itemKey, name);
                    }
                } else if ((event.key === 'ContextMenu' || (event.shiftKey && event.key === 'F10')) && rowRef.current != null) {
                    event.preventDefault();
                    openMenu(rowRef.current);
                }
            }}
            sx={{
                outline: 'none',
                minWidth: 0,
                ...(isOperator && {
                    border: 1,
                    borderColor: 'divider',
                    borderRadius: 1,
                }),
                '&:focus-visible > [data-tree-row]': focusRing,
                '& > [data-tree-row] [data-actions]': {opacity: 0},
                '&:focus > [data-tree-row] [data-actions], & > [data-tree-row]:hover [data-actions], & > [data-tree-row]:focus-within [data-actions]': {
                    opacity: 1,
                },
            }}
        >
            <Box
                ref={rowRef}
                data-tree-row
                {...dropProps}
                onContextMenu={(event) => {
                    if (isNode && rowRef.current != null) {
                        event.preventDefault();
                        openMenu(rowRef.current);
                    }
                }}
                sx={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 0.75,
                    flexWrap: 'wrap',
                    width: '100%',
                    p: 0.75,
                    maxWidth: '100%',
                    borderRadius: isOperator ? 1 : 0.5,
                    opacity: isGrabbed ? 0.5 : 1,
                    ...(isGrabCandidate && {
                        outline: '1px dashed',
                        outlineColor: 'primary.main',
                        outlineOffset: 2,
                    }),
                    ...((isDropTarget || isGrabTarget) && focusRing),
                }}
            >
                {group != null ? <IconButton size="small"
                                             tabIndex={-1}
                                             aria-label={`${name}: ${isCollapsed ? 'aufklappen' : 'zuklappen'}`}
                                             sx={{p: 0.25}}
                                             onClick={() => ctx.toggleCollapsed(itemKey)}>{isCollapsed ?
                        <ChevronRight/> : <KeyboardArrowDown/>}</IconButton>
                    : <Box aria-hidden
                           sx={{
                               width: 28,
                               flexShrink: 0,
                           }}/>}
                {hasPhrase && <Typography variant="body2"
                                          color="text.secondary"
                                          aria-hidden
                                          sx={{
                                              minWidth: 72,
                                              maxWidth: '100%',
                                              overflowWrap: 'anywhere',
                                          }}>{phrase}</Typography>}
                {children}
                {!isCollapsed && !isStringNullOrEmpty(props.lead) && <Typography variant="body2"
                                                                 color="text.secondary"
                                                                 aria-hidden>{props.lead}</Typography>}
                {group == null && !isStringNullOrEmpty(suffix) && <Typography variant="body2"
                                                                              color="text.secondary"
                                                                              aria-hidden>{suffix}</Typography>}
                {props.resultType != null && isStringNullOrEmpty(props.lead) && <Typography variant="caption"
                                                                                            color="text.secondary">Ergebnistyp: {NoCodeDataTypeLabels[props.resultType]}</Typography>}
                {result != null && resultId != null && <NoCodeTestResult id={resultId}
                                                                         result={result}
                                                                         compact/>}
                {onRemoveOperand != null && ctx.grab == null && <Tooltip title="Eingabe entfernen">
                    <IconButton
                        data-no-code-tree-action
                        size="small"
                        color="error"
                        aria-label={`Eingabe „${parameterLabel}“ entfernen`}
                        onClick={onRemoveOperand}
                    >
                        <Remove fontSize="small"/>
                    </IconButton>
                </Tooltip>}
                {
                    isNode &&
                    <IconButton
                        data-actions
                        size="small"
                        tabIndex={-1}
                        aria-label={`Aktionen für „${name}“`}
                        aria-haspopup="menu"
                        onClick={(event) => openMenu(event.currentTarget)}
                        sx={{
                            p: 0.25,
                            mt: 0.25,
                            ml: isCollapsed ? 'auto' : undefined,
                            color: 'text.secondary',
                        }}
                    >
                        <MoreVert fontSize="small"/>
                    </IconButton>
                }
            </Box>

            {summaryId != null && props.summary != null &&
                <Box data-no-code-summary sx={{px: 1, pb: 0.75, minWidth: 0}}>
                    <NoCodeExpressionSummary id={summaryId} summary={props.summary}/>
                </Box>}

            {
                group != null && !isCollapsed &&
                <Box
                    role="group"
                    sx={{
                        display: 'flex',
                        flexDirection: 'column',
                        gap: 0.5,
                        mt: 0.25,
                        ml: (props.depth ?? 0) < 3 ? 1.5 : 0,
                        pl: (props.depth ?? 0) < 3 ? 0.75 : 0,
                        pb: 0.75,
                        pr: 0.75,
                        borderColor: 'divider',
                    }}
                >
                    {group}
                </Box>
            }

            {!isCollapsed && groupAction != null && <Box sx={{px: 1, pb: 0.75}}>{groupAction}</Box>}

            {group != null && !isStringNullOrEmpty(suffix) && <Typography variant="body2"
                                                                          color="text.secondary"
                                                                          aria-hidden
                                                                          sx={{
                                                                              px: 1,
                                                                              pb: 0.75,
                                                                          }}>{suffix}</Typography>}

        </Box>
    );
}

function NoCodeEditorSlotView(props: SlotProps) {
    const {
        node,
        ctx,
    } = props;
    const key = node?.id ?? socketKey(props.parentId, props.index);
    const visibleError = ctx.validated.has(key) || ctx.touched.has(key) ? props.error : undefined;
    const viewProps = {
        ...props,
        error: visibleError,
    };

    if (node == null) {
        return <NoCodeEditorSocketView {...viewProps}/>;
    }

    if (node.type === 'NoCodeExpression') {
        return <NoCodeEditorExpressionView {...viewProps} childErrors={props.error?.subErrors}
                                           node={node}/>;
    }

    return <NoCodeEditorLeafView {...viewProps} node={node}/>;
}

function NoCodeEditorSocketView(props: SlotProps) {
    const {
        parentId,
        index,
        parameter,
        ctx,
        error,
    } = props;
    const hintId = useNormalizedReactId();
    const errorId = error?.error ? `${hintId}-error` : undefined;
    const socketRef = useRef<HTMLButtonElement>(null);
    const [anchor, setAnchor] = useState<HTMLElement | null>(null);
    useEffect(() => {
        if (ctx.readOnly) setAnchor(null);
    }, [ctx.readOnly]);
    const Icon = NoCodeDataTypeIcons[parameter.type];
    const name = isStringNullOrEmpty(parameter.label) ? 'Ausdruck' : parameter.label;
    const selectionLabel = parameter.options.length > 0 ? `${name} auswählen` : 'Wert festlegen';
    const openSelector = () => {
        if (!ctx.readOnly) setAnchor(socketRef.current);
    };
    const picker = <Popover
        open={!ctx.readOnly && anchor != null}
        anchorEl={anchor}
        onClose={() => setAnchor(null)}
        anchorOrigin={{
            vertical: 'bottom',
            horizontal: 'left',
        }}
        slotProps={{
            paper: {
                sx: {
                    p: 0.5,
                    width: 448,
                    maxWidth: 'calc(100vw - 32px)',
                    maxHeight: 'calc(100dvh - 64px)',
                },
            },
        }}
    >
        <NoCodeOperandEditorSelector allOperators={ctx.allOperators}
                                     label={parentId == null && parameter.options.length === 0 ? 'Ausdruck hinzufügen' : name}
                                     hint={parameter.description}
                                     desiredType={parameter.type}
                                     parameterOptions={parameter.options}
                                     contextType={ctx.contextType}
                                     onChange={(operand, editAfterInsert) => {
                                         setAnchor(null);
                                         ctx.insert(parentId, index, operand, editAfterInsert);
                                     }}/>
    </Popover>;

    if (parentId == null) {
        const key = socketKey(parentId, index);
        return <Box role="treeitem"
                    data-key={key}
                    data-index={index}
                    data-socket=""
                    tabIndex={ctx.activeKey === key ? 0 : -1}
                    aria-invalid={errorId != null || undefined}
                    aria-label={`${name}: leer, ${NoCodeDataTypeLabels[parameter.type]}`}
                    aria-describedby={[!ctx.readOnly ? hintId : undefined, errorId].filter(Boolean).join(' ') || undefined}
                    onKeyDown={(event) => {
                        if (event.target === event.currentTarget && (event.key === 'Enter' || event.key === ' ')) {
                            event.preventDefault();
                            openSelector();
                        }
                    }}
                    sx={{
                        p: 2,
                        border: 1,
                        borderColor: 'divider',
                        borderRadius: 1,
                        outline: 'none',
                        '&:focus-visible': focusRing,
                    }}>
            <Typography variant="h6"
                        component="div">Noch kein Ausdruck festgelegt</Typography>
            {!ctx.readOnly && <Typography id={hintId}
                                          variant="body2"
                                          color="text.secondary"
                                          sx={{
                                              mt: 0.5,
                                              mb: 1.5,
                                              maxWidth: 680,
                                          }}>
                {ctx.emptyDescription ?? 'Legen Sie einen Wert selbst fest, ermitteln Sie ihn mit einer Funktion oder übernehmen Sie ihn aus einer verfügbaren Datenquelle.'}
            </Typography>}
            {!ctx.readOnly && <Button ref={socketRef}
                                      tabIndex={-1}
                                      variant="outlined"
                                      startIcon={<Add/>}
                                      onClick={openSelector}>
                Ausdruck hinzufügen
            </Button>}
            {errorId != null && <Typography id={errorId}
                                            variant="caption"
                                            color="error"
                                            sx={{
                                                display: 'block',
                                                mt: 1,
                                            }}>{error?.error}</Typography>}
            {picker}
        </Box>;
    }

    return <NoCodeEditorTreeItem depth={props.depth}
                                 invalid={errorId != null}
                                 itemKey={socketKey(parentId, index)}
                                 slot={{
                                     parentId,
                                     index,
                                 }}
                                 parameter={parameter}
                                 ariaLabel={`leer, ${NoCodeDataTypeLabels[parameter.type]}`}
                                 describedBy={[errorId]}
                                 ctx={ctx}
                                 onRemoveOperand={props.onRemoveOperand}
                                 onActivate={openSelector}>
        <PillHint parameter={parameter}>
            <ButtonBase ref={socketRef}
                        disabled={ctx.readOnly}
                        tabIndex={-1}
                        aria-label={parameter.options.length > 0 ? selectionLabel : `${name} festlegen`}
                        onClick={openSelector}
                        sx={{
                            height: 28,
                            px: 1,
                            gap: 0.5,
                            borderRadius: 14,
                            border: '1px dashed',
                            borderColor: 'text.disabled',
                            color: 'text.secondary',
                            typography: 'body2',
                        }}>
                <Icon fontSize="small"
                      aria-hidden/>
                {selectionLabel}
            </ButtonBase>
        </PillHint>
        {errorId != null && <Typography id={errorId}
                                        variant="caption"
                                        color="error">{error?.error}</Typography>}
        {picker}
    </NoCodeEditorTreeItem>;
}

function NoCodeEditorExpressionView(props: SlotProps & {
    node: NoCodeEditorExpressionNode;
    childErrors?: NoCodeOperandError[] | null
}) {
    const {
        node,
        parentId,
        index,
        parameter: slotParameter,
        error,
        ctx,
    } = props;
    const [showOperatorPicker, setShowOperatorPicker] = useState(false);
    const [pendingOperator, setPendingOperator] = useState<NoCodeOperatorDetailsDTO | null>(null);
    const pendingOperatorRef = useRef<NoCodeOperatorDetailsDTO | null>(null);

    useEffect(() => {
        if (ctx.readOnly) {
            setShowOperatorPicker(false);
            setPendingOperator(null);
            pendingOperatorRef.current = null;
        }
    }, [ctx.readOnly]);

    const changeOperator = (op: NoCodeOperatorDetailsDTO, count: number) => {
        ctx.dispatch({
            type: 'replaceNode',
            nodeId: node.id,
            node: {
                ...node,
                operatorIdentifier: op.identifier,
                operands: Array.from({length: count}, (_, i) => node.operands[i] ?? null),
            },
        });
        setPendingOperator(null);
    };

    const operator = ctx.allOperators.find((op) => op.identifier === node.operatorIdentifier);
    const variadicInputKind = operator == null ? undefined : getNoCodeVariadicInputKind(operator);
    const canResize = variadicInputKind != null && !ctx.readOnly;
    const signature = operator == null ? undefined : resolveNoCodeSignature(operator, {
        operandCount: node.operands.length,
    });
    const operands = useMemo(() => node.operands.map(toNoCodeOperand), [node.operands]);
    // Malformed or older expressions remain fully visible even when their operator no longer accepts these operands.
    const {parameters, useSentence} = resolveNoCodeExpressionPresentation(
        Array.from({length: Math.max(signature?.parameters.length ?? 0, node.operands.length)}, (_, i) =>
            signature?.parameters[i] ?? {
                type: NoCodeDataType.Runtime,
                label: `Parameter ${i + 1}`,
                description: null,
                options: [],
            }), operands);
    const suggestions = useNoCodeParameterSuggestions(operands, parameters, ctx.allElements);
    const parameterCountError = operator != null && findMatchingNoCodeSignature(operator, node.operands.length) == null
        ? 'Die Anzahl der Parameter passt nicht zur Funktion. Wählen Sie die Funktion erneut aus, um sie anzupassen.'
        : null;
    const name = operator?.label ?? (
        isStringNullOrEmpty(node.operatorIdentifier)
            ? 'Keine Funktion ausgewählt'
            : 'Unbekannte Funktion'
    );
    const dataType = signature?.returnType ?? NoCodeDataType.Runtime;
    const errorText = error?.error ?? (operator == null && node.operatorIdentifier ? 'Die ausgewählte Funktion ist nicht verfügbar.' : parameterCountError);
    const errorId = errorText != null ? `no-code-editor-error-${node.id}` : undefined;
    const warning = getNoCodeEditorTypeConflict(node, slotParameter, ctx.allElements, ctx.allOperators);
    const warningId = warning != null ? `no-code-editor-warning-${node.id}` : undefined;

    return (
        <NoCodeEditorTreeItem
            itemKey={node.id}
            slot={{
                parentId,
                index,
            }}
            name={name}
            parameter={slotParameter}
            depth={props.depth}
            resultType={dataType}
            lead={useSentence && variadicInputKind == null ? operator?.sentenceLead : null}
            summary={ctx.collapsed.has(node.id)
                ? summarizeNoCode(toNoCodeOperand(node), ctx.allElements, ctx.allOperators, ctx.presentation)
                : undefined}
            invalid={errorId != null}
            ariaLabel={`Funktion ${name}, ${NoCodeDataTypeLabels[dataType]}`}
            describedBy={[errorId, warningId]}
            showResult
            ctx={ctx}
            onActivate={() => {
                if (ctx.readOnly) {
                    const details = getNodeDetails(node, ctx.allOperators);
                    if (details != null) ctx.showDescription(node.id, details);
                } else setShowOperatorPicker(true);
            }}
            onRemove={() => ctx.remove(node.id)}
            onRemoveOperand={props.onRemoveOperand}
            group={parameters.length > 0 ? parameters.map((parameter, operandIndex) => (
                <NoCodeEditorSlotView
                    key={node.operands[operandIndex]?.id ?? socketKey(node.id, operandIndex)}
                    node={node.operands[operandIndex] ?? null}
                    parentId={node.id}
                    index={operandIndex}
                    parameter={variadicInputKind == null ? parameter : {...parameter, sentencePhrase: null}}
                    suggestions={suggestions[operandIndex]}
                    depth={(props.depth ?? 0) + 1}
                    error={props.childErrors?.[operandIndex]}
                    onRemoveOperand={canResize && operandIndex >= MIN_VARIADIC_OPERANDS
                        ? () => ctx.removeOperand(node.id, operandIndex, parameter.label)
                        : undefined}
                    ctx={ctx}
                />
            )) : undefined}
            groupAction={canResize && ctx.grab == null ? <Button
                data-no-code-tree-action
                size="small"
                startIcon={<Add/>}
                onClick={() => ctx.appendOperand(node.id)}
            >
                {variadicInputKind === 'text' ? 'Weiteren Wert hinzufügen' : 'Weitere Zahl hinzufügen'}
            </Button> : undefined}
        >
            <PillHint
                summary={operator?.abstractDescription}
                parameter={slotParameter}
                onMore={operator == null ? undefined : () => ctx.showDescription(node.id, getNodeDetails(node, ctx.allOperators)!)}
            >
                <NoCodePill
                    drag={ctx.readOnly ? undefined : {
                        getItems: () => [{
                            [DRAG_TYPE]: node.id,
                            'text/plain': name,
                        }],
                        onDragStart: () => ctx.startPointerDrag(node.id, name),
                        onDragEnd: ctx.endPointerDrag,
                    }}
                    dataType={dataType}
                    label={name}
                    dismissLabel={`Funktion „${name}“ löschen`}
                    error={errorText}
                    errorId={errorId}
                    warning={warning}
                    warningId={warningId}
                    tabIndex={-1}
                    onClick={() => {
                        if (ctx.readOnly) {
                            const details = getNodeDetails(node, ctx.allOperators);
                            if (details != null) ctx.showDescription(node.id, details);
                        } else setShowOperatorPicker(true);
                    }}
                    onDismiss={ctx.readOnly ? undefined : () => ctx.remove(node.id)}
                />
            </PillHint>

            <SelectOperatorDialog
                open={!ctx.readOnly && showOperatorPicker}
                mode="replace"
                operators={ctx.allOperators.filter((op) => op.signatures.length > 0)}
                onSelect={(op) => {
                    setShowOperatorPicker(false);
                    const counts = [...new Set(op.signatures.map((candidate) => candidate.parameters.length))];
                    if (counts.length === 1 && op.variableArgumentCount && node.operands.length > counts[0]) {
                        changeOperator(op, node.operands.length);
                    } else if (counts.length === 1 && !node.operands.slice(counts[0]).some((child) => child != null)) {
                        changeOperator(op, counts[0]);
                    } else if (counts.length > 0) {
                        pendingOperatorRef.current = op;
                    }
                }}
                onClose={() => setShowOperatorPicker(false)}
                onExited={() => {
                    // Wait for the picker's focus trap to release before opening the confirmation.
                    setPendingOperator(pendingOperatorRef.current);
                    pendingOperatorRef.current = null;
                }}
                desiredReturnType={slotParameter.type}
            />
            {pendingOperator != null && !ctx.readOnly && <NoCodeOperatorChangeDialog
                key={pendingOperator.identifier}
                operator={pendingOperator}
                node={node}
                onClose={() => setPendingOperator(null)}
                onApply={(count) => changeOperator(pendingOperator, count)}
            />}
        </NoCodeEditorTreeItem>
    );
}

function NoCodeEditorLeafView(props: SlotProps & { node: NoCodeEditorLeafNode }) {
    const {
        node,
        parentId,
        index,
        parameter,
        error,
        ctx,
    } = props;
    const pillRef = useRef<HTMLButtonElement>(null);
    const [anchor, setAnchor] = useState<HTMLElement | null>(null);

    useEffect(() => {
        if (!ctx.readOnly && ctx.autoEditId === node.id) {
            setAnchor(pillRef.current);
        }
        if (ctx.readOnly) setAnchor(null);
    }, [ctx.autoEditId, node.id, ctx.readOnly]);

    const {
        label,
        dataType,
        dismissLabel,
        summary,
    } = describeLeaf(node, parameter, ctx.allElements, props.suggestions, ctx.presentation);
    const errorId = error?.error != null ? `no-code-editor-error-${node.id}` : undefined;
    // Explicit literal constraints reflect the operator's parser, which may accept
    // number formats that differ from the generic runtime cast checked by the warning.
    const warning = node.type === 'NoCodeStaticValue' && parameter.numberConstraints != null
        ? null
        : getNoCodeEditorTypeConflict(node, parameter, ctx.allElements, ctx.allOperators);
    const warningId = warning != null ? `no-code-editor-warning-${node.id}` : undefined;

    return (
        <NoCodeEditorTreeItem
            itemKey={node.id}
            slot={{
                parentId,
                index,
            }}
            name={label}
            parameter={parameter}
            ariaLabel={`${label}, ${NoCodeDataTypeLabels[dataType]}`}
            describedBy={[errorId, warningId]}
            depth={props.depth}
            invalid={errorId != null}
            showResult={node.type !== 'NoCodeStaticValue' || ctx.results?.get(node.id)?.status === NoCodeDryRunStatus.Skipped}
            ctx={ctx}
            onActivate={() => ctx.readOnly
                ? ctx.showDescription(node.id, getNodeDetails(node, ctx.allOperators)!)
                : setAnchor(pillRef.current)}
            onRemove={() => ctx.remove(node.id)}
            onRemoveOperand={props.onRemoveOperand}
        >
            <PillHint
                summary={summary}
                parameter={parameter}
                onMore={() => ctx.showDescription(node.id, getNodeDetails(node, ctx.allOperators)!)}
            >
                <NoCodePill
                    drag={ctx.readOnly ? undefined : {
                        getItems: () => [{
                            [DRAG_TYPE]: node.id,
                            'text/plain': label,
                        }],
                        onDragStart: () => ctx.startPointerDrag(node.id, label),
                        onDragEnd: ctx.endPointerDrag,
                    }}
                    ref={pillRef}
                    dataType={dataType}
                    label={label}
                    dismissLabel={dismissLabel}
                    preserveWhitespace={node.type === 'NoCodeStaticValue' && parameter.preserveWhitespace}
                    error={error?.error}
                    errorId={errorId}
                    warning={warning}
                    warningId={warningId}
                    tabIndex={-1}
                    onClick={(event) => ctx.readOnly
                        ? ctx.showDescription(node.id, getNodeDetails(node, ctx.allOperators)!)
                        : setAnchor(event.currentTarget)}
                    onDismiss={ctx.readOnly ? undefined : () => ctx.remove(node.id)}
                />
            </PillHint>

            {/* Share value controls with the existing operand editor. */}
            <Popover
                open={!ctx.readOnly && anchor != null}
                anchorEl={anchor}
                onClose={() => {
                    ctx.touch(node.id);
                    setAnchor(null);
                }}
                anchorOrigin={{
                    vertical: 'bottom',
                    horizontal: 'left',
                }}
                slotProps={{
                    paper: {
                        sx: {
                            p: 2,
                            width: 480,
                            maxWidth: '100%',
                        },
                    },
                }}
            >
                <Box onBlur={(event) => {
                    if (event.target instanceof HTMLElement && event.target.matches('input, textarea, [role="combobox"]')) ctx.touch(node.id);
                }}>
                    <NoCodeOperandEditor
                        parameter={parameter}
                        suggestions={props.suggestions}
                        operand={toNoCodeOperand(node)}
                        onChange={(operand) => {
                            const replacement = toNoCodeEditorNode(operand);
                            if (replacement == null) {
                                ctx.remove(node.id);
                            } else {
                                ctx.dispatch({
                                    type: 'replaceNode',
                                    nodeId: node.id,
                                    node: replacement,
                                });
                            }
                        }}
                        allOperators={ctx.allOperators}
                        allElements={ctx.allElements}
                        contextType={ctx.contextType}
                        operandError={error ?? undefined}
                    />
                </Box>
            </Popover>
        </NoCodeEditorTreeItem>
    );
}

function describeLeaf(
    node: NoCodeEditorLeafNode,
    parameter: NoCodeParameter,
    allElements: ElementWithParents[],
    suggestions: NoCodeParameterOption[] = [],
    presentation: NoCodePresentationContext = {},
): { label: string; dataType: NoCodeDataType; dismissLabel: string; summary: string } {
    switch (node.type) {
        case 'NoCodeStaticValue': {
            const options = parameter.options.length > 0 ? parameter.options
                : suggestions.length > 0 ? suggestions
                    : parameter.type === NoCodeDataType.Boolean ? BOOL_DEFAULT_OPTIONS : [];
            const whitespaceText = parameter.preserveWhitespace && typeof node.value === 'string'
                && !options.some(option => option.value === node.value)
                ? formatNoCodeWhitespaceText(node.value)
                : undefined;
            const label = whitespaceText ?? formatNoCodeValue(node.value, parameter.type, options, true);
            const empty = node.value == null || (parameter.preserveWhitespace
                ? node.value === ''
                : typeof node.value === 'string' && isStringNullOrEmpty(node.value));
            return {
                label,
                dataType: parameter.type,
                dismissLabel: whitespaceText == null ? `Wert „${label}“ löschen` : `Wert löschen (${whitespaceText})`,
                summary: whitespaceText != null ? `Ergibt ${whitespaceText}.`
                    : empty ? 'Ergibt einen leeren Wert.' : `Ergibt den festgelegten Wert „${label}“.`,
            };
        }
        case 'NoCodeReference': {
            const element = allElements.find((entry) => entry.element.id === node.elementId)?.element;
            const label = element != null
                ? generateComponentTitle(element)
                : isStringNullOrEmpty(node.elementId) ? 'Kein Formularelement ausgewählt' : 'Unbekanntes Formularelement';
            return {
                label,
                dataType: element != null ? NoCodeDataTypeMap[element.type] : NoCodeDataType.Runtime,
                dismissLabel: `Verweis auf Formularelement „${label}“ löschen`,
                summary: element != null
                    ? `Ergibt den aktuellen Wert des Formularelements „${label}“.`
                    : isStringNullOrEmpty(node.elementId)
                        ? 'Es ist noch kein Formularelement ausgewählt.'
                        : 'Das ausgewählte Formularelement ist nicht mehr vorhanden.',
            };
        }
        default: {
            const label = humanizeNoCode(node, allElements, [], presentation);
            return {
                label,
                dataType: NoCodeDataType.Runtime,
                dismissLabel: `${label} löschen`,
                summary: summarizeDataReference(node),
            };
        }
    }
}

function summarizeDataReference(
    node: Exclude<NoCodeEditorLeafNode, { type: 'NoCodeStaticValue' | 'NoCodeReference' }>,
): string {
    const path = isStringNullOrEmpty(node.path) ? null : node.path;
    switch (node.type) {
        case 'NoCodeProcessDataReference':
            return path == null ? 'Ergibt die gesamten Vorgangsdaten.' : `Ergibt den Wert unter „${path}“ aus den Vorgangsdaten.`;
        case 'NoCodeInstanceDataReference':
            return path == null
                ? 'Ergibt die gesamten geschützten Vorgangsdaten.'
                : `Ergibt den Wert unter „${path}“ aus den geschützten Vorgangsdaten.`;
        case 'NoCodeNodeDataReference':
            if (isStringNullOrEmpty(node.nodeDataKey)) {
                return 'Es ist noch kein Datenschlüssel angegeben.';
            }
            return path == null
                ? `Ergibt die Elementdaten mit dem Schlüssel „${node.nodeDataKey}“.`
                : `Ergibt den Wert unter „${path}“ aus den Elementdaten mit dem Schlüssel „${node.nodeDataKey}“.`;
    }
}

/** The full description of an operator, or of the kind of a value. Null for operators that cannot be resolved. */
function getNodeDetails(node: NoCodeEditorNode, allOperators: NoCodeOperatorDetailsDTO[]): NodeDetails | null {
    if (node.type !== 'NoCodeExpression') {
        return NoCodeOperandKindHelp[node.type];
    }
    const operator = allOperators.find((op) => op.identifier === node.operatorIdentifier);
    return operator == null ? null : {
        title: operator.label,
        details: <MarkdownContent markdown={operator.description}
                                  headingBaseLevel={3}/>,
    };
}

interface PillHintProps {
    /** What the pill evaluates to. */
    summary?: string | null;
    /** The parameter the pill fills; its description says what belongs there. */
    parameter: NoCodeParameter;
    onMore?: () => void;
    children: ReactElement;
}

/**
 * Explains a pill on hover. Keyboard users reach the full description through the action menu, because the tree
 * items keep focus off the pills.
 */
function PillHint({
                      summary,
                      parameter,
                      onMore,
                      children,
                  }: PillHintProps) {
    const hasSummary = !isStringNullOrEmpty(summary);
    const hasParameterHint = !isStringNullOrEmpty(parameter.description);
    if (!hasSummary && !hasParameterHint) {
        return children;
    }

    return (
        <HintTooltip
            arrow
            placement="left"
            title={
                <>
                    {hasSummary && <Box>{summary}</Box>}
                    {
                        hasParameterHint &&
                        <Typography
                            variant="caption"
                            color="inherit"
                            component="p"
                            sx={{
                                mt: hasSummary ? 1 : 0,
                                opacity: 0.85,
                            }}
                        >
                            {isStringNullOrEmpty(parameter.label) ? parameter.description : `${parameter.label}: ${parameter.description}`}
                        </Typography>
                    }
                    {
                        onMore != null &&
                        <Link
                            component="button"
                            color="inherit"
                            onClick={onMore}
                        >
                            Mehr...
                        </Link>
                    }
                </>
            }
        >
            <Box component="span"
                 sx={{
                     display: 'inline-flex',
                     maxWidth: '100%',
                 }}>
                {children}
            </Box>
        </HintTooltip>
    );
}
