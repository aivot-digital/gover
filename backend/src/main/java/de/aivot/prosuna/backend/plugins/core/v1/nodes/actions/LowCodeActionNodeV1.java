package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.prosuna.backend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.prosuna.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.prosuna.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.elements.form.content.AlertContentElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.prosuna.backend.elements.utils.ElementPOJOMapper;
import de.aivot.prosuna.backend.enums.AlertType;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.javascript.models.JavascriptCode;
import de.aivot.prosuna.backend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionType;
import de.aivot.prosuna.backend.process.enums.ProcessNodeType;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionSummaryContext;
import de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodePort;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.process.services.ProcessDataService;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.detail;

@Component
public class LowCodeActionNodeV1 implements ProcessNodeDefinition<LowCodeActionNodeV1.LowCodeActionNodeConfig> {
    public static final String NODE_KEY = "js";

    private static final String PORT_NAME = "output";

    private static final String CODE_FIELD_KEY = "js_code";
    private static final String DEFAULT_CODE = """
            // Bestehende Vorgangsdaten übernehmen und ergänzen oder überschreiben.
            ({
                ...$,
            });
            """;
    private final JavascriptEngineFactoryService javascriptEngineFactoryService;

    public LowCodeActionNodeV1(JavascriptEngineFactoryService javascriptEngineFactoryService) {
        this.javascriptEngineFactoryService = javascriptEngineFactoryService;
    }

    @Nonnull
    @Override
    public String generateExecutionSummary(@Nonnull ProcessNodeExecutionSummaryContext<LowCodeActionNodeV1.LowCodeActionNodeConfig> context) {
        var summary = new ExecutionSummaryMarkdown(context);
        return "Die Low-Code-Verarbeitung wurde abgeschlossen."
                + detail("Kurzbeschreibung", context.thisNode().getDescription());
    }

