import {type KeyboardEvent, type ReactElement, type ReactNode, useEffect, useLayoutEffect, useRef, useState} from 'react';
import {Box, Button, ButtonBase, Dialog, DialogContent, IconButton, Link, Menu, MenuItem, Popover, type SxProps, type Theme, Tooltip, Typography} from '@mui/material';
import MoreVert from '@aivot/mui-material-symbols-400-n25-outlined/MoreVert';
import {useDrag, useDrop, VisuallyHidden} from 'react-aria-components';
import {v4 as uuid4} from 'uuid';
import {
    NoCodeOperand,
    NoCodeOperandError,
} from '../../../models/functions/no-code-expression';
import {
    NoCodeOperatorDetailsDTO,
    NoCodeParameter,
    resolveNoCodeSignature,
} from '../../../models/dtos/no-code-operator-details-dto';
import {NoCodeDataType, NoCodeDataTypeLabels} from '../../../data/no-code-data-type';
import {ElementWithParents} from '../../../utils/flatten-elements';
import {generateComponentTitle} from '../../../utils/generate-component-title';
import {isStringNullOrEmpty} from '../../../utils/string-utils';
import {useNormalizedReactId} from '../../../hooks/use-normalized-react-id';
import {SelectOperatorDialog} from '../../../dialogs/select-operator-dialog/select-operator-dialog';
import {HintTooltip} from '../../../components/hint-tooltip/hint-tooltip';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {MarkdownContent} from '../../../components/markdown-content/markdown-content';
import {NoCodeDryRunResultDTO, NoCodeDryRunStatus} from '../../../models/dtos/no-code-dry-run-dto';
import {NoCodeDataTypeMap} from '../data/no-code-data-type-map';
import {humanizeNoCode} from '../utils/humanize-no-code';
import {NoCodeOperandKindHelp} from '../data/no-code-operand-kind-help';
import {usePillNoCodeStore} from '../hooks/use-pill-no-code-store';
import {
    findPillNoCodeNode,
    findPillNoCodeSlot,
    PillNoCodeAction,
    PillNoCodeExpressionNode,
    PillNoCodeLeafNode,
    PillNoCodeNode,
    pillNoCodeReducer,
    PillNoCodeSlot,
    toNoCodeOperand,
    toPillNoCodeNode,
} from '../utils/pill-no-code-store';
import {getPillNoCodeTypeConflict} from '../utils/pill-no-code-types';
import {NoCodeDataTypeIcons, NoCodePill} from './no-code-pill';
import {PillNoCodeDryRunPanel} from './pill-no-code-dry-run-panel';
import {NoCodeOperandEditor, NoCodeOperandEditorContextType} from './no-code-operand-editor';
import {NoCodeOperandEditorSelector} from './no-code-operand-editor-selector';
import {BOOL_DEFAULT_OPTIONS} from './no-code-operand-editor-static-value';

interface PillNoCodeEditorProps {
    parameter: NoCodeParameter;
    operand: NoCodeOperand | undefined | null;
    onChange: (operand: NoCodeOperand | undefined | null) => void;
    allOperators: NoCodeOperatorDetailsDTO[];
    allElements: ElementWithParents[];
    contextType?: NoCodeOperandEditorContextType;
    operandError?: NoCodeOperandError;
}

/** A node picked up for moving, with the keys of the tree items it can be dropped on in DOM order. */
interface GrabState {
    nodeId: string;
    name: string;
    targets: string[];
    targetIndex: number;
}

interface MenuState {
    nodeId: string;
    name: string;
    anchor: HTMLElement;
    up: PillNoCodeSlot | null;
    down: PillNoCodeSlot | null;
}

interface EditorContext {
    allOperators: NoCodeOperatorDetailsDTO[];
    allElements: ElementWithParents[];
    contextType: NoCodeOperandEditorContextType;
    dispatch: (action: PillNoCodeAction) => void;
    insert: (parentId: string | null, index: number, operand: NoCodeOperand) => void;
    /** Removes a node and moves focus to the socket it leaves behind. */
    remove: (nodeId: string) => void;
    /** A leaf inserted into an empty slot opens its value editor right away. */
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
    canDropPointerDrag: (slot: PillNoCodeSlot) => boolean;
    dropPointerDrag: (slot: PillNoCodeSlot) => void;
    /** Dry run results by node ID while the dry run panel is open. */
    results: Map<string, NoCodeDryRunResultDTO> | null;
}

/** The full description of a node, shown in a dialog. */
interface NodeDetails {
    title: string;
    details: ReactNode;
}

interface SlotProps {
    node: PillNoCodeNode | null;
    parentId: string | null;
    index: number;
    parameter: NoCodeParameter;
    error: NoCodeOperandError | null | undefined;
    ctx: EditorContext;
}

