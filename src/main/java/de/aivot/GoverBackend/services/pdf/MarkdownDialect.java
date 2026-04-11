package de.aivot.GoverBackend.services.pdf;

import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nullable;
import org.commonmark.Extension;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.footnotes.FootnotesExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.task.list.items.TaskListItemsExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.dialect.AbstractDialect;
import org.thymeleaf.dialect.IExpressionObjectDialect;
import org.thymeleaf.expression.IExpressionObjectFactory;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class MarkdownDialect extends AbstractDialect implements IExpressionObjectDialect {
    public static final String ID = "markdown";

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
            .escapeHtml(true)
            .sanitizeUrls(true)
            .build();

    public MarkdownDialect() {
        super(ID);
    }

    public String render(@Nullable String markdown) {
        if (StringUtils.isNullOrEmpty(markdown)) {
            return "";
        }

        return RENDERER.render(PARSER.parse(markdown));
    }

    @Override
    public IExpressionObjectFactory getExpressionObjectFactory() {
        return new IExpressionObjectFactory() {

            @Override
            public Set<String> getAllExpressionObjectNames() {
                return Collections.singleton(ID);
            }

            @Override
            public Object buildObject(IExpressionContext context, String expressionObjectName) {
                return new MarkdownDialect();
            }

            @Override
            public boolean isCacheable(String expressionObjectName) {
                return true;
            }
        };
    }
}
