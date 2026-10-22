import ReactMarkdown, {type Components, type Options} from 'react-markdown';
import remarkBreaks from 'remark-breaks';
import remarkGfm from 'remark-gfm';

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

// The labels match the backend renderer. Only screen readers announce the heading of the footnotes.
const remarkRehypeOptions: Options['remarkRehypeOptions'] = {
    footnoteLabel: 'Fußnoten',
    footnoteLabelProperties: {className: ['visually-hidden']},
    footnoteBackLabel: (referenceIndex, rereferenceIndex) => rereferenceIndex > 1
        ? `Zurück zum ${rereferenceIndex}. Verweis auf Fußnote ${referenceIndex + 1}`
        : `Zurück zum Verweis auf Fußnote ${referenceIndex + 1}`,
};

// Applied after caller overrides. Like the backend renderer, images only contribute their alternative text, so
// rendering Markdown never makes the browser fetch a URL chosen by the author of the text.
const enforcedComponents: Components = {
    img: ({alt}) => alt ?? null,
};

interface MarkdownRendererProps {
    markdown?: string | null;
    components?: Components;
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
                remarkGfm,
                remarkBreaks,
            ]}
            remarkRehypeOptions={remarkRehypeOptions}
            urlTransform={sanitizeMarkdownUrl}
        >
            {props.markdown ?? ''}
        </ReactMarkdown>
    );
}
