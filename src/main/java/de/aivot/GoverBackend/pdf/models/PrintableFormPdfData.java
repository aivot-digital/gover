package de.aivot.GoverBackend.pdf.models;

import de.aivot.GoverBackend.elements.models.elements.layout.FormLayoutElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.UUID;

/**
 * Minimal form data required by the printable blank-form PDF templates.
 */
public class PrintableFormPdfData {
    private String slug;
    private String internalTitle;
    private Integer version;
    private String publicTitle;
    private FormLayoutElement rootElement;
    private UUID pdfTemplateKey;

    @Nullable
    public String getSlug() {
        return slug;
    }

    public PrintableFormPdfData setSlug(@Nullable String slug) {
        this.slug = slug;
        return this;
    }

    @Nullable
    public String getInternalTitle() {
        return internalTitle;
    }

    public PrintableFormPdfData setInternalTitle(@Nullable String internalTitle) {
        this.internalTitle = internalTitle;
        return this;
    }

    @Nullable
    public Integer getVersion() {
        return version;
    }

    public PrintableFormPdfData setVersion(@Nullable Integer version) {
        this.version = version;
        return this;
    }

    @Nullable
    public String getPublicTitle() {
        return publicTitle;
    }

    public PrintableFormPdfData setPublicTitle(@Nullable String publicTitle) {
        this.publicTitle = publicTitle;
        return this;
    }

    @Nullable
    public FormLayoutElement getRootElement() {
        return rootElement;
    }

    public PrintableFormPdfData setRootElement(@Nullable FormLayoutElement rootElement) {
        this.rootElement = rootElement;
        return this;
    }

    @Nullable
    public UUID getPdfTemplateKey() {
        return pdfTemplateKey;
    }

    public PrintableFormPdfData setPdfTemplateKey(@Nullable UUID pdfTemplateKey) {
        this.pdfTemplateKey = pdfTemplateKey;
        return this;
    }

    @Nonnull
    public static PrintableFormPdfData fromLegacyForm(@Nonnull de.aivot.GoverBackend.form.entities.VFormVersionWithDetailsEntity form) {
        return new PrintableFormPdfData()
                .setSlug(form.getSlug())
                .setInternalTitle(form.getInternalTitle())
                .setVersion(form.getVersion())
                .setPublicTitle(form.getPublicTitle())
                .setRootElement(form.getRootElement())
                .setPdfTemplateKey(form.getPdfTemplateKey());
    }
}
