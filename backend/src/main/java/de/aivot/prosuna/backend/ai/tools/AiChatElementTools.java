package de.aivot.prosuna.backend.ai.tools;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import com.github.victools.jsonschema.generator.*;
import de.aivot.prosuna.backend.ai.cache.entities.AiUiElementChatSessionCacheEntity;
import de.aivot.prosuna.backend.ai.cache.repositories.AiUiElementChatSessionCacheRepository;
import de.aivot.prosuna.backend.ai.models.ChatContextModel;
import de.aivot.prosuna.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.elements.models.elements.LayoutElement;
import de.aivot.prosuna.backend.enums.ElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonPointer;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AiChatElementTools {
    private final JsonMapper jsonMapper;
    private final AiUiElementChatSessionCacheRepository aiUiElementChatSessionCacheRepository;

    public AiChatElementTools(@Nonnull JsonMapper jsonMapper,
                              @Nonnull AiUiElementChatSessionCacheRepository aiUiElementChatSessionCacheRepository) {
        this.jsonMapper = jsonMapper;
        this.aiUiElementChatSessionCacheRepository = aiUiElementChatSessionCacheRepository;
    }

    @Nonnull
    @Tool(
            name = "liste-verfuegbare-elemente",
            description = """
                    Liste alle verfügbaren Element Typen für Formular-Elemente mit ihren numerischen Typ-Schlüsseln, Anzeigenamen und Beschreibungen auf.
                    Gibt eine Liste mit dem Typ-Schüssel, dem Namen und der Beschreibung des jeweiligen Formular-Elementes zurück.
                    Das Zeilenformat lautet: Typ-Schlüssel = 3, Anzeigenamen = "Gruppe", Beschreibung = "Eine Beschreibung des Elementes".
                    Über das Werkzeug "liste-eigenschaften-fuer-element" kannst du dir die JSON-Eigenschaftsnamen eines Formularelementtyps zurückgeben lassen.
                    """
    )
    public String listElementTypes() {
        StringBuilder sb = new StringBuilder();

        for (var val : ElementType.values()) {
            var line = String.format(
                    "- Typ-Schlüssel = %d, Anzeigenamen = %s, Beschreibung = %s\n",
                    val.getKey(),
                    val.getDisplayName(),
                    val.getDescription()
            );
            sb.append(line);
        }

        return sb.toString();
    }

    @Nonnull
    @Tool(
            name = "liste-eigenschaften-fuer-element",
            description = """
                    Gibt die JSON-Eigenschaftsnamen eines Formularelementtyps einschließlich geerbter Eigenschaften zurück.
                    Jede Zeile enthält einen Namen im Format "- name", in der von Jackson ermittelten Reihenfolge.
                    Verschachtelte Eigenschaften werden nicht aufgelistet.
                    Mit dem Werkzeug "hole-json-schema-fuer-element-eigenschaft" kannst du dir das eigenständige JSON-Schema für eine bestimmte Eigenschaft eines Formularelementtyps zurückgeben lassen.
                    """
    )
    public String listElementTypeProperties(
            @ToolParam(description = "Numerische Typ-Schlüssel des Formularelements aus liste-verfuegbare-elemente, beispielsweise 15 für ein Text-Feld.")
            @Nonnull Integer typeKey
    ) {
        var elementClass = ElementType
                .findElement(typeKey)
                .map(BaseElement::getElementClassByType)
                .orElse(null);
        if (elementClass == null) {
            return "Die Eigenschaften konnten nicht abgerufen werden: Der Formularelementtyp wird nicht unterstützt.";
        }

        var introspector = jsonMapper.serializationConfig().classIntrospectorInstance();
        var javaType = jsonMapper.constructType(elementClass);
        return introspector.introspectForSerialization(javaType, introspector.introspectClassAnnotations(javaType))
                .findProperties().stream()
                .filter(property -> property.couldSerialize())
                .map(property -> "- " + property.getName())
                .collect(Collectors.joining("\n"));
    }

    @Nonnull
    @Tool(
            name = "hole-json-schema-fuer-element-eigenschaft",
            description = """
                    Rufe das eigenständige JSON-Schema für eine Eigenschaft eines bestimmten Typs von Formular-Element ab.
                    Das Schema beschreibt die Struktur des Java-Typs der Eigenschaft einschließlich verschachtelter Typen.
                    Die Eigenschaft wird anhand ihres wörtlichen Namens am Formularelement ausgewählt; verschachtelte Pfade oder zusammengesetzte Eigenschaftsnamen werden nicht unterstützt.
                    Eigenschaftsspezifische Annotationen, Nullbarkeit, Standardwerte und fachliche Regeln werden nicht ausgewertet.
                    """
    )
    private String getJsonSchemaForElementProperty(
            @ToolParam(description = "Numerische Typ-Schlüssel des Formularelements aus liste-verfuegbare-elemente, beispielsweise 15 für ein Text-Feld.")
            @Nonnull Integer typeKey,
            @ToolParam(description = "Der wörtliche Eigenschaftsname am ausgewählten Formularelement, kein verschachtelter Pfad.")
            @Nonnull String propertyName
    ) {
        var elementClass = ElementType.findElement(typeKey)
                .map(BaseElement::getElementClassByType)
                .orElse(null);
        if (elementClass == null) {
            return "Das Schema der Eigenschaft konnte nicht abgerufen werden: Der Formularelementtyp wird nicht unterstützt.";
        }

        var introspector = jsonMapper.serializationConfig().classIntrospectorInstance();
        var javaType = jsonMapper.constructType(elementClass);
        var property = introspector.introspectForSerialization(javaType, introspector.introspectClassAnnotations(javaType))
                .findProperties().stream()
                .filter(candidate -> candidate.couldSerialize() && candidate.getName().equals(propertyName))
                .findFirst().orElse(null);
        if (property == null) {
            return "Das Schema der Eigenschaft konnte nicht abgerufen werden: Die Eigenschaft ist für diesen Formularelementtyp nicht verfügbar.";
        }

        var config = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)
                .with(Option.EXTRA_OPEN_API_FORMAT_VALUES)
                .without(Option.FLATTENED_ENUMS_FROM_TOSTRING)
                .build();
        var schema = new SchemaGenerator(config)
                .generateSchema(resolveSchemaType(property.getPrimaryType(), new TypeResolver()));
        return jsonMapper.writeValueAsString(schema);
    }

    @Nonnull
    private ResolvedType resolveSchemaType(@Nonnull JavaType type, @Nonnull TypeResolver resolver) {
        if (type.isArrayType()) {
            return resolver.arrayType(resolveSchemaType(type.getContentType(), resolver));
        }
        var parameters = new ResolvedType[type.containedTypeCount()];
        for (int index = 0; index < parameters.length; index++) {
            parameters[index] = resolveSchemaType(type.containedType(index), resolver);
        }
        return resolver.resolve(type.getRawClass(), parameters);
    }

    @Nonnull
    @Tool(name = "hole-formularstruktur", description = """
            Rufe die vollständige aktuelle Formularstruktur als eingerückte Liste der Formularelemente ab.
            Jede Zeile enthält ausschließlich die Element-ID des Formularelements, die numerische Typ-Schlüssel und den JSON-Pfad, mit zwei Leerzeichen Einrückung je untergeordneter Ebene.
            Das Zeilenformat lautet: - Element-ID="element-id", Typ-Schlüssel=3, Pfad="/children/0", Name="Der Name des Elementes". Der Pfad des Wurzelelements ist eine leere Zeichenfolge.
            Nutze dies, um dir einen Überblick über die aktuelle Formularstruktur zu verschaffen und die Pfade bestimmter Formularelemente zur weiteren Prüfung oder Bearbeitung zu finden.
            """)
    public String getElementsTree(@Nonnull ToolContext toolContext) {
        var cacheEntity = getCacheEntity(toolContext);
        var currentElement = cacheEntity.getCurrentElementJson();
        if (currentElement == null) {
            return "Es ist noch kein Formularentwurf verfügbar.";
        }

        try {
            var tree = new StringBuilder();
            appendElementTree(getCurrentElementTree(cacheEntity), "", 0, tree);
            return tree.toString();
        } catch (IllegalArgumentException exception) {
            return "Die Formularstruktur konnte nicht abgerufen werden: " + exception.getMessage();
        }
    }

    private void appendElementTree(@Nonnull JsonNode element,
                                   @Nonnull String path,
                                   int depth,
                                   @Nonnull StringBuilder tree) {
        if (!tree.isEmpty()) {
            tree.append('\n');
        }
        tree.append("  ".repeat(depth))
                .append("- Element-ID=").append(jsonMapper.writeValueAsString(element.get("id")))
                .append(", Typ-Schlüssel=").append(jsonMapper.writeValueAsString(element.get("type")))
                .append(", Pfad=").append(jsonMapper.writeValueAsString(path))
                .append(", Name=").append(jsonMapper.writeValueAsString(element.get("name")));

        var children = element.path("children");
        if (children.isArray()) {
            for (int index = 0; index < children.size(); index++) {
                appendElementTree(children.get(index), path + "/children/" + index, depth + 1, tree);
            }
        }
    }

    @Nonnull
    @Tool(name = "hole-element-an-pfad", description = """
            Rufe ein einzelnes Formularelement ohne children anhand seines Pfads als JSON ab.
            Finde Kindelemente mit hole-formularstruktur und rufe sie einzeln über ihre Pfade ab.
            Der Pfad ist ein JSON-Pointer zum Formularelement: eine leere Zeichenfolge für das Wurzelelement oder beispielsweise /children/0/children/1.
            Es werden ausschließlich /children/<index>-Segmente mit nicht negativen Indizes ohne führende Nullen unterstützt.
            Gibt alle Eigenschaften außer children oder eine Fehlermeldung zurück, die mit 'Das Formularelement konnte nicht abgerufen werden:' beginnt.
            """)
    public String getElementByPath(
            @ToolParam(description = "JSON-Pointer zu einem Formularelement: eine leere Zeichenfolge für das Wurzelelement oder beispielsweise /children/0/children/1.")
            @Nonnull String path,
            @Nonnull ToolContext toolContext
    ) {
        var cacheEntity = getCacheEntity(toolContext);
        try {
            var pointer = parseElementPath(path);
            var tree = getCurrentElementTree(cacheEntity);
            var element = resolveElement(tree, pointer);
            element.remove("children");
            return jsonMapper.writeValueAsString(element);
        } catch (IllegalArgumentException exception) {
            return "Das Formularelement konnte nicht abgerufen werden: " + exception.getMessage();
        }
    }


    @Nonnull
    @Tool(name = "erstelle-element", description = """
            Erstelle ein Formularelement mit einer serverseitig erzeugten ID und den Standardwerten des Datenmodells und hänge es an die Liste der Kindelemente eines Layouts an.
            Verwende eine numerische Typ-ID aus liste-verfuegbare-elemente und einen vorhandenen Pfad zum Elternelement aus hole-formularstruktur.
            Der Pfad zum Elternelement ist für das Wurzelelement leer oder besteht aus /children/<index>-Segmenten.
            Bestehende Kindelemente und Eigenschaften bleiben erhalten. Die vollständige Formularstruktur muss vor dem Speichern die Strukturprüfung bestehen.
            Gibt nach dem Speichern im Sitzungscache JSON mit id, numerischem type und vollständigem path des neuen Formularelements zurück.
            Verwende aktualisiere-element-eigenschaften mit dem zurückgegebenen Pfad, um das neue Formularelement in einem Aufruf zu konfigurieren.
            Bei ungültigen Eingaben wird eine Fehlermeldung zurückgegeben, die mit 'Das Formularelement konnte nicht erstellt werden:' beginnt. Der Cache bleibt unverändert.
            """)
    public String createElement(
            @ToolParam(description = "JSON-Pointer zum übergeordneten Layout: eine leere Zeichenfolge für das Wurzelelement oder beispielsweise /children/0.")
            @Nonnull String parentPath,
            @ToolParam(description = "Numerische Typ-ID des Formularelements aus liste-verfuegbare-elemente.")
            @Nonnull Integer type,
            @Nonnull ToolContext toolContext
    ) {
        var cacheEntity = getCacheEntity(toolContext);
        String updatedElementJson;
        String result;
        try {
            var elementType = ElementType.findElement(type)
                    .orElseThrow(() -> new IllegalArgumentException("Der Formularelementtyp wird nicht unterstützt."));
            var tree = getCurrentElementTree(cacheEntity);
            var parent = resolveElement(tree, parseElementPath(parentPath));
            if (!(jsonMapper.treeToValue(parent, BaseElement.class) instanceof LayoutElement<?>)) {
                throw new IllegalArgumentException("Das Elternelement muss ein Layout sein, das Kindelemente unterstützt.");
            }

            var existingChildren = parent.get("children");
            ArrayNode children;
            if (existingChildren == null || existingChildren.isNull()) {
                children = parent.putArray("children");
            } else if (existingChildren instanceof ArrayNode array) {
                children = array;
            } else {
                throw new IllegalArgumentException("Die Kindelemente des Elternelements müssen als Array vorliegen.");
            }

            var element = ElementType.getElementClass(elementType);
            var path = parentPath + "/children/" + children.size();
            children.add(jsonMapper.valueToTree(element));
            validateStructure(tree);
            updatedElementJson = jsonMapper.writeValueAsString(tree);
            result = jsonMapper.writeValueAsString(jsonMapper.createObjectNode()
                    .put("id", element.getId())
                    .put("type", elementType.getKey())
                    .put("path", path));
        } catch (ElementDataConversionException exception) {
            return "Das Formularelement konnte nicht erstellt werden: Der Formularelementtyp wird nicht unterstützt.";
        } catch (JacksonException exception) {
            return "Das Formularelement konnte nicht erstellt werden: Die Formularstruktur muss unterstützte Formularelementtypen sowie kompatible Kindelemente und Eigenschaftswerte enthalten.";
        } catch (IllegalArgumentException exception) {
            return "Das Formularelement konnte nicht erstellt werden: " + exception.getMessage();
        }

        var updatedCache = new AiUiElementChatSessionCacheEntity()
                .setId(cacheEntity.getId())
                .setTargetRootType(cacheEntity.getTargetRootType())
                .setCurrentElementJson(updatedElementJson);
        aiUiElementChatSessionCacheRepository.save(updatedCache);
        return result;
    }

    @Nonnull
    @Tool(name = "aktualisiere-element-eigenschaften", description = """
            Aktualisiere oder erstelle mehrere Eigenschaften eines Formularelements in einem Aufruf.
            Der Pfad ist für das Wurzelelement leer oder besteht aus /children/<index>-Segmenten ohne negative Indizes oder führende Nullen.
            Übergib alle gewünschten Eigenschaften gemeinsam als JSON-Objekt. Eigenschaftsnamen werden wörtlich interpretiert; null setzt einen JSON-null-Wert, ohne die Eigenschaft zu entfernen.
            Nicht angegebene Eigenschaften bleiben erhalten. id, type und children dürfen aktualisiert werden; children ersetzt dabei die vollständige Kinderliste des Elements.
            Die vollständige aktualisierte Formularstruktur muss sich als Formularelement deserialisieren lassen, bevor sie im Sitzungscache gespeichert wird.
            Nachfolgende Tool-Aufrufe sehen die gespeicherten Änderungen. Geprüft wird ausschließlich die Struktur, nicht die fachlichen Regeln oder der vorgegebene Typ des Wurzelelements.
            Gibt nach dem Speichern eine Bestätigung oder bei ungültigen Eingaben eine Fehlermeldung zurück, die mit 'Die Aktualisierung ist fehlgeschlagen:' beginnt.
            Alle Eigenschaften werden atomar aktualisiert. Bei einer ungültigen Aktualisierung bleibt der zwischengespeicherte Formularentwurf unverändert.
            """)
    public String updatePropertiesOfElement(
            @ToolParam(description = "JSON-Pointer zu einem Formularelement: eine leere Zeichenfolge für das Wurzelelement oder beispielsweise /children/0/children/1.")
            @Nonnull String path,
            @ToolParam(description = "JSON-Objekt mit allen zu aktualisierenden Eigenschaften. Werte können Skalare, Objekte, Arrays oder null sein.")
            @Nonnull Map<String, Object> properties,
            @Nonnull ToolContext toolContext
    ) {
        var cacheEntity = getCacheEntity(toolContext);
        if (properties == null || properties.isEmpty()) {
            return "Die Aktualisierung ist fehlgeschlagen: Es muss mindestens eine Eigenschaft angegeben werden.";
        }
        if (properties.keySet().stream().anyMatch(property -> property == null || property.isBlank())) {
            return "Die Aktualisierung ist fehlgeschlagen: Eigenschaftsnamen dürfen weder leer sein noch ausschließlich aus Leerraum bestehen.";
        }

        ObjectNode tree;
        ObjectNode element;
        try {
            var pointer = parseElementPath(path);
            tree = getCurrentElementTree(cacheEntity);
            element = resolveElement(tree, pointer);
        } catch (IllegalArgumentException exception) {
            return "Die Aktualisierung ist fehlgeschlagen: " + exception.getMessage();
        }

        String updatedElementJson;
        try {
            properties.forEach((property, value) -> element.set(property, jsonMapper.valueToTree(value)));
            validateStructure(tree);
            // Keep the JSON representation so validation does not discard extension properties or add model defaults.
            updatedElementJson = jsonMapper.writeValueAsString(tree);
        } catch (JacksonException | IllegalArgumentException exception) {
            return "Die Aktualisierung ist fehlgeschlagen: Die Werte müssen eine Formularstruktur mit unterstützten Typen und kompatiblen Eigenschaftswerten ergeben.";
        }

        var updatedCache = new AiUiElementChatSessionCacheEntity()
                .setId(cacheEntity.getId())
                .setTargetRootType(cacheEntity.getTargetRootType())
                .setCurrentElementJson(updatedElementJson);
        aiUiElementChatSessionCacheRepository.save(updatedCache);

        return "Die Eigenschaften des Formularelements wurden erfolgreich aktualisiert.";
    }

    @Nonnull
    private JsonPointer parseElementPath(@Nullable String path) {
        if (path == null || !path.matches("(?:/children/(?:0|[1-9][0-9]*))*")) {
            throw new IllegalArgumentException("Verwende einen leeren Pfad für das Wurzelelement oder /children/<index>-Segmente mit nicht negativen Indizes ohne führende Nullen.");
        }
        return JsonPointer.compile(path);
    }

    @Nonnull
    private ObjectNode getCurrentElementTree(@Nonnull AiUiElementChatSessionCacheEntity cacheEntity) {
        var currentElement = cacheEntity.getCurrentElementJson();
        if (currentElement == null) {
            throw new IllegalArgumentException("Es ist noch kein Formularentwurf verfügbar.");
        }
        try {
            var tree = jsonMapper.reader()
                    .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .readTree(currentElement);
            if (tree instanceof ObjectNode object) {
                return object;
            }
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Der zwischengespeicherte Formularentwurf ist kein gültiges JSON-Objekt.", exception);
        }
        throw new IllegalArgumentException("Der zwischengespeicherte Formularentwurf ist kein gültiges JSON-Objekt.");
    }

    @Nonnull
    private ObjectNode resolveElement(@Nonnull ObjectNode tree, @Nonnull JsonPointer pointer) {
        JsonNode element = tree;
        while (!pointer.matches()) {
            var children = element.path("children");
            if (!children.isArray()) {
                throw new IllegalArgumentException("Unter dem angegebenen Pfad existiert kein Formularelement.");
            }
            var indexPointer = pointer.tail();
            element = children.path(indexPointer.getMatchingIndex());
            if (!element.isObject()) {
                throw new IllegalArgumentException("Unter dem angegebenen Pfad existiert kein Formularelement.");
            }
            pointer = indexPointer.tail();
        }
        return (ObjectNode) element;
    }

    @Nonnull
    @Tool(name = "pruefe-formularstruktur", description = """
            Prüfe, ob sich ein JSON-Objekt einschließlich seiner Kindelemente als Formularelement deserialisieren lässt.
            Geprüft wird die Struktur der Formularelemente, nicht die Vollständigkeit oder die fachlichen Regeln eines fertigen Formulars.
            Gibt bei gültiger Struktur eine Erfolgsmeldung und bei ungültiger Struktur eine Fehlermeldung zurück.
            """)
    public String verifyElementStructure(
            @ToolParam(description = "Die JSON-Darstellung der zu prüfenden Formularstruktur.")
            @Nonnull String json
    ) {
        try {
            var structure = jsonMapper.reader()
                    .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .readTree(json);
            validateStructure(structure);
        } catch (JacksonException exception) {
            return "Die JSON-Struktur ist ungültig: Erwartet wird ein einzelnes Formularelementobjekt mit einem unterstützten Typ und kompatiblen Eigenschaftswerten.";
        } catch (IllegalArgumentException exception) {
            return "Die JSON-Struktur ist ungültig: " + exception.getMessage();
        }

        return "Die JSON-Struktur ist gültig.";
    }

    private void validateStructure(@Nullable JsonNode structure) {
        if (structure == null || !structure.isObject()) {
            throw new IllegalArgumentException("Die Formularstruktur muss ein JSON-Objekt sein.");
        }
        validateChildren(jsonMapper.treeToValue(structure, BaseElement.class));
    }

    private void validateChildren(@Nullable BaseElement element) {
        if (element == null) {
            throw new IllegalArgumentException("Die Liste der Kindelemente darf keine null-Einträge enthalten.");
        }
        if (element instanceof LayoutElement<?> layout) {
            layout.getChildren().forEach(this::validateChildren);
        }
    }

    @Nonnull
    private AiUiElementChatSessionCacheEntity getCacheEntity(@Nonnull ToolContext toolContext) throws IllegalStateException {
        var context = ChatContextModel.fromToolContext(toolContext);

        if (context.getAppContext() != ChatContextModel.AppContext.FormEditor) {
            throw new IllegalStateException("Die aktuelle Chatsitzung befindet sich nicht im Formularbearbeitungsmodus; es ist kein Formularentwurf verfügbar.");
        }

        var cacheEntity = aiUiElementChatSessionCacheRepository
                .findById(context.sessionId());

        if (cacheEntity.isEmpty()) {
            throw new IllegalStateException("Die aktuelle Chatsitzung befindet sich im Formularbearbeitungsmodus, aber der Formularentwurf fehlt im Cache.");
        }

        return cacheEntity.get();
    }
}
