package de.aivot.prosuna.backend.ai.tools;

import de.aivot.prosuna.backend.ai.services.AiInputValueSchemaService;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import com.github.victools.jsonschema.generator.*;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.enums.ElementType;
import jakarta.annotation.Nonnull;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

import java.util.stream.Collectors;

@Service
public class AiChatElementSchemaTools {
    private final JsonMapper jsonMapper;
    private final AiInputValueSchemaService valueSchemas;

    public AiChatElementSchemaTools(@Nonnull JsonMapper jsonMapper, @Nonnull AiInputValueSchemaService valueSchemas) {
        this.jsonMapper = jsonMapper;
        this.valueSchemas = valueSchemas;
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
    public Object listElementTypes(
            @ToolParam(description = "Optionaler Suchbegriff für Elementnamen und Beschreibungen", required = false) @jakarta.annotation.Nullable String query,
            @ToolParam(description = "Offset ab 0", required = false) @jakarta.annotation.Nullable Integer offset,
            @ToolParam(description = "Anzahl, Standard 20, höchstens 50", required = false) @jakarta.annotation.Nullable Integer limit) {
        return de.aivot.prosuna.backend.ai.services.AiToolResults.page(listElementTypes().lines()
                .filter(line -> de.aivot.prosuna.backend.ai.services.AiToolResults.matches(query, line)).toList(), offset, limit);
    }

    @Nonnull
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

    @Tool(name = "hole-eingabewert-schema", description = "Liefert das JSON-Schema des Eingabewertes eines Elementtyps, nicht der Elementkonfiguration. Feldabhängige Regeln über hole-knotenkonfigurationsfeld abrufen.")
    public Object getInputValueSchema(@ToolParam(description = "Numerischer Elementtyp aus liste-verfuegbare-elemente.") Integer typeKey) {
        try {
            return valueSchemas.forType(typeKey);
        } catch (IllegalArgumentException exception) {
            return java.util.Map.of("error", exception.getMessage());
        }
    }
}
