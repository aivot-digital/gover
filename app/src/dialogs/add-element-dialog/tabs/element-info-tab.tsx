import React, {type ReactNode} from 'react';
import {Box} from '@mui/material';
import {PhoneNumberFieldComponent} from '../../../components/phone-number-field/phone-number-field-component';
import {EmailFieldComponent} from '../../../components/email-field/email-field-component';
import {NumberFieldComponent} from '../../../components/number-field/number-field-component';
import {RadioFieldComponent} from '../../../components/radio-field/radio-field-component';
import {SelectFieldComponent} from '../../../components/select-field/select-field-component';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';
import {getElementNameForType} from '../../../data/element-type/element-names';
import {getElementIconForType} from '../../../data/element-type/element-icons';
import {ElementType} from '../../../data/element-type/element-type';
import {type AnyElement} from '../../../models/elements/any-element';
import {generateElementWithDefaultValues} from '../../../utils/generate-element-with-default-values';
import {getElementDescriptionForType, getElementGroupLabelForType} from '../element-dialog-metadata';
import {SelectionDetailsPanel} from '../../../components/selection-dialog/selection-details-panel';
import {SelectFieldPresentation} from '../../../models/elements/form/input/select-field-presentation';
import {ElementDescriptionContent} from '../element-description-content';
import {inputElementDescriptions} from '../element-descriptions/input-element-descriptions';
import {temporalElementDescriptions} from '../element-descriptions/temporal-element-descriptions';
import {displayElementDescriptions} from '../element-descriptions/display-element-descriptions';
import {specializedElementDescriptions} from '../element-descriptions/specialized-element-descriptions';

