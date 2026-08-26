import {useEffect} from 'react';
import {useLexicalComposerContext} from '@lexical/react/LexicalComposerContext';
import {alpha, type Theme} from '@mui/material/styles';
import {
    $applyNodeReplacement,
    $createTextNode,
    $isTextNode,
    type EditorConfig,
    type LexicalNode,
    type NodeKey,
    type SerializedTextNode,
    type Spread,
    TextNode,
} from 'lexical';
import {createCodeSyntaxColors} from '../expandable-code-block/code-syntax-theme';
import {
    findDynamicTextTokenMatch,
    getDynamicTextSyntaxSegments,
    getDynamicTextTokenKind,
    type DynamicTextSyntaxRole,
} from './dynamic-text-syntax';

type HighlightedSyntaxRole = Exclude<DynamicTextSyntaxRole, 'plain'>;

const HIGHLIGHTED_SYNTAX_ROLES: HighlightedSyntaxRole[] = [
    'comment',
    'constant',
    'function',
    'keyword',
    'number',
    'property',
    'punctuation',
    'string',
];
const CUSTOM_HIGHLIGHT_PREFIX = 'prosuna-dynamic-text';
const customHighlightRanges = new Map<HTMLElement, Map<HighlightedSyntaxRole, Range[]>>();

function getCustomHighlightName(role: HighlightedSyntaxRole): string {
    return `${CUSTOM_HIGHLIGHT_PREFIX}-${role}`;
}

function supportsCustomHighlights(): boolean {
    return typeof CSS !== 'undefined' && CSS.highlights != null && typeof Highlight !== 'undefined';
}

function refreshCustomHighlights() {
    if (!supportsCustomHighlights()) {
        return;
    }

    for (const role of HIGHLIGHTED_SYNTAX_ROLES) {
        const ranges = Array.from(customHighlightRanges.values())
            .flatMap((rangesByRole) => rangesByRole.get(role) ?? []);
        const name = getCustomHighlightName(role);

        if (ranges.length === 0) {
            CSS.highlights.delete(name);
        } else {
            CSS.highlights.set(name, new Highlight(...ranges));
        }
    }
}

function getTextNodes(element: HTMLElement): Text[] {
    const walker = element.ownerDocument.createTreeWalker(element, 4);
    const nodes: Text[] = [];
    let currentNode = walker.nextNode();

    while (currentNode != null) {
        nodes.push(currentNode as Text);
        currentNode = walker.nextNode();
    }

    return nodes;
}

function getRangeBoundary(textNodes: Text[], absoluteOffset: number): {node: Text; offset: number} | null {
    let consumedLength = 0;

    for (const textNode of textNodes) {
        const nodeLength = textNode.data.length;
        if (absoluteOffset <= consumedLength + nodeLength) {
            return {
                node: textNode,
                offset: absoluteOffset - consumedLength,
            };
        }
        consumedLength += nodeLength;
    }

    return null;
}

function createCustomHighlightRanges(container: HTMLElement): Map<HighlightedSyntaxRole, Range[]> {
    const rangesByRole = new Map<HighlightedSyntaxRole, Range[]>(
        HIGHLIGHTED_SYNTAX_ROLES.map((role) => [role, []]),
    );

    for (const token of Array.from(container.querySelectorAll<HTMLElement>('.dynamic-text-token'))) {
        const textNodes = getTextNodes(token);
        const value = textNodes.map((textNode) => textNode.data).join('');
        let segmentStart = 0;

        for (const segment of getDynamicTextSyntaxSegments(value)) {
            const segmentEnd = segmentStart + segment.value.length;
            if (segment.role !== 'plain' && segmentEnd > segmentStart) {
                const start = getRangeBoundary(textNodes, segmentStart);
                const end = getRangeBoundary(textNodes, segmentEnd);
                if (start != null && end != null) {
                    const range = token.ownerDocument.createRange();
                    range.setStart(start.node, start.offset);
                    range.setEnd(end.node, end.offset);
                    rangesByRole.get(segment.role)?.push(range);
                }
            }
            segmentStart = segmentEnd;
        }
    }

    return rangesByRole;
}

export function useDynamicTextSyntaxHighlights(container: HTMLElement | null) {
    useEffect(() => {
        if (container == null || !supportsCustomHighlights()) {
            return;
        }

        const updateHighlights = () => {
            customHighlightRanges.set(container, createCustomHighlightRanges(container));
            container.dataset.dynamicTextRangeHighlighting = 'true';
            refreshCustomHighlights();
        };

        updateHighlights();
        const observer = new MutationObserver(updateHighlights);
        observer.observe(container, {
            childList: true,
            characterData: true,
            subtree: true,
        });

        return () => {
            observer.disconnect();
            delete container.dataset.dynamicTextRangeHighlighting;
            customHighlightRanges.delete(container);
            refreshCustomHighlights();
        };
    }, [container]);
}

export type SerializedDynamicTextTokenNode = Spread<{
    type: 'dynamic-text-token';
    version: 1;
}, SerializedTextNode>;

export class DynamicTextTokenNode extends TextNode {
    static getType(): string {
        return 'dynamic-text-token';
    }

    static clone(node: DynamicTextTokenNode): DynamicTextTokenNode {
        return new DynamicTextTokenNode(node.__text, node.__key);
    }

