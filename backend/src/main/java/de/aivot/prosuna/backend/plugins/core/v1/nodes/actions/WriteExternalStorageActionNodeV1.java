package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.prosuna.backend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.prosuna.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.ReplicatingContainerLayoutElementElementPOJOBinding;
import de.aivot.prosuna.backend.elements.enums.InputMode;
import de.aivot.prosuna.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.EffectiveElementValues;
import de.aivot.prosuna.backend.elements.models.elements.ElementVisibilityFunctions;
import de.aivot.prosuna.backend.elements.models.elements.form.content.HeadlineContentElement;
import de.aivot.prosuna.backend.elements.models.elements.form.content.RichTextContentElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.*;
import de.aivot.prosuna.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.EffectiveReplicatingContainerLayoutElementValue;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.prosuna.backend.elements.models.input.DynamicTextPolicy;
import de.aivot.prosuna.backend.elements.utils.ElementPOJOMapper;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.nocode.models.NoCodeExpression;
import de.aivot.prosuna.backend.nocode.models.NoCodeReference;
import de.aivot.prosuna.backend.nocode.models.NoCodeStaticValue;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.plugins.core.v1.operators.common.NoCodeEqualsOperator;
import de.aivot.prosuna.backend.plugins.core.v1.operators.object.NoCodeObjectGetOperator;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentSetEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionLogLevel;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionType;
import de.aivot.prosuna.backend.process.enums.ProcessNodeType;
import de.aivot.prosuna.backend.process.enums.TemplateOutputFormat;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionMissingValue;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionSummaryContext;
import de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodeOutput;
import de.aivot.prosuna.backend.process.models.ProcessNodePort;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeConfigurationValidationContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.process.services.ProcessInstanceAttachmentService;
import de.aivot.prosuna.backend.process.services.ProcessInstanceAttachmentSetService;
import de.aivot.prosuna.backend.process.services.TemplateRenderService;
import de.aivot.prosuna.backend.storage.entities.StorageProviderEntity;
import de.aivot.prosuna.backend.storage.enums.StorageProviderType;
import de.aivot.prosuna.backend.storage.models.StorageItemMetadata;
import de.aivot.prosuna.backend.storage.models.StorageProviderMetadataAttribute;
import de.aivot.prosuna.backend.storage.repositories.StorageProviderRepository;
import de.aivot.prosuna.backend.storage.services.StorageProviderDefinitionService;
import de.aivot.prosuna.backend.storage.services.StorageService;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.text;
import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.section;
import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.detail;
import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.map;
import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.list;
import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.document;

@Component
public class WriteExternalStorageActionNodeV1 implements ProcessNodeDefinition<WriteExternalStorageActionNodeV1.WriteExternalStorageActionNodeConfig> {
    public static final String NODE_KEY = "write_external_storage";

    private static final String PORT_NAME = "output";

    private static final String OUTPUT_NAME_RESULTS = "results";
    private static final String OUTPUT_NAME_STORAGE_PATHS_FROM_ROOT = "storagePathsFromRoot";
    private static final String OUTPUT_NAME_FILE_NAMES = "fileNames";
    private static final String OUTPUT_NAME_COUNT = "count";
    private static final String OUTPUT_RESULTS_TYPE_DEFINITION =
            "Array<{ storageProviderId: number; attachmentSetDataKey: string; targetFolderPath: string; " +
                    "storagePathsFromRoot: Array<string>; fileNames: Array<string>; count: number; }>";

    private static final String METADATA_ATTRIBUTES_PREFIX = "meta__attributes";

    private final TemplateRenderService templateRenderService;
    private final ProcessInstanceAttachmentService processInstanceAttachmentService;
    private final ProcessInstanceAttachmentSetService processInstanceAttachmentSetService;
    private final StorageService storageService;
    private final StorageProviderRepository storageProviderRepository;
    private final StorageProviderDefinitionService storageProviderDefinitionService;

    public WriteExternalStorageActionNodeV1(TemplateRenderService templateRenderService,
                                            ProcessInstanceAttachmentService processInstanceAttachmentService,
                                            ProcessInstanceAttachmentSetService processInstanceAttachmentSetService,
                                            StorageService storageService,
                                            StorageProviderRepository storageProviderRepository,
                                            StorageProviderDefinitionService storageProviderDefinitionService) {
        this.templateRenderService = templateRenderService;
        this.processInstanceAttachmentService = processInstanceAttachmentService;
        this.processInstanceAttachmentSetService = processInstanceAttachmentSetService;
        this.storageService = storageService;
        this.storageProviderRepository = storageProviderRepository;
        this.storageProviderDefinitionService = storageProviderDefinitionService;
    }

    @Nonnull
    @Override
    public String generateExecutionSummary(@Nonnull ProcessNodeExecutionSummaryContext<WriteExternalStorageActionNodeV1.WriteExternalStorageActionNodeConfig> context) {
        var summary = new ExecutionSummaryMarkdown(context);
        var markdown = new StringBuilder("Die Ablage wurde" + summary.completedAt() + " abgeschlossen.");
        var documents = list(summary.metadata(ExecutionSummaryMarkdown.DOCUMENTS));
        if (documents.isEmpty()) {
            if (summary.data(OUTPUT_NAME_COUNT) instanceof Number count && Double.isFinite(count.doubleValue())) {
                if (count.doubleValue() == 0) {
                    markdown.append("\n\nEs wurden keine Anlagen gespeichert.");
                } else if (count.doubleValue() > 0) {
                    markdown.append(detail("Ausgeführte Speicherungen", count));
                }
            }
        }
        for (var entry : documents) {
            var storedDocument = map(entry);
            var fileName = StringUtils.toNullableTrimmedString(storedDocument.get("fileName"));
            var providerName = StringUtils.toNullableTrimmedString(storedDocument.get("providerName"));
            markdown.append("\n\nDie Datei");
            if (fileName != null) {
                markdown.append(" „").append(text(fileName)).append("“");
            }
            markdown.append(" wurde");
            if (providerName != null) {
                markdown.append(" beim Speicheranbieter „").append(text(providerName)).append("“");
            }
            markdown.append(" gespeichert.")
                    .append(detail("Zielpfad", storedDocument.get("path")))
                    .append(detail("Datenschlüssel des Anlagensatzes", storedDocument.get("dataKey")));
            var attachmentKey = StringUtils.toNullableTrimmedString(storedDocument.get("attachmentKey"));
            if (attachmentKey != null) {
                markdown.append(section("Ursprüngliche Anlage", document(attachmentKey, "Ursprüngliche Anlage ansehen")));
            }
        }
        markdown.append(detail("Kurzbeschreibung", context.thisNode().getDescription()));
        return markdown.toString();
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
        return "Anlagen bei Speicheranbieter speichern";
    }

