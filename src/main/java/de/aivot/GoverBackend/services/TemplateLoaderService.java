package de.aivot.GoverBackend.services;


import de.aivot.GoverBackend.services.pdf.NumberFormatDialect;
import de.aivot.GoverBackend.services.pdf.QrCodeDialect;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

import java.util.Map;

public class TemplateLoaderService {
    public String processTemplate(
            String templateName,
            Map<String, Object> templateData,
            TemplateMode mode
    ) {
        var templateEngine = new TemplateEngine();
        templateEngine.addDialect(new Java8TimeDialect());
        templateEngine.addDialect(new NumberFormatDialect());
        templateEngine.addDialect(new QrCodeDialect());

        templateEngine.addTemplateResolver(getAssetTemplateResolver(mode));
        templateEngine.addTemplateResolver(getFileTemplateResolver(mode));
        templateEngine.addTemplateResolver(getClassLoaderTemplateResolver(mode));

        var context = new Context();
        context.setVariables(templateData);

        return templateEngine.process(templateName, context);
    }

    private ITemplateResolver getAssetTemplateResolver(TemplateMode mode) {
        var resolver = new FileTemplateResolver();

        resolver.setPrefix("./data/assets/");
        resolver.setTemplateMode(mode);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCheckExistence(true);
        resolver.setOrder(0);

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
