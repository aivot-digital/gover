import {type AnyElement} from '../../../models/elements/any-element';
import {ElementType} from '../../../data/element-type/element-type';
import {InputMode, isAuthoredInputValue} from '../../../models/input-mode';
import {ORDER_SEGMENT} from '../../../utils/three-way-merge';

type UnionKeys<T> = T extends unknown ? keyof T : never;

// Keep the names aligned with the controls in the form element editors.
const ElementPropertyLabels = {
    addLabel: 'Beschriftung der Schaltfläche „Hinzufügen“',
    alertType: 'Hinweistyp',
    allowDuplicates: 'Doppelte Einträge erlauben',
    allowLocalDomains: 'Lokale Domains zulassen',
    allowReadOnlyStorageProviders: 'Schreibgeschützte Speicheranbieter auswählbar',
    allowedMimeTypes: 'Erlaubte MIME-Typen',
    allowedStorageProviderTypes: 'Auswählbare Speicheranbieter-Typen',
    allowedTypes: 'Auswählbare Typen',
    alt: 'Alternativtext (Alt-Text)',
    assetVisibility: 'Sichtbarkeit',
    attachmentSetKey: 'Schlüssel des Anlagensatzes',
    autocomplete: 'Automatisches Ausfüllen durch den Browser',
    caption: 'Bildbeschreibung (Caption)',
    centerLatitude: 'Start-Breitengrad',
    centerLongitude: 'Start-Längengrad',
    children: 'Elemente',
    codeListKey: 'System-Codeliste',
    color: 'Farbe',
    content: 'Anzuzeigender Text',
    copyValueTemplate: 'Vorlage für den kopierten Wert',
    copyable: 'Kopieren des Wertes in Zwischenablage ermöglichen',
    customerTaskEvent: 'Ereigniskennung',
    dataLabelAttributeKey: 'Attribut für die Hauptbezeichnung',
    dataModelKey: 'Datenmodell',
    decimalPlaces: 'Anzahl der Dezimalstellen',
    defaultCountry: 'Vorausgewähltes Land',
    dependsOnSelectFieldId: 'Abhängiges Auswahlfeld (optional)',
    destinationKey: 'Datenschlüssel',
    dialogTitle: 'Dialogtitel',
    disableAssignmentContextRepeatExecutionAssigneePreferenceOptions: 'Auswahl zur erneuten Ausführung deaktivieren',
    disableProcessInstanceAssigneeOption: 'Auswahl der für den Vorgang verantwortlichen Person deaktivieren',
    disableWildCards: 'Wildcards deaktivieren',
    disabled: 'Eingabe deaktiviert',
    displayContext: 'Darstellungskontext',
    displayInline: 'Optionen nebeneinander anzeigen',
    documentsToAttach: 'Benötigte Dokumente',
    documentsToReceive: 'Dokumente, die ausfüllende Personen erhalten',
    dynamicTextPolicy: 'Einstellungen für dynamische Texte',
    editorHeight: 'Editor-Höhe',
    elementType: 'Erwarteter Wurzeltyp',
    eligiblePersons: 'Berechtigter Personenkreis',
    expectedCosts: 'Kosten',
    expiring: 'Fristen des Formulars',
    extensions: 'Erlaubte Dateiendungen',
    fields: 'Spalten',
    headline: 'Überschrift',
    headlineTemplate: 'Überschrift des einzelnen Datensatzes',
    height: 'Höhe',
    hint: 'Hinweis',
    href: 'Link',
    icon: 'Symbol (Icon)',
    id: 'Kennung',
    initiativeLink: 'Link zur ergänzenden Information',
    initiativeLogoLink: 'Ergänzendes Logo zur Kurzbeschreibung',
    initiativeName: 'Alternativtext für das ergänzende Logo',
    inputModePolicy: 'Erlaubte Eingabearten',
    isMultifile: 'Mehrere Anlagen zulässig',
    isMultiline: 'Mehrzeilige Texteingabe',
    label: 'Titel',
    language: 'Sprache',
    largestUnit: 'Größte Einheit',
    managingDepartmentId: 'Betreuende Organisationseinheit',
    marketplaceLink: 'Marktplatz-Baustein',
    maxCharacters: 'Maximalanzahl an Zeichen',
    maxDuration: 'Höchstdauer',
    maxFiles: 'Maximalanzahl an Anlagen',
    maxItemLength: 'Maximale Zeichenanzahl je Eintrag',
    maxItems: 'Maximale Anzahl',
    maxSlots: 'Maximale Anzahl an Identitäten',
    maximumOptions: 'Maximalanzahl',
    maximumRows: 'Maximalanzahl der hinzuzufügenden Zeilen',
    maximumSets: 'Maximalanzahl der hinzuzufügenden Datensätze',
    metadata: 'Metadaten',
    minCharacters: 'Minimalanzahl an Zeichen',
    minDuration: 'Mindestdauer',
    minFiles: 'Mindestanzahl an Anlagen',
    minItems: 'Minimale Anzahl',
    minimumRequiredOptions: 'Mindestanzahl',
    minimumRequiredRows: 'Mindestanzahl der hinzuzufügenden Zeilen',
    minimumRequiredSets: 'Mindestanzahl der hinzuzufügenden Datensätze',
    mode: 'Uhrzeit-Format',
    name: 'Interner Name',
    offlineSignatureNeeded: 'Das Formular erfordert eine Unterschrift',
    offlineSubmissionText: 'Hinweise zur Abgabe des Formularvordrucks',
    openExternalEditor: 'Externen Editor öffnen',
    openInNewTab: 'In neuem Tab öffnen',
    optionalSlotsAllowed: 'Optionale Identitäten zulassen',
    options: 'Optionen',
    optionsSource: 'Optionen definieren über',
    organization: 'Organisation',
    override: 'Dynamische Struktur',
    pasteSeparators: 'Trennzeichen beim Einfügen',
    pdfTemplateKey: 'PDF-Vorlage',
    placeholder: 'Platzhalter',
    precision: 'Genauigkeit',
    preferredCountries: 'Bevorzugte Länder',
    prefix: 'Präfix',
    presentation: 'Darstellung der Auswahl',
    privacyText: 'Text zu den Datenschutzhinweisen',
    processAccessConstraint: 'Auswahl auf Prozessrechte beschränken',
    publicTitle: 'Öffentlicher Titel & Überschrift des Formulars',
    reducedMode: 'Reduzierte Werkzeugleiste erzwingen',
    removeLabel: 'Beschriftung der Schaltfläche „Entfernen“',
    required: 'Pflichtangabe',
    requiresCommunication: 'Kommunikationsmöglichkeit erforderlich',
    responsibleDepartmentId: 'Verantwortliche Organisationseinheit',
    returnType: 'Rückgabetyp',
    scopeProcessDataKeyInputElementId: 'Untergeordnete Schlüssel von',
    showOnFormIndexPage: 'Im öffentlichen Formularverzeichnis anzeigen',
    small: 'Kompakte Überschrift verwenden',
    src: 'URL (Link zur Grafikdatei)',
    staffTaskEvent: 'Ereigniskennung',
    storageProviderId: 'Speicheranbieter',
    storageProviderSelectHint: 'Hinweis Speicheranbieter-Auswahl',
    submittedFileName: 'Dateiname bei Übermittlung',
    suffix: 'Einheit',
    suggestions: 'Vorschläge',
    supportingDocuments: 'Relevante Dokumente',
    tabTitle: 'Titel des Formulars im Browser-Tab',
    teaserText: 'Kurzbeschreibung',
    technical: 'Verborgenes Feld',
    testProtocolSet: 'Testprotokolle',
    text: 'Text',
    textPostSubmit: 'Text nach der Übermittlung',
    textPreSubmit: 'Text vor der Übermittlung',
    textProcessingTime: 'Bearbeitungszeit',
    title: 'Titel',
    toggleButtons: 'Auswahl über Schaltflächen',
    type: 'Elementtyp',
    uppercase: 'In Großbuchstaben anzeigen',
    validation: 'Validierung',
    validationMode: 'Prüfung',
    value: 'Dynamischer Wert',
    variant: 'Variante',
    visibility: 'Sichtbarkeit',
    weight: 'Breite (Darstellung)',
    width: 'Breite',
    wordWrap: 'Zeilenumbruch aktivieren',
    zoom: 'Start-Zoomstufe',
} satisfies Record<UnionKeys<AnyElement>, string>;

