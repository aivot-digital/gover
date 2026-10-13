package de.aivot.prosuna.backend.services;


import de.aivot.prosuna.backend.services.pdf.NumberFormatDialect;
import de.aivot.prosuna.backend.services.pdf.QrCodeDialect;
import de.aivot.prosuna.backend.services.pdf.MarkdownDialect;
import org.thymeleaf.context.Context;
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.DefaultTemplateResolver;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

import java.util.Map;
import java.util.Set;

/**
 * Renders the mail and PDF templates of the application.
 * <p>
 * Templates are only resolved by name from the bundled classpath templates and from the {@code ./templates/} directory,
 * which operators can use to override them. Asset storage must never be a template source by name: assets can be
 * created at chosen paths by staff members with ordinary permissions, and the templates are evaluated with Spring
 * expressions and the data of the rendered mail or document. Content that was explicitly selected as a template, such
 * as the custom PDF template of a form, is rendered with {@link #processTemplateContent(String, Map, TemplateMode)}.
 */
public class TemplateLoaderService {
    private static final String CONTENT_TEMPLATE_NAME = "prosuna-template-content";

    public String processTemplate(
            String templateName,
            Map<String, Object> templateData,
            TemplateMode mode
    ) {
        var templateEngine = createTemplateEngine(mode);
        return templateEngine.process(templateName, createContext(templateData));
    }

    /**
     * Renders the given template content. Fragment references in the content still resolve to the regular template
     * sources, so the content can reuse the bundled templates but cannot load further templates from asset storage.
     */
    public String processTemplateContent(
            String templateContent,
            Map<String, Object> templateData,
            TemplateMode mode
    ) {
        var templateEngine = createTemplateEngine(mode);
        templateEngine.addTemplateResolver(getContentTemplateResolver(templateContent, mode));
        return templateEngine.process(CONTENT_TEMPLATE_NAME, createContext(templateData));
    }

    private SpringTemplateEngine createTemplateEngine(TemplateMode mode) {
        var templateEngine = new SpringTemplateEngine();
        templateEngine.addDialect(new Java8TimeDialect());
        templateEngine.addDialect(new NumberFormatDialect());
        templateEngine.addDialect(new QrCodeDialect());
        templateEngine.addDialect(new MarkdownDialect());

        templateEngine.addTemplateResolver(getFileTemplateResolver(mode));
        templateEngine.addTemplateResolver(getClassLoaderTemplateResolver(mode));

        return templateEngine;
    }

    private Context createContext(Map<String, Object> templateData) {
        var context = new Context();
        context.setVariables(templateData);
        return context;
    }

    private ITemplateResolver getContentTemplateResolver(String templateContent, TemplateMode mode) {
        var resolver = new DefaultTemplateResolver();

        resolver.setTemplate(templateContent);
        resolver.setTemplateMode(mode);
        resolver.setResolvablePatterns(Set.of(CONTENT_TEMPLATE_NAME));
        // Precede the file and classpath resolvers, so a template file with the same name cannot replace the content.
        resolver.setOrder(-1);

        return resolver;
    }

    private ITemplateResolver getFileTemplateResolver(TemplateMode mode) {
        var resolver = new FileTemplateResolver();

        resolver.setPrefix("./templates/");
        resolver.setTemplateMode(mode);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCheckExistence(true);
        resolver.setOrder(0);

        return resolver;
    }

    private ITemplateResolver getClassLoaderTemplateResolver(TemplateMode mode) {
        var resolver = new ClassLoaderTemplateResolver();

        resolver.setPrefix("templates/");
        resolver.setTemplateMode(mode);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCheckExistence(true);
        resolver.setOrder(1);

        return resolver;
    }
}
