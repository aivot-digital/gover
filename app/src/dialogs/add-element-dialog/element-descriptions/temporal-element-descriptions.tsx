import React, {type ReactNode} from 'react';
import {ElementType} from '../../../data/element-type/element-type';
import {ElementDescriptionContent} from '../element-description-content';
import {DateFieldComponent} from '../../../components/date-field/date-field-component';
import {DateFieldComponentModelMode} from '../../../models/elements/form/input/date-field-element';
import {TimeFieldComponent} from '../../../components/time-field/time-field-component';
import {TimeFieldComponentModelMode} from '../../../models/elements/form/input/time-field-element';
import {DateTimeFieldComponent} from '../../../components/date-time-field/date-time-field-component';
import {DateRangeFieldComponent} from '../../../components/date-range-field/date-range-field-component';
import {TimeRangeFieldComponent} from '../../../components/time-range-field/time-range-field-component';
import {DateTimeRangeFieldComponent} from '../../../components/date-time-range-field/date-time-range-field-component';
import {DurationFieldComponent} from '../../../components/duration-field/duration-field-component';
import {DurationLargestUnit, DurationPrecision} from '../../../models/elements/form/input/duration-field-element';

export const temporalElementDescriptions: Partial<Record<ElementType, ReactNode>> = {
    [ElementType.Date]: (
        <ElementDescriptionContent
            key={ElementType.Date}
            usage="Erfassen Sie ein Kalenderdatum, zum Beispiel ein Geburtsdatum oder den Monat einer Abrechnung."
            settings={'- **„Datums-Format“:** Wählen Sie ein vollständiges Datum, Monat und Jahr oder nur das Jahr.'}
            notes={'Das Feld erfasst ein Datum ohne Uhrzeit und Zeitzone. Für einen konkreten Zeitpunkt verwenden Sie **„Datum und Uhrzeit“**, für einen Zeitraum **„Datumsspanne“**.'}
            technicalDetails={[
                '- Datentyp für No-Code-Ausdrücke: Datumswert (`Date`).',
                '- Der gespeicherte Wert folgt der gewählten Genauigkeit: `2026-07-29`, `2026-07` oder `2026`.',
                '- Die sichtbare Schreibweise `29.07.2026` wird als `2026-07-29` bereitgestellt.',
            ].join('\n')}
        >
            <DateFieldComponent label="Geburtsdatum" value="1990-05-12" mode={DateFieldComponentModelMode.Day}
                onChange={() => {}}/>
            <DateFieldComponent label="Abrechnungsmonat" value="2026-07" mode={DateFieldComponentModelMode.Month}
                onChange={() => {}}/>
        </ElementDescriptionContent>
    ),
    [ElementType.Time]: (
        <ElementDescriptionContent
            key={ElementType.Time}
            usage="Erfassen Sie eine Uhrzeit, zum Beispiel den Beginn einer Veranstaltung oder eine genaue Messzeit."
            settings={'- **„Uhrzeit-Format“:** Wählen Sie Stunden und Minuten oder ergänzen Sie Sekunden.'}
            notes={'Eine Uhrzeit enthält kein Datum. Für Termine mit einem bestimmten Tag verwenden Sie **„Datum und Uhrzeit“**, für eine Dauer **„Zeitdauer“**.'}
            technicalDetails={[
                '- Datentyp für No-Code-Ausdrücke: Uhrzeitwert (`Time`), zum Beispiel `09:30` oder `09:30:15`.',
                '- Im Minutenformat wird der Sekundenanteil auf `0` gesetzt.',
                '- Die Uhrzeit enthält keine Zeitzonenangabe.',
            ].join('\n')}
        >
            <TimeFieldComponent label="Beginn" value="09:30" onChange={() => {}}/>
            <TimeFieldComponent label="Messzeit" value="09:30:15" mode={TimeFieldComponentModelMode.Second}
                onChange={() => {}}/>
        </ElementDescriptionContent>
    ),
    [ElementType.DateTime]: (
        <ElementDescriptionContent
            key={ElementType.DateTime}
            usage="Erfassen Sie einen konkreten Zeitpunkt mit Datum und Uhrzeit, zum Beispiel einen Termin."
            settings={'- **„Uhrzeit-Format“:** Legen Sie fest, ob die Eingabe und Anzeige Minuten oder auch Sekunden umfasst.'}
            notes={'Die Eingabe und Anzeige verwenden die Zeitzone der Prosuna-Instanz. Für Angaben ohne Uhrzeit verwenden Sie **„Datum“**.'}
            technicalDetails={[
                '- Datentyp für No-Code-Ausdrücke: Datums- und Zeitwert (`DateTime`).',
                '- Der Wert beschreibt einen absoluten Zeitpunkt als ISO-Zeitstempel mit Zeitzonenangabe, zum Beispiel `2026-07-29T07:00:00Z`.',
                '- Eine Anzeige mit Minutengenauigkeit entfernt keine zusätzlichen Sekunden aus bereits vorhandenen Zeitstempeln.',
            ].join('\n')}
        >
            <DateTimeFieldComponent label="Termin" value="2026-07-29T07:00:00Z" onChange={() => {}}/>
        </ElementDescriptionContent>
    ),
    [ElementType.DateRange]: (
        <ElementDescriptionContent
            key={ElementType.DateRange}
            usage="Erfassen Sie Beginn und Ende eines Zeitraums in einem gemeinsamen Feld, zum Beispiel die Tage einer Veranstaltung."
            settings={'- **„Datums-Format“:** Wählen Sie vollständige Datumsangaben, Monate oder Jahre für beide Grenzen.'}
            notes={'Bei einer Eingabe sind beide Grenzen erforderlich; der Beginn darf nicht nach dem Ende liegen. Für einen Zeitraum mit Uhrzeiten verwenden Sie **„Datums- und Zeitspanne“**.'}
            technicalDetails={[
                '- Datentyp für No-Code-Ausdrücke: Objekt (`Object`) mit `start` und `end`.',
                '- Beispiel: `{"start":"2026-07-29","end":"2026-07-31"}`. Bei Monats- oder Jahresangaben werden die Werte entsprechend verkürzt.',
                '- Für Datumsfunktionen verarbeiten Sie den jeweiligen Wert aus `start` oder `end`.',
            ].join('\n')}
        >
            <DateRangeFieldComponent label="Veranstaltungszeitraum" value={{start: '2026-07-29', end: '2026-07-31'}}
                onChange={() => {}}/>
        </ElementDescriptionContent>
    ),
    [ElementType.TimeRange]: (
        <ElementDescriptionContent
            key={ElementType.TimeRange}
            usage="Erfassen Sie eine Start- und Enduhrzeit, zum Beispiel die Öffnungszeit eines Angebots."
            settings={'- **„Uhrzeit-Format“:** Wählen Sie Minuten- oder Sekundengenauigkeit für beide Uhrzeiten.'}
            notes={'Bei einer Eingabe sind beide Uhrzeiten erforderlich; die Startuhrzeit darf nicht nach der Enduhrzeit liegen. Für Zeiträume über Mitternacht verwenden Sie **„Datums- und Zeitspanne“** mit den zugehörigen Tagen.'}
            technicalDetails={[
                '- Datentyp für No-Code-Ausdrücke: Objekt (`Object`) mit `start` und `end`, zum Beispiel `{"start":"09:00","end":"17:00"}`.',
                '- Die einzelnen Werte sind Uhrzeiten ohne Datum und Zeitzone. Im Minutenformat wird ihr Sekundenanteil auf `0` gesetzt.',
                '- Die Zeitspanne liefert keine berechnete Dauer. Verarbeiten Sie Beginn und Ende für eine Zeitberechnung getrennt.',
            ].join('\n')}
        >
            <TimeRangeFieldComponent label="Öffnungszeit" value={{start: '09:00', end: '17:00'}}
                onChange={() => {}}/>
        </ElementDescriptionContent>
    ),
    [ElementType.DateTimeRange]: (
        <ElementDescriptionContent
            key={ElementType.DateTimeRange}
            usage="Erfassen Sie Beginn und Ende mit Datum und Uhrzeit, zum Beispiel für eine mehrtägige Reservierung."
            settings={'- **„Uhrzeit-Format“:** Legen Sie die Minuten- oder Sekundengenauigkeit der Eingabe und Anzeige fest.'}
            notes={'Bei einer Eingabe sind beide Zeitpunkte erforderlich; der Beginn darf nicht nach dem Ende liegen. Die Eingabe und Anzeige verwenden die Zeitzone der Prosuna-Instanz.'}
            technicalDetails={[
                '- Datentyp für No-Code-Ausdrücke: Objekt (`Object`) mit `start` und `end`.',
                '- Beide Werte sind absolute ISO-Zeitstempel, zum Beispiel `{"start":"2026-07-29T07:00:00Z","end":"2026-07-29T09:00:00Z"}`.',
                '- Für Datums- und Zeitfunktionen verarbeiten Sie den jeweiligen Wert aus `start` oder `end`. Eine Dauer wird nicht zusätzlich gespeichert.',
            ].join('\n')}
        >
            <DateTimeRangeFieldComponent label="Reservierungszeitraum"
                value={{start: '2026-07-29T07:00:00Z', end: '2026-07-29T09:00:00Z'}} onChange={() => {}}/>
        </ElementDescriptionContent>
    ),
    [ElementType.Duration]: (
        <ElementDescriptionContent
            key={ElementType.Duration}
            usage="Erfassen Sie, wie lange etwas dauert, zum Beispiel die Bearbeitung einer Aufgabe oder die Nutzung eines Geräts."
            settings={[
                '- **„Größte Einheit“:** Wählen Sie Stunden oder ergänzen Sie Tage.',
                '- **„Genauigkeit“:** Legen Sie fest, ob ganze Minuten oder auch Sekunden eingegeben werden.',
                '- **„Mindestdauer“** und **„Höchstdauer“:** Begrenzen Sie die erlaubte Dauer bei Bedarf.',
            ].join('\n')}
            notes={'Ein Tag entspricht immer 24 Stunden. Das Feld erfasst eine nicht negative Dauer ohne Startdatum. Für Beginn und Ende verwenden Sie ein Element für Zeitspannen.'}
            technicalDetails={[
                '- Datentyp: Zahlenwert (`Number`), auch in No-Code-Ausdrücken. Gespeichert wird die gesamte Dauer in Sekunden.',
                '- 2 Stunden und 30 Minuten liefern `9000`. Die Anzeigeeinheiten ändern den gespeicherten Wert nicht.',
                '- Bei Minutengenauigkeit muss der Sekundenwert durch `60` teilbar sein. Sekundenbruchteile sind nicht zulässig.',
                '- Ohne Eingabe ist kein Wert vorhanden; `0` ist eine gültige Dauer.',
            ].join('\n')}
        >
            <DurationFieldComponent label="Zeitaufwand für eine Aufgabe" value={9000} onChange={() => {}}
                hint="Die Aufgabe benötigt 2 Stunden und 30 Minuten."/>
            <DurationFieldComponent label="Betriebsdauer eines Geräts" value={99015}
                largestUnit={DurationLargestUnit.Day} precision={DurationPrecision.Second} onChange={() => {}}
                hint="Das Gerät war 1 Tag, 3 Stunden, 30 Minuten und 15 Sekunden in Betrieb."/>
        </ElementDescriptionContent>
    ),
};