interface PropertySchema {
    labels: Readonly<Record<string, string>>;
    properties?: Readonly<Record<string, PropertySchema>>;
    items?: PropertySchema;
    /** A map's keys are authored identifiers, even when they happen to match a schema property. */
    mapValues?: PropertySchema | true;
}

const codeSchema: PropertySchema = {labels: {code: 'Low-Code-Skript'}};
const referenceSchema: PropertySchema = {
    labels: {source: 'Datenebene', path: 'Datenschlüssel', nodeDataKey: 'Datenschlüssel des Prozesselements', markdown: 'Markdown'},
};
const noCodeSchema: PropertySchema = {
    labels: {
        type: 'Ausdruckstyp', operatorIdentifier: 'Funktion', operands: 'Parameter', elementId: 'Formularelement',
        path: 'Datenschlüssel', nodeDataKey: 'Datenschlüssel des Prozesselements', value: 'Wert',
    },
};
noCodeSchema.properties = {operands: {labels: {}, items: noCodeSchema}};
const conditionSchema: PropertySchema = {
    labels: {
        operator: 'Verknüpfung', conditions: 'Bedingungen', conditionsSets: 'Bedingungsgruppen',
        conditionSetUnmetMessage: 'Fehlermeldung',
    },
};
conditionSchema.properties = {
    conditionsSets: {labels: {}, items: conditionSchema},
    conditions: {labels: {}, items: {labels: {
        operator: 'Vergleich', reference: 'Formularelement', target: 'Vergleichswert', value: 'Wert', conditionUnmetMessage: 'Fehlermeldung',
    }}},
};
const ruleSchema: PropertySchema = {
    labels: {
        type: 'Eingabeart', requirements: 'Fachliche Anforderungen', noCode: 'No-Code-Ausdruck',
        noCodeList: 'Validierungsregeln', javascriptCode: 'Low-Code-Skript', referencedIds: 'Referenzierte Elemente',
        conditionSet: 'Bedingungen', fieldNoCodeMap: 'Eigenschaften',
    },
    properties: {
        noCode: noCodeSchema,
        javascriptCode: codeSchema,
        conditionSet: conditionSchema,
        noCodeList: {labels: {}, items: {labels: {noCode: 'Bedingung', message: 'Fehlermeldung'}, properties: {noCode: noCodeSchema}}},
        fieldNoCodeMap: {labels: {}, mapValues: noCodeSchema},
    },
};

