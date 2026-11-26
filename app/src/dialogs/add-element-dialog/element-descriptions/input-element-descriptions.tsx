import React, {type ReactNode, useState} from 'react';
import {Box, Typography} from '@mui/material';
import {ElementType} from '../../../data/element-type/element-type';
import {CheckboxFieldComponent} from '../../../components/checkbox-field/checkbox-field-component';
import {MultiChoiceCombobox} from '../../../components/multi-checkbox-field/multi-choice-combobox';
import {MultiCheckboxComponent} from '../../../components/multi-checkbox-field/multi-checkbox-component';
import {ChipInputFieldComponent} from '../../../components/chip-input-field/chip-input-field-component';
import {RichTextInputComponent} from '../../../components/rich-text-input-component/rich-text-input-component';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';
import {TableFieldComponent2} from '../../../components/table-field/table-field-component-2';
import {FileUploadComponent} from '../../../components/file-upload-field/file-upload-component';
import {ElementDescriptionContent} from '../element-description-content';

function ChipInputExamples() {
    const [topics, setTopics] = useState<string[] | null>(['Wohnen', 'Familie']);
    const [references, setReferences] = useState<string[] | null>(null);

    return (
        <>
            <Box>
                <ChipInputFieldComponent
                    label="Themen"
                    value={topics}
                    onChange={setTopics}
                    placeholder="Thema hinzufügen"
                    suggestions={['Wohnen', 'Familie', 'Bildung', 'Gesundheit']}
                    minItems={2}
                    maxItems={5}
                    required
                    hint="Wählen Sie Vorschläge oder ergänzen Sie eigene Themen."
                />
            </Box>
            <Box>
                <ChipInputFieldComponent
                    label="Aktenzeichen"
                    value={references}
                    onChange={setReferences}
                    placeholder="z. B. AZ-2026-001"
                    maxItemLength={20}
                    hint="Fügen Sie mehrere Aktenzeichen zeilenweise ein. Höchstens 20 Zeichen je Eintrag."
                />
            </Box>
        </>
    );
}