const TREE_ITEM_SELECTOR = '[role="treeitem"]';
const DRAG_TYPE = 'application/x-prosuna-no-code-node';

const focusRing = {outline: '2px solid', outlineColor: 'primary.main', outlineOffset: 2};

function socketKey(parentId: string | null, index: number): string {
    return `socket:${parentId ?? 'root'}:${index}`;
}

function slotOf(item: HTMLElement): PillNoCodeSlot {
    return {parentId: item.dataset.parentId ?? null, index: Number(item.dataset.index)};
}

function getTreeItems(tree: HTMLElement | null): HTMLElement[] {
    return tree == null ? [] : Array.from(tree.querySelectorAll<HTMLElement>(TREE_ITEM_SELECTOR));
}

function describeTarget(item: HTMLElement): string {
    const swap = item.dataset.socket == null ? ', die Werte werden getauscht' : '';
    return `Ziel: ${item.getAttribute('aria-label')}${swap}.`;
}

/**
 * Token-based no-code editor, enabled by the `pill-no-code` local storage flag.
 * Shares its props with NoCodeOperandEditor so both can be swapped in place.
 *
 * Keyboard moving is handled here instead of by react-aria, whose Enter/Tab scheme would collide with the tree
 * navigation; react-aria provides the pointer drag and drop.
 */