function elementSchema(element?: AnyElement): PropertySchema {
    const labels: Record<string, string> = {...ElementPropertyLabels};
    if (element?.type === ElementType.LinkButton || element?.type === ElementType.ProcessAttachmentDisplay) {
        labels.label = 'Beschriftung';
    }
    if (element?.type === ElementType.Headline) labels.content = 'Überschrift';
    if (element?.type === ElementType.Step) labels.title = 'Titel des Abschnitts';
    if (element?.type === ElementType.Spacer) labels.height = 'Abstand';
    if (element?.type === ElementType.Date || element?.type === ElementType.DateRange) labels.mode = 'Datums-Format';
    if (element?.type === ElementType.Text || element?.type === ElementType.ChipInput) labels.optionsSource = 'Vorschläge definieren über';
    if (element?.type === ElementType.AssignmentContext) labels.text = 'Beschreibungstext';
    return {
        labels,
        properties: {
            value: ruleSchema, validation: ruleSchema, visibility: ruleSchema, override: ruleSchema,
            fields: {labels: {}, items: {labels: {
                key: 'Schlüssel', label: 'Titel', datatype: 'Zahl-Angabe', placeholder: 'Platzhalter',
                decimalPlaces: 'Dezimalstellen', optional: 'Optionale Angabe', disabled: 'Eingabe deaktiviert',
            }}},
            options: {labels: {}, items: {labels: {value: 'Wert', label: 'Beschriftung', group: 'Gruppe'}}},
            metadata: {
                labels: {identitySourceId: 'Zugeordnete Identität', identityMappings: 'Attribute zuordnen', userInfoIdentifier: 'Identitätsattribut'},
                properties: {identityMappings: {labels: {}, mapValues: true}},
            },
            marketplaceLink: {labels: {marketplaceId: 'Kennung', marketplaceVersion: 'Version'}},
            inputModePolicy: {labels: {allowedModes: 'Erlaubte Eingabearten', defaultMode: 'Vorausgewählte Eingabeart', allowedVariableSources: 'Erlaubte Datenebenen'}},
            dynamicTextPolicy: {labels: {variableSuggestionSources: 'Datenebenen für Variablen', outputFormat: 'Ausgabeformat'}},
            processAccessConstraint: {labels: {processId: 'Prozess-ID', processVersion: 'Prozessversion', requiredPermissions: 'Notwendige Berechtigungen'}},
        },
    };
}

