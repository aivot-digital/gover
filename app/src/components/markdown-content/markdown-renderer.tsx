import type {Link, LinkReference, Nodes, Parent, Root} from 'mdast';
import {gfmAutolinkLiteralFromMarkdown} from 'mdast-util-gfm-autolink-literal';
import {gfmStrikethroughFromMarkdown} from 'mdast-util-gfm-strikethrough';
import {gfmTaskListItemFromMarkdown} from 'mdast-util-gfm-task-list-item';
import {gfmAutolinkLiteral} from 'micromark-extension-gfm-autolink-literal';
import {gfmStrikethrough} from 'micromark-extension-gfm-strikethrough';
import {gfmTaskListItem} from 'micromark-extension-gfm-task-list-item';
import ReactMarkdown, {type Components} from 'react-markdown';
import remarkBreaks from 'remark-breaks';
// Types the micromark and mdast extensions in the processor data.
import type {} from 'remark-parse';
import type {Processor} from 'unified';

/**
 * Link protocols kept by the frontend and the backend Markdown renderer. They match the rich text editor, so a link
 * authored there keeps its target. Relative URLs are always kept.
 */
export const MarkdownAllowedUrlProtocols: readonly string[] = ['http', 'https', 'mailto', 'sms', 'tel'];

const SurroundingHtmlSpaces = /^[ \t\n\f\r]+|[ \t\n\f\r]+$/g;

/**
 * Mirrors commonmark-java's `DefaultUrlSanitizer` used by the backend: a URL is relative when `/`, `?` or `#` comes
 * before the first `:`, otherwise its protocol must be allowed. Disallowed targets become an empty URL.
 */
export function sanitizeMarkdownUrl(url: string): string {
    const trimmedUrl = url.replace(SurroundingHtmlSpaces, '');
    const delimiterIndex = trimmedUrl.search(/[/?#:]/);
    if (delimiterIndex < 0 || trimmedUrl[delimiterIndex] !== ':') {
        return trimmedUrl;
    }

    const protocol = trimmedUrl.slice(0, delimiterIndex).toLowerCase();
    return MarkdownAllowedUrlProtocols.includes(protocol) ? trimmedUrl : '';
}

function getLinkLabel(node: Nodes): string {
    if (node.type === 'text' || node.type === 'inlineCode') {
        return node.value;
    }
    if (node.type === 'image') {
        return node.alt ?? '';
    }
    return 'children' in node ? node.children.map(getLinkLabel).join('') : '';
}

/** Mirrors `MarkdownRenderer.showsItsTarget` of the backend: bare web and e-mail addresses already show their target. */
function showsItsTarget(link: Link | LinkReference, target: string): boolean {
    const label = getLinkLabel(link);
    return [label, `http://${label}`, `https://${label}`, `mailto:${label}`].includes(target);
}

function footnoteSyntaxAsText(parent: Parent, source: string) {
    parent.children = parent.children.map((child): Parent['children'][number] => {
        if ((child.type === 'definition' || child.type === 'linkReference') && child.identifier.startsWith('^')) {
            const value = source.slice(child.position?.start.offset, child.position?.end.offset);
            return child.type === 'definition'
                ? {type: 'paragraph', children: [{type: 'text', value}]}
                : {type: 'text', value};
        }
        if ('children' in child) {
            footnoteSyntaxAsText(child, source);
        }
        return child;
    });
}

/**
 * The GitHub Flavored Markdown of `remark-gfm` without tables and footnotes, which the rich text editor cannot edit.
 * As in the backend, footnote syntax stays text: CommonMark reads `[^1]: Hinweis` as a link reference definition,
 * which hides the line and links every `[^1]` to its text.
 */
function remarkSupportedGfm(this: Processor) {
    const data = this.data();
    (data.micromarkExtensions ??= []).push(gfmAutolinkLiteral(), gfmStrikethrough(), gfmTaskListItem());
    (data.fromMarkdownExtensions ??= []).push(gfmAutolinkLiteralFromMarkdown(), gfmStrikethroughFromMarkdown(), gfmTaskListItemFromMarkdown());

    return (tree: Root, file: {value: unknown}) => footnoteSyntaxAsText(tree, String(file.value));
}

function collectDefinitions(node: Nodes, definitions: Map<string, string>) {
    if (node.type === 'definition' && !definitions.has(node.identifier)) {
        definitions.set(node.identifier, node.url);
    } else if ('children' in node) {
        node.children.forEach((child) => collectDefinitions(child, definitions));
    }
}

function rewriteLinks(parent: Parent, definitions: Map<string, string>, revealTargets: boolean) {
    parent.children = parent.children.flatMap((child): Parent['children'] => {
        if ('children' in child) {
            rewriteLinks(child, definitions, revealTargets);
        }
        if (child.type !== 'link' && child.type !== 'linkReference') {
            return [child];
        }

        const target = child.type === 'link' ? child.url : definitions.get(child.identifier) ?? '';
        if (showsItsTarget(child, target)) {
            // As in the backend, text instead of a link whose target the sanitizer would empty.
            return target !== '' && sanitizeMarkdownUrl(target) === '' ? child.children : [child];
        }
        if (!revealTargets) {
            return [child];
        }
        return target === '' ? child.children : [...child.children, {type: 'text', value: ` (${target})`}];
    });
}

/**
 * Keeps URLs with a protocol that is not allowed as text when they would be linked as their own text. With
 * `revealTargets`, every other link becomes its text followed by its target, as `MarkdownTemplateValues.revealLinkTargets`
 * does in the backend for formatted template values.
 */
function remarkLinkTargets({revealTargets}: {revealTargets: boolean}) {
    return (tree: Root) => {
        const definitions = new Map<string, string>();
        collectDefinitions(tree, definitions);
        rewriteLinks(tree, definitions, revealTargets);
    };
}

const defaultComponents: Components = {
    a: ({href, node: _node, ...props}) => {
        const isExternalLink = href != null && /^(https?:)?\/\//.test(href);

        return (
            <a
                href={href}
                {...props}
                target={isExternalLink ? '_blank' : undefined}
                rel={isExternalLink ? 'noopener noreferrer' : undefined}
            />
        );
    },
};

// Applied after caller overrides. Like the backend renderer, images only contribute their alternative text, so
// rendering Markdown never makes the browser fetch a URL chosen by the author of the text.
const enforcedComponents: Components = {
    img: ({alt}) => alt ?? null,
};

interface MarkdownRendererProps {
    markdown?: string | null;
    components?: Components;
    /** Writes link targets next to their text. Use it for Markdown entered by customers, so links cannot hide their target. */
    revealLinkTargets?: boolean;
}

/**
 * Renders Markdown with the rules of the backend `MarkdownRenderer`. Both renderers are checked against
 * `backend/src/test/resources/markdown/markdown-rendering-cases.json`, so change their rules together.
 *
 * Raw HTML stays literal text because no raw-HTML plugin such as `rehype-raw` is used; the backend escapes it too.
 */
export function MarkdownRenderer(props: MarkdownRendererProps) {
    return (
        <ReactMarkdown
            components={{
                ...defaultComponents,
                ...props.components,
                ...enforcedComponents,
            }}
            remarkPlugins={[
                remarkSupportedGfm,
                remarkBreaks,
                [remarkLinkTargets, {revealTargets: props.revealLinkTargets === true}],
            ]}
            urlTransform={sanitizeMarkdownUrl}
        >
            {props.markdown ?? ''}
        </ReactMarkdown>
    );
}
