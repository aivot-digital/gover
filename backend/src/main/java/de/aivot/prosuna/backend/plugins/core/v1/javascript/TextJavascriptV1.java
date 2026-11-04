package de.aivot.prosuna.backend.plugins.core.v1.javascript;

import de.aivot.prosuna.backend.javascript.providers.JavascriptFunctionProvider;
import de.aivot.prosuna.backend.markdown.utils.MarkdownRenderer;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.graalvm.polyglot.HostAccess;
import org.springframework.stereotype.Service;

@Service
public class TextJavascriptV1 implements JavascriptFunctionProvider {
    @Override
    public @Nonnull String getComponentKey() {
        return "text";
    }

    @Nonnull
    @Override
    public String getComponentVersion() {
        return "1.0.0";
    }

    @Nonnull
    @Override
    public String getParentPluginKey() {
        return CorePlugin.PLUGIN_KEY;
    }

    @Nonnull
    @Override
    public String getName() {
        return "Textfunktionen";
    }

    @Nonnull
    @Override
    public String getAbstract() {
        return "Dieses Modul stellt Funktionen zur Umwandlung von Texten bereit.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return """
                Stellt JavaScript-Hilfsfunktionen für Texte bereit.

                Werte aus Markdown-Eingaben lassen sich in einfachen Text umwandeln. Formatierungszeichen und Escape-Zeichen entfallen, Links werden als Text mit ihrer Adresse geschrieben, Links mit nicht erlaubter Adresse nur als Text. Die Variablenauswahl nutzt diese Funktion, wenn ein Wert aus einer Markdown-Eingabe als Text eingefügt wird.
                """;
    }

    @Override
    public String[] getMethodTypeDefinitions() {
        return new String[]{
                "fromMarkdown(markdown: string | null | undefined): string;"
        };
    }

    /** Returns the text a reader sees, as in the plain-text part of mails. A missing value becomes an empty text. */
    @HostAccess.Export
    @Nonnull
    public String fromMarkdown(@Nullable String markdown) {
        return MarkdownRenderer.renderPlainText(markdown);
    }
}