export function PillNoCodeEditor(props: PillNoCodeEditorProps) {
    const {
        parameter,
        operand,
        onChange,
        allOperators,
        allElements,
        contextType = 'BOTH',
        operandError,
    } = props;

    const [root, dispatch] = usePillNoCodeStore(operand, onChange);
    const [autoEditId, setAutoEditId] = useState<string | null>(null);
    const [activeKey, setActiveKey] = useState<string | null>(null);
    const [focusRequest, setFocusRequest] = useState<{key: string} | null>(null);
    // A new id per message, so repeating the same text is announced again.
    const [announcement, setAnnouncement] = useState<{text: string; id: number} | null>(null);
    const [grab, setGrab] = useState<GrabState | null>(null);
    const [menu, setMenu] = useState<MenuState | null>(null);
    const [pointerDrag, setPointerDrag] = useState<{nodeId: string; name: string} | null>(null);
    const [results, setResults] = useState<Map<string, NoCodeDryRunResultDTO> | null>(null);
    const [wrapTarget, setWrapTarget] = useState<{nodeId: string; name: string; returnType: NoCodeDataType} | null>(null);
    // Kept until the dialog has closed, so its content stays during the transition and focus can return.
    const [description, setDescription] = useState<NodeDetails & {nodeId: string; open: boolean} | null>(null);
    const treeRef = useRef<HTMLDivElement>(null);
    // Blur and Tab handlers can run before the next render, so they read the grab state from here.
    const grabRef = useRef<GrabState | null>(null);
    // What happens once the menu or the operator dialog has finished closing; focusing earlier races their focus trap.
    const afterMenuRef = useRef<{nodeId: string; name: string; wrap: boolean; describe?: NodeDetails} | null>(null);
    const focusAfterDialogRef = useRef<string | null>(null);
    const helpId = `${useNormalizedReactId()}-help`;

    const requestFocus = (key: string) => {
        setActiveKey(key);
        setFocusRequest({key});
    };
    // A menu or dialog hides the rest of the page from screen readers until it has closed, live region included.
    // Messages caused from inside them wait until then, otherwise they would not be read.
    const deferredAnnouncementRef = useRef<{text: string | null} | null>(null);
    const showAnnouncement = (text: string) => setAnnouncement((current) => ({text, id: (current?.id ?? 0) + 1}));
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

    const slotParameterType = (slot: PillNoCodeSlot): NoCodeDataType => {
        if (slot.parentId == null) {
            return parameter.type;
        }
        const parent = findPillNoCodeNode(root, slot.parentId);
        const operator = parent?.type === 'NoCodeExpression'
            ? allOperators.find((op) => op.identifier === parent.operatorIdentifier)
            : undefined;
        if (parent?.type !== 'NoCodeExpression' || operator == null) {
            return NoCodeDataType.Runtime;
        }
        return resolveNoCodeSignature(operator, {operandCount: parent.operands.length || undefined})
            .parameters[slot.index]?.type ?? NoCodeDataType.Runtime;
    };

    const fitsSlot = (node: PillNoCodeNode | null, slot: PillNoCodeSlot) => (
        node == null || getPillNoCodeTypeConflict(node, slotParameterType(slot), allElements, allOperators) == null
    );

    // Incompatible types are rejected like structurally impossible moves, for every way of moving.
    const canMove = (nodeId: string, slot: PillNoCodeSlot) => {
        if (pillNoCodeReducer(root, {type: 'moveNode', nodeId, ...slot}) === root) {
            return false;
        }
        const source = findPillNoCodeSlot(root, nodeId);
        const parent = slot.parentId == null ? null : findPillNoCodeNode(root, slot.parentId);
        const occupant = slot.parentId == null
            ? root
            : parent?.type === 'NoCodeExpression' ? parent.operands[slot.index] ?? null : null;
        // A swap moves the occupant into the source slot, so it has to fit there as well.
        return fitsSlot(findPillNoCodeNode(root, nodeId), slot) && (source == null || fitsSlot(occupant, source));
    };

    const move = (nodeId: string, slot: PillNoCodeSlot, name: string) => {
        dispatch({type: 'moveNode', nodeId, ...slot});
        announce(`„${name}“ wurde verschoben.`);
    };

    const findItem = (key: string) => getTreeItems(treeRef.current).find((item) => item.dataset.key === key);

    const startGrab = (nodeId: string, name: string) => {
        const items = getTreeItems(treeRef.current);
        const grabbed = items.find((item) => item.dataset.key === nodeId);
        const targets = items.filter((item) => item !== grabbed && canMove(nodeId, slotOf(item)));
        if (grabbed == null || targets.length === 0) {
            announce(`Für „${name}“ ist kein Ziel zum Verschieben verfügbar.`);
            return;
        }

        // Start at the first target after the grabbed item, which is usually where the user wants to go.
        const following = targets.findIndex((item) => grabbed.compareDocumentPosition(item) & Node.DOCUMENT_POSITION_FOLLOWING);
        const targetIndex = Math.max(following, 0);
        updateGrab({nodeId, name, targets: targets.map((item) => item.dataset.key!), targetIndex});
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
        updateGrab({...current, targetIndex: clamped});
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

    const ctx: EditorContext = {
        allOperators,
        allElements,
        contextType,
        dispatch,
        autoEditId,
        activeKey,
        grab,
        startGrab,
        results,
        showDescription: (nodeId, details) => setDescription({...details, nodeId, open: true}),
        insert: (parentId, index, inserted) => {
            const node = toPillNoCodeNode(inserted);
            if (node == null) {
                return;
            }
            dispatch({type: 'insertNode', parentId, index, node});
            setAutoEditId(node.type === 'NoCodeExpression' ? null : node.id);
            requestFocus(node.id);
        },
        remove: (nodeId) => {
            const slot = findPillNoCodeSlot(root, nodeId);
            dispatch({type: 'removeNode', nodeId});
            if (slot != null) {
                requestFocus(socketKey(slot.parentId, slot.index));
            }
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
            afterMenuRef.current = {nodeId, name, wrap: false};
            setMenu({
                nodeId,
                name,
                anchor,
                up: sibling(item?.previousElementSibling),
                down: sibling(item?.nextElementSibling),
            });
        },
        startPointerDrag: (nodeId, name) => setPointerDrag({nodeId, name}),
        endPointerDrag: () => setPointerDrag(null),
        canDropPointerDrag: (slot) => pointerDrag != null && canMove(pointerDrag.nodeId, slot),
        dropPointerDrag: (slot) => {
            if (pointerDrag != null && canMove(pointerDrag.nodeId, slot)) {
                move(pointerDrag.nodeId, slot, pointerDrag.name);
            }
        },
    };

    const handleKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
        const tree = treeRef.current;
        const target = event.target as HTMLElement;
        // Events from popovers and dialogs bubble through the React tree although they render in a portal. A key
        // already handled by a tree item, such as the Space that starts moving, must not be handled twice.
        if (tree == null || !tree.contains(target) || event.defaultPrevented) {
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
                next = item.querySelector<HTMLElement>(`:scope > [role="group"] > ${TREE_ITEM_SELECTOR}`);
                break;
            case 'ArrowLeft':
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

    const menuNode = menu == null ? null : findPillNoCodeNode(root, menu.nodeId);
    const menuDetails = menuNode == null ? null : getNodeDetails(menuNode, allOperators);

    const closeDescription = () => setDescription((current) => current == null ? null : {...current, open: false});

    const runMenuAction = (action: (menu: MenuState) => void) => {
        if (menu != null) {
            deferAnnouncements();
            action(menu);
        }
        setMenu(null);
    };

    return (
        <Box>
            {
                grab != null &&
                <Box sx={{display: 'flex', alignItems: 'center', gap: 1, mb: 1}}>
                    <Typography variant="caption" color="text.secondary">
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

            <Box
                ref={treeRef}
                role="tree"
                aria-label={isStringNullOrEmpty(parameter.label) ? 'No-Code-Ausdruck' : parameter.label}
                aria-describedby={helpId}
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
                <PillNoCodeSlotView
                    node={root}
                    parentId={null}
                    index={0}
                    parameter={parameter}
                    error={operandError}
                    ctx={ctx}
                />
            </Box>

            <PillNoCodeDryRunPanel root={root} allElements={allElements} onResults={setResults}/>

            <VisuallyHidden id={helpId}>
                Die Pfeiltasten wechseln zwischen den Einträgen. Die Eingabetaste bearbeitet, die Leertaste nimmt zum
                Verschieben auf, Entfernen löscht und Umschalt+F10 öffnet weitere Aktionen.
            </VisuallyHidden>
            <VisuallyHidden role="status">
                {announcement != null && <span key={announcement.id}>{announcement.text}</span>}
            </VisuallyHidden>

            <Menu
                open={menu != null}
                anchorEl={menu?.anchor}
                onClose={() => setMenu(null)}
                // The tree item takes focus once the menu is gone; focusing it earlier would race the focus trap.
                disableRestoreFocus
                slotProps={{
                    transition: {
                        onExited: () => {
                            flushAnnouncements();
                            const after = afterMenuRef.current;
                            afterMenuRef.current = null;
                            const slot = after == null ? null : findPillNoCodeSlot(root, after.nodeId);
                            if (after?.describe != null) {
                                setDescription({...after.describe, nodeId: after.nodeId, open: true});
                            } else if (after?.wrap && slot != null) {
                                setWrapTarget({
                                    nodeId: after.nodeId,
                                    name: after.name,
                                    returnType: slotParameterType(slot),
                                });
                            } else if (after != null) {
                                requestFocus(after.nodeId);
                            }
                        },
                    },
                }}
            >
                <MenuItem
                    disabled={menu?.up == null}
                    onClick={() => runMenuAction((current) => current.up != null && move(current.nodeId, current.up, current.name))}
                >
                    Nach oben verschieben
                </MenuItem>
                <MenuItem
                    disabled={menu?.down == null}
                    onClick={() => runMenuAction((current) => current.down != null && move(current.nodeId, current.down, current.name))}
                >
                    Nach unten verschieben
                </MenuItem>
                <MenuItem onClick={() => runMenuAction((current) => startGrab(current.nodeId, current.name))}>
                    An andere Stelle verschieben
                </MenuItem>
                <MenuItem
                    onClick={() => runMenuAction((current) => {
                        afterMenuRef.current = {nodeId: current.nodeId, name: current.name, wrap: true};
                    })}
                >
                    Übergeordneten Operator hinzufügen
                </MenuItem>
                {
                    menuDetails != null &&
                    <MenuItem
                        onClick={() => runMenuAction((current) => {
                            afterMenuRef.current = {nodeId: current.nodeId, name: current.name, wrap: false, describe: menuDetails};
                        })}
                    >
                        Beschreibung anzeigen
                    </MenuItem>
                }
            </Menu>

            <Dialog
                open={description?.open ?? false}
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
                <DialogTitleWithClose onClose={closeDescription}>
                    {description?.title}
                </DialogTitleWithClose>
                <DialogContent tabIndex={0}>
                    {description?.details}
                </DialogContent>
            </Dialog>

            <SelectOperatorDialog
                open={wrapTarget != null}
                // The node becomes the first parameter, so operators without parameters would hide it.
                operators={allOperators.filter((op) => op.signatures.some((signature) => signature.parameters.length > 0))}
                desiredReturnType={wrapTarget?.returnType ?? NoCodeDataType.Runtime}
                onSelect={(op) => {
                    if (wrapTarget == null) {
                        return;
                    }
                    const wrapperId = uuid4();
                    deferAnnouncements();
                    dispatch({type: 'wrapNode', nodeId: wrapTarget.nodeId, wrapperId, operatorIdentifier: op.identifier});
                    announce(`Operator „${op.label}“ hinzugefügt. „${wrapTarget.name}“ ist jetzt sein erster Parameter.`);
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

interface PillNoCodeTreeItemProps {
    itemKey: string;
    slot: PillNoCodeSlot;
    /** Name in announcements and the action menu. Sockets have none and cannot be moved. */
    name?: string;
    /** The parameter the item fills; its label names the item and its sentence parts surround it. */
    parameter: NoCodeParameter;
    /** Text after an operator that starts the sentence of its parameters. */
    lead?: string | null;
    ariaLabel: string;
    /** IDs of the error and warning texts that describe the item. */
    describedBy?: (string | undefined)[];
    /** Dry run results are shown for values that are not literally written in the expression. */
    showResult?: boolean;
    ctx: EditorContext;
    onActivate: () => void;
    onRemove?: () => void;
    /** The pill or socket shown next to the parameter label. */
    children: ReactNode;
    /** Child tree items of an operator, rendered as an indented group. */
    group?: ReactNode;
}

function PillNoCodeTreeItem(props: PillNoCodeTreeItemProps) {
    const {itemKey, slot, name, parameter, lead, ariaLabel, describedBy = [], showResult = false, ctx, onActivate, onRemove, children, group} = props;
    const parameterLabel = parameter.label;
    // An empty phrase still takes the caption width, so operands of the same operator stay aligned.
    const phrase = parameter.sentencePhrase ?? parameterLabel;
    const hasPhrase = parameter.sentencePhrase != null || !isStringNullOrEmpty(parameterLabel);
    const suffix = isStringNullOrEmpty(parameter.sentenceSuffix) ? null : parameter.sentenceSuffix;
    const rowRef = useRef<HTMLDivElement>(null);
    const isNode = name != null;
    const result = showResult ? ctx.results?.get(itemKey) : undefined;
    const resultId = result == null ? undefined : `pill-no-code-result-${itemKey}`;
    const descriptionIds = [...describedBy, resultId].filter((id) => id != null).join(' ');

    const {dragProps, isDragging} = useDrag({
        isDisabled: !isNode,
        // Without a drag button react-aria would start its own keyboard drag on Enter; the tree handles keys itself.
        hasDragButton: true,
        getItems: () => [{[DRAG_TYPE]: itemKey, 'text/plain': name ?? ''}],
        getAllowedDropOperations: () => ['move'],
        onDragStart: () => ctx.startPointerDrag(itemKey, name ?? ''),
        onDragEnd: () => ctx.endPointerDrag(),
    });
    const {dropProps, isDropTarget} = useDrop({
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
                } else if ((event.key === 'Delete' || event.key === 'Backspace') && onRemove != null) {
                    event.preventDefault();
                    onRemove();
                } else if (event.key === ' ') {
                    event.preventDefault();
                    if (name != null) {
                        ctx.startGrab(itemKey, name);
                    }
                } else if ((event.key === 'ContextMenu' || (event.shiftKey && event.key === 'F10')) && rowRef.current != null) {
                    event.preventDefault();
                    openMenu(rowRef.current);
                }
            }}
            sx={{
                outline: 'none',
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
                {...dragProps}
                {...dropProps}
                onContextMenu={(event) => {
                    if (isNode && rowRef.current != null) {
                        event.preventDefault();
                        openMenu(rowRef.current);
                    }
                }}
                sx={{
                    display: 'flex',
                    alignItems: 'flex-start',
                    gap: 1,
                    width: 'fit-content',
                    maxWidth: '100%',
                    borderRadius: 3.5,
                    opacity: isDragging || isGrabbed ? 0.5 : 1,
                    ...(isGrabCandidate && {outline: '1px dashed', outlineColor: 'primary.main', outlineOffset: 2}),
                    ...((isDropTarget || isGrabTarget) && focusRing),
                }}
            >
                {
                    hasPhrase &&
                    <SentenceText sx={{minWidth: 72}}>
                        {phrase}
                    </SentenceText>
                }
                {children}
                {!isStringNullOrEmpty(lead) && <SentenceText>{lead}</SentenceText>}
                {/* The suffix of an operator follows its operands, so the sentence reads in order. */}
                {suffix != null && group == null && <SentenceText>{suffix}</SentenceText>}
                {result != null && resultId != null && <PillNoCodeDryRunResult id={resultId} result={result}/>}
                {
                    isNode &&
                    <IconButton
                        data-actions
                        size="small"
                        tabIndex={-1}
                        aria-label={`Aktionen für „${name}“`}
                        aria-haspopup="menu"
                        onClick={(event) => openMenu(event.currentTarget)}
                        sx={{p: 0.25, mt: 0.25, color: 'text.secondary'}}
                    >
                        <MoreVert fontSize="small"/>
                    </IconButton>
                }
            </Box>

            {
                group != null &&
                // CSS borders draw the connecting lines, so nothing is measured in the DOM.
                <Box
                    role="group"
                    sx={{
                        display: 'flex',
                        flexDirection: 'column',
                        gap: 1,
                        mt: 1,
                        ml: 1.5,
                        pl: 2,
                        borderLeft: 1,
                        borderColor: 'divider',
                    }}
                >
                    {group}
                </Box>
            }

            {
                suffix != null && group != null &&
                <SentenceText sx={{display: 'block', mt: 1}}>
                    {suffix}
                </SentenceText>
            }
        </Box>
    );
}

/** Words of the sentence an expression reads as. Screen readers get the parameter label in the item name instead. */
function SentenceText({children, sx}: {children: ReactNode; sx?: SxProps<Theme>}) {
    return (
        <Typography
            aria-hidden
            variant="caption"
            color="text.secondary"
            sx={[{lineHeight: '28px', whiteSpace: 'nowrap'}, ...(Array.isArray(sx) ? sx : [sx])]}
        >
            {children}
        </Typography>
    );
}

function PillNoCodeSlotView(props: SlotProps) {
    const {node} = props;

    if (node == null) {
        return <PillNoCodeSocketView {...props}/>;
    }

    if (node.type === 'NoCodeExpression') {
        return <PillNoCodeExpressionView {...props} node={node}/>;
    }

    return <PillNoCodeLeafView {...props} node={node}/>;
}

function PillNoCodeSocketView(props: SlotProps) {
    const {parentId, index, parameter, ctx} = props;
    const socketRef = useRef<HTMLButtonElement>(null);
    const [anchor, setAnchor] = useState<HTMLElement | null>(null);
    const Icon = NoCodeDataTypeIcons[parameter.type];
    const name = isStringNullOrEmpty(parameter.label) ? 'Ausdruck' : parameter.label;

    return (
        <PillNoCodeTreeItem
            itemKey={socketKey(parentId, index)}
            slot={{parentId, index}}
            parameter={parameter}
            ariaLabel={`leer, ${NoCodeDataTypeLabels[parameter.type]}`}
            ctx={ctx}
            onActivate={() => setAnchor(socketRef.current)}
        >
            <PillHint parameter={parameter}>
                <ButtonBase
                    ref={socketRef}
                    tabIndex={-1}
                    aria-label={`${name} festlegen`}
                    onClick={(event) => setAnchor(event.currentTarget)}
                    sx={{
                        height: 28,
                        px: 1,
                        gap: 0.5,
                        borderRadius: 14,
                        border: '1px dashed',
                        borderColor: 'text.disabled',
                        color: 'text.secondary',
                        typography: 'body2',
                    }}
                >
                    <Icon fontSize="small" aria-hidden/>
                    Festlegen
                </ButtonBase>
            </PillHint>

            <Popover
                open={anchor != null}
                anchorEl={anchor}
                onClose={() => setAnchor(null)}
                anchorOrigin={{vertical: 'bottom', horizontal: 'left'}}
                slotProps={{paper: {sx: {p: 2, maxWidth: '100%'}}}}
            >
                <NoCodeOperandEditorSelector
                    allOperators={ctx.allOperators}
                    label={name}
                    hint={parameter.description}
                    contextType={ctx.contextType}
                    onChange={(operand) => {
                        setAnchor(null);
                        ctx.insert(parentId, index, operand);
                    }}
                />
            </Popover>
        </PillNoCodeTreeItem>
    );
}

function PillNoCodeExpressionView(props: SlotProps & {node: PillNoCodeExpressionNode}) {
    const {node, parentId, index, parameter: slotParameter, error, ctx} = props;
    const [showOperatorPicker, setShowOperatorPicker] = useState(false);

    const operator = ctx.allOperators.find((op) => op.identifier === node.operatorIdentifier);
    const signature = operator == null ? undefined : resolveNoCodeSignature(operator, {
        operandCount: node.operands.length > 0 ? node.operands.length : undefined,
    });
    const parameters: NoCodeParameter[] = signature?.parameters ?? node.operands.map((_, operandIndex) => ({
        type: NoCodeDataType.Runtime,
        label: `Parameter ${operandIndex + 1}`,
        description: null,
        options: [],
    }));
    const name = operator?.label ?? (
        isStringNullOrEmpty(node.operatorIdentifier)
            ? 'Kein Operator ausgewählt'
            : 'Unbekannter Operator'
    );
    const dataType = signature?.returnType ?? NoCodeDataType.Runtime;
    const errorId = error?.error != null ? `pill-no-code-error-${node.id}` : undefined;
    const warning = getPillNoCodeTypeConflict(node, slotParameter.type, ctx.allElements, ctx.allOperators);
    const warningId = warning != null ? `pill-no-code-warning-${node.id}` : undefined;

    return (
        <PillNoCodeTreeItem
            itemKey={node.id}
            slot={{parentId, index}}
            name={name}
            parameter={slotParameter}
            lead={operator?.sentenceLead}
            ariaLabel={`Operator ${name}, ${NoCodeDataTypeLabels[dataType]}`}
            describedBy={[errorId, warningId]}
            showResult
            ctx={ctx}
            onActivate={() => setShowOperatorPicker(true)}
            onRemove={() => ctx.remove(node.id)}
            group={parameters.map((parameter, operandIndex) => (
                <PillNoCodeSlotView
                    key={node.operands[operandIndex]?.id ?? socketKey(node.id, operandIndex)}
                    node={node.operands[operandIndex] ?? null}
                    parentId={node.id}
                    index={operandIndex}
                    parameter={parameter}
                    error={error?.subErrors?.[operandIndex]}
                    ctx={ctx}
                />
            ))}
        >
            <PillHint
                summary={operator?.abstractDescription}
                parameter={slotParameter}
                onMore={operator == null ? undefined : () => ctx.showDescription(node.id, getNodeDetails(node, ctx.allOperators)!)}
            >
                <NoCodePill
                    dataType={dataType}
                    label={name}
                    dismissLabel={`Operator „${name}“ löschen`}
                    error={error?.error}
                    errorId={errorId}
                    warning={warning}
                    warningId={warningId}
                    tabIndex={-1}
                    onClick={() => setShowOperatorPicker(true)}
                    onDismiss={() => ctx.remove(node.id)}
                />
            </PillHint>

            <SelectOperatorDialog
                open={showOperatorPicker}
                operators={ctx.allOperators}
                onSelect={(op) => {
                    setShowOperatorPicker(false);
                    ctx.dispatch({
                        type: 'replaceNode',
                        nodeId: node.id,
                        node: {...node, operatorIdentifier: op.identifier},
                    });
                }}
                onClose={() => setShowOperatorPicker(false)}
                desiredReturnType={NoCodeDataType.Runtime}
            />
        </PillNoCodeTreeItem>
    );
}

function PillNoCodeLeafView(props: SlotProps & {node: PillNoCodeLeafNode}) {
    const {node, parentId, index, parameter, error, ctx} = props;
    const pillRef = useRef<HTMLButtonElement>(null);
    const [anchor, setAnchor] = useState<HTMLElement | null>(null);

    useEffect(() => {
        if (ctx.autoEditId === node.id) {
            setAnchor(pillRef.current);
        }
    }, [ctx.autoEditId, node.id]);

    const {label, dataType, dismissLabel, summary} = describeLeaf(node, parameter, ctx.allElements);
    const errorId = error?.error != null ? `pill-no-code-error-${node.id}` : undefined;
    const warning = getPillNoCodeTypeConflict(node, parameter.type, ctx.allElements, ctx.allOperators);
    const warningId = warning != null ? `pill-no-code-warning-${node.id}` : undefined;

    return (
        <PillNoCodeTreeItem
            itemKey={node.id}
            slot={{parentId, index}}
            name={label}
            parameter={parameter}
            ariaLabel={`${label}, ${NoCodeDataTypeLabels[dataType]}`}
            describedBy={[errorId, warningId]}
            showResult={node.type !== 'NoCodeStaticValue'}
            ctx={ctx}
            onActivate={() => setAnchor(pillRef.current)}
            onRemove={() => ctx.remove(node.id)}
        >
            <PillHint
                summary={summary}
                parameter={parameter}
                onMore={() => ctx.showDescription(node.id, getNodeDetails(node, ctx.allOperators)!)}
            >
                <NoCodePill
                    ref={pillRef}
                    dataType={dataType}
                    label={label}
                    dismissLabel={dismissLabel}
                    error={error?.error}
                    errorId={errorId}
                    warning={warning}
                    warningId={warningId}
                    tabIndex={-1}
                    onClick={(event) => setAnchor(event.currentTarget)}
                    onDismiss={() => ctx.remove(node.id)}
                />
            </PillHint>

            {/* shortcut: values are edited with the existing operand editors until the pill editor has its own. */}
            <Popover
                open={anchor != null}
                anchorEl={anchor}
                onClose={() => setAnchor(null)}
                anchorOrigin={{vertical: 'bottom', horizontal: 'left'}}
                slotProps={{paper: {sx: {p: 2, width: 480, maxWidth: '100%'}}}}
            >
                <NoCodeOperandEditor
                    parameter={parameter}
                    operand={toNoCodeOperand(node)}
                    onChange={(operand) => {
                        const replacement = toPillNoCodeNode(operand);
                        if (replacement == null) {
                            ctx.remove(node.id);
                        } else {
                            ctx.dispatch({type: 'replaceNode', nodeId: node.id, node: replacement});
                        }
                    }}
                    allOperators={ctx.allOperators}
                    allElements={ctx.allElements}
                    contextType={ctx.contextType}
                    operandError={error ?? undefined}
                />
            </Popover>
        </PillNoCodeTreeItem>
    );
}

function describeLeaf(
    node: PillNoCodeLeafNode,
    parameter: NoCodeParameter,
    allElements: ElementWithParents[],
): {label: string; dataType: NoCodeDataType; dismissLabel: string; summary: string} {
    switch (node.type) {
        case 'NoCodeStaticValue': {
            const options = parameter.options.length > 0 || parameter.type !== NoCodeDataType.Boolean
                ? parameter.options
                : BOOL_DEFAULT_OPTIONS;
            const label = options.find((option) => option.value === node.value)?.label ??
                (isStringNullOrEmpty(node.value) ? 'Leerer Wert' : node.value!);
            return {
                label,
                dataType: parameter.type,
                dismissLabel: `Festen Wert „${label}“ löschen`,
                summary: isStringNullOrEmpty(node.value) ? 'Ergibt einen leeren Wert.' : `Ergibt den eingegebenen Wert „${label}“.`,
            };
        }
        case 'NoCodeReference': {
            const element = allElements.find((entry) => entry.element.id === node.elementId)?.element;
            const label = element != null
                ? generateComponentTitle(element)
                : isStringNullOrEmpty(node.elementId) ? 'Kein Element ausgewählt' : 'Unbekanntes Element';
            return {
                label,
                dataType: element != null ? NoCodeDataTypeMap[element.type] : NoCodeDataType.Runtime,
                dismissLabel: `Wert des Elements „${label}“ löschen`,
                summary: element != null
                    ? `Ergibt den aktuellen Wert des Elements „${label}“.`
                    : isStringNullOrEmpty(node.elementId)
                        ? 'Es ist noch kein Element ausgewählt.'
                        : 'Das ausgewählte Element ist nicht mehr vorhanden.',
            };
        }
        default: {
            const label = humanizeNoCode(node, allElements, []);
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
    node: Exclude<PillNoCodeLeafNode, {type: 'NoCodeStaticValue' | 'NoCodeReference'}>,
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
function getNodeDetails(node: PillNoCodeNode, allOperators: NoCodeOperatorDetailsDTO[]): NodeDetails | null {
    if (node.type !== 'NoCodeExpression') {
        return NoCodeOperandKindHelp[node.type];
    }
    const operator = allOperators.find((op) => op.identifier === node.operatorIdentifier);
    return operator == null ? null : {
        title: operator.label,
        details: <MarkdownContent markdown={operator.description}/>,
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
function PillHint({summary, parameter, onMore, children}: PillHintProps) {
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
                            color="text.secondary"
                            component="p"
                            sx={{mt: hasSummary ? 1 : 0}}
                        >
                            {isStringNullOrEmpty(parameter.label) ? parameter.description : `${parameter.label}: ${parameter.description}`}
                        </Typography>
                    }
                    {
                        onMore != null &&
                        <Link
                            component="button"
                            onClick={onMore}
                        >
                            Mehr...
                        </Link>
                    }
                </>
            }
        >
            <Box component="span" sx={{display: 'inline-flex', maxWidth: '100%'}}>
                {children}
            </Box>
        </HintTooltip>
    );
}

function PillNoCodeDryRunResult({id, result}: {id: string; result: NoCodeDryRunResultDTO}) {
    if (result.status === NoCodeDryRunStatus.Evaluated) {
        const text = formatDryRunValue(result.value);
        const shortText = text.length > 60 ? `${text.slice(0, 59)}…` : text;
        return (
            <Tooltip title={shortText === text ? '' : text} describeChild>
                <Typography id={id} variant="body2" color="text.secondary" sx={{lineHeight: '28px', whiteSpace: 'nowrap'}}>
                    <VisuallyHidden elementType="span">Testergebnis:</VisuallyHidden>
                    {' '}
                    <span aria-hidden>→ </span>
                    {shortText}
                </Typography>
            </Tooltip>
        );
    }

    // Expressions skipped because a parameter could not be evaluated would repeat that reason, so they stay short.
    const dependent = result.operands.some((operand) => operand.status !== NoCodeDryRunStatus.Evaluated);
    const failed = result.status === NoCodeDryRunStatus.Failed;
    const label = failed ? 'Fehler' : 'Nicht ausgewertet';
    return (
        <Typography
            id={id}
            variant="body2"
            color={failed ? 'error' : 'text.secondary'}
            sx={{lineHeight: '20px', py: '4px', fontStyle: failed ? undefined : 'italic'}}
        >
            {
                dependent && !failed
                    ? <>{label}<VisuallyHidden elementType="span">: {result.message}</VisuallyHidden></>
                    : `${label}: ${result.message}`
            }
        </Typography>
    );
}

function formatDryRunValue(value: unknown): string {
    if (value == null) {
        return 'leer';
    }
    if (typeof value === 'boolean') {
        return value ? 'wahr' : 'falsch';
    }
    if (typeof value === 'string') {
        return `„${value}“`;
    }
    return typeof value === 'object' ? JSON.stringify(value) : String(value);
}
