package de.aivot.prosuna.backend.services;


import de.aivot.prosuna.backend.services.pdf.NumberFormatDialect;
import de.aivot.prosuna.backend.services.pdf.QrCodeDialect;
import de.aivot.prosuna.backend.services.pdf.MarkdownDialect;
import jakarta.annotation.Nullable;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.context.Context;
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.DefaultTemplateResolver;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Renders the mail and PDF templates of the application.
 * <p>
 * Templates are only resolved by name from the bundled classpath templates and from the {@code ./templates/} directory,
 * which operators can use to override them. Asset storage must never be a template source by name: assets can be
 * created at chosen paths by staff members with ordinary permissions, and the templates are evaluated with Spring
 * expressions and the data of the rendered mail or document. Content that was explicitly selected as a template, such
 * as the custom PDF template of a form, is rendered with {@link #processTemplateContent(String, Map, TemplateMode)}.
 * <p>
 * The file and classpath resolvers append template names to their directory without any containment check, so a name
 * with parent segments, such as a fragment reference {@code ~{../data/assets/...}}, would read any file or classpath
 * resource the backend can access. Template names that could leave the template directories are therefore never
 * resolved.
 */
public class TemplateLoaderService {
    private static final String CONTENT_TEMPLATE_NAME = "prosuna-template-content";
    private static final String CONTENT_FRAGMENT_DIRECTORY = "form-parts/";

    public String processTemplate(
            String templateName,
            Map<String, Object> templateData,
            TemplateMode mode
    ) {
        var templateEngine = createTemplateEngine(mode, TemplateLoaderService::isTemplateNameWithinTemplateDirectory);
        return templateEngine.process(templateName, createContext(templateData));
    }

    /**
     * Renders the given template content. Fragment references in the content only resolve to the bundled form
     * fragments in {@code form-parts/}, including their overrides in {@code ./templates/}. The content can reuse these
     * fragments but cannot load other templates, files or assets.
     */
    public String processTemplateContent(
            String templateContent,
            Map<String, Object> templateData,
            TemplateMode mode
    ) {
        var templateEngine = createTemplateEngine(mode, TemplateLoaderService::isContentFragmentName);
        templateEngine.addTemplateResolver(getContentTemplateResolver(templateContent, mode));
        return templateEngine.process(CONTENT_TEMPLATE_NAME, createContext(templateData));
    }

    /**
     * Returns the template name relative to the template directory, or {@code null} if the name could leave it.
     */
    @Nullable
    static String normalizeTemplateName(@Nullable String templateName) {
        if (templateName == null || templateName.isBlank() || templateName.contains("\\")) {
            return null;
        }

        var segments = new ArrayList<String>();
        for (var segment : templateName.split("/")) {
            if (segment.isEmpty() || segment.equals(".")) {
                continue;
            }
            if (segment.equals("..")) {
                return null;
            }
            segments.add(segment);
        }

        return segments.isEmpty() ? null : String.join("/", segments);
    }

    private static boolean isTemplateNameWithinTemplateDirectory(String templateName) {
        return normalizeTemplateName(templateName) != null;
    }

    private static boolean isContentFragmentName(String templateName) {
        var normalizedName = normalizeTemplateName(templateName);
        return normalizedName != null && normalizedName.startsWith(CONTENT_FRAGMENT_DIRECTORY);
    }

    private SpringTemplateEngine createTemplateEngine(TemplateMode mode, Predicate<String> isPermittedTemplateName) {
        var templateEngine = new SpringTemplateEngine();
        templateEngine.addDialect(new Java8TimeDialect());
        templateEngine.addDialect(new NumberFormatDialect());
        templateEngine.addDialect(new QrCodeDialect());
        templateEngine.addDialect(new MarkdownDialect());

        templateEngine.addTemplateResolver(getFileTemplateResolver(mode, isPermittedTemplateName));
        templateEngine.addTemplateResolver(getClassLoaderTemplateResolver(mode, isPermittedTemplateName));

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

    private ITemplateResolver getFileTemplateResolver(TemplateMode mode, Predicate<String> isPermittedTemplateName) {
        var resolver = new FileTemplateResolver() {
            // Resolvable patterns cannot restrict the names, because a pattern such as "form-parts/*" also matches
            // "form-parts/../../secret". Check each name before the resolver appends it to its directory.
            @Override
            protected boolean computeResolvable(IEngineConfiguration configuration,
                                                String ownerTemplate,
                                                String template,
                                                Map<String, Object> templateResolutionAttributes) {
                return isPermittedTemplateName.test(template) &&
                        super.computeResolvable(configuration, ownerTemplate, template, templateResolutionAttributes);
            }
        };

        resolver.setPrefix("./templates/");
        resolver.setTemplateMode(mode);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCheckExistence(true);
        resolver.setOrder(0);

        return resolver;
    }

    private ITemplateResolver getClassLoaderTemplateResolver(TemplateMode mode, Predicate<String> isPermittedTemplateName) {
        var resolver = new ClassLoaderTemplateResolver() {
            @Override
            protected boolean computeResolvable(IEngineConfiguration configuration,
                                                String ownerTemplate,
                                                String template,
                                                Map<String, Object> templateResolutionAttributes) {
                return isPermittedTemplateName.test(template) &&
                        super.computeResolvable(configuration, ownerTemplate, template, templateResolutionAttributes);
            }
        };

        resolver.setPrefix("templates/");
        resolver.setTemplateMode(mode);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCheckExistence(true);
        resolver.setOrder(1);

        return resolver;
    }
}
