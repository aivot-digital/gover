package de.aivot.prosuna.backend.services.pdf;

import de.aivot.prosuna.backend.markdown.utils.MarkdownRenderer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.dialect.AbstractDialect;
import org.thymeleaf.dialect.IExpressionObjectDialect;
import org.thymeleaf.expression.IExpressionObjectFactory;

import java.util.Collections;
import java.util.Set;

/** Exposes {@link MarkdownRenderer} to Thymeleaf templates as {@code #markdown}. */
public class MarkdownDialect extends AbstractDialect implements IExpressionObjectDialect {
    public static final String ID = "markdown";

    public MarkdownDialect() {
        super(ID);
    }

    @Nonnull
    public String render(@Nullable String markdown) {
        return MarkdownRenderer.renderHtml(markdown);
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