    @Nonnull
    @Override
    public String getComponentKey() {
        return NODE_KEY;
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
    public ProcessNodeType getType() {
        return ProcessNodeType.Action;
    }

    @Nonnull
    @Override
    public ProcessNodeExecutionType[] getExecutionTypes() {
        return new ProcessNodeExecutionType[]{ProcessNodeExecutionType.Automatic};
    }

    @Nonnull
    @Override
    public String getName() {
        return "Low-Code ausführen";
    }

    @Nonnull
    @Override
    public String getAbstract() {
        return "Verarbeitet die Vorgangsdaten mit einem eigenen Low-Code-Skript in JavaScript.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie dieses Prozesselement?

                Verwenden Sie dieses Prozesselement, um Vorgangsdaten automatisch mit einem eigenen Low-Code-Skript in JavaScript zu verarbeiten. Es eignet sich für besondere Regeln oder Verarbeitungsschritte, die sich mit den vorhandenen Prozesselementen und No-Code-Ausdrücken nicht ausreichend abbilden lassen. Für die Einrichtung benötigen Sie JavaScript-Kenntnisse.

                # Was richten Sie ein?

                Hinterlegen Sie Ihr Skript im Feld „Low-Code-Skript (JavaScript)“. Es kann Daten aus drei Ebenen lesen:

                - `$` ist der Zugriff auf die Vorgangsdatenebene, zum Beispiel `$.person.name`.
                - `$$` ist der Zugriff auf die geschützte Vorgangsdatenebene, zum Beispiel `$$.caseNumber` für die Vorgangskennung.
                - `_` ist der Zugriff auf die Elementdatenebene, zum Beispiel `_.pruefung.ergebnis` für das Ergebnis eines vorherigen Elements mit dem Datenschlüssel `pruefung`.

                Wenn Sie einen neuen Vorgangsdatenstand übernehmen möchten, verwenden Sie ein Objekt als Ergebnis des letzten Ausdrucks. Der vorbelegte Code verwendet `...$`, um die bisherigen Vorgangsdaten in dieses Objekt zu übernehmen. Ergänzen oder überschreiben Sie darin die gewünschten Werte.

                # Was passiert bei der Ausführung?

                Prosuna führt das Skript automatisch auf dem Server aus. Das Ergebnisobjekt ersetzt die bisherigen Vorgangsdaten vollständig. Alle Werte, die weitere Schritte benötigen, müssen deshalb darin enthalten sein. Nicht enthaltene Felder werden aus den Vorgangsdaten entfernt.

                Ergibt der letzte Ausdruck `null` oder `undefined`, bleiben die bisherigen Vorgangsdaten bestehen. Änderungen nur an der JavaScript-Variablen `$` reichen für die Übernahme nicht aus; maßgeblich ist das Ergebnis des letzten Ausdrucks.

                Nach erfolgreicher Verarbeitung wird der Vorgang über den Ausgang „Low-Code-Verarbeitung abgeschlossen“ fortgesetzt. Tritt ein Fehler bei der Verarbeitung auf, schlägt die Aufgabe fehl.

                # Welches Ergebnis steht danach bereit?

                Nachfolgende Schritte arbeiten mit dem übernommenen Vorgangsdatenstand. Im Vorgangsverlauf steht, dass die Low-Code-Verarbeitung abgeschlossen wurde, ergänzt um die Kurzbeschreibung des Elements. Hinterlegen Sie dort eine verständliche Erklärung der fachlichen Verarbeitung. Die Änderungen an den Vorgangsdaten können Sie mit dem vorherigen Datenstand vergleichen.

                Bei erfolgreicher Verarbeitung werden außerdem die Skriptausgaben als Elementdaten gespeichert: `stdout` enthält die Standardausgabe, beispielsweise aus `console.log`, und `stderr` die Fehlerausgabe, beispielsweise aus `console.error`. Diese Ausgaben dienen der technischen Untersuchung.

                # Wie sieht ein Beispiel aus?

                Die Vorgangsdaten enthalten `anzahl: 3` und `einzelpreis: 10`. Dieses Skript ergänzt den Gesamtbetrag und übernimmt die übrigen Angaben:

                ```js
                ({
                    ...$,
                    gesamtbetrag: $.anzahl * $.einzelpreis
                });
                ```

                Danach enthalten die Vorgangsdaten zusätzlich `gesamtbetrag: 30`. Für eine solche einfache Berechnung eignet sich auch „No-Code ausführen“; das Beispiel zeigt, wie Sie bisherige Angaben in einem Skript erhalten und ergänzen.

                # Was ist zu beachten?

                Mit `...$` übernehmen Sie die oberste Ebene der bisherigen Vorgangsdaten. Wenn Sie ein darin enthaltenes Objekt ersetzen, übernehmen Sie auch dessen bisherige Angaben ausdrücklich, sofern diese erhalten bleiben sollen. Ein leeres Ergebnisobjekt `({})` entfernt alle bisherigen Vorgangsdaten.

                Das Skript wird unmittelbar ausgeführt. Ein alleinstehendes `return` auf der obersten Skriptebene ist deshalb nicht zulässig. Wenn Sie das Ergebnisobjekt unmittelbar wie im Beispiel im Skript notieren, umschließen Sie es mit runden Klammern. Nach diesem Ausdruck darf keine weitere Anweisung folgen, die das Skriptergebnis verändert; ein nachfolgendes `console.log` ergibt beispielsweise `undefined`.

                Für die JavaScript-Ausführung stehen die Prosuna-Funktionen der Laufzeitumgebung bereit. Browser-Funktionen und externe JavaScript-Pakete sind nicht allgemein verfügbar. Prüfen Sie Ihr Skript mit passenden Beispieldaten in einem Testvorgang.
                """;
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionConfigurationLayoutContext context) throws ResponseException {
        try {
            ConfigLayoutElement layout = ElementPOJOMapper.createFromPOJO(LowCodeActionNodeConfig.class);

            var processDataNotice = new AlertContentElement();
            processDataNotice.setId("process-data-replacement-notice");
            processDataNotice.setText("Das Ergebnisobjekt ersetzt die bisherigen Vorgangsdaten vollständig. Geben Sie alle Werte zurück, die weitere Schritte benötigen. Nicht enthaltene Felder werden aus den Vorgangsdaten entfernt.");
            processDataNotice.setAlertType(AlertType.Info);
            layout.insertChildAfter(processDataNotice, CODE_FIELD_KEY);

            return layout;
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(e, "Die Einstellungen für „Low-Code ausführen“ konnten nicht geladen werden.");
        }
    }

    @Nonnull
    @Override
    public AuthoredElementValues getInitialConfiguration() {
        return new AuthoredElementValues()
                .putLiteral(CODE_FIELD_KEY, DEFAULT_CODE);
    }

    @Nonnull
    @Override
    public Class<LowCodeActionNodeConfig> getNodeConfigurationClass() {
        return LowCodeActionNodeConfig.class;
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts(@Nonnull LowCodeActionNodeConfig configuration) {
        return List.of(
                new ProcessNodePort(
                        PORT_NAME,
                        "Low-Code-Verarbeitung abgeschlossen",
                        "Der Vorgang wird hier fortgesetzt, nachdem die Verarbeitung mit dem Low-Code-Skript erfolgreich abgeschlossen wurde."
                )
        );
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<LowCodeActionNodeConfig> context) throws ProcessNodeExecutionException {
        var configuration = context.getConfigurationOfExecutingNode();

        var code = configuration.code == null ? "" : configuration.code;

        var jsCode = new JavascriptCode()
                .setCode(code);

        try (var engine = javascriptEngineFactoryService.getEngine()) {
            ProcessDataService
                    .fillJsEngineWithData(context.getCurrentProcessExecutionData(), engine);

            try {
                var result = engine
                        .evaluateCode(jsCode);

                return new ProcessNodeExecutionResultTaskCompleted()
                        .setViaPort(PORT_NAME)
                        .setProcessData(result.asMap())
                        .setNodeData(Map.of(
                                "stdout", result.getStdOutput(),
                                "stderr", result.getErrOutput()
                        ));
            } catch (Exception e) {
                throw new ProcessNodeExecutionExceptionUnknown(
                        e,
                        "Die Verarbeitung mit dem Low-Code-Skript konnte nicht abgeschlossen werden."
                );
            }
        } catch (ProcessNodeExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Die Umgebung für die Low-Code-Ausführung konnte nicht verwendet werden."
            );
        }
    }

    @LayoutElementPOJOBinding(id = NODE_KEY, type = ElementType.ConfigLayout)
    public static class LowCodeActionNodeConfig {
        public static final String CODE = CODE_FIELD_KEY;

        @InputElementPOJOBinding(id = CODE, type = ElementType.CodeInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Low-Code-Skript (JavaScript)"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Das Ergebnis des letzten Ausdrucks bestimmt den neuen Vorgangsdatenstand. Mit „...$“ im Ergebnisobjekt übernehmen Sie die bisherigen Vorgangsdaten und können sie ergänzen oder überschreiben."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String code;
    }
}
