package de.aivot.prosuna.backend.markdown.utils;

import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.commonmark.Extension;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.footnotes.FootnoteDefinition;
import org.commonmark.ext.footnotes.FootnoteReference;
import org.commonmark.ext.footnotes.FootnotesExtension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.task.list.items.TaskListItemsExtension;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Code;
import org.commonmark.node.HardLineBreak;
import org.commonmark.node.Image;
import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;
import org.commonmark.parser.PostProcessor;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.AttributeProvider;
import org.commonmark.renderer.html.DefaultUrlSanitizer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.renderer.text.LineBreakRendering;
import org.commonmark.renderer.text.TextContentNodeRendererContext;
import org.commonmark.renderer.text.TextContentRenderer;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Renders Markdown to HTML for every backend output channel: PDF documents, HTML template slots, e-mails and
 * mailbox messages. The text part of e-mails uses the plain text rendering.
 *
 * <p>The frontend counterpart is {@code MarkdownContent} in {@code app/src/components/markdown-content/}. Both
 * implementations are checked against the shared cases in {@code markdown/markdown-rendering-cases.json} of the
 * backend test resources, so the supported syntax and the safety rules must be changed on both sides together.</p>
 *
 * <p>Rendered Markdown regularly contains process data entered by customers. The rules are therefore the same for
 * staff-authored and customer-entered text: raw HTML is escaped, link targets are limited to
 * {@link #ALLOWED_URL_PROTOCOLS}, and images are reduced to their alternative text. Rendering an image would let the
 * PDF renderer, mail clients and browsers fetch arbitrary URLs.</p>
 */
public final class MarkdownRenderer {
    /**
     * The protocols the rich text editor accepts for links, so a link authored there keeps its target. Relative URLs
     * are always kept.
     */
    public static final List<String> ALLOWED_URL_PROTOCOLS = List.of("http", "https", "mailto", "sms", "tel");

    // Shared with MarkdownTemplateValues, which has to read values exactly as they are rendered later.
    static final List<Extension> EXTENSIONS = List.of(
            AutolinkExtension.create(),
            FootnotesExtension.create(),
            TablesExtension.create(),
            StrikethroughExtension.builder()
                    .requireTwoTildes(false)
                    .build(),
            TaskListItemsExtension.create()
    );
    private static final DefaultUrlSanitizer URL_SANITIZER = new DefaultUrlSanitizer(ALLOWED_URL_PROTOCOLS);
    static final Parser PARSER = Parser.builder()
            .extensions(EXTENSIONS)
            // Runs after the autolink extension, which links URLs with any protocol.
            .postProcessor(new DisallowedAutolinksAsText())
            .build();
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder()
            .extensions(EXTENSIONS)
            .nodeRendererFactory(ImageAltTextRenderer::new)
            .attributeProviderFactory(context -> new FootnoteBackLinkLabels())
            // The rich text editor and the frontend renderer keep single line breaks as well.
            .softbreak("<br />\n")
            .escapeHtml(true)
            .sanitizeUrls(true)
            .urlSanitizer(URL_SANITIZER)
            // Matches the URL normalization of the frontend renderer, for example for umlauts in link targets.
            .percentEncodeUrls(true)
            .build();
    private static final TextContentRenderer TEXT_RENDERER = TextContentRenderer.builder()
            .extensions(EXTENSIONS)
            .nodeRendererFactory(PlainTextRenderer::new)
            .lineBreakRendering(LineBreakRendering.SEPARATE_BLOCKS)
            .build();

    private MarkdownRenderer() {
    }

    @Nonnull
    public static String renderHtml(@Nullable String markdown) {
        if (StringUtils.isNullOrEmpty(markdown)) {
            return "";
        }

        return RENDERER.render(PARSER.parse(markdown));
    }

    /**
     * Renders Markdown as readable plain text, for example for the text part of a mail. Formatting characters are
     * dropped, links are written as their text followed by their target, and images as their alternative text.
     */
    @Nonnull
    public static String renderPlainText(@Nullable String markdown) {
        if (StringUtils.isNullOrEmpty(markdown)) {
            return "";
        }

        return TEXT_RENDERER.render(PARSER.parse(markdown)).strip();
    }

    /** Whether the visible text of a link already is its target, as for bare web and e-mail addresses. */
    static boolean showsItsTarget(@Nonnull Link link) {
        var label = new StringBuilder();
        link.accept(new AbstractVisitor() {
            @Override
            public void visit(Text text) {
                label.append(text.getLiteral());
            }

            @Override
            public void visit(Code code) {
                label.append(code.getLiteral());
            }
        });

        var target = link.getDestination();
        return target.equals(label.toString())
                || target.equals("http://" + label)
                || target.equals("https://" + label)
                || target.equals("mailto:" + label);
    }

    /**
     * Turns links that show their target back into text when that target has a protocol that is not allowed, such as
     * {@code file:///etc/passwd}. Sanitizing would otherwise render them as links with an empty target.
     */
    private static final class DisallowedAutolinksAsText extends AbstractVisitor implements PostProcessor {
        @Override
        public Node process(Node node) {
            node.accept(this);
            return node;
        }

        @Override
        public void visit(Link link) {
            var target = link.getDestination();
            if (target.isEmpty() || !showsItsTarget(link) || !URL_SANITIZER.sanitizeLinkUrl(target).isEmpty()) {
                return;
            }

            var child = link.getFirstChild();
            while (child != null) {
                var next = child.getNext();
                link.insertBefore(child);
                child = next;
            }
            link.unlink();
        }
    }

    /**
     * Labels the links from a footnote back to its references in German, as the frontend renderer does. The footnote
     * extension writes English labels and marks each link with its reference, such as {@code 1} or {@code 1-2} for
     * the second reference to footnote 1.
     */
    private static final class FootnoteBackLinkLabels implements AttributeProvider {
        @Override
        public void setAttributes(Node node, String tagName, Map<String, String> attributes) {
            var reference = attributes.get("data-footnote-backref-idx");
            if (!"a".equals(tagName) || reference == null || !attributes.containsKey("aria-label")) {
                return;
            }

            var parts = reference.split("-", 2);
            attributes.put("aria-label", parts.length == 1
                    ? "Zurück zum Verweis auf Fußnote " + parts[0]
                    : "Zurück zum " + parts[1] + ". Verweis auf Fußnote " + parts[0]);
        }
    }

    private static final class ImageAltTextRenderer implements NodeRenderer {
        private final HtmlNodeRendererContext context;

        private ImageAltTextRenderer(@Nonnull HtmlNodeRendererContext context) {
            this.context = context;
        }

        @Override
        public Set<Class<? extends Node>> getNodeTypes() {
            return Set.of(Image.class);
        }

        @Override
        public void render(Node node) {
            var altText = new AltTextCollector();
            node.accept(altText);
            context.getWriter().text(altText.toString());
        }
    }

    private static final class PlainTextRenderer implements NodeRenderer {
        private final TextContentNodeRendererContext context;

        private PlainTextRenderer(@Nonnull TextContentNodeRendererContext context) {
            this.context = context;
        }

        @Override
        public Set<Class<? extends Node>> getNodeTypes() {
            return Set.of(Link.class, Image.class, Code.class, FootnoteReference.class, FootnoteDefinition.class);
        }

        @Override
        public void render(Node node) {
            var writer = context.getWriter();
            switch (node) {
                case Link link -> {
                    renderChildren(link);
                    if (!showsItsTarget(link) && !link.getDestination().isBlank()) {
                        writer.write(" (" + link.getDestination() + ")");
                    }
                }
                case Code code -> writer.write(code.getLiteral());
                case FootnoteReference reference -> writer.write("[" + reference.getLabel() + "]");
                case FootnoteDefinition definition -> {
                    writer.write("[" + definition.getLabel() + "] ");
                    renderChildren(definition);
                }
                // The alternative text, as in the HTML output.
                default -> renderChildren(node);
            }
        }

        private void renderChildren(@Nonnull Node parent) {
            var child = parent.getFirstChild();
            while (child != null) {
                var next = child.getNext();
                context.render(child);
                child = next;
            }
        }
    }

    private static final class AltTextCollector extends AbstractVisitor {
        private final StringBuilder altText = new StringBuilder();

        @Override
        public void visit(Text text) {
            altText.append(text.getLiteral());
        }

        @Override
        public void visit(Code code) {
            altText.append(code.getLiteral());
        }

        @Override
        public void visit(SoftLineBreak softLineBreak) {
            altText.append('\n');
        }

        @Override
        public void visit(HardLineBreak hardLineBreak) {
            altText.append('\n');
        }

        @Override
        public String toString() {
            return altText.toString();
        }
    }
}
