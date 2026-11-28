import {type ReactNode} from 'react';
import Person from '@aivot/mui-material-symbols-400-n25-outlined/Person';
import {ElementType} from '../../../data/element-type/element-type';
import {ElementDescriptionContent} from '../element-description-content';
import {DomainUserSelectFieldComponent} from '../../../components/domain-user-select-field/domain-user-select-field-component';
import {AssignmentContextFieldComponent} from '../../../components/assignment-context-field/assignment-context-field-component';
import {DataModelSelectFieldComponent} from '../../../components/data-model-select-field/data-model-select-field-component';
import {DataObjectSelectFieldComponent} from '../../../components/data-object-select-field/data-object-select-field-component';
import {CodeInputFieldComponent} from '../../../components/code-input-field/code-input-field-component';
import {
    createDomainAndUserSelectValueKey,
    createOrgUnitOptionValue,
    createTeamOptionValue,
    createUserOptionValue,
    type DomainAndUserSelectOption,
} from '../../../components/domain-user-select-field/domain-user-select-options';
import {getDepartmentTypeIcons} from '../../../modules/departments/utils/department-utils';
import {ModuleIcons} from '../../../shells/staff/data/module-icons';

const personOptions: DomainAndUserSelectOption[] = [
    {
        value: createOrgUnitOptionValue(1),
        key: createDomainAndUserSelectValueKey(createOrgUnitOptionValue(1)),
        label: 'Ordnungsamt',
        subLabel: 'Bereich',
        group: 'Organisationseinheiten',
        icon: getDepartmentTypeIcons(3),
    },
    {
        value: createTeamOptionValue(7),
        key: createDomainAndUserSelectValueKey(createTeamOptionValue(7)),
        label: 'Bürgerbüro Nord',
        group: 'Teams',
        icon: ModuleIcons.teams,
    },
    {
        value: createUserOptionValue('user_123'),
        key: createDomainAndUserSelectValueKey(createUserOptionValue('user_123')),
        label: 'Wagner, Marie-Therese',
        subLabel: 'm.wagner@example.org',
        group: 'Mitarbeitende',
        icon: <Person/>,
    },
];