export function describeElementProperty(path: string[], elements: unknown[], element?: AnyElement): string {
    return describePropertyPath(path, elements, elementSchema(element));
}

/** Configuration IDs and extension properties have no fixed schema; retain their authored keys. */
export function describeConfigurationProperty(path: string[], values: unknown[]): string {
    return describePropertyPath(path, values, undefined, true);
}

function describePropertyPath(path: string[], values: unknown[], schema?: PropertySchema, authored = false): string {
    const labels: string[] = [];
    let listLabel: string | undefined;
    for (const segment of path) {
        if (authored && values.some(isAuthoredInputValue)) {
            if (segment === 'value' && values.some((value) => isAuthoredInputValue(value) && value.type === InputMode.Literal)) {
                values = childValues(values, segment);
                authored = false;
                continue;
            }
            schema = {
                labels: {type: 'Eingabeart', reference: 'Variable', operand: 'No-Code-Ausdruck', code: 'Low-Code-Skript'},
                properties: {reference: referenceSchema, operand: noCodeSchema},
            };
        }

        if (segment === ORDER_SEGMENT) {
            if (listLabel != null) labels[labels.length - 1] = `Reihenfolge der ${listLabel}`;
            else labels.push('Reihenfolge');
            continue;
        }

        const itemId = /^\[(.*)]$/.exec(segment)?.[1];
        if (itemId != null) {
            values = values.map((value) => Array.isArray(value) ? value.find((item) => record(item)?.id != null && String(record(item)?.id) === itemId) : undefined);
            const name = values.flatMap((value) => {
                const item = record(value);
                return [item?.label, item?.title, item?.name];
            }).find((value) => typeof value === 'string' && value.trim().length > 0);
            labels.push(`Eintrag „${name ?? itemId}“`);
            schema = schema?.items;
            listLabel = undefined;
            continue;
        }

        const label = schema?.mapValues != null ? segment : schema?.labels[segment] ?? segment;
        // javascriptCode.code stores the script in a wrapper whose visible name was already added.
        if (schema !== codeSchema || segment !== 'code') labels.push(label);
        listLabel = schema?.mapValues == null ? schema?.labels[segment] : undefined;
        values = childValues(values, segment);
        schema = schema?.mapValues != null ? (schema.mapValues === true ? undefined : schema.mapValues) : schema?.properties?.[segment];
        authored = false;
    }
    return labels.join(' › ') || 'Wert';
}

function childValues(values: unknown[], key: string): unknown[] {
    return values.map((value) => record(value)?.[key]);
}

function record(value: unknown): Record<string, unknown> | undefined {
    return value != null && typeof value === 'object' && !Array.isArray(value) ? value as Record<string, unknown> : undefined;
}
