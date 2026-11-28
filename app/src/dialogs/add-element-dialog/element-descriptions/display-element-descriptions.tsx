import React, {type ReactNode} from 'react';
import {Box, Button, Typography} from '@mui/material';
import Info from '@aivot/mui-material-symbols-400-n25-outlined/Info';
import OpenInNew from '@aivot/mui-material-symbols-400-n25-outlined/OpenInNew';
import CheckCircle from '@aivot/mui-material-symbols-400-n25-outlined/CheckCircle';
import {ElementType} from '../../../data/element-type/element-type';
import {AppInfo} from '../../../app-info';
import {AlertComponent} from '../../../components/alert/alert-component';
import {HeadlineComponent} from '../../../components/headline/headline-component';
import {RichtextComponent} from '../../../components/richtext/richtext.component';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';
import {ProcessAttachmentDisplayComponent} from '../../../components/process-attachment-display/process-attachment-display-component';
import {TextFieldSummary} from '../../../summaries/text-field-summary';
import {NumberSummary} from '../../../summaries/number-summary';
import {generateElementWithDefaultValues} from '../../../utils/generate-element-with-default-values';
import {ElementDescriptionContent} from '../element-description-content';

function SummaryExample() {
    const data = {
        authoredElementValues: {},
        derivedData: {elementStates: {}, effectiveValues: {}},
    };

    return <Box>
        <TextFieldSummary
            {...data}
            model={{...generateElementWithDefaultValues(ElementType.Text), label: 'Vorname'}}
            value="Max"
        />
        <TextFieldSummary
            {...data}
            model={{...generateElementWithDefaultValues(ElementType.Text), label: 'Nachname'}}
            value="Mustermann"
        />
        <NumberSummary
            {...data}
            model={{...generateElementWithDefaultValues(ElementType.Number), label: 'Alter'}}
            value={42}
        />
    </Box>;
}

