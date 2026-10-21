package de.aivot.prosuna.backend.markdown.utils;

import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.commonmark.Extension;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.footnotes.FootnotesExtension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.task.list.items.TaskListItemsExtension;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Code;
import org.commonmark.node.HardLineBreak;
import org.commonmark.node.Image;
import org.commonmark.node.Node;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.DefaultUrlSanitizer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlRenderer;

import java.util.List;
import java.util.Set;

/**
 * Renders Markdown to HTML for every backend output channel: PDF documents, HTML template slots, e-mails and
 * mailbox messages.
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

    private static final List<Extension> EXTENSIONS = List.of(
            AutolinkExtension.create(),
            FootnotesExtension.create(),
            TablesExtension.create(),
            StrikethroughExtension.builder()
                    .requireTwoTildes(false)
                    .build(),
            TaskListItemsExtension.create()
    );
    private static final Parser PARSER = Parser.builder()
            .extensions(EXTENSIONS)
            .build();
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder()
            .extensions(EXTENSIONS)
            .nodeRendererFactory(ImageAltTextRenderer::new)
            // The rich text editor and the frontend renderer keep single line breaks as well.
            .softbreak("<br />\n")
            .escapeHtml(true)
            .sanitizeUrls(true)
            .urlSanitizer(new DefaultUrlSanitizer(ALLOWED_URL_PROTOCOLS))
            // Matches the URL normalization of the frontend renderer, for example for umlauts in link targets.
            .percentEncodeUrls(true)
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