export const inputElementDescriptions: Partial<Record<ElementType, ReactNode>> = {
    [ElementType.Checkbox]: (
        <ElementDescriptionContent
            key={ElementType.Checkbox}
            usage="Erfassen Sie eine einzelne Bestätigung oder lassen Sie eine Option aktivieren, zum Beispiel eine abweichende Lieferadresse."
            settings="Mit **„Pflichtangabe“** legen Sie fest, dass das Feld ausgewählt werden muss."
            notes="Für eine Frage mit den ausdrücklichen Antworten „Ja“ und „Nein“ verwenden Sie **„Einzelauswahl“** mit Optionsfeldern. Ein nicht ausgewähltes Bestätigungsfeld unterscheidet nicht zwischen einer bewussten Ablehnung und einer ausgelassenen Bestätigung."
            technicalDetails={[
                '- Datentyp: Wahrheitswert (`Boolean`). Ausgewählt liefert das Feld `true`, nicht ausgewählt `false`.',
                '- Verwenden Sie den Wahrheitswert für Bedingungen und Vergleiche in No-Code-Ausdrücken; die Beschriftung wird nicht als Wert gespeichert.',
            ].join('\n')}
        >
            <CheckboxFieldComponent
                label="Abweichende Lieferadresse verwenden"
                hint="Aktivieren Sie diese Option, wenn die Lieferung an eine andere Adresse erfolgen soll."
                value
                onChange={() => {}}
            />
        </ElementDescriptionContent>
    ),
    [ElementType.MultiCheckbox]: (
        <ElementDescriptionContent
            key={ElementType.MultiCheckbox}
            usage="Lassen Sie mehrere Optionen aus einer festgelegten Liste auswählen, zum Beispiel Tätigkeitsbereiche oder benötigte Leistungen."
            settings={[
                '- **„Darstellung der Auswahl“:** Eine Auswahlfeld-Liste zeigt die Optionen direkt. Die durchsuchbare Auswahl filtert längere Listen beim Tippen.',
                '- **„Optionen definieren über“:** Hinterlegen Sie Optionen manuell oder übernehmen Sie sie aus einer System-Codeliste.',
                '- Mit **„Mindestanzahl“** und **„Maximalanzahl“** begrenzen Sie die Anzahl ausgewählter Optionen.',
            ].join('\n')}
            notes="Eigene Texte sind nicht erlaubt. Verwenden Sie dafür **„Mehrfach-Texteingabe“**. Wenn genau eine Option gewählt werden soll, verwenden Sie **„Einzelauswahl“**."
            technicalDetails={[
                '- Datentyp: Liste (`List`) von Textwerten (`String`). Gespeichert werden die Optionswerte, nicht ihre sichtbaren Beschriftungen.',
                '- Die Auswahl „Bildung“ und „Wohnen“ kann zum Beispiel `["bildung", "wohnen"]` liefern. Auch eine einzelne Auswahl bleibt eine Liste.',
                '- Prüfen Sie in No-Code-Ausdrücken, ob die Liste den gesuchten Optionswert enthält. Ohne Auswahl ist kein Optionswert vorhanden.',
            ].join('\n')}
        >
            <MultiCheckboxComponent
                label="Benötigte Leistungen"
                value={['beratung', 'bescheinigung']}
                options={[
                    {value: 'beratung', label: 'Beratung'},
                    {value: 'bescheinigung', label: 'Bescheinigung'},
                    {value: 'termin', label: 'Termin'},
                ]}
                onChange={() => {}}
            />
            <MultiChoiceCombobox
                label="Tätigkeitsbereiche"
                value={['bildung']}
                options={[
                    {value: 'bildung', label: 'Bildung'},
                    {value: 'wohnen', label: 'Wohnen'},
                    {value: 'gesundheit', label: 'Gesundheit'},
                ]}
                onChange={() => {}}
            />
        </ElementDescriptionContent>
    ),
    [ElementType.ChipInput]: (
        <ElementDescriptionContent
            key={ElementType.ChipInput}
            usage="Erfassen Sie mehrere kurze Texte als einzelne Einträge, zum Beispiel Stichwörter oder Aktenzeichen. Die Einträge können frei eingegeben oder aus Vorschlägen übernommen werden."
            settings={[
                '- **„Minimale Anzahl“**, **„Maximale Anzahl“** und **„Maximale Zeichenanzahl je Eintrag“:** Begrenzen Sie Anzahl und Länge der Einträge.',
                '- **„Vorschläge definieren über“:** Hinterlegen Sie Vorschläge manuell oder aus einer System-Codeliste.',
                '- Mit **„Trennzeichen beim Einfügen“** legen Sie fest, wie eingefügte Texte aufgeteilt werden. Über **„Doppelte Einträge erlauben“** steuern Sie Wiederholungen.',
            ].join('\n')}
            notes="Einträge werden mit Enter oder beim Verlassen des Feldes übernommen. Mehrere Einträge lassen sich aus der Zwischenablage einfügen. Wenn nur vorgegebene Optionen erlaubt sein sollen, verwenden Sie **„Mehrfachauswahl“**."
            technicalDetails={[
                '- Datentyp: Liste (`List`) von Textwerten (`String`), zum Beispiel `["Wohnen", "Familie"]`.',
                '- Auch bei Vorschlägen aus einer Codeliste wird der Text selbst gespeichert. Es gibt keinen getrennten technischen Optionswert.',
                '- Leerzeichen am Anfang und Ende eines Eintrags werden entfernt; leere Einträge werden nicht gespeichert.',
                '- Verwenden Sie Listenfunktionen für die Weiterverarbeitung. Die Einträge sind kein einzelner, durch Kommas getrennter Text.',
            ].join('\n')}
        >
            <ChipInputExamples/>
        </ElementDescriptionContent>
    ),
    [ElementType.RichTextInput]: (
        <ElementDescriptionContent
            key={ElementType.RichTextInput}
            usage="Erfassen Sie Texte, die bereits bei der Eingabe formatiert werden können, zum Beispiel Erläuterungen mit Fettschrift und Listen."
            settings={[
                '- **„Maximalanzahl an Zeichen“:** Begrenzt den gespeicherten Text einschließlich der Formatierungszeichen.',
                '- **„Reduzierte Werkzeugleiste erzwingen“:** Beschränkt die angebotenen Formatierungen in der Bearbeitungsoberfläche.',
            ].join('\n')}
            notes="Formulare für Beteiligte verwenden eine reduzierte Werkzeugleiste. Die Bearbeitungsoberfläche bietet zusätzliche Formatierungen und bei bearbeitbaren Feldern die Ansicht **„Markdown-Quelle“**. Für unformatierten Text verwenden Sie **„Texteingabe“**, für reine Erläuterungen ohne Eingabe **„Textanzeige“**."
            technicalDetails={[
                '- Datentyp: Textwert (`String`) mit Markdown-Auszeichnung. Ein fett dargestelltes Wort kann zum Beispiel als `**Hinweis**` gespeichert werden.',
                '- Formatierungszeichen und Linkziele zählen zur Zeichenbegrenzung. Textvergleiche in No-Code-Ausdrücken beziehen sich auf den gespeicherten Markdown-Text.',
            ].join('\n')}
        >
            <RichTextInputComponent
                label="Beschreibung des Anliegens"
                value={'Bitte beachten Sie **folgende Punkte**:\n\n- Veranstaltungsort\n- Geplanter Termin'}
                onChange={() => {}}
                hint="Gestalten Sie Ihren Text zum Beispiel mit Fettschrift oder Listen."
            />
        </ElementDescriptionContent>
    ),
    [ElementType.Table]: (
        <ElementDescriptionContent
            key={ElementType.Table}
            usage="Erfassen Sie gleichartig aufgebaute Angaben in Zeilen mit Text- und Zahlenspalten, zum Beispiel Bezeichnungen und Mengen."
            settings={[
                '- Legen Sie pro Spalte **„Schlüssel“** und **„Titel“** fest. Über **„Zahl-Angabe“** wählen Sie eine Zahlenspalte; **„Dezimalstellen“** bestimmt deren Genauigkeit.',
                '- **„Optionale Angabe“** erlaubt leere Zellen in einer Spalte. Mit den Einstellungen zur Mindest- und Maximalanzahl begrenzen Sie die Zahl der Zeilen.',
            ].join('\n')}
            notes="Für andere Feldarten oder umfangreichere Angaben pro Eintrag verwenden Sie **„Feldgruppe für mehrere Einträge“**. Eine Mindestanzahl an Zeilen ersetzt nicht die Einstellung **„Pflichtangabe“**."
            technicalDetails={[
                '- Datentyp: Liste (`List`) von Objekten (`Object`). Jede Zeile enthält die Zellwerte unter den konfigurierten Spaltenschlüsseln.',
                '- Beispiel: `[{"bezeichnung":"Unterlage A","menge":2}]`. Zahlenspalten liefern Zahlenwerte, Textspalten Textwerte.',
                '- Verwenden Sie bei der Weiterverarbeitung die Spaltenschlüssel, nicht die sichtbaren Spaltentitel. Ohne Zeilen ist kein Tabellenwert vorhanden.',
            ].join('\n')}
        >
            <TableFieldComponent2
                label="Benötigte Unterlagen"
                fields={[
                    {key: 'bezeichnung', label: 'Bezeichnung'},
                    {key: 'menge', label: 'Menge', type: 'number'},
                ]}
                value={[
                    {bezeichnung: 'Unterlage A', menge: 2},
                    {bezeichnung: 'Unterlage B', menge: 1},
                ]}
                createDefaultRow={() => ({bezeichnung: '', menge: 1})}
                onChange={() => {}}
                readOnly
            />
        </ElementDescriptionContent>
    ),
    [ElementType.ReplicatingContainer]: (
        <ElementDescriptionContent
            key={ElementType.ReplicatingContainer}
            usage="Erfassen Sie mehrere Einträge mit denselben Formularelementen, zum Beispiel Angaben zu mehreren Personen oder Fahrzeugen. Jeder Eintrag erhält eine eigene Gruppe von Feldern."
            settings={[
                '- Legen Sie die enthaltenen Formularelemente fest und passen Sie **„Überschrift des einzelnen Datensatzes“** sowie die Beschriftungen zum Hinzufügen und Entfernen an.',
                '- Mit der Mindest- und Maximalanzahl begrenzen Sie die Anzahl der Einträge. **„Sortieren der Datensätze erlauben“** ermöglicht es, ihre Reihenfolge zu ändern.',
            ].join('\n')}
            notes="Verwenden Sie die Feldgruppe, wenn ein Eintrag unterschiedliche Feldarten benötigt. Für eine kompakte Liste ausschließlich aus Text- und Zahlenspalten eignet sich **„Tabelle“**. Eine Mindestanzahl ersetzt nicht die Einstellung **„Pflichtangabe“**."
            technicalDetails={[
                '- Datentyp: Liste (`List`) mit den Werten der enthaltenen Formularelemente je Eintrag. Die Datentypen dieser Felder bleiben erhalten.',
                '- Intern besitzt jeder Eintrag eine Kennung `id` und seine Feldwerte unter `values`, zum Beispiel `{"id":"person-1","values":{"vorname":"Max"}}` bei einem Feld mit der Kennung `vorname`.',
                '- Die Liste unterscheidet sich von Tabellenzeilen: Die Feldwerte sind innerhalb eines Eintrags unter `values` zusammengefasst.',
            ].join('\n')}
        >
            <Box sx={{display: 'flex', flexDirection: 'column', gap: 1.5}}>
                {[1, 2].map((index) => (
                    <Box key={index} sx={{p: 2, border: '1px solid', borderColor: 'divider', borderRadius: 2}}>
                        <Typography variant="subtitle2">Person {index}</Typography>
                        <TextFieldComponent label="Vorname" value={index === 1 ? 'Max' : 'Erika'} onChange={() => {}}/>
                        <TextFieldComponent label="Nachname" value={index === 1 ? 'Mustermann' : 'Beispiel'} onChange={() => {}}/>
                    </Box>
                ))}
            </Box>
        </ElementDescriptionContent>
    ),
    [ElementType.FileUpload]: (
        <ElementDescriptionContent
            key={ElementType.FileUpload}
            usage="Lassen Sie Dateien als Anlagen zu einem Vorgang übermitteln, zum Beispiel Nachweise im PDF-Format oder Fotos."
            settings={[
                '- **„Erlaubte Dateiendungen“:** Legen Sie fest, welche Dateiarten angenommen werden. **„Dateiname bei Übermittlung“** bestimmt den Namen für die weitere Verarbeitung.',
                '- **„Mehrere Anlagen zulässig“** erlaubt mehrere Dateien. **„Mindestanzahl an Anlagen“** und **„Maximalanzahl an Anlagen“** begrenzen dann die Anzahl.',
            ].join('\n')}
            notes="Pro Datei sind höchstens 10 MB zulässig. Weitere Grenzen bei der Übertragung an Folgesysteme hängen von der jeweiligen Anbindung ab. Für bereits vorhandene Anlagen eines Vorgangs verwenden Sie **„Anlagensatz-Anzeige“**."
            technicalDetails={[
                '- Datentyp: Liste (`List`) von Dateiobjekten, auch wenn nur eine Anlage erlaubt ist.',
                '- Nach dem Hochladen enthält ein Eintrag `name`, `uri` und `size`; zusätzlich kann `originalFileName` den ursprünglichen Dateinamen enthalten. `size` gibt die Dateigröße in Byte an.',
                '- Die Liste enthält Dateiverweise und Metadaten, nicht den Dateiinhalt. Für die Weiterverarbeitung werden die Anlagen über die vorgesehenen Datei- und Prozessfunktionen verwendet.',
                '- Beispiel für Metadaten: `{"name":"Nachweis.pdf","size":245000}`. Der vollständige Eintrag enthält außerdem den beim Hochladen vergebenen Verweis `uri`.',
            ].join('\n')}
        >
            <FileUploadComponent
                id="example-file-upload"
                label="Nachweise hochladen"
                value={undefined}
                onChange={() => {}}
                extensions={['pdf', 'png', 'jpg']}
                hint="PDF-, PNG- oder JPG-Dateien können hier hochgeladen werden."
                isMultifile
                maxFiles={3}
            />
        </ElementDescriptionContent>
    ),
    [ElementType.MapPoint]: (
        <ElementDescriptionContent
            key={ElementType.MapPoint}
            usage="Lassen Sie einen Standort auf einer Karte auswählen oder über eine Adresse suchen."
            settings="**„Start-Breitengrad“** und **„Start-Längengrad“** legen das Kartenzentrum fest, solange kein Punkt gewählt wurde. **„Start-Zoomstufe“** bestimmt den anfänglichen Kartenausschnitt."
            notes="Das Element befindet sich in einer technischen Preview; ein produktiver Einsatz wird derzeit nicht empfohlen. Die Karte und die Adresssuche benötigen Zugriff auf die verwendeten OpenStreetMap- und Nominatim-Dienste."
            technicalDetails={[
                '- Datentyp: Objekt (`Object`) mit `latitude` (Breitengrad), `longitude` (Längengrad) und `address` (Adressbeschreibung).',
                '- Beispiel: `{"latitude":52.52,"longitude":13.405,"address":"Berlin, Deutschland"}`. Die Koordinaten sind Zahlenwerte; die Adressbeschreibung ist ein Textwert.',
                '- Verwenden Sie für die Weiterverarbeitung die benötigte Objekteigenschaft. Eine Adressbeschreibung kann fehlen; sie ist keine getrennt aufgegliederte postalische Adresse.',
            ].join('\n')}
        />
    ),
};
