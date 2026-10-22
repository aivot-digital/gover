import type {ReactNode} from 'react';
import {Box} from '@mui/material';
import {
    addExportVisitor$,
    addImportVisitor$,
    addLexicalNode$,
    Cell,
    type LexicalExportVisitor,
    type MdastImportVisitor,
    realmPlugin,
    useCellValue,
} from '@mdxeditor/editor';
import {
    DecoratorNode,
    type ElementNode,
    type LexicalNode,
    type NodeKey,
    type SerializedLexicalNode,
    type Spread,
} from 'lexical';
import type {Nodes, Parent, RootContent} from 'mdast';

const sourceModeAvailable$ = Cell(true);

type SerializedProtectedMarkdownNode = Spread<{
    mdastNode: RootContent;
    inline: boolean;
}, SerializedLexicalNode>;

function getPlainText(node: Nodes): string {
    if ('value' in node) {
        return node.value;
    }
    return 'children' in node ? node.children.map(getPlainText).join('') : '';
}

/**
 * Writes the construct as Markdown for display only, without titles and in one normalized form. Source mode shows the
 * Markdown as it is stored.
 */
function describeMarkdown(node: RootContent): string {
    switch (node.type) {
        case 'code':
            return ['```' + [node.lang, node.meta].filter(Boolean).join(' '), node.value, '```'].join('\n');
        case 'thematicBreak':
            return '---';
        case 'image':
            return `![${node.alt ?? ''}](${node.url})`;
        case 'imageReference':
            return `![${node.alt ?? ''}][${node.label ?? node.identifier}]`;
        case 'linkReference':
            return `[${getPlainText(node)}][${node.label ?? node.identifier}]`;
        case 'definition':
            return `[${node.label ?? node.identifier}]: ${node.url}`;
        default:
            return getPlainText(node);
    }
}

function withoutPositions(node: RootContent): RootContent {
    return JSON.parse(JSON.stringify(node, (key, value) => key === 'position' ? undefined : value));
}

function ProtectedMarkdown(props: {markdown: string; inline: boolean}) {
    const sourceModeAvailable = useCellValue(sourceModeAvailable$);
    const note = sourceModeAvailable
        ? 'Diesen Abschnitt können Sie nur in der Markdown-Quelle bearbeiten.'
        : 'Diesen Abschnitt können Sie hier nicht bearbeiten.';
    const sourceSx = {
        fontFamily: 'Monaco, monospace',
        fontSize: '0.875em',
        whiteSpace: 'pre-wrap',
        overflowWrap: 'anywhere',
    } as const;

    if (props.inline) {
        return (
            <Box
                component="span"
                title={note}
                sx={{
                    ...sourceSx,
                    backgroundColor: 'action.hover',
                    borderRadius: 1,
                    padding: '0.1em 0.3em',
                }}
            >
                {props.markdown}
            </Box>
        );
    }

    return (
        <Box
            sx={{
                my: 1,
                px: 1.5,
                py: 1,
                backgroundColor: 'action.hover',
                border: '1px dashed',
                borderColor: 'divider',
                borderRadius: 1,
            }}
        >
            <Box
                component="pre"
                sx={{
                    ...sourceSx,
                    m: 0,
                }}
            >
                {props.markdown}
            </Box>
            <Box
                component="span"
                sx={{
                    display: 'block',
                    mt: 0.5,
                    typography: 'caption',
                    color: 'text.secondary',
                }}
            >
                {note}
            </Box>
        </Box>
    );
}

/**
 * Holds a Markdown construct the editor cannot edit, such as a code block. The construct is shown, but it can only be
 * changed in source mode, and the Markdown writer writes the same construct back.
 */
export class ProtectedMarkdownNode extends DecoratorNode<ReactNode> {
    __mdastNode: RootContent;
    __inline: boolean;

    static getType(): string {
        return 'prosuna-protected-markdown';
    }

    static clone(node: ProtectedMarkdownNode): ProtectedMarkdownNode {
        return new ProtectedMarkdownNode(node.__mdastNode, node.__inline, node.__key);
    }

    static importJSON(serializedNode: SerializedProtectedMarkdownNode): ProtectedMarkdownNode {
        return new ProtectedMarkdownNode(serializedNode.mdastNode, serializedNode.inline);
    }

    constructor(mdastNode: RootContent, inline: boolean, key?: NodeKey) {
        super(key);
        this.__mdastNode = mdastNode;
        this.__inline = inline;
    }

    exportJSON(): SerializedProtectedMarkdownNode {
        return {
            ...super.exportJSON(),
            mdastNode: this.__mdastNode,
            inline: this.__inline,
        };
    }

    createDOM(): HTMLElement {
        return document.createElement(this.__inline ? 'span' : 'div');
    }

    updateDOM(): false {
        return false;
    }

    isInline(): boolean {
        return this.__inline;
    }

    getTextContent(): string {
        return describeMarkdown(this.__mdastNode);
    }

    getMdastNode(): RootContent {
        return this.getLatest().__mdastNode;
    }

    decorate(): ReactNode {
        return (
            <ProtectedMarkdown
                markdown={describeMarkdown(this.__mdastNode)}
                inline={this.__inline}
            />
        );
    }
}

function $isProtectedMarkdownNode(node: LexicalNode | null | undefined): node is ProtectedMarkdownNode {
    return node instanceof ProtectedMarkdownNode;
}

const protectedMarkdownImportVisitor: MdastImportVisitor<RootContent> = {
    // Matches every node no other visitor imports. MDXEditor would otherwise stop showing the text and ignore edits.
    testNode: () => true,
    priority: -1000,
    visitNode({mdastNode, mdastParent, lexicalParent}) {
        // Only the root holds blocks; everything below it, including list items and quotes, holds inline content.
        const inline = mdastParent?.type !== 'root';
        // Appended directly, because addAndStepInto would also import the children into this node.
        (lexicalParent as ElementNode).append(new ProtectedMarkdownNode(withoutPositions(mdastNode), inline));
    },
};

const protectedMarkdownExportVisitor: LexicalExportVisitor<ProtectedMarkdownNode, RootContent> = {
    testLexicalNode: $isProtectedMarkdownNode,
    visitLexicalNode({lexicalNode, mdastParent, actions}) {
        actions.appendToParent(mdastParent as Parent, structuredClone(lexicalNode.getMdastNode()) as Parent['children'][number]);
    },
};

interface ProtectedMarkdownPluginParams {
    sourceModeAvailable: boolean;
}

export const protectedMarkdownPlugin = realmPlugin<ProtectedMarkdownPluginParams>({
    init(realm, params) {
        realm.pubIn({
            [addLexicalNode$]: ProtectedMarkdownNode,
            [addImportVisitor$]: protectedMarkdownImportVisitor,
            [addExportVisitor$]: protectedMarkdownExportVisitor,
            [sourceModeAvailable$]: params?.sourceModeAvailable ?? true,
        });
    },
    update(realm, params) {
        realm.pub(sourceModeAvailable$, params?.sourceModeAvailable ?? true);
    },
});