const elementDescriptions: Partial<Record<ElementType, ReactNode>> = {
    [ElementType.PhoneNumber]: (
        <ElementDescriptionContent
            key={ElementType.PhoneNumber}
            usage={'Erfassen Sie Telefonnummern für Rückfragen oder Terminabsprachen.'}
            settings={[
                '- **„Vorausgewähltes Land“:** Legt die Ländervorwahl für leere Eingaben fest. Standardmäßig ist Deutschland vorausgewählt.',
                '- **„Bevorzugte Länder“:** Stellt häufig benötigte Länder an den Anfang der Länderauswahl. Alle übrigen Länder bleiben verfügbar.',
                '- **„Prüfung“:** Die voreingestellte **„Plausibilitätsprüfung“** prüft die mögliche Nummernlänge. Die **„Nummernplanprüfung“** berücksichtigt zusätzlich bekannte Nummernbereiche und Nummernstrukturen.',
            ].join('\n')}
            notes={'Ob eine Nummer vergeben oder erreichbar ist, wird nicht geprüft. Die Nummernplanprüfung kann neue Nummernbereiche ablehnen, wenn sie in den Prüfdaten noch fehlen. Direkt wählbare Durchwahlen gehören zur Nummer; Zusätze wie „danach 7 wählen“ erfassen Sie in einer separaten **„Texteingabe“**.'}
            technicalDetails={[
                '- Datentyp: Textwert (`String`), auch in No-Code-Ausdrücken.',
                '- Akzeptierte Telefonnummern werden im internationalen Format E.164 gespeichert, zum Beispiel `+4930123456`. Leerzeichen und Trennzeichen werden entfernt.',
                '- In der Eingabe zählt die ausgewählte Ländervorwahl allein noch nicht als Telefonnummer.',
            ].join('\n')}
        >
            <Box>
                <PhoneNumberFieldComponent
                    label="Telefonnummer für Rückfragen"
                    value="+4930123456"
                    onChange={() => {}}
                />
            </Box>

            <Box>
                <PhoneNumberFieldComponent
                    label="Telefonnummer in Österreich"
                    defaultCountry="AT"
                    value="+436641234567"
                    onChange={() => {}}
                />
            </Box>
        </ElementDescriptionContent>
    ),
    [ElementType.Email]: (
        <ElementDescriptionContent
            key={ElementType.Email}
            usage={'Erfassen Sie eine E-Mail-Adresse, zum Beispiel für Benachrichtigungen zu einem Vorgang.'}
            settings={'- **„Lokale Domains zulassen“:** Erlaubt Adressen ohne Punkt im Domainnamen, zum Beispiel `poststelle@verwaltung`. Standardmäßig ist diese Option ausgeschaltet.'}
            notes={'Adresslisten und Angaben mit Anzeigenamen werden nicht akzeptiert. Umlaute sind nur im Domainnamen hinter dem `@` zulässig. Ob das Postfach existiert oder erreichbar ist, wird nicht geprüft. Für lokale Domains muss der genutzte Mail-Anbieter die Zustellung unterstützen.'}
            technicalDetails={[
                '- Datentyp: Textwert (`String`), auch in No-Code-Ausdrücken.',
                '- Leerzeichen am Anfang und Ende werden entfernt. Die Groß- und Kleinschreibung vor dem `@` bleibt erhalten.',
                '- Domainnamen werden kleingeschrieben und bei Bedarf in ihre technische ASCII-Schreibweise umgewandelt: `kontakt@büro.de` wird zu `kontakt@xn--bro-hoa.de`.',
            ].join('\n')}
        >
            <Box>
                <EmailFieldComponent
                    label="E-Mail-Adresse für Benachrichtigungen"
                    value="vorname.nachname@example.org"
                    onChange={() => {}}
                />
            </Box>

            <Box>
                <EmailFieldComponent
                    label="Interne E-Mail-Adresse"
                    value="poststelle@verwaltung"
                    allowLocalDomains
                    onChange={() => {}}
                    hint="In diesem Beispiel sind lokale Domains zugelassen."
                />
            </Box>
        </ElementDescriptionContent>
    ),
    [ElementType.Number]: (
        <ElementDescriptionContent
            key={ElementType.Number}
            usage={'Erfassen Sie Mengen, Beträge oder Maße, die Sie berechnen oder numerisch vergleichen möchten.'}
            settings={[
                '- **„Anzahl der Dezimalstellen“:** Bestimmt die Rundung der Eingabe und die Anzahl der angezeigten Nachkommastellen. Mit `0` oder ohne Angabe werden ganze Zahlen verwendet.',
                '- **„Einheit“:** Ergänzen Sie die Anzeige zum Beispiel um `kg`, `€` oder `Stück`.',
            ].join('\n')}
            notes={'Für Kennungen wie Postleitzahlen oder Aktenzeichen verwenden Sie **„Texteingabe“**. Dort bleiben führende Nullen erhalten.'}
            technicalDetails={[
                '- Datentyp: Zahlenwert (`Number`), auch in No-Code-Ausdrücken.',
                '- Die Anzeige `42,31 kg` liefert den Zahlenwert `42.31`. Die Einheit gehört nicht zum Wert und bewirkt keine Umrechnung.',
                '- Zulässiger Wertebereich: `-2147483648` bis `2147483648`.',
                '- Ohne Eingabe ist kein Wert vorhanden; `0` ist ein gültiger Zahlenwert.',
            ].join('\n')}
        >
            <Box>
                <NumberFieldComponent
                    label="Gewicht"
                    value={42.31}
                    decimalPlaces={2}
                    suffix="kg"
                    onChange={() => {
                    }}
                    hint="Geben Sie das Gewicht in Kilogramm an."
                />
            </Box>

            <Box>
                <NumberFieldComponent
                    label="Anzahl der Teilnehmenden"
                    value={19}
                    decimalPlaces={0}
                    suffix="Personen"
                    onChange={() => {
                    }}
                    hint="Geben Sie eine ganze Zahl an."
                />
            </Box>
        </ElementDescriptionContent>
    ),
    [ElementType.Select]: (
        <ElementDescriptionContent
            key={ElementType.Select}
            usage={'Lassen Sie eine Option aus einer festgelegten Liste auswählen, zum Beispiel den bevorzugten Kontaktweg.'}
            settings={[
                '- **„Darstellung der Auswahl“:** Optionsfelder zeigen alle Optionen direkt; ein Auswahlmenü spart Platz. Die durchsuchbare Auswahl filtert die Liste beim Tippen.',
                '- **„Optionen definieren über“:** Hinterlegen Sie Optionen manuell oder übernehmen Sie sie aus einer System-Codeliste.',
                '- **„Beschriftung“** und **„Wert“:** Legen Sie die sichtbare Bezeichnung und den gespeicherten Wert einer Option fest.',
            ].join('\n')}
            notes={'Auch die durchsuchbare Auswahl erlaubt nur vorgegebene Optionen. Für freie Eingaben verwenden Sie **„Texteingabe“**, für mehrere Optionen **„Mehrfachauswahl“**.'}
            technicalDetails={[
                '- Datentyp: Textwert (`String`), unabhängig von der Darstellung.',
                '- Gespeichert wird der **„Wert“** der Option, zum Beispiel `email` für die Beschriftung „E-Mail“. Verwenden Sie diesen Wert für Vergleiche in No-Code-Ausdrücken.',
                '- Auch Werte wie `42` bleiben Text.',
                '- Ohne Auswahl ist kein Wert vorhanden.',
            ].join('\n')}
        >
            <Box>
                <RadioFieldComponent label="Kontaktweg" value="email" onChange={() => {}}
                    options={[{value: 'email', label: 'E-Mail'}, {value: 'post', label: 'Post'}]}/>
            </Box>
            <Box>
                <SelectFieldComponent
                    label="Veranstaltungsart"
                    value="market"
                    options={[
                        {
                            label: 'Straßenfest',
                            value: 'street_festival',
                        },
                        {
                            label: 'Markt',
                            value: 'market',
                        },
                        {
                            label: 'Sonstige Veranstaltung',
                            value: 'other',
                        },
                    ]}
                    onChange={() => {
                    }}
                    hint="Wählen Sie eine Veranstaltungsart aus dem Auswahlmenü aus."
                />
            </Box>

            <Box>
                <SelectFieldComponent
                    label="Durchsuchbare Einzelauswahl"
                    value={undefined}
                    options={[
                        {
                            label: 'Baugenehmigung',
                            value: 'building_permit',
                        },
                        {
                            label: 'Gewerbeanmeldung',
                            value: 'business_registration',
                        },
                        {
                            label: 'Meldebescheinigung',
                            value: 'registration_certificate',
                        },
                    ]}
                    onChange={() => {
                    }}
                    presentation={SelectFieldPresentation.Combobox}
                    hint="Tippen Sie, um die vorgegebenen Optionen zu filtern."
                />
            </Box>
        </ElementDescriptionContent>
    ),
    [ElementType.Text]: (
        <ElementDescriptionContent
            key={ElementType.Text}
            usage={'Erfassen Sie frei formulierte Angaben, zum Beispiel Namen, Aktenzeichen oder die Beschreibung eines Anliegens.'}
            settings={[
                '- **„Mehrzeilige Texteingabe“:** Ermöglicht Angaben mit Zeilenumbrüchen.',
                '- **„Minimalanzahl an Zeichen“** und **„Maximalanzahl an Zeichen“:** Begrenzen die Textlänge. Mit `0` oder ohne Angabe setzen Sie keine Grenze.',
                '- **„Vorschläge definieren über“:** Hinterlegen Sie für einzeilige Eingaben Vorschläge manuell oder aus einer System-Codeliste. Eigene Texte bleiben möglich.',
            ].join('\n')}
            notes={'Eine Mindestlänge macht das Feld nicht zur Pflichtangabe. Aktivieren Sie dafür **„Pflichtangabe“**. Für fest vorgegebene Optionen verwenden Sie **„Einzelauswahl“**, für Zahlen zur Berechnung **„Zahl“** und für formatierten Text **„Texteingabe mit Formatierung“**.'}
            technicalDetails={[
                '- Datentyp: Textwert (`String`), auch in No-Code-Ausdrücken. Mehrzeilige Eingaben bleiben ein einzelner Textwert mit Zeilenumbrüchen.',
                '- Führende Nullen wie in `00123` bleiben erhalten.',
                '- Leerzeichen am Anfang und Ende werden entfernt.',
                '- Bei einem übernommenen Vorschlag wird der Vorschlagstext gespeichert.',
                '- Ohne Eingabe ist kein Wert vorhanden.',
            ].join('\n')}
        >
            <Box>
                <TextFieldComponent
                    label="Aktenzeichen"
                    value="00123"
                    onChange={() => {
                    }}
                    maxCharacters={20}
                    hint="Geben Sie das Aktenzeichen aus Ihrem Schreiben an. Höchstens 20 Zeichen."
                />
            </Box>

            <Box>
                <TextFieldComponent
                    label="Beschreibung des Anliegens"
                    multiline={true}
                    value={'Ich möchte eine Veranstaltung anmelden.\nSie findet auf dem Marktplatz statt.'}
                    onChange={() => {
                    }}
                />
            </Box>
        </ElementDescriptionContent>
    ),
    ...inputElementDescriptions,
    ...temporalElementDescriptions,
    ...displayElementDescriptions,
    ...specializedElementDescriptions,
    [ElementType.FormLayout]: null,
    [ElementType.DialogLayout]: null,
    [ElementType.StepperLayout]: null,
    [ElementType.ConfigLayout]: null,
    [ElementType.FunctionInput]: null,
    [ElementType.TabLayout]: null,
    [ElementType.SecretSelectInput]: null,
    [ElementType.DepartmentSelectInput]: null,
    [ElementType.SubmittedStep]: null,
};