    @Nonnull
    @Override
    public String getAbstract() {
        return "Speichert alle Anlagen ausgewählter Anlagensätze bei den festgelegten Speicheranbietern.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return """
                ### Wofür verwenden Sie dieses Prozesselement?

                Legen Sie Anlagen eines Vorgangs bei einem eingerichteten Speicheranbieter ab, zum Beispiel in einem externen Dateispeicher oder in „Dateien und Medien“. Die ursprünglichen Anlagen bleiben im Vorgang erhalten.

                ### Was richten Sie ein?

                Fügen Sie für jede gewünschte Ablage einen Eintrag hinzu. Wählen Sie darin genau einen Anlagensatz sowie einen beschreibbaren Speicheranbieter und den Zielordner aus. Prosuna ergänzt die Dateinamen automatisch. Der Zielordnerpfad kann Angaben aus dem Vorgang als Vorlagenausdrücke enthalten.

                Mit „Fehlenden oder leeren Anlagensatz überspringen“ legen Sie fest, dass dieser Eintrag ausgelassen werden darf, wenn der Anlagensatz im Vorgang fehlt oder keine Anlagen enthält. Ohne diese Option führen solche Fälle zu einem Fehler.

                Optional aktivieren Sie „Dateinamen anpassen“ und geben einen Basisnamen ohne Dateiendung an. Angaben aus dem Vorgang können über Variablen eingefügt werden. Prosuna übernimmt die ursprüngliche Dateiendung und ergänzt immer eine laufende Nummer, zum Beispiel „Nachweis-1.pdf“.

                Wenn der Speicheranbieter zusätzliche Angaben unterstützt und dafür Felder bereitstellt, können Sie diese unter „Zusätzliche Angaben (Metadaten)“ befüllen. Diese Angaben gelten für alle Dateien desselben Ablageeintrags.

                ### Was passiert bei der Ausführung?

                Prosuna verarbeitet die Einträge nacheinander, ermittelt die zugehörigen Anlagen und setzt die Werte im Zielordnerpfad ein. Fehlende Ordner werden angelegt. Gehören Anlagen zu einer Gruppe, wird deren Gruppierung unterhalb des Zielordners als weitere Ordnerstruktur übernommen.

                Die Dateien werden mit ihrem vorhandenen oder angepassten Namen beim Speicheranbieter gespeichert. Die Nummerierung beginnt für jeden Ablageeintrag bei 1. Im Zielordnerpfad und im eigenen Basisnamen wird „#“ durch die jeweilige Dateinummer ersetzt.

                Wenn alle Einträge verarbeitet oder zulässigerweise übersprungen wurden, setzt Prosuna den Vorgang über „Ablage abgeschlossen“ fort. Dafür entsteht keine Mitarbeitendenaufgabe.

                ### Welches Ergebnis steht danach bereit?

                In den Elementdaten stehen die Ablageergebnisse, die tatsächlichen Speicherpfade und Dateinamen sowie die Anzahl der ausgeführten Dateiablagen bereit. Die Ablageergebnisse ordnen die gespeicherten Dateien ihrem Speicheranbieter und Anlagensatz zu. Übersprungene Einträge sind darin nicht enthalten.

                Der Vorgangsverlauf zeigt die gespeicherten Dateien mit Anbieter und Zielpfad. Ein Link führt jeweils zur ursprünglichen Anlage im Vorgang. Auch das Überspringen optionaler Anlagensätze ist erkennbar.

                ### Wie sieht ein Beispiel aus?

                Der Anlagensatz „Nachweise“ enthält zwei PDFs. Sie wählen den Zielordner `/Vorgänge/{{ $$.caseNumber }}/` und den eigenen Basisnamen „Nachweis“. Prosuna legt die Dateien dort als „Nachweis-1.pdf“ und „Nachweis-2.pdf“ ab. Gehört eine Anlage zur Gruppe „Einkommen“, wird sie im entsprechenden Unterordner gespeichert. Die beiden ursprünglichen Anlagen bleiben im Vorgang erhalten.

                ### Was ist zu beachten?

                Dieses Element verarbeitet vollständige Anlagensätze. Eine einzelne Datei aus einem Dateiverweis können Sie hier derzeit nicht auswählen.

                Eine Datei, die am Zielpfad bereits vorhanden ist, wird überschrieben. Prüfen Sie deshalb Zielordner und Dateinamen vor der Verwendung in einem Testvorgang.

                Bei angepassten Dateinamen wird immer eine laufende Nummer ergänzt, auch wenn Sie selbst „#“ verwenden. Aus dem Basisnamen „Nachweis-#“ kann deshalb „Nachweis-1-1.pdf“ entstehen. Die ursprüngliche Dateiendung bleibt erhalten.

                Die Option zum Überspringen gilt nur für fehlende oder leere Anlagensätze. Bei anderen Fehlern bricht die Aufgabe ab. Zuvor gespeicherte Dateien und angelegte Ordner können dann am Ziel bereits vorhanden sein; die Ablage wird nicht vollständig zurückgenommen.
                """;
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionConfigurationLayoutContext context) throws ResponseException {
        ConfigLayoutElement layout;
        try {
            layout = ElementPOJOMapper.createFromPOJO(WriteExternalStorageActionNodeConfig.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerErrorWithDetails(
                    e,
                    "Die Einstellungen für „Anlagen bei Speicheranbieter speichern“ konnten nicht geladen werden.",
                    null
            );
        }

        layout
                .findChild(WriteExternalStorageConfig.ATTACHMENT_SET_DATA_KEYS_FIELD_ID, ProcessInstanceAttachmentSetSelectElement.class)
                .ifPresent(field -> field
                        .setMinItems(1)
                        .setMaxItems(1));

        layout
                .findChild(WriteExternalStorageConfig.STORAGE_PATH_FIELD_ID, StoragePathSelectorInputElement.class)
                .ifPresent(field -> field.setAllowedStorageProviderTypes(List.of(
                        StorageProviderType.Assets,
                        StorageProviderType.External
                )));

        layout
                .findChild(WriteExternalStorageConfig.FILE_NAME_FIELD_ID, TextInputElement.class)
                .ifPresent(field -> field.setVisibility(ElementVisibilityFunctions
                        .of(NoCodeExpression.of(
                                NoCodeEqualsOperator.OPERATOR_ID,
                                NoCodeReference.of(WriteExternalStorageConfig.CUSTOMIZE_FILE_NAME_FIELD_ID),
                                NoCodeStaticValue.of(true)
                        ))
                        .recalculateReferencedIds()));

        var allStorageProvidersWithMetadata = storageProviderRepository
                .findAllByMetadataAttributesNotEmptyAndReadOnlyStorageIsFalseAndTypeIsIn(List.of(StorageProviderType.Assets, StorageProviderType.External));

        layout
                .findChild(WriteExternalStorageActionNodeConfig.ATTACHMENT_SETS_FIELD_ID, ReplicatingContainerLayoutElement.class)
                .ifPresent(container -> {
                    allStorageProvidersWithMetadata
                            .stream()
                            .filter(this::storageProviderSupportsMetadataAttributes)
                            .forEach(provider -> {
                                var group = new GroupLayoutElement();
                                group.setId(metadataAttributesGroupId(provider.getId()));

                                group.setVisibility(
                                        ElementVisibilityFunctions
                                                .of(
                                                        NoCodeExpression
                                                                .of(
                                                                        NoCodeEqualsOperator.OPERATOR_ID,
                                                                        NoCodeExpression
                                                                                .of(
                                                                                        NoCodeObjectGetOperator.OPERATOR_ID,
                                                                                        NoCodeReference.of(WriteExternalStorageConfig.STORAGE_PATH_FIELD_ID),
                                                                                        NoCodeStaticValue.of("storageProviderId")
                                                                                ),
                                                                        NoCodeStaticValue.of(provider.getId())
                                                                )
                                                )
                                                .recalculateReferencedIds()
                                );

                                var headline = new HeadlineContentElement();
                                headline.setId(metadataAttributesHeadlineId(provider.getId()));
                                headline.setContent("Zusätzliche Angaben (Metadaten)");
                                group.addChild(headline);

                                var description = new RichTextContentElement();
                                description.setId(metadataAttributesDescriptionId(provider.getId()));
                                description.setContent("Diese Angaben werden für alle Dateien dieses Eintrags an den Speicheranbieter übergeben. Angaben aus dem Vorgang können über Variablen eingefügt werden.");
                                group.addChild(description);

                                for (var m : provider.getMetadataAttributes()) {
                                    var in = new TextInputElement();
                                    in.setId(metadataAttributeFieldId(provider.getId(), m.getKey()));
                                    in.setLabel(m.getLabel());
                                    in.setHint(m.getDescription());
                                    in.setDynamicTextPolicy(new DynamicTextPolicy());
                                    group.addChild(in);
                                }

                                container.addChild(group);
                            });
                });

        return layout;
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts(@Nonnull WriteExternalStorageActionNodeConfig configuration) {
        return List.of(
                new ProcessNodePort(
                        PORT_NAME,
                        "Ablage abgeschlossen",
                        "Der Vorgang wird hier fortgesetzt, nachdem die Anlagen gespeichert oder fehlende beziehungsweise leere optionale Anlagensätze übersprungen wurden."
                )
        );
    }

    @Nonnull
    @Override
    public List<ProcessNodeOutput> getOutputs() {
        return List.of(
                new ProcessNodeOutput(OUTPUT_NAME_RESULTS, "Ablageergebnisse", "Die Ergebnisse der tatsächlich verarbeiteten Ablageeinträge mit Speicheranbieter, Anlagensatzschlüssel, Basisordner, gespeicherten Dateipfaden, Dateinamen und Anzahl. Übersprungene Einträge sind nicht enthalten.", OUTPUT_RESULTS_TYPE_DEFINITION),
                new ProcessNodeOutput(OUTPUT_NAME_STORAGE_PATHS_FROM_ROOT, "Speicherpfade", "Die tatsächlichen Dateipfade innerhalb der jeweiligen Speicheranbieter. Die Zuordnung zu den Anbietern steht in den Ablageergebnissen.", "Array<string>"),
                new ProcessNodeOutput(OUTPUT_NAME_FILE_NAMES, "Dateinamen", "Die vom Speicheranbieter zurückgegebenen Namen der gespeicherten Dateien.", "Array<string>"),
                new ProcessNodeOutput(OUTPUT_NAME_COUNT, "Anzahl gespeicherter Dateien", "Die Gesamtzahl der in dieser Ausführung ausgeführten Speicherungen von Anlagen. Eine Anlage kann bei mehreren Ablagezielen mehrfach gezählt werden.", "number")
        );
    }

    @Nonnull
    @Override
    public Class<WriteExternalStorageActionNodeConfig> getNodeConfigurationClass() {
        return WriteExternalStorageActionNodeConfig.class;
    }

    @Nullable
    @Override
    public Map<String, List<String>> validateConfiguration(
            @Nonnull ProcessNodeConfigurationValidationContext<WriteExternalStorageActionNodeConfig> context
    ) {
        var configuration = context.configuration();
        var errors = new HashMap<String, List<String>>();

        // A dynamic container has no authoring rows to inspect. Its shape and all row values are validated after the
        // container resolves at runtime.
        if (context.isDeferred(WriteExternalStorageActionNodeConfig.ATTACHMENT_SETS_FIELD_ID)) {
            return null;
        }

        var attachmentSetConfigs = configuration.attachmentSets == null ? List.<WriteExternalStorageConfig>of() : configuration.attachmentSets;
        if (attachmentSetConfigs.isEmpty()) {
            addError(errors, WriteExternalStorageActionNodeConfig.ATTACHMENT_SETS_FIELD_ID, "Legen Sie mindestens einen Ablageeintrag an.");
        }

        for (var i = 0; i < attachmentSetConfigs.size(); i++) {
            var rowIndex = i + 1;
            var attachmentSetConfig = attachmentSetConfigs.get(i);

            if (resolveSingleAttachmentSetDataKey(attachmentSetConfig.attachmentSetDataKeys) == null) {
                addError(errors, WriteExternalStorageConfig.ATTACHMENT_SET_DATA_KEYS_FIELD_ID, "Eintrag %d: Wählen Sie genau einen Anlagensatz aus.".formatted(rowIndex));
            }

            var storageProviderId = resolveStorageProviderId(attachmentSetConfig.storagePath);
            StorageProviderEntity storageProvider = null;
            if (storageProviderId == null) {
                addError(errors, WriteExternalStorageConfig.STORAGE_PATH_FIELD_ID, "Eintrag %d: Wählen Sie einen Speicheranbieter aus.".formatted(rowIndex));
            } else {
                storageProvider = storageProviderRepository.findById(storageProviderId).orElse(null);
                if (storageProvider == null) {
                    addError(errors, WriteExternalStorageConfig.STORAGE_PATH_FIELD_ID, "Eintrag %d: Der ausgewählte Speicheranbieter wurde nicht gefunden.".formatted(rowIndex));
                } else if (Boolean.TRUE.equals(storageProvider.getReadOnlyStorage())) {
                    addError(errors, WriteExternalStorageConfig.STORAGE_PATH_FIELD_ID, "Eintrag %d: Der ausgewählte Speicheranbieter ist schreibgeschützt.".formatted(rowIndex));
                } else if (StorageProviderType.Attachments.equals(storageProvider.getType())) {
                    addError(errors, WriteExternalStorageConfig.STORAGE_PATH_FIELD_ID, "Eintrag %d: Der ausgewählte Speicheranbieter ist ein Speicher für Vorgangsanlagen und kann nicht als Ziel verwendet werden.".formatted(rowIndex));
                }
            }

            var targetPath = resolveConfiguredPath(attachmentSetConfig.storagePath);
            if (targetPath == null) {
                addError(errors, WriteExternalStorageConfig.STORAGE_PATH_FIELD_ID, "Eintrag %d: Geben Sie einen Zielordnerpfad an.".formatted(rowIndex));
            } else {
                var diagnostics = templateRenderService.validateInterpolationSyntax(targetPath);
                if (!diagnostics.isEmpty()) {
                    for (var diagnostic : diagnostics) {
                        addError(
                                errors,
                                WriteExternalStorageConfig.STORAGE_PATH_FIELD_ID,
                                "Eintrag %d, Zeile %d: %s".formatted(rowIndex, diagnostic.lineNumber(), diagnostic.message())
                        );
                    }
                }
            }

            if (Boolean.TRUE.equals(attachmentSetConfig.customizeFileName) && !context.isDeferred(
                    WriteExternalStorageActionNodeConfig.ATTACHMENT_SETS_FIELD_ID,
                    i,
                    WriteExternalStorageConfig.FILE_NAME_FIELD_ID
            )) {
                var fileNameTemplate = StringUtils.toNullableTrimmedString(attachmentSetConfig.fileName);
                if (fileNameTemplate == null) {
                    addError(errors, WriteExternalStorageConfig.FILE_NAME_FIELD_ID, "Eintrag %d: Geben Sie einen Dateinamen für die Ablage an.".formatted(rowIndex));
                }
            }
        }

        return errors.isEmpty() ? null : errors;
    }

    @Nonnull
    @Override
    public AuthoredElementValues cleanConfigurationForExport(@Nonnull AuthoredElementValues configuration) {
        var attachmentSets = ReplicatingContainerLayoutElement._formatValue(
                configuration.getLiteral(WriteExternalStorageActionNodeConfig.ATTACHMENT_SETS_FIELD_ID)
        );
        if (attachmentSets != null) {
            for (var attachmentSet : attachmentSets) {
                if (attachmentSet.getValues() == null) {
                    continue;
                }
                var storagePath = attachmentSet.getValues().getLiteral(WriteExternalStorageConfig.STORAGE_PATH_FIELD_ID);
                if (storagePath instanceof Map<?, ?> storagePathMap) {
                    var cleanedStoragePath = new HashMap<>(storagePathMap);
                    cleanedStoragePath.remove("storageProviderId");
                    attachmentSet.getValues().putLiteral(WriteExternalStorageConfig.STORAGE_PATH_FIELD_ID, cleanedStoragePath);
                }
            }
            // JSON-backed rows are converted to new instances by the formatter, so mutations must be written back.
            configuration.putLiteral(WriteExternalStorageActionNodeConfig.ATTACHMENT_SETS_FIELD_ID, attachmentSets);
        }
        return configuration;
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<WriteExternalStorageActionNodeConfig> context) throws ProcessNodeExecutionException {
        var configuration = context.getConfigurationOfExecutingNode();
        var attachmentSetConfigs = configuration.attachmentSets == null ? List.<WriteExternalStorageConfig>of() : configuration.attachmentSets;
        if (attachmentSetConfigs.isEmpty()) {
            throw new ProcessNodeExecutionExceptionMissingValue("Legen Sie mindestens einen Ablageeintrag an.");
        }

        var storagePaths = new ArrayList<String>();
        var fileNames = new ArrayList<String>();
        var results = new ArrayList<Map<String, Object>>();
        var createdFolders = new HashSet<String>();
        var summaryDocuments = new ArrayList<Map<String, Object>>();

        for (var rowIndex = 0; rowIndex < attachmentSetConfigs.size(); rowIndex++) {
            var attachmentSetConfig = attachmentSetConfigs.get(rowIndex);
            var attachmentSetDataKey = resolveSingleAttachmentSetDataKey(attachmentSetConfig.attachmentSetDataKeys);
            if (attachmentSetDataKey == null) {
                throw new ProcessNodeExecutionExceptionMissingValue("Eintrag %d: Wählen Sie genau einen Anlagensatz aus.".formatted(rowIndex + 1));
            }

            var attachments = resolveProcessAttachmentsBySetDataKey(
                    context,
                    attachmentSetDataKey,
                    Boolean.TRUE.equals(attachmentSetConfig.ignoreEmptyAttachmentSet)
            );
            if (attachments.isEmpty()) {
                continue;
            }

            var storageProviderId = resolveRequiredStorageProviderId(attachmentSetConfig.storagePath);
            var storageProvider = ensureWritableStorageProvider(storageProviderId);

            var renderedTargetFolderPath = interpolateTargetPath(context, resolveConfiguredPath(attachmentSetConfig.storagePath), rowIndex + 1);
            var normalizedTargetFolderPath = normalizeFolderPath(renderedTargetFolderPath);
            if (normalizedTargetFolderPath == null) {
                throw new ProcessNodeExecutionExceptionMissingValue("Eintrag %d: Geben Sie einen Zielordnerpfad an.".formatted(rowIndex + 1));
            }

            var storageItemMetadata = resolveStorageItemMetadata(context, storageProvider, rowIndex);
            var setStoragePaths = new ArrayList<String>();
            var setFileNames = new ArrayList<String>();
            var usedCustomFileNames = new LinkedHashSet<String>();

            for (var i = 0; i < attachments.size(); i++) {
                var attachment = attachments.get(i);
                var attachmentIndex = i + 1;
                var targetFolderPath = applyFileIndex(normalizedTargetFolderPath, attachmentIndex);
                for (var attachmentGroupSegment : resolveGroupPathSegments(attachment.getGroup(), rowIndex + 1)) {
                    targetFolderPath = appendPathSegment(targetFolderPath, attachmentGroupSegment);
                }
                var storedFileName = resolveStoredFileName(attachmentSetConfig, attachment.getFileName(), attachmentIndex, usedCustomFileNames, rowIndex + 1);
                var targetPath = appendPathSegment(targetFolderPath, storedFileName);
                ensureParentFolders(storageProviderId, targetPath, createdFolders);

                try (var attachmentContent = storageService.getDocumentContent(
                        attachment.getStorageProviderId(),
                        attachment.getStoragePathFromRoot()
                )) {
                    var storedDocument = storageService.storeDocument(
                            storageProviderId,
                            targetPath,
                            attachmentContent,
                            storageItemMetadata
                    );
                    storagePaths.add(storedDocument.getPathFromRoot());
                    fileNames.add(storedDocument.getName());
                    setStoragePaths.add(storedDocument.getPathFromRoot());
                    setFileNames.add(storedDocument.getName());
                    summaryDocuments.add(Map.of(
                            "providerName", storageProvider.getName(),
                            "path", storedDocument.getPathFromRoot(),
                            "fileName", storedDocument.getName(),
                            "attachmentKey", attachment.getKey(),
                            "dataKey", attachmentSetDataKey
                    ));
                } catch (IOException | ResponseException e) {
                    throw new ProcessNodeExecutionExceptionUnknown(
                            e,
                            "Beim Speichern der Anlage %s beim Speicheranbieter %d ist ein Fehler aufgetreten.",
                            StringUtils.quote(attachment.getFileName()),
                            storageProviderId
                    );
                }
            }

            results.add(Map.of(
                    "storageProviderId", storageProviderId,
                    "attachmentSetDataKey", attachmentSetDataKey,
                    "targetFolderPath", normalizedTargetFolderPath,
                    "storagePathsFromRoot", setStoragePaths,
                    "fileNames", setFileNames,
                    "count", setStoragePaths.size()
            ));
        }

        var metadata = new HashMap<String, Object>();
        metadata.put(OUTPUT_NAME_RESULTS, results);
        metadata.put(OUTPUT_NAME_STORAGE_PATHS_FROM_ROOT, storagePaths);
        metadata.put(OUTPUT_NAME_FILE_NAMES, fileNames);
        metadata.put(OUTPUT_NAME_COUNT, storagePaths.size());

        return new ProcessNodeExecutionResultTaskCompleted()
                .setViaPort(PORT_NAME)
                .setNodeData(metadata)
                .setRuntimeData(ExecutionSummaryMarkdown.withMetadata(null,
                        Map.of(ExecutionSummaryMarkdown.DOCUMENTS, summaryDocuments)));
    }

    @Nonnull
    private StorageProviderEntity ensureWritableStorageProvider(@Nonnull Integer storageProviderId) throws ProcessNodeExecutionException {
        var storageProvider = storageProviderRepository
                .findById(storageProviderId)
                .orElseThrow(() -> new ProcessNodeExecutionExceptionInvalidConfiguration(
                        "Der Speicheranbieter mit der ID %d wurde nicht gefunden.",
                        storageProviderId
                ));

        if (Boolean.TRUE.equals(storageProvider.getReadOnlyStorage())) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Der Speicheranbieter %s (ID %d) ist schreibgeschützt.",
                    StringUtils.quote(storageProvider.getName()),
                    storageProviderId
            );
        } else if (StorageProviderType.Attachments.equals(storageProvider.getType())) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Der Speicheranbieter %s (ID %d) ist ein Speicher für Vorgangsanlagen und kann nicht als Ziel verwendet werden.",
                    StringUtils.quote(storageProvider.getName()),
                    storageProviderId
            );
        }

        return storageProvider;
    }

    @Nullable
    private String resolveSingleAttachmentSetDataKey(@Nullable List<String> attachmentSetDataKeys) {
        if (attachmentSetDataKeys == null) {
            return null;
        }

        var dataKeys = attachmentSetDataKeys
                .stream()
                .map(StringUtils::toNullableTrimmedString)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        return dataKeys.size() == 1 ? dataKeys.getFirst() : null;
    }

    @Nonnull
    private List<ProcessInstanceAttachmentEntity> resolveProcessAttachmentsBySetDataKey(@Nonnull ProcessNodeExecutionInitContext<WriteExternalStorageActionNodeConfig> context,
                                                                                        @Nonnull String attachmentSetDataKey,
                                                                                        boolean ignoreEmptyAttachmentSet) throws ProcessNodeExecutionException {
        var attachmentSets = processInstanceAttachmentSetService
                .findAllByProcessInstanceIdAndDataKey(context.getThisProcessInstance().getId(), attachmentSetDataKey)
                .stream()
                .sorted(Comparator.comparing(attachmentSet -> attachmentSet.getId()))
                .toList();

        if (attachmentSets.isEmpty()) {
            if (ignoreEmptyAttachmentSet) {
                logSkippedOptionalAttachmentSet(context, attachmentSetDataKey);
                return List.of();
            }

            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Der Anlagensatz mit dem Datenschlüssel %s wurde im Vorgang %d nicht gefunden.",
                    StringUtils.quote(attachmentSetDataKey),
                    context.getThisProcessInstance().getId()
            );
        }

        var attachments = new ArrayList<ProcessInstanceAttachmentEntity>();
        for (var attachmentSet : attachmentSets) {
            attachments.addAll(processInstanceAttachmentService
                    .findAllByAttachmentSetId(attachmentSet.getId())
                    .stream()
                    .sorted(Comparator
                            .comparing(ProcessInstanceAttachmentEntity::getPosition)
                            .thenComparing(attachment -> Objects.toString(attachment.getKey(), "")))
                    .toList());
        }

        if (attachments.isEmpty()) {
            if (ignoreEmptyAttachmentSet) {
                logSkippedOptionalAttachmentSet(context, resolveAttachmentSetLogName(attachmentSets, attachmentSetDataKey));
                return List.of();
            }

            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Der Anlagensatz mit dem Datenschlüssel %s enthält keine Anlagen.",
                    StringUtils.quote(attachmentSetDataKey)
            );
        }

        return attachments;
    }

    @Nonnull
    private static String resolveAttachmentSetLogName(@Nonnull List<ProcessInstanceAttachmentSetEntity> attachmentSets,
                                                      @Nonnull String fallback) {
        return attachmentSets
                .stream()
                .map(ProcessInstanceAttachmentSetEntity::getName)
                .map(StringUtils::toNullableTrimmedString)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(fallback);
    }

    private static void logSkippedOptionalAttachmentSet(@Nonnull ProcessNodeExecutionInitContext<WriteExternalStorageActionNodeConfig> context,
                                                        @Nonnull String attachmentSetName) {
        context.getLogger().logf(
                ProcessNodeExecutionLogLevel.Info,
                false,
                true,
                true,
                null,
                null,
                null,
                "Optionaler Anlagensatz übersprungen",
                Map.of(),
                "Für den optionalen Anlagensatz %s waren keine Anlagen zum Speichern vorhanden. Der Eintrag wurde übersprungen.",
                StringUtils.quote(attachmentSetName)
        );
    }

    @Nonnull
    private String interpolateTargetPath(@Nonnull ProcessNodeExecutionInitContext<WriteExternalStorageActionNodeConfig> context,
                                         @Nullable String targetPath,
                                         int rowIndex) throws ProcessNodeExecutionException {
        try {
            var renderedTargetPath = templateRenderService.interpolate(
                    context.getCurrentProcessExecutionData(),
                    targetPath,
                    TemplateOutputFormat.SingleLineText
            );
            if (renderedTargetPath == null) {
                throw new ProcessNodeExecutionExceptionMissingValue("Eintrag %d: Geben Sie einen Zielordnerpfad an.".formatted(rowIndex));
            }
            return renderedTargetPath;
        } catch (IllegalArgumentException e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(e, "Eintrag %d: Der Vorlagenausdruck im Zielordnerpfad konnte nicht verarbeitet werden: %s", rowIndex, e.getMessage());
        } catch (ProcessNodeExecutionException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ProcessNodeExecutionExceptionUnknown(e, "Eintrag %d: Der Zielordnerpfad konnte nicht ermittelt werden.", rowIndex);
        }
    }

    @Nonnull
    private String resolveStoredFileName(@Nonnull WriteExternalStorageConfig attachmentSetConfig,
                                         @Nonnull String originalFileName,
                                         int attachmentIndex,
                                         @Nonnull Set<String> usedCustomFileNames,
                                         int rowIndex) throws ProcessNodeExecutionException {
        if (!Boolean.TRUE.equals(attachmentSetConfig.customizeFileName)) {
            return originalFileName;
        }

        var fileName = StringUtils.toNullableTrimmedString(attachmentSetConfig.fileName);
        if (fileName == null) {
            throw new ProcessNodeExecutionExceptionMissingValue("Eintrag %d: Geben Sie einen Dateinamen für die Ablage an.".formatted(rowIndex));
        }

        // Text input values have already passed through runtime derivation. Re-rendering the result here would make
        // Variable, No-Code, and Low-Code output executable as template syntax a second time.
        var configuredBaseFileName = removeExtensionFromConfiguredFileName(fileName)
                .replace("#", Integer.toString(attachmentIndex));
        var resolvedFileName = StringUtils
                .extractExtensionFromFileName(originalFileName)
                .map(extension -> configuredBaseFileName + "." + extension)
                .orElse(configuredBaseFileName);

        resolvedFileName = appendNumericSuffix(resolvedFileName, attachmentIndex);
        resolvedFileName = ensureUniqueFileName(resolvedFileName, usedCustomFileNames);
        validateResolvedFileName(resolvedFileName, rowIndex);
        usedCustomFileNames.add(resolvedFileName);
        return resolvedFileName;
    }

    @Nonnull
    private static String removeExtensionFromConfiguredFileName(@Nonnull String configuredFileName) {
        var lastDotIndex = configuredFileName.lastIndexOf('.');
        if (lastDotIndex <= 0) {
            return configuredFileName;
        }

        return configuredFileName.substring(0, lastDotIndex);
    }

    @Nonnull
    private static String ensureUniqueFileName(@Nonnull String requestedFileName,
                                               @Nonnull Set<String> usedFileNames) {
        if (!usedFileNames.contains(requestedFileName)) {
            return requestedFileName;
        }

        for (var suffix = 2; ; suffix++) {
            var candidate = appendNumericSuffix(requestedFileName, suffix);
            if (!usedFileNames.contains(candidate)) {
                return candidate;
            }
        }
    }

    @Nonnull
    private static String appendNumericSuffix(@Nonnull String fileName,
                                              int suffix) {
        var lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex <= 0) {
            return fileName + "-" + suffix;
        }

        return fileName.substring(0, lastDotIndex) +
                "-" +
                suffix +
                fileName.substring(lastDotIndex);
    }

    private static void validateResolvedFileName(@Nonnull String resolvedFileName,
                                                 int rowIndex) throws ProcessNodeExecutionException {
        if (resolvedFileName.length() > 255) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Eintrag %d: Der fertige Dateiname darf einschließlich Nummer und Dateiendung höchstens 255 Zeichen lang sein.",
                    rowIndex
            );
        }

        if (resolvedFileName.contains("/") || resolvedFileName.contains("\\") || resolvedFileName.contains("\r") || resolvedFileName.contains("\n")) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Eintrag %d: Der Dateiname darf keine Schrägstriche, umgekehrten Schrägstriche oder Zeilenumbrüche enthalten.",
                    rowIndex
            );
        }
    }

    @Nonnull
    private static List<String> resolveGroupPathSegments(@Nullable String groupPath,
                                                         int rowIndex) throws ProcessNodeExecutionException {
        var normalizedGroupPath = StringUtils.toNullableTrimmedString(groupPath);
        if (normalizedGroupPath == null) {
            return List.of();
        }

        if (normalizedGroupPath.contains("\\") || normalizedGroupPath.contains("\r") || normalizedGroupPath.contains("\n")) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Eintrag %d: Die Gruppe der Anlage kann nicht als Unterordner verwendet werden.",
                    rowIndex
            );
        }

        var groupPathSegments = new ArrayList<String>();
        // Keep empty segments visible so malformed subgroup paths fail instead of being normalized away.
        for (var rawSegment : normalizedGroupPath.split("/", -1)) {
            var segment = StringUtils.toNullableTrimmedString(rawSegment);
            if (segment == null || ".".equals(segment) || "..".equals(segment)) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                        "Eintrag %d: Die Gruppe der Anlage kann nicht als Unterordner verwendet werden.",
                        rowIndex
                );
            }

            groupPathSegments.add(segment);
        }

        return groupPathSegments;
    }

    @Nullable
    private static String normalizeFolderPath(@Nullable String path) {
        var normalizedPath = StringUtils.toNullableTrimmedString(path);
        if (normalizedPath == null) {
            return null;
        }

        normalizedPath = normalizedPath.replace('\\', '/').replaceAll("/+", "/");
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }
        if (!normalizedPath.endsWith("/")) {
            normalizedPath += "/";
        }

        return normalizedPath;
    }

    @Nonnull
    private static String applyFileIndex(@Nonnull String path,
                                         int attachmentIndex) {
        return path.replace("#", Integer.toString(attachmentIndex));
    }

    @Nonnull
    private static String appendPathSegment(@Nonnull String folderPath,
                                            @Nonnull String fileName) {
        if (folderPath.endsWith("/")) {
            return folderPath + fileName;
        }
        return folderPath + "/" + fileName;
    }

    @Nullable
    private static Integer resolveStorageProviderId(@Nullable StoragePathSelectorInputElementValue storagePath) {
        return storagePath == null ? null : storagePath.getStorageProviderId();
    }

    @Nullable
    private static String resolveConfiguredPath(@Nullable StoragePathSelectorInputElementValue storagePath) {
        return storagePath == null ? null : StringUtils.toNullableTrimmedString(storagePath.getPath());
    }

    private boolean storageProviderSupportsMetadataAttributes(@Nonnull StorageProviderEntity storageProvider) {
        return storageProviderDefinitionService
                .retrieveProviderDefinition(
                        storageProvider.getStorageProviderDefinitionKey(),
                        storageProvider.getStorageProviderDefinitionVersion()
                )
                .map(definition -> Boolean.TRUE.equals(definition.getSupportsMetadataAttributes()))
                .orElse(false);
    }

    @Nonnull
    private StorageItemMetadata resolveStorageItemMetadata(@Nonnull ProcessNodeExecutionInitContext<WriteExternalStorageActionNodeConfig> context,
                                                           @Nonnull StorageProviderEntity storageProvider,
                                                           int rowIndex) throws ProcessNodeExecutionException {
        var metadataAttributes = resolveStorageProviderMetadataAttributes(storageProvider);
        if (metadataAttributes.isEmpty() || !storageProviderSupportsMetadataAttributes(storageProvider)) {
            return StorageItemMetadata.empty();
        }

        var effectiveRowValues = resolveEffectiveAttachmentSetRowValues(
                context.getEffectiveConfigurationValuesOfExecutingNode(),
                rowIndex
        );
        var authoredRowValues = effectiveRowValues == null
                ? resolveAuthoredAttachmentSetRowValues(context.getThisNode().getConfiguration(), rowIndex)
                : null;
        if (effectiveRowValues == null && authoredRowValues == null) {
            return StorageItemMetadata.empty();
        }

        var metadata = new StorageItemMetadata();
        for (var metadataAttribute : metadataAttributes) {
            var metadataKey = StringUtils.toNullableTrimmedString(metadataAttribute.getKey());
            if (metadataKey == null) {
                continue;
            }

            var fieldId = metadataAttributeFieldId(storageProvider.getId(), metadataKey);
            var rawValue = effectiveRowValues != null
                    ? effectiveRowValues.get(fieldId)
                    : authoredRowValues.getLiteral(fieldId);
            var metadataValueTemplate = rawValue == null ? null : StringUtils.toNullableTrimmedString(rawValue.toString());
            if (metadataValueTemplate == null) {
                continue;
            }

            // Effective row values are already resolved centrally. The authored fallback is retained for isolated
            // callers that do not provide effective configuration values and is safe because getLiteral rejects
            // executable input modes.
            var renderedMetadataValue = effectiveRowValues != null
                    ? metadataValueTemplate
                    : interpolateMetadataValue(context, metadataValueTemplate, rowIndex + 1, metadataKey);
            var normalizedMetadataValue = StringUtils.toNullableTrimmedString(renderedMetadataValue);
            if (normalizedMetadataValue != null) {
                metadata.put(metadataKey, normalizedMetadataValue);
            }
        }

        return metadata;
    }

    @Nonnull
    private static List<StorageProviderMetadataAttribute> resolveStorageProviderMetadataAttributes(@Nonnull StorageProviderEntity storageProvider) {
        return storageProvider.getMetadataAttributes() == null ? List.of() : storageProvider.getMetadataAttributes();
    }

    @Nullable
    private static AuthoredElementValues resolveAuthoredAttachmentSetRowValues(
            @Nullable AuthoredElementValues configuration,
            int rowIndex
    ) {
        if (configuration == null) {
            return null;
        }

        var attachmentSetValues = ReplicatingContainerLayoutElement._formatValue(
                configuration.getLiteral(WriteExternalStorageActionNodeConfig.ATTACHMENT_SETS_FIELD_ID)
        );
        if (attachmentSetValues == null || rowIndex < 0 || rowIndex >= attachmentSetValues.size()) {
            return null;
        }

        return attachmentSetValues.get(rowIndex).getValues();
    }

    @Nullable
    private static EffectiveElementValues resolveEffectiveAttachmentSetRowValues(
            @Nullable EffectiveElementValues configuration,
            int rowIndex
    ) {
        if (configuration == null) {
            return null;
        }

        var rawRows = configuration.get(WriteExternalStorageActionNodeConfig.ATTACHMENT_SETS_FIELD_ID);
        if (!(rawRows instanceof List<?> rows) || rowIndex < 0 || rowIndex >= rows.size()) {
            return null;
        }

        var rawRow = rows.get(rowIndex);
        if (rawRow instanceof EffectiveReplicatingContainerLayoutElementValue row) {
            return row.getValues();
        }
        if (!(rawRow instanceof Map<?, ?> rowMap)) {
            return null;
        }

        var rawValues = rowMap.containsKey("values") ? rowMap.get("values") : rowMap;
        if (!(rawValues instanceof Map<?, ?> values)) {
            return null;
        }

        var effectiveValues = new EffectiveElementValues();
        values.forEach((key, value) -> {
            if (key instanceof String fieldId) {
                effectiveValues.put(fieldId, value);
            }
        });
        return effectiveValues;
    }

    @Nonnull
    private String interpolateMetadataValue(@Nonnull ProcessNodeExecutionInitContext<WriteExternalStorageActionNodeConfig> context,
                                            @Nonnull String metadataValue,
                                            int rowIndex,
                                            @Nonnull String metadataKey) throws ProcessNodeExecutionException {
        try {
            var renderedValue = templateRenderService.interpolate(
                    context.getCurrentProcessExecutionData(),
                    metadataValue,
                    TemplateOutputFormat.SingleLineText
            );
            return renderedValue == null ? "" : renderedValue;
        } catch (IllegalArgumentException e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    e,
                    "Eintrag %d: Der Vorlagenausdruck für die zusätzliche Angabe %s konnte nicht verarbeitet werden: %s",
                    rowIndex,
                    StringUtils.quote(metadataKey),
                    e.getMessage()
            );
        } catch (RuntimeException e) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Eintrag %d: Die zusätzliche Angabe %s konnte nicht ermittelt werden.",
                    rowIndex,
                    StringUtils.quote(metadataKey)
            );
        }
    }

    @Nonnull
    private static String metadataAttributesGroupId(@Nonnull Integer storageProviderId) {
        return "%s_%d".formatted(METADATA_ATTRIBUTES_PREFIX, storageProviderId);
    }

    @Nonnull
    private static String metadataAttributesHeadlineId(@Nonnull Integer storageProviderId) {
        return "%s_hdl".formatted(metadataAttributesGroupId(storageProviderId));
    }

    @Nonnull
    private static String metadataAttributesDescriptionId(@Nonnull Integer storageProviderId) {
        return "%s_desc".formatted(metadataAttributesGroupId(storageProviderId));
    }

    @Nonnull
    private static String metadataAttributeFieldId(@Nonnull Integer storageProviderId,
                                                   @Nonnull String metadataKey) {
        return "%s_%s".formatted(metadataAttributesGroupId(storageProviderId), metadataKey);
    }

    @Nonnull
    private static Integer resolveRequiredStorageProviderId(@Nullable StoragePathSelectorInputElementValue storagePath) throws ProcessNodeExecutionExceptionMissingValue {
        var storageProviderId = resolveStorageProviderId(storagePath);
        if (storageProviderId == null) {
            throw new ProcessNodeExecutionExceptionMissingValue("Wählen Sie einen Speicheranbieter aus.");
        }
        return storageProviderId;
    }

    private static void addError(@Nonnull Map<String, List<String>> errors,
                                 @Nonnull String fieldId,
                                 @Nonnull String message) {
        errors.computeIfAbsent(fieldId, ignored -> new ArrayList<>()).add(message);
    }

    private void ensureParentFolders(@Nonnull Integer storageProviderId,
                                     @Nonnull String targetPath,
                                     @Nonnull HashSet<String> createdFolders) throws ProcessNodeExecutionException {
        var normalizedPath = targetPath.replace('\\', '/');
        var lastSlashIndex = normalizedPath.lastIndexOf('/');
        if (lastSlashIndex <= 0) {
            return;
        }

        var parentPath = normalizedPath.substring(0, lastSlashIndex);
        var currentPath = new StringBuilder();
        for (var segment : parentPath.split("/")) {
            if (segment.isEmpty()) {
                continue;
            }

            currentPath.append('/').append(segment);
            var folderPath = currentPath + "/";
            if (!createdFolders.add(storageProviderId + ":" + folderPath)) {
                continue;
            }

            try {
                storageService.createFolder(storageProviderId, folderPath);
            } catch (ResponseException e) {
                throw new ProcessNodeExecutionExceptionUnknown(
                        e,
                        "Beim Anlegen des Zielordners %s beim Speicheranbieter %d ist ein Fehler aufgetreten.",
                        StringUtils.quote(folderPath),
                        storageProviderId
                );
            }
        }
    }

    @LayoutElementPOJOBinding(id = NODE_KEY, type = ElementType.ConfigLayout)
    public static class WriteExternalStorageActionNodeConfig {
        public static final String ATTACHMENT_SETS_FIELD_ID = "attachment_sets";

        public List<WriteExternalStorageConfig> attachmentSets;
    }

    @ReplicatingContainerLayoutElementElementPOJOBinding(
            id = WriteExternalStorageActionNodeConfig.ATTACHMENT_SETS_FIELD_ID,
            properties = {
            @ElementPOJOBindingProperty(key = "label", strValue = "Ablageeinträge"),
            @ElementPOJOBindingProperty(key = "hint", strValue = "Legen Sie für jeden Anlagensatz fest, wo seine Anlagen gespeichert werden sollen."),
            @ElementPOJOBindingProperty(key = "required", boolValue = true),
            @ElementPOJOBindingProperty(key = "headlineTemplate", strValue = "Ablageeintrag #"),
            @ElementPOJOBindingProperty(key = "addLabel", strValue = "Ablageeintrag hinzufügen"),
            @ElementPOJOBindingProperty(key = "removeLabel", strValue = "Ablageeintrag entfernen")
    })
    public static class WriteExternalStorageConfig {
        public static final String ATTACHMENT_SET_DATA_KEYS_FIELD_ID = "attachment_set_data_keys";
        public static final String STORAGE_PATH_FIELD_ID = "storage_path";
        public static final String IGNORE_EMPTY_ATTACHMENT_SET_FIELD_ID = "ignore_empty_attachment_set";
        public static final String CUSTOMIZE_FILE_NAME_FIELD_ID = "customize_file_name";
        public static final String FILE_NAME_FIELD_ID = "file_name";

        @InputElementPOJOBinding(id = ATTACHMENT_SET_DATA_KEYS_FIELD_ID, type = ElementType.ProcessInstanceAttachmentSetSelect, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Anlagensatz"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Wählen Sie den Anlagensatz aus, dessen Anlagen gespeichert werden sollen."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "minItems", intValue = 1),
                @ElementPOJOBindingProperty(key = "maxItems", intValue = 1),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 12.0)
        })
        public List<String> attachmentSetDataKeys;

        @InputElementPOJOBinding(id = STORAGE_PATH_FIELD_ID, type = ElementType.StoragePathSelector, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Ablageziel"),
                @ElementPOJOBindingProperty(key = "storageProviderSelectHint", strValue = "Wählen Sie den Speicheranbieter aus, bei dem die Anlagen gespeichert werden sollen."),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Geben Sie den Zielordner an oder wählen Sie einen vorhandenen Ordner aus. Prosuna ergänzt die Dateinamen automatisch. Der Pfad kann Angaben aus dem Vorgang als Vorlagenausdrücke enthalten. „#“ wird für jede Anlage durch deren Dateinummer ersetzt."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 12.0),
                @ElementPOJOBindingProperty(key = "allowReadOnlyStorageProviders", falseValue = true)
        })
        public StoragePathSelectorInputElementValue storagePath;

        /**
         * If true, missing attachment sets and attachment sets without files are logged and skipped instead of failing execution.
         */
        @InputElementPOJOBinding(id = IGNORE_EMPTY_ATTACHMENT_SET_FIELD_ID, type = ElementType.Checkbox, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Fehlenden oder leeren Anlagensatz überspringen"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Ist der Anlagensatz im Vorgang nicht vorhanden oder enthält er keine Anlagen, wird dieser Eintrag ausgelassen. Fehler beim Lesen oder Speichern von Dateien werden dadurch nicht übersprungen."),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 12.0)
        })
        @Nullable
        public Boolean ignoreEmptyAttachmentSet;

        @InputElementPOJOBinding(id = CUSTOMIZE_FILE_NAME_FIELD_ID, type = ElementType.Checkbox, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Dateinamen anpassen"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Verwenden Sie bei der Ablage eigene Dateinamen. Die ursprünglichen Anlagen im Vorgang werden dadurch nicht umbenannt."),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 12.0)
        })
        public Boolean customizeFileName;

        @InputElementPOJOBinding(id = FILE_NAME_FIELD_ID, type = ElementType.Text,
                dynamicText = true,
                allowedInputModes = {InputMode.Literal, InputMode.Variable, InputMode.NoCode, InputMode.LowCode}, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Dateiname bei der Ablage"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Geben Sie einen Basisnamen ohne Dateiendung an. Angaben aus dem Vorgang können über Variablen eingefügt werden. Prosuna übernimmt die ursprüngliche Dateiendung und ergänzt immer eine laufende Nummer, zum Beispiel Nachweis-1.pdf."),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 12.0)
        })
        public String fileName;
    }
}
