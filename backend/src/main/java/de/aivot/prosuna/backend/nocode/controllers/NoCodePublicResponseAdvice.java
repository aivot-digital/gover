package de.aivot.prosuna.backend.nocode.controllers;

import de.aivot.prosuna.backend.nocode.services.NoCodeEditorMetadataCleaner;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Covers initial layouts, task views, derived data and error payloads at the public JSON boundary.
 */
@ControllerAdvice
public class NoCodePublicResponseAdvice implements ResponseBodyAdvice<Object> {
    private final NoCodeEditorMetadataCleaner cleaner;

    public NoCodePublicResponseAdvice(@Nonnull NoCodeEditorMetadataCleaner cleaner) {
        this.cleaner = cleaner;
    }

    @Override
    public boolean supports(@Nonnull MethodParameter returnType, @Nonnull Class<? extends HttpMessageConverter<?>> converterType) {
        return JacksonJsonHttpMessageConverter.class.isAssignableFrom(converterType);
    }

    @Override
    @Nullable
    public Object beforeBodyWrite(@Nullable Object body, @Nonnull MethodParameter returnType,
                                  @Nonnull MediaType contentType, @Nonnull Class<? extends HttpMessageConverter<?>> converterType,
                                  @Nonnull ServerHttpRequest request, @Nonnull ServerHttpResponse response) {
        // Use the application path (without a possible servlet context prefix), not forwarded headers.
        var path = request.getURI().getPath();
        if (request instanceof org.springframework.http.server.ServletServerHttpRequest servletRequest) {
            var servlet = servletRequest.getServletRequest();
            path = servlet.getRequestURI().substring(servlet.getContextPath().length());
        }
        return body != null && path.startsWith("/api/public/") ? cleaner.clean(body) : body;
    }
}