export function ElementInfoTab({
                                   type,
                                   parentElement,
                                   onAddElement,
                                   primaryActionLabel,
                                   primaryActionIcon,
                                   primaryActionDisabledReason,
                                   onClose,
                               }: {
    type: ElementType,
    parentElement?: AnyElement,
    onAddElement: (element: AnyElement) => void,
    primaryActionLabel: string,
    primaryActionIcon: ReactNode,
    primaryActionDisabledReason?: string,
    onClose: () => void,
}) {
    const ElementIcon = getElementIconForType(type);

    const handleAddElement = () => {
        if (primaryActionDisabledReason != null) {
            return;
        }

        const newElement = generateElementWithDefaultValues(type, parentElement);
        if (newElement != null) {
            onAddElement(newElement);
        }
    };

    return (
        <SelectionDetailsPanel
            contentKey={type}
            icon={<ElementIcon sx={{fontSize: 20, color: 'text.secondary'}}/>}
            label={getElementGroupLabelForType(type)}
            title={getElementNameForType(type)}
            description={getElementDescriptionForType(type)}
            primaryActionLabel={primaryActionLabel}
            primaryActionIcon={primaryActionIcon}
            onPrimaryAction={handleAddElement}
            primaryActionDisabled={primaryActionDisabledReason != null}
            primaryActionDisabledTooltip={primaryActionDisabledReason}
            onClose={onClose}
        >
            {elementDescriptions[type]}
        </SelectionDetailsPanel>
    );
}