export const specializedElementDescriptions: Partial<Record<ElementType, ReactNode>> = {
    [ElementType.DomainAndUserSelect]: (
        <ElementDescriptionContent
            key={ElementType.DomainAndUserSelect}
            usage="Lassen Sie Organisationseinheiten, Teams und einzelne Mitarbeiter:innen auswählen, zum Beispiel einen Personenkreis für einen Bearbeitungsschritt."
            settings={[
                '- **„Auswählbare Typen“:** Legt fest, welche der drei Kategorien angeboten werden.',
                '- **„Minimale Anzahl“** und **„Maximale Anzahl“:** Begrenzen die Anzahl ausgewählter Einträge.',
                '- **„Auswahl auf Prozessrechte beschränken“:** Filtert die Optionen anhand von Prozess-ID, Prozessversion und den benötigten Berechtigungen.',
            ].join('\n')}
            notes="Eine Mindestanzahl macht das Feld nicht zur Pflichtangabe. Für einen Personenkreis mit zusätzlichen Bevorzugungen bei der Zuweisung verwenden Sie **„Verantwortlicher Personenkreis“**."
            technicalDetails={[
                '- Datentyp: Liste (`List`) von Objekten (`Object`). Jeder Eintrag enthält `type` und `id`.',
                '- Die Typen heißen `orgUnit`, `team` und `user`. Kennungen werden als Text gespeichert, zum Beispiel `{"type":"team","id":"7"}`.',
                '- Gespeichert werden die ausgewählten Einträge; eine Organisationseinheit oder ein Team wird dabei nicht durch die Liste seiner Mitglieder ersetzt.',
            ].join('\n')}
        >
            <DomainUserSelectFieldComponent
                label="Personenkreis"
                value={[{type: 'orgUnit', id: '1'}, {type: 'user', id: 'user_123'}]}
                onChange={() => {}}
                options={personOptions}
                hint="Wählen Sie Organisationseinheiten, Teams oder einzelne Mitarbeiter:innen aus."
            />
        </ElementDescriptionContent>
    ),
    [ElementType.AssignmentContext]: (
        <ElementDescriptionContent
            key={ElementType.AssignmentContext}
            usage="Legen Sie den Personenkreis fest, aus dem eine Aufgabe zugewiesen werden soll. Zusätzliche Bevorzugungen berücksichtigen zum Beispiel die zuvor zuständige Person."
            settings={[
                '- **„Auswählbare Typen“:** Beschränkt die Auswahl auf Organisationseinheiten, Teams oder einzelne Mitarbeiter:innen.',
                '- **„Minimale Anzahl“** und **„Maximale Anzahl“:** Begrenzen den Umfang der Auswahl.',
                '- **„Auswahl auf Prozessrechte beschränken“:** Schränkt die angebotenen Einträge anhand eines Prozesses und der benötigten Berechtigungen ein.',
            ].join('\n')}
            notes="Bevorzugungen setzen einen ausgewählten Personenkreis voraus. Für die automatische Zuweisung werden nur Personen mit den erforderlichen Berechtigungen berücksichtigt. Eine Bevorzugung garantiert deshalb keine bestimmte zuständige Person."
            technicalDetails={[
                '- Datentyp: Objekt (`Object`) mit `domainAndUserSelection`, `generalAssigneePreference` und `repeatExecutionAssigneePreference`.',
                '- Der Personenkreis ist eine Liste von Einträgen mit `type` und `id`. Die weiteren Angaben beschreiben die Bevorzugung bei der Zuweisung und bei erneuter Ausführung.',
                '- Der Feldwert beschreibt die Zuweisungsregeln; er ist nicht die Kennung der später tatsächlich zuständigen Person.',
            ].join('\n')}
        >
            <AssignmentContextFieldComponent
                domainAndUserSelectionLabel="Verantwortlicher Personenkreis"
                value={{
                    domainAndUserSelection: [{type: 'orgUnit', id: '1'}, {type: 'user', id: 'user_123'}],
                    generalAssigneePreference: 'previousProcessStepAssignee',
                    repeatExecutionAssigneePreference: 'previousIterationAssignee',
                }}
                onChange={() => {}}
                options={personOptions}
            />
        </ElementDescriptionContent>
    ),
    [ElementType.DataModelSelect]: (
        <ElementDescriptionContent
            key={ElementType.DataModelSelect}
            usage="Lassen Sie genau ein vorhandenes Datenmodell auswählen, zum Beispiel das Datenmodell für ein Adressbuch."
            notes="Die Auswahl setzt Zugriff auf die Datenmodelle voraus. Ein Datenmodell beschreibt den Aufbau seiner Datenobjekte. Für die Auswahl eines konkreten Eintrags verwenden Sie **„Datenobjekt-Auswahl“**."
            technicalDetails={[
                '- Datentyp: Textwert (`String`). Gespeichert wird der Schlüssel des Datenmodells, zum Beispiel `adressen`.',
                '- Der sichtbare Name und die Definition des Datenmodells sind nicht Bestandteil des Feldwerts. Ohne Auswahl steht kein Schlüssel zur Verfügung.',
            ].join('\n')}
        >
            <DataModelSelectFieldComponent
                label="Datenmodell"
                value="adressen"
                onChange={() => {}}
                options={[
                    {key: 'adressen', value: 'adressen', label: 'Adressbuch', subLabel: 'adressen', icon: ModuleIcons.dataModels},
                    {key: 'anliegen', value: 'anliegen', label: 'Anliegen', subLabel: 'anliegen', icon: ModuleIcons.dataModels},
                ]}
            />
        </ElementDescriptionContent>
    ),
    [ElementType.DataObjectSelect]: (
        <ElementDescriptionContent
            key={ElementType.DataObjectSelect}
            usage="Lassen Sie einen vorhandenen Eintrag aus einem Datenmodell auswählen, zum Beispiel einen Kontakt aus einem Adressbuch."
            settings={[
                '- **„Datenmodell“:** Legt fest, aus welchem Datenmodell die Einträge stammen.',
                '- **„Attribut für die Hauptbezeichnung“:** Bestimmt, welcher Attributwert als Bezeichnung angezeigt wird, zum Beispiel der Name eines Kontakts.',
            ].join('\n')}
            notes="Ein Datenmodell muss festgelegt sein. Die Auswahl benötigt Zugriff auf dessen Datenobjekte. Sie referenziert einen vorhandenen Eintrag und legt keinen neuen an."
            technicalDetails={[
                '- Datentyp: Textwert (`String`). Gespeichert wird die Kennung des Datenobjekts, zum Beispiel `obj_001`.',
                '- Die Attribute des Datenobjekts sind nicht Bestandteil des Feldwerts. Für ihre Verarbeitung wird die Kennung zusammen mit dem konfigurierten Datenmodell verwendet.',
            ].join('\n')}
        >
            <DataObjectSelectFieldComponent
                label="Kontakt"
                value="obj_001"
                dataModelKey="adressen"
                dataLabelAttributeKey="name"
                onChange={() => {}}
                options={[
                    {key: 'obj_001', value: 'obj_001', label: 'Max Mustermann', subLabel: 'adressen · obj_001', icon: ModuleIcons.dataObjects},
                    {key: 'obj_002', value: 'obj_002', label: 'Erika Mustermann', subLabel: 'adressen · obj_002', icon: ModuleIcons.dataObjects},
                ]}
            />
        </ElementDescriptionContent>
    ),
    [ElementType.NoCodeInput]: (
        <ElementDescriptionContent
            key={ElementType.NoCodeInput}
            usage="Erfassen Sie eine Regel oder Berechnung als No-Code-Ausdruck. Über **„Bearbeiten“** wird der Ausdruck im integrierten Editor zusammengestellt."
            settings="**„Rückgabetyp“** legt den erwarteten Ergebnistyp des Ausdrucks fest, zum Beispiel Wahrheitswert, Text oder Zahl. Standardmäßig wird ein Wahrheitswert erwartet."
            notes="Dieses Element dient zum Konfigurieren eines Ausdrucks. Welche Datenquellen verfügbar sind und wann der Ausdruck ausgewertet wird, bestimmt der verwendende Prozessschritt."
            technicalDetails={[
                '- Datentyp: Objekt (`Object`) mit dem Eintrag `noCode`, der den strukturierten Ausdruck enthält.',
                '- Der gespeicherte Ausdruck und sein späteres Ergebnis sind unterschiedliche Werte. Der konfigurierte Rückgabetyp beschreibt das Ergebnis, nicht den Datentyp des Feldwerts.',
                '- Ohne hinterlegten Ausdruck ist keine Regel konfiguriert.',
            ].join('\n')}
        />
    ),
    [ElementType.ProcessDataKeyInput]: (
        <ElementDescriptionContent
            key={ElementType.ProcessDataKeyInput}
            usage="Legen Sie fest, auf welchen Datenschlüssel in den Vorgangsdaten eine Konfiguration zugreifen soll, zum Beispiel auf den Namen einer Person."
            settings={[
                '- **„Wildcards deaktivieren“:** Verhindert Datenpfade mit Sternchen für mehrere Listeneinträge.',
                '- **„Untergeordnete Schlüssel von“:** Beschränkt die Auswahl auf Schlüssel unterhalb des Werts eines anderen Datenschlüssel-Feldes.',
            ].join('\n')}
            notes="Im Prozesseditor können verfügbare Datenschlüssel ausgewählt werden. Das Feld erfasst den Verweis auf Daten; es zeigt nicht den dort gespeicherten Inhalt an."
            technicalDetails={[
                '- Datentyp: Textwert (`String`), zum Beispiel `person.name`, `items[0].name` oder bei erlaubten Wildcards `items[*].name`.',
                '- Der angezeigte Präfix `$.` gehört nicht zum gespeicherten Feldwert. Bei einer übergeordneten Auswahl wird der untergeordnete Pfad relativ dazu gespeichert.',
            ].join('\n')}
        />
    ),
    [ElementType.ProcessInstanceAttachmentSetSelect]: (
        <ElementDescriptionContent
            key={ElementType.ProcessInstanceAttachmentSetSelect}
            usage="Wählen Sie im Prozess verfügbare Anlagensätze aus, zum Beispiel Nachweise, die ein späterer Schritt weiterverarbeiten soll."
            settings={[
                '- **„Minimale Anzahl“** und **„Maximale Anzahl“:** Begrenzen die Zahl ausgewählter Anlagensätze.',
                '- Mit **„Maximale Anzahl“** `1` wird eine Einzelauswahl angezeigt.',
            ].join('\n')}
            notes="Die angebotenen Anlagensätze stammen aus dem vorherigen Prozessverlauf. Eine Mindestanzahl ersetzt die Einstellung **„Pflichtangabe“** nicht. Das Feld wählt ganze Anlagensätze, keine einzelnen Dateien."
            technicalDetails={[
                '- Datentyp: Liste (`List`) von Textwerten (`String`) mit den Datenschlüsseln der ausgewählten Anlagensätze.',
                '- Auch bei einer Einzelauswahl bleibt der Wert eine Liste, zum Beispiel `["nachweise"]`. Die Dateien selbst sind nicht Bestandteil des Feldwerts.',
            ].join('\n')}
        />
    ),
    [ElementType.ProcessIdentityIdInput]: (
        <ElementDescriptionContent
            key={ElementType.ProcessIdentityIdInput}
            usage="Wählen Sie eine im Prozess verfügbare Identität aus, zum Beispiel die Identität, an die eine Nachricht oder eine Aufgabe für Beteiligte gerichtet werden soll."
            notes="Die Identität muss im vorherigen Prozessverlauf bereitgestellt worden sein. Dieses Element wählt eine vorhandene Identität aus; die Wege zu ihrer Erfassung legen Sie über **„Identitätseingabe“** fest."
            technicalDetails={[
                '- Datentyp: Textwert (`String`). Gespeichert wird die Kennung der Identität.',
                '- Name, E-Mail-Adresse und weitere Identitätsattribute gehören nicht zum Feldwert. Ohne Auswahl steht keine Kennung zur Verfügung.',
            ].join('\n')}
        />
    ),
    [ElementType.AssetSelectInput]: (
        <ElementDescriptionContent
            key={ElementType.AssetSelectInput}
            usage="Wählen Sie eine bereitgestellte Datei aus, zum Beispiel eine Dokumentvorlage oder ein Bild für eine Konfiguration."
            settings={[
                '- **„Sichtbarkeit“:** Beschränkt die Auswahl auf öffentliche oder private Dateien.',
                '- **„Erlaubte MIME-Typen“:** Filtert nach Dateitypen. Ohne Eintrag werden alle Dateitypen angeboten.',
                '- **„Speicheranbieter“:** Beschränkt die Dateiauswahl auf einen Speicheranbieter einschließlich seiner Ordner.',
            ].join('\n')}
            notes="Die Datei muss über die Dateiauswahl zugänglich sein. Für Anlagen, die eine ausfüllende Person zu einem Vorgang hochladen soll, verwenden Sie **„Anlage(n)“**."
            technicalDetails={[
                '- Datentyp: Textwert (`String`). Gespeichert wird der eindeutige Dateischlüssel als UUID.',
                '- Dateiname, Dateiinhalte und Downloadadresse sind nicht Bestandteil des Feldwerts.',
            ].join('\n')}
        />
    ),
    [ElementType.HtmlTemplateInput]: (
        <ElementDescriptionContent
            key={ElementType.HtmlTemplateInput}
            usage="Wählen Sie eine HTML-Vorlage aus und passen Sie deren vorgesehene Text- oder Bildbereiche an, zum Beispiel für ein später erzeugtes Dokument."
            settings="Über **„HTML-Vorlage auswählen“** hinterlegen Sie eine HTML-Datei. **„Anpassen“** öffnet die in der Vorlage vorgesehenen bearbeitbaren Bereiche."
            notes="Die HTML-Datei muss bereitgestellt sein und passende bearbeitbare Bereiche enthalten. Beim Wechsel der Vorlage werden die bisherigen Anpassungen dieses Feldes zurückgesetzt."
            technicalDetails={[
                '- Datentyp: Objekt (`Object`) mit `assetKey` für den Dateischlüssel der Vorlage und `slots` für die zugeordneten Anpassungen.',
                '- Die Vorlage kennzeichnet Bereiche über `data-slot` und ihren Typ über `data-slot-type`. Unterstützt werden `text`, `richtext` und `image`.',
                '- Der Feldwert enthält die Konfiguration. Das fertige HTML wird erst bei der Verarbeitung der Vorlage erzeugt.',
            ].join('\n')}
        />
    ),
    [ElementType.StoragePathSelector]: (
        <ElementDescriptionContent
            key={ElementType.StoragePathSelector}
            usage="Legen Sie einen Speicheranbieter und einen zugehörigen Pfad für einen Verarbeitungsschritt fest, zum Beispiel den Ablageordner für erzeugte Dokumente."
            settings={[
                '- **„Auswählbare Speicheranbieter-Typen“:** Legt fest, welche Arten von Speicheranbietern angeboten werden.',
                '- **„Schreibgeschützte Speicheranbieter auswählbar“:** Erlaubt zusätzlich Speicheranbieter, die nur gelesen werden können.',
            ].join('\n')}
            notes="Wählen Sie zuerst einen Speicheranbieter und geben Sie dann einen Pfad ein oder wählen Sie einen Ordner. Ein schreibgeschützter Speicheranbieter eignet sich nicht als Ziel zum Speichern neuer Dateien."
            technicalDetails={[
                '- Datentyp: Objekt (`Object`) mit `storageProviderId` als Anbieterkennung und `path` als Textwert.',
                '- Eine vollständige Angabe benötigt beide Werte. Der Pfad bezieht sich auf den ausgewählten Speicheranbieter; die Auswahl selbst überträgt keine Dateien.',
            ].join('\n')}
        />
    ),
    [ElementType.CodeInput]: (
        <ElementDescriptionContent
            key={ElementType.CodeInput}
            usage="Erfassen Sie mehrzeiligen Quelltext mit Syntaxhervorhebung, zum Beispiel ein Low-Code-Skript oder JSON für eine Konfiguration."
            settings={[
                '- **„Sprache“:** Passt Syntaxhervorhebung und Editor-Unterstützung an, zum Beispiel für JavaScript, JSON oder HTML.',
                '- **„Editor-Höhe“** und **„Zeilenumbruch aktivieren“:** Bestimmen die Darstellung längerer Inhalte.',
            ].join('\n')}
            notes="Die gewählte Sprache steuert die Editor-Unterstützung. Ob und wie der erfasste Code ausgeführt oder verarbeitet wird, bestimmt die Konfiguration, die dieses Feld verwendet."
            technicalDetails={[
                '- Datentyp: Textwert (`String`). Auch JSON wird als Quelltext gespeichert, nicht als Datenobjekt.',
                '- Der Feldwert ist der erfasste Code, nicht dessen Ausführungsergebnis.',
            ].join('\n')}
        >
            <CodeInputFieldComponent
                label="JavaScript-Code"
                value={'function greet(name) {\n    return `Hallo ${name}`;\n}'}
                onChange={() => {}}
                language="javascript"
                hint="Der Quelltext wird als Text gespeichert."
                height="240px"
            />
        </ElementDescriptionContent>
    ),
    [ElementType.UiDefinitionInput]: (
        <ElementDescriptionContent
            key={ElementType.UiDefinitionInput}
            usage="Hinterlegen Sie eine UI-Definition für eine konfigurierte Oberfläche. Das Feld zeigt eine Zusammenfassung und öffnet den Editor zur Gestaltung der enthaltenen Formularelemente."
            settings="**„Erwarteter Wurzeltyp“** schränkt den Starttyp der UI-Definition ein, zum Beispiel auf ein Formular oder eine Gruppierung. Ohne Festlegung bleibt der Typ offen."
            notes="Je nach Verwendung öffnet sich die Bearbeitung in einem Dialog oder im zugehörigen Editor. Die verfügbaren Elemente richten sich nach dem vorgesehenen Einsatz der Oberfläche."
            technicalDetails={[
                '- Datentyp: Objekt (`Object`) mit dem Wurzelelement der UI-Definition und gegebenenfalls dessen untergeordneten Elementen.',
                '- Gespeichert wird die Definition der Oberfläche. Die später darin eingegebenen Angaben sind nicht Bestandteil dieses Feldwerts.',
            ].join('\n')}
        />
    ),
    [ElementType.IdentityConfigElement]: (
        <ElementDescriptionContent
            key={ElementType.IdentityConfigElement}
            usage="Legen Sie fest, welche Identitäten im Prozess erfasst werden sollen und welche Identitätsanbieter dafür zur Verfügung stehen. Optional kann eine direkte E-Mail-Eingabe als Alternative zugelassen werden."
            settings={[
                '- **„Titel“** und **„Beschreibung“:** Erläutern der ausfüllenden Person die jeweilige Identität.',
                '- **„Aktive Identitätsanbieter“** und **„E-Mail-Adresse zulassen“:** Bestimmen die verfügbaren Wege zur Erfassung.',
                '- **„Eindeutiger Schlüssel“:** Legt fest, wie die Identität im Prozess referenziert wird.',
            ].join('\n')}
            notes="Jede konfigurierte Identität benötigt einen Titel und mindestens einen Identitätsanbieter oder die E-Mail-Alternative. Bei bestimmten Identitätsanbietern ist zusätzlich ein Mindest-Vertrauensniveau erforderlich."
            technicalDetails={[
                '- Datentyp: Liste (`List`) von Konfigurationsobjekten, unter anderem mit `id`, `title`, `allowsMail`, `isOptional` und den Anbieteroptionen in `options`.',
                '- Der Feldwert beschreibt, wie Identitäten erfasst werden sollen. Die später angemeldeten Personen und ihre Identitätsattribute sind ein gesondertes Ergebnis des Prozessschritts.',
            ].join('\n')}
        />
    ),
    [ElementType.PaymentConfigElement]: (
        <ElementDescriptionContent
            key={ElementType.PaymentConfigElement}
            usage="Legen Sie fest, über welchen Zahlungsanbieter eine Zahlung abgewickelt werden soll und welche Zahlungspositionen sie umfasst."
            settings={[
                '- **„Zahlungsanbieter“**, **„Verwendungszweck“** und **„Beschreibung“:** Bestimmen Anbieter und Angaben zur Zahlung.',
                '- **„Zahlungspositionen“:** Legt Beträge, Mengen und Steuersätze fest. Beträge und Mengen können fest angegeben oder berechnet werden.',
                '- **„Personen- oder Organisationsangaben zuordnen“:** Übernimmt Angaben zur zahlenden Person oder Organisation aus den Vorgangsdaten.',
            ].join('\n')}
            notes="Ein Zahlungsanbieter muss eingerichtet sein. Eine vollständige Konfiguration benötigt einen Verwendungszweck, eine Beschreibung und mindestens eine Zahlungsposition. Die Zahlung wird im dafür vorgesehenen Prozessschritt abgewickelt."
            technicalDetails={[
                '- Datentyp: Objekt (`Object`) mit der Zahlungskonfiguration, unter anderem `paymentProviderKey`, `purpose`, `description` und der Liste `items`.',
                '- Berechnungen für Beträge und Mengen werden als Ausdrücke oder Skripte hinterlegt und später ausgewertet.',
                '- Die Konfiguration ist kein Zahlungsnachweis. Zahlungsstatus und Ergebnisse der Abwicklung gehören nicht zu diesem Feldwert.',
            ].join('\n')}
        />
    ),
};