export const displayElementDescriptions: Partial<Record<ElementType, ReactNode>> = {
    [ElementType.Alert]: (
        <ElementDescriptionContent
            key={ElementType.Alert}
            usage="Heben Sie wichtige Informationen hervor, zum Beispiel Hinweise zu benötigten Nachweisen oder Warnungen vor unvollständigen Angaben."
            settings={[
                '- **„Hinweistyp“:** Wählen Sie zwischen Information, Erfolg, Warnung und Fehler.',
                '- **„Titel“** und **„Text“:** Benennen und erläutern Sie den Hinweis. Der Text kann formatiert werden.',
            ].join('\n')}
            notes="Ein Hinweis zeigt Informationen an. Er prüft keine Eingaben und verhindert für sich genommen keine Übermittlung."
        >
            <AlertComponent color="info" title="Benötigte Nachweise"
                            text="Halten Sie die Nachweise als PDF bereit."/>
            <AlertComponent color="warning" title="Frist beachten"
                            text="Übermitteln Sie Ihre Angaben bis zum genannten Termin."/>
        </ElementDescriptionContent>
    ),
    [ElementType.Image]: (
        <ElementDescriptionContent
            key={ElementType.Image}
            usage="Ergänzen Sie die Oberfläche um eine Grafik, zum Beispiel eine Erläuterung oder ein Logo."
            settings={[
                '- **„URL (Link zur Grafikdatei)“:** Wählen Sie ein Bild aus **„Dateien und Medien“** oder geben Sie eine erreichbare Bild-URL an.',
                '- **„Bildbeschreibung (Caption)“:** Ergänzen Sie bei Bedarf eine sichtbare Bildunterschrift.',
                '- **„Alternativtext (Alt-Text)“:** Beschreiben Sie den relevanten Bildinhalt für die Nutzung mit Screenreadern.',
            ].join('\n')}
            notes="Bilder werden auf hellen und dunklen Hintergründen unverändert verwendet. Prüfen Sie die Erkennbarkeit in beiden Darstellungen und vermitteln Sie wichtige Informationen auch als Text."
        >
            <Box component="figure" sx={{m: 0, maxWidth: 380}}>
                <Box component="img"
                     src={`${AppInfo.mode === 'staff' ? '/staff' : ''}/assets/images/prosuna-beispiel-grafik.jpg`}
                     alt="Prosuna-Logo auf blauem Hintergrund"
                     sx={{display: 'block', width: '100%', borderRadius: 2, border: '1px solid', borderColor: 'divider'}}/>
                <Typography component="figcaption" variant="caption" color="text.secondary"
                            sx={{display: 'block', mt: 0.75}}>
                    Beispielgrafik mit optionaler Bildunterschrift
                </Typography>
            </Box>
        </ElementDescriptionContent>
    ),
    [ElementType.LinkButton]: (
        <ElementDescriptionContent
            key={ElementType.LinkButton}
            usage="Bieten Sie einen Link als Schaltfläche an oder lösen Sie eine vom Prozesselement unterstützte Aufgabenaktion aus."
            settings={[
                '- **„Aktion“:** Wählen Sie einen Link oder eine Aufgabenaktion für Mitarbeitende beziehungsweise externe Beteiligte.',
                '- **„Beschriftung“**, **„Variante“**, **„Farbe“** und **„Symbol (Icon)“:** Legen Sie die Darstellung fest.',
                '- Bei Links bestimmen Sie über **„In neuem Tab öffnen“**, wo das Ziel angezeigt wird.',
            ].join('\n')}
            notes="Aufgabenaktionen funktionieren nur in der passenden Aufgabenansicht. Außerhalb dieser Ansicht ist die Schaltfläche deaktiviert. Vor einer Aufgabenaktion werden die Formulareingaben geprüft."
            technicalDetails="Für Aufgabenaktionen geben Sie unter **„Ereigniskennung“** eine vom Prozesselement unterstützte Kennung an. Eine frei gewählte Kennung legt keine neue Aktion an. Die Schaltfläche erzeugt keinen eigenen Eingabewert."
        >
            <Box sx={{display: 'flex', flexWrap: 'wrap', gap: 2}}>
                <Button variant="outlined" startIcon={<Info/>} endIcon={<OpenInNew/>}>
                    Website öffnen
                </Button>
                <Button variant="contained" color="success" startIcon={<CheckCircle/>}>
                    Aufgabe abschließen
                </Button>
            </Box>
        </ElementDescriptionContent>
    ),
    [ElementType.GroupLayout]: (
        <ElementDescriptionContent
            key={ElementType.GroupLayout}
            usage="Fassen Sie zusammengehörige Formularelemente zusammen, zum Beispiel die Felder für Kontaktdaten."
            settings="Fügen Sie die gewünschten Elemente innerhalb der Gruppierung hinzu und legen Sie dort ihre Reihenfolge und Eigenschaften fest."
            notes="Eine Gruppierung erzeugt keine eigene sichtbare Überschrift. Fügen Sie dafür ein Element **„Überschrift“** hinzu. Für wiederholbare Angaben verwenden Sie **„Feldgruppe für mehrere Einträge“**."
            technicalDetails="Die Gruppierung erzeugt keinen eigenen Eingabewert. Für die Weiterverarbeitung verwenden Sie die Werte der enthaltenen Eingabefelder."
        >
            <Box sx={{p: 2, border: '1px solid', borderColor: 'divider', borderRadius: 2,
                      display: 'flex', flexDirection: 'column', gap: 2}}>
                <HeadlineComponent content="Kontaktdaten" small/>
                <TextFieldComponent label="Vorname" value="Max" onChange={() => {}}/>
                <TextFieldComponent label="Nachname" value="Mustermann" onChange={() => {}}/>
            </Box>
        </ElementDescriptionContent>
    ),
    [ElementType.Headline]: (
        <ElementDescriptionContent
            key={ElementType.Headline}
            usage="Gliedern Sie die Oberfläche mit Überschriften in verständliche Themenbereiche."
            settings={[
                '- **„Überschrift“:** Geben Sie den anzuzeigenden Text an.',
                '- **„Kompakte Überschrift verwenden“:** Zeigen Sie die Überschrift mit kleinerer Schrift und weniger Abstand an.',
                '- **„In Großbuchstaben anzeigen“:** Heben Sie die Überschrift bei Bedarf zusätzlich hervor.',
            ].join('\n')}
            notes="Die reguläre und die kompakte Darstellung sind semantisch gleichrangig: Screenreader erkennen beide als Überschrift derselben Ebene."
        >
            <HeadlineComponent content="Persönliche Angaben" small={false}/>
            <HeadlineComponent content="Kontaktdaten" small/>
        </ElementDescriptionContent>
    ),
    [ElementType.RichText]: (
        <ElementDescriptionContent
            key={ElementType.RichText}
            usage="Zeigen Sie Erläuterungen oder ergänzende Informationen als formatierten Text an."
            settings="Gestalten Sie **„Anzuzeigender Text“** zum Beispiel mit Fettschrift, Listen und Links."
            notes="Dieses Element zeigt einen von Ihnen hinterlegten Text an. Wenn Personen selbst formatierten Text eingeben sollen, verwenden Sie **„Texteingabe mit Formatierung“**."
        >
            <RichtextComponent content={'Bitte halten Sie **folgende Nachweise** bereit:\n\n- Einkommensnachweis\n- Mietvertrag'}/>
        </ElementDescriptionContent>
    ),
    [ElementType.Spacer]: (
        <ElementDescriptionContent
            key={ElementType.Spacer}
            usage="Schaffen Sie gezielt zusätzlichen Abstand zwischen Inhalten, um Themenbereiche optisch zu trennen."
            settings="Legen Sie unter **„Abstand“** die Höhe in Pixeln fest."
            notes="Der Abstand fügt eine leere Fläche ein. Für eine inhaltliche Gliederung verwenden Sie zusätzlich Überschriften oder Gruppierungen."
        >
            <Box sx={{p: 2, border: '1px solid', borderColor: 'divider', borderRadius: 2}}>
                <Typography variant="body2">Persönliche Angaben</Typography>
                <Box sx={{height: 32}}/>
                <Typography variant="body2">Weitere Hinweise</Typography>
            </Box>
        </ElementDescriptionContent>
    ),
    [ElementType.SummaryLayout]: (
        <ElementDescriptionContent
            key={ElementType.SummaryLayout}
            usage="Stellen Sie ausgewählte Angaben als kompakte Übersicht dar, zum Beispiel bereits bekannte Kontaktdaten innerhalb einer Aufgabe."
            settings="Fügen Sie die Elemente hinzu, deren Beschriftungen und Werte in der Übersicht erscheinen sollen."
            notes="Die enthaltenen Elemente zeigen Werte an und nehmen keine Eingaben entgegen. Für die Übersicht aller Formularangaben verwenden Sie den Abschnitt **„Zusammenfassung“**."
            technicalDetails="Die Zusammenfassung erzeugt keinen eigenen Eingabewert. Die Datentypen der angezeigten Werte richten sich nach den enthaltenen Elementen."
        >
            <SummaryExample/>
        </ElementDescriptionContent>
    ),
    [ElementType.ProcessAttachmentDisplay]: (
        <ElementDescriptionContent
            key={ElementType.ProcessAttachmentDisplay}
            usage="Stellen Sie vorhandene Anlagen eines Vorgangs zum Ansehen und Herunterladen bereit, zum Beispiel Nachweise zur Prüfung oder erstellte Bescheide."
            settings={[
                '- **„Schlüssel des Anlagensatzes“:** Wählen Sie aus, welche Anlagen angezeigt werden.',
                '- **„Beschriftung“** und **„Hinweis“:** Erläutern Sie bei Bedarf, um welche Anlagen es sich handelt.',
            ].join('\n')}
            notes="Das Element lädt keine neuen Dateien hoch. Im Modellierungsmodus wird eine Vorschau angezeigt; Anlagen können dort nicht angesehen oder heruntergeladen werden."
            technicalDetails="Es werden Anlagen aus allen Anlagensätzen des Vorgangs mit dem konfigurierten Schlüssel angezeigt. Der Schlüssel bezeichnet den Anlagensatz, keinen Dateinamen. Die Anzeige erzeugt keinen eigenen Eingabewert."
        >
            <ProcessAttachmentDisplayComponent labelText="Übermittelte Anlagen" items={[
                {key: 'income', fileName: 'Einkommensnachweis.pdf', originalFileName: 'Einkommensnachweis.pdf', sizeInBytes: 245000},
                {key: 'rental', fileName: 'Mietvertrag.pdf', originalFileName: 'Mietvertrag.pdf', sizeInBytes: 1520000},
            ]}/>
        </ElementDescriptionContent>
    ),
    [ElementType.Step]: (
        <ElementDescriptionContent
            key={ElementType.Step}
            usage="Teilen Sie ein mehrstufiges Formular in überschaubare Abschnitte auf, zum Beispiel persönliche Angaben und Nachweise. Innerhalb eines Standardabschnitts können Sie Eingaben, Texte und weitere Formularelemente anordnen."
            settings="Geben Sie unter **„Titel des Abschnitts“** eine passende Bezeichnung an und wählen Sie bei Bedarf ein **„Symbol (Icon) für diesen Abschnitt“**."
            notes="Sie können mehrere Standardabschnitte verwenden. Wenn Sie Inhalte innerhalb eines Abschnitts zusammenfassen möchten, verwenden Sie eine Gruppierung."
            technicalDetails="Der Standardabschnitt erzeugt keinen eigenen Eingabewert. Die erfassten Werte stammen aus seinen enthaltenen Eingabefeldern."
        />
    ),
    [ElementType.IntroductionStep]: (
        <ElementDescriptionContent
            key={ElementType.IntroductionStep}
            usage="Führen Sie ausfüllende Personen in das Formular ein, zum Beispiel mit Informationen zu benötigten Dokumenten, Kosten und Fristen. Der Abschnitt enthält auch die Kenntnisnahme der Datenschutzhinweise."
            settings={[
                '- **„Kurzbeschreibung“:** Erläutern Sie den Zweck des Formulars; ein ergänzendes Logo ist möglich.',
                '- **„Kosten“**, **„Fristen des Formulars“** und **„Benötigte Dokumente“:** Bereiten Sie auf das Ausfüllen vor.',
                '- **„Text zu den Datenschutzhinweisen“:** Hinterlegen Sie die Hinweise. Zusätzlich können Sie eigene Formularelemente einfügen.',
            ].join('\n')}
            notes="In einem Formular kann dieser Abschnitt nur einmal hinzugefügt werden. Die Kenntnisnahme der Datenschutzhinweise muss bestätigt werden."
            technicalDetails={[
                '- Die Kenntnisnahme liefert einen Wahrheitswert (`Boolean`): `true` bedeutet bestätigt.',
                '- Ein fehlender Wert oder `false` erfüllt die erforderliche Bestätigung nicht. Werte zusätzlicher Eingabefelder werden getrennt erfasst.',
                '- Mit `{privacy}Hinweise zum Datenschutz{/privacy}` können Sie im Hinweistext den Datenschutzdialog verlinken.',
            ].join('\n')}
        />
    ),
    [ElementType.SummaryStep]: (
        <ElementDescriptionContent
            key={ElementType.SummaryStep}
            usage="Lassen Sie ausfüllende Personen ihre erfassten Angaben vor der Übermittlung prüfen. Die Übersicht ermöglicht die Rückkehr zum zugehörigen Abschnitt und verlangt eine Bestätigung der Datenprüfung."
            notes="Die Übersicht wird aus den Formularangaben zusammengestellt. In einem Formular kann dieser Abschnitt nur einmal hinzugefügt werden. Für eine gezielt zusammengestellte Übersicht innerhalb eines anderen Abschnitts verwenden Sie das Element **„Zusammenfassung“**."
            technicalDetails="Die Bestätigung der Datenprüfung liefert einen Wahrheitswert (`Boolean`). Nur `true` erfüllt die erforderliche Bestätigung; `false` oder ein fehlender Wert reichen nicht aus. Die angezeigten Formularangaben behalten ihre eigenen Datentypen."
        >
            <SummaryExample/>
        </ElementDescriptionContent>
    ),
    [ElementType.SubmitStep]: (
        <ElementDescriptionContent
            key={ElementType.SubmitStep}
            usage="Zeigen Sie abschließende Hinweise zur Übermittlung und zum weiteren Ablauf. Eine Captcha-Prüfung schützt die Übermittlung vor automatisierten Eingaben."
            settings={[
                '- **„Text vor der Übermittlung“** und **„Text nach der Übermittlung“:** Erläutern Sie den Abschluss und den weiteren Ablauf.',
                '- **„Bearbeitungszeit“** und **„Dokumente, die ausfüllende Personen erhalten“:** Informieren Sie über erwartete Ergebnisse.',
            ].join('\n')}
            notes="Dieser Abschnitt wird innerhalb eines Formulars dargestellt und kann dort nur einmal hinzugefügt werden. Für die Übermittlung ist eine gültige Captcha-Bestätigung erforderlich."
            technicalDetails="Der Captcha-Nachweis ist ein technisches Objekt mit `payload` und gegebenenfalls `expiresAt`. Er dient der Sicherheitsprüfung und ist kein fachlicher Eingabewert zur Weiterverarbeitung mit No-Code-Operatoren."
        />
    ),
};