    static importJSON(serializedNode: SerializedDynamicTextTokenNode): DynamicTextTokenNode {
        return $createDynamicTextTokenNode().updateFromJSON(serializedNode);
    }

    constructor(text = '', key?: NodeKey) {
        super(text, key);
    }

    createDOM(config: EditorConfig): HTMLElement {
        const element = super.createDOM(config);
        updateDynamicTextTokenElement(element, this.getTextContent());
        return element;
    }

    updateDOM(prevNode: this, dom: HTMLElement, config: EditorConfig): boolean {
        const didUpdate = super.updateDOM(prevNode, dom, config);
        updateDynamicTextTokenElement(dom, this.getTextContent());
        return didUpdate;
    }

    exportJSON(): SerializedDynamicTextTokenNode {
        return {
            ...super.exportJSON(),
            type: 'dynamic-text-token',
            version: 1,
        };
    }

    isTextEntity(): true {
        return true;
    }
}

function updateDynamicTextTokenElement(element: HTMLElement, value: string) {
    const kind = getDynamicTextTokenKind(value) ?? 'output';
    element.classList.add('dynamic-text-token');
    element.dataset.dynamicTextKind = kind;
    element.setAttribute('spellcheck', 'false');
    element.title = value.replaceAll(/\s+/g, ' ').trim();
}

export function $createDynamicTextTokenNode(text = ''): DynamicTextTokenNode {
    return $applyNodeReplacement(new DynamicTextTokenNode(text));
}

export function $isDynamicTextTokenNode(node: LexicalNode | null | undefined): node is DynamicTextTokenNode {
    return node instanceof DynamicTextTokenNode;
}

function transformPlainTextNode(node: TextNode) {
    if ($isDynamicTextTokenNode(node) || !node.isSimpleText()) {
        return;
    }

    const match = findDynamicTextTokenMatch(node.getTextContent());
    if (match == null) {
        return;
    }

    const splitNodes = match.start === 0
        ? node.splitText(match.end)
        : node.splitText(match.start, match.end);
    const matchedNode = match.start === 0 ? splitNodes[0] : splitNodes[1];

    matchedNode.replace(
        $createDynamicTextTokenNode(matchedNode.getTextContent())
            .setFormat(matchedNode.getFormat())
            .setStyle(matchedNode.getStyle()),
    );
}

function transformDynamicTextTokenNode(node: DynamicTextTokenNode) {
    const value = node.getTextContent();
    const match = findDynamicTextTokenMatch(value);
    if (match != null && match.start === 0 && match.end === value.length) {
        return;
    }

    node.replace(
        $createTextNode(value)
            .setFormat(node.getFormat())
            .setStyle(node.getStyle()),
    );
}

export function DynamicTextLexicalPlugin() {
    const [editor] = useLexicalComposerContext();

    useEffect(() => {
        const unregisterTextTransform = editor.registerNodeTransform(TextNode, transformPlainTextNode);
        const unregisterDynamicTextTransform = editor.registerNodeTransform(
            DynamicTextTokenNode,
            transformDynamicTextTokenNode,
        );

        return () => {
            unregisterTextTransform();
            unregisterDynamicTextTransform();
        };
    }, [editor]);

    return null;
}

export function getDynamicTextTokenStyles(theme: Theme) {
    const syntaxColors = createCodeSyntaxColors(theme);
    const chipBackground = alpha(theme.palette.text.primary, theme.palette.mode === 'dark' ? 0.09 : 0.045);

    return {
        '& .dynamic-text-token': {
            display: 'inline',
            boxDecorationBreak: 'clone',
            WebkitBoxDecorationBreak: 'clone',
            mx: '1px',
            px: 0.5,
            py: '1px',
            border: '1px solid',
            borderColor: alpha(theme.palette.text.primary, theme.palette.mode === 'dark' ? 0.22 : 0.16),
            borderRadius: 0.75,
            bgcolor: chipBackground,
            color: 'text.primary',
            fontFamily: 'monospace',
            fontSize: '0.9em',
            whiteSpace: 'nowrap',
            caretColor: theme.palette.text.primary,
        },
        '& .dynamic-text-token[data-dynamic-text-kind="comment"]': {
            fontStyle: 'italic',
        },
        ...Object.fromEntries(HIGHLIGHTED_SYNTAX_ROLES.map((role) => [
            `& .dynamic-text-token::highlight(${getCustomHighlightName(role)})`,
            {color: syntaxColors[role]},
        ])),
        '@container (max-width: 360px)': {
            '&[data-dynamic-text-multiline="true"] .dynamic-text-token': {
                whiteSpace: 'normal',
                overflowWrap: 'anywhere',
                boxDecorationBreak: 'slice',
                WebkitBoxDecorationBreak: 'slice',
            },
        },
        '@media (forced-colors: active)': {
            '& .dynamic-text-token': {
                color: 'CanvasText',
                WebkitTextFillColor: 'CanvasText',
                backgroundImage: 'none',
                backgroundColor: 'Canvas',
                backgroundClip: 'border-box',
                WebkitBackgroundClip: 'border-box',
                borderColor: 'CanvasText',
            },
        },
    };
}
