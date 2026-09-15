package de.aivot.prosuna.backend.plugins.form;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class FormModuleWebMvcConfiguration implements WebMvcConfigurer {
    private final ProsunaConfig prosunaConfig;

    public FormModuleWebMvcConfiguration(ProsunaConfig prosunaConfig) {
        this.prosunaConfig = prosunaConfig;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry
                .addInterceptor(new FormModuleInterceptor(prosunaConfig))
                .addPathPatterns(
                        "/api/forms/v1/**",
                        "/api/public/forms/**",
                        "/api/public/form/**"
                );
    }

    private record FormModuleInterceptor(@Nonnull ProsunaConfig prosunaConfig) implements HandlerInterceptor {
        @Override
        public boolean preHandle(@Nonnull HttpServletRequest request,
                                 @Nonnull HttpServletResponse response,
                                 @Nonnull Object handler) throws Exception {
            if (!prosunaConfig.isFormModuleEnabled()) {
                // Disabled public form URLs should look absent so external callers cannot depend on a hidden module.
                throw ResponseException.notFound();
            }

            return true;
        }
    }
}
