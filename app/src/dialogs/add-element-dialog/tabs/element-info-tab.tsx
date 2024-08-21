import React from 'react';
import {Box, DialogContent, Divider, IconButton, Tooltip, Typography} from '@mui/material';
import CloseOutlinedIcon from '@mui/icons-material/CloseOutlined';
import {getElementNameForType} from '../../../data/element-type/element-names';
import {type ElementTypesMap} from '../../../data/element-type/element-types-map';
import {ElementType} from '../../../data/element-type/element-type';
import {AlertComponent} from '../../../components/alert/alert-component';
import {CheckboxFieldComponent} from '../../../components/checkbox-field/checkbox-field-component';
import {HeadlineComponent} from '../../../components/headline/headline-component';
import {DateFieldComponent} from '../../../components/date-field/date-field-component';
import {DateFieldComponentModelMode} from '../../../models/elements/form/input/date-field-element';
import {MultiCheckboxComponent} from '../../../components/multi-checkbox-field/multi-checkbox-component';
import {NumberFieldComponent} from '../../../components/number-field/number-field-component';
import {RadioFieldComponent} from '../../../components/radio-field/radio-field-component';
import {SelectFieldComponent} from '../../../components/select-field/select-field-component';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';

const elementDescriptions: ElementTypesMap<React.ReactNode | null> = {
    [ElementType.Alert]: (
        <Box>
            <Typography>
                Das Hinweis-Element dient Ihnen dazu, den Nutzer:innen wichtige Informationen visuell hervorgehoben
                zu vermitteln. Hinweise werden farblich unterschiedlich ausgezeichnet, um die Gewichtung
                einer Information zu verdeutlichen.
            </Typography>

            <Divider sx={{my: 4}}>
                Beispiele
            </Divider>

            <Box>
                <AlertComponent
                    text="Der Text des Erfolgs-Hinweises"
                    title="Titel des Erfolgs-Hinweises"
                    color="success"
                />

                <AlertComponent
                    text="Der Text des Info-Hinweises"
                    title="Titel des Info-Hinweises"
                    color="info"
                />

                <AlertComponent
                    text="Der Text des Warn-Hinweises"
                    title="Titel des Warn-Hinweises"
                    color="warning"
                />

                <AlertComponent
                    text="Der Text des Fehler-Hinweises"
                    title="Titel des Fehler-Hinweises"
                    color="error"
                />
            </Box>
        </Box>
    ),

    [ElementType.Image]: (
        <Box>
            <Typography>
                Das Bild-Element dient Ihnen dazu, ein beliebiges Bild darzustellen.
            </Typography>
            <Typography sx={{mt: 2}}>
                Bitte beachten Sie, dass Sie Bilder nur via URL einbinden können.
                Das Bild muss also auf einem Server hochgeladen und über das Internet erreichbar sein.
            </Typography>
        </Box>
    ),
    [ElementType.Container]: (
        <Box>
            <Typography>
                Das Gruppierungs-Element erlaubt es Ihnen, mehrere Elemente semantisch zusammenzufassen.
                Dies sorgt für eine bessere Übersichtlichkeit und eine bessere Wartbarkeit.
            </Typography>

            <Typography sx={{mt: 2}}>
                Gruppierungen können zudem als Vorlagen abgespeichert werden.
            </Typography>
        </Box>
    ),
    [ElementType.Step]: null,
    [ElementType.Root]: null,
    [ElementType.Checkbox]: (
        <Box>
            <Typography>
                Das Checkbox (Ja/Nein)-Element ermöglicht Ihnen die Einholung einfacher Bestätigungen
                Ihrer Nutzer:innen. Die Eingabe wird als boolscher Wert (Ja/Nein, True/False) verarbeitet.
            </Typography>

            <Divider sx={{my: 4}}>
                Beispiele
            </Divider>

            <Box sx={{mt: 2}}>
                <CheckboxFieldComponent
                    label="Beschriftung der Checkbox"
                    hint="Hinweis der Checkbox"
                    value={true}
                    onChange={() => {
                    }}
                />
            </Box>

            <Box sx={{mt: 2}}>
                <CheckboxFieldComponent
                    label="Fehlerhafte Checkbox"
                    error="Fehler der Checkbox"
                    value={false}
                    onChange={() => {
                    }}
                />
            </Box>
        </Box>
    ),
    [ElementType.Date]: (
        <Box>
            <Typography>
                Das Datums-Element dient Ihnen dazu, Datumseingaben von Ihren Nutzer:innen entgegenzunehmen.
                Es beinhaltet zudem vielfältige Möglichkeiten für die Validierung der getätigten Eingaben.
            </Typography>

            <Divider sx={{my: 4}}>
                Beispiele
            </Divider>

            <Box sx={{mt: 2}}>
                <DateFieldComponent
                    label="Datumsfeld"
                    mode={DateFieldComponentModelMode.Day}
                    value={new Date().toISOString()}
                    onChange={() => {
                    }}
                    hint="Der Hinweis für das Datumsfeld"
                />
            </Box>

            <Box sx={{mt: 2}}>
                <DateFieldComponent
                    label="Datumsfeld"
                    mode={DateFieldComponentModelMode.Month}
                    value={new Date().toISOString()}
                    onChange={() => {
                    }}
                    error="Der Fehler für das Datumsfeld"
                />
            </Box>

            <Box sx={{mt: 2}}>
                <DateFieldComponent
                    label="Datumsfeld"
                    mode={DateFieldComponentModelMode.Year}
                    value={new Date().toISOString()}
                    onChange={() => {
                    }}
                />
            </Box>
        </Box>
    ),
    [ElementType.Headline]: (
        <Box>
            <Typography>
                Das Überschriften-Element unterstützt Sie dabei, einen Abschnitt in sinnvolle Unterbereiche zu gliedern.
            </Typography>

            <Typography sx={{mt: 2}}>
                Achten Sie darauf, Überschriften semantisch korrekt zu benutzen, wenn diese direkt aufeinander folgen.
                Eine kleine Überschrift steht somit immer unter einer großen Überschrift.
            </Typography>

            <Divider sx={{my: 4}}>
                Beispiele
            </Divider>

            <Box sx={{mt: 2}}>
                <HeadlineComponent
                    content="Große Überschrift"
                    small={false}
                />
            </Box>

            <Box sx={{mt: 2}}>
                <HeadlineComponent
                    content="Kleine Überschrift"
                    small={true}
                />
            </Box>
        </Box>
    ),
    [ElementType.MultiCheckbox]: (
        <Box>
            <Typography>
                Das Checkbox (Mehrfachauswahl)-Element dient zur Abfrage einer oder mehrerer Möglichkeiten bei
                Ihren Nutzer:innen.
            </Typography>

            <Divider sx={{my: 4}}>
                Beispiele
            </Divider>

            <Box sx={{mt: 2}}>
                <MultiCheckboxComponent
                    label="Einfache Checkbox (Mehrfachauswahl)"
                    value={['Option 2', 'Option 3']}
                    options={['Option 1', 'Option 2', 'Option 3']}
                    onChange={() => {
                    }}
                    hint="Hinweis der Checkbox (Mehrfachauswahl)"
                />
            </Box>
        </Box>
    ),
    [ElementType.Number]: (
        <Box>
            <Typography>
                Das Zahl-Element ermöglicht Ihnen, numerische Eingaben von Nutzer:innen entgegenzunehmen.
                Es beinhaltet zudem vielfältige Möglichkeiten für die Validierung der getätigten Eingaben.
            </Typography>

            <Divider sx={{my: 4}}>
                Beispiele
            </Divider>

            <Box sx={{mt: 2}}>
                <NumberFieldComponent
                    label="Zahleneingabe mit 2 Dezimalstellen"
                    value={42.31}
                    decimalPlaces={2}
                    suffix="KG"
                    onChange={() => {
                    }}
                    hint="Hinweis des Zahlen-Elements"
                />
            </Box>

            <Box sx={{mt: 2}}>
                <NumberFieldComponent
                    label="Zahleneingabe ohne Dezimalstellen"
                    value={19}
                    decimalPlaces={0}
                    suffix="Hunde"
                    onChange={() => {
                    }}
                    error="Fehler des Zahlen-Elements"
                />
            </Box>
        </Box>
    ),
    [ElementType.ReplicatingContainer]: (
        <Box>
            <Typography>
                Das Strukturierte Listeneingabe-Element ermöglicht Ihnen die wiederholte Abfrage von Datensätzen.
                Ein Datensatz repräsentiert mehrere zusammengehörige Abfragen wie z.B. Vorname und Nachname.
                So können Sie beispielsweise Angaben für mehrere Personen erheben, bei denen immer wieder
                der Vorname und Nachname abgefragt wird.
            </Typography>

            <Typography sx={{mt: 2}}>
                Die Anzahl an abzufragenden Datensätzen kann durch Sie festgelegt werden.
            </Typography>
        </Box>
    ),
    [ElementType.Richtext]: (
        <Box>
            <Typography>
                Das Fließtext-Element ermöglicht Ihnen die Einbindung von formatiertem Text.
                Auf diese Weise können Sie Nutzer:innen zusätzliche Informationen gezielt darstellen.
            </Typography>
        </Box>
    ),
    [ElementType.Radio]: (
        <Box>
            <Typography>
                Das Radio Button (Einfachauswahl)-Element dient zur Abfrage exakt einer Möglichkeit aus
                mehreren Möglichkeiten.
            </Typography>

            <Typography sx={{mt: 2}}>
                Eine optische Alternative zum Radio-Button (Einfachauswahl)-Element stellt das
                Dropdown (Einfachauswahl)-Element dar.
            </Typography>

            <Divider sx={{my: 4}}>
                Beispiele
            </Divider>

            <Box sx={{mt: 2}}>
                <RadioFieldComponent
                    label="Einfacher Radio-Button"
                    value={'Option 2'}
                    options={['Option 1', 'Option 2', 'Option 3']}
                    onChange={() => {
                    }}
                    hint="Hinweis des Radio-Buttons (Einfachauswahl)"
                />
            </Box>

            <Box sx={{mt: 2}}>
                <RadioFieldComponent
                    label="Einfacher Radio-Button"
                    value={'Option 3'}
                    options={['Option 1', 'Option 2', 'Option 3']}
                    onChange={() => {
                    }}
                    required
                />
            </Box>

            <Box sx={{mt: 2}}>
                <RadioFieldComponent
                    label="Einfacher Radio-Button"
                    value={undefined}
                    options={['Option 1', 'Option 2', 'Option 3']}
                    onChange={() => {
                    }}
                    error="Fehler des Radio-Buttons (Einfachauswahl)"
                />
            </Box>
        </Box>
    ),
    [ElementType.Select]: (
        <Box>
            <Typography>
                Das Dropdown (Einfachauswahl)-Element dient zur Abfrage exakt einer Möglichkeit aus
                mehreren Möglichkeiten.
            </Typography>

            <Typography sx={{mt: 2}}>
                Eine optische Alternative zum Dropdown (Einfachauswahl)-Element stellt das
                Radio-Button (Einfachauswahl)-Element dar.
            </Typography>

            <Divider sx={{my: 4}}>
                Beispiele
            </Divider>

            <Box sx={{mt: 2}}>
                <SelectFieldComponent
                    label="Einfaches Dropdown"
                    value={'2'}
                    options={[
                        {
                            label: 'Option 1',
                            value: '1',
                        },
                        {
                            label: 'Option 2',
                            value: '2',
                        },
                        {
                            label: 'Option 3',
                            value: '3',
                        },
                    ]}
                    onChange={() => {
                    }}
                    hint="Hinweis des Dropdowns (Einfachauswahl)"
                />
            </Box>

            <Box sx={{mt: 2}}>
                <SelectFieldComponent
                    label="Einfaches Dropdown"
                    value={'3'}
                    options={[
                        {
                            label: 'Option 1',
                            value: '1',
                        },
                        {
                            label: 'Option 2',
                            value: '2',
                        },
                        {
                            label: 'Option 3',
                            value: '3',
                        },
                    ]}
                    onChange={() => {
                    }}
                    hint="Hinweis des Dropdowns (Einfachauswahl)"
                    required
                />
            </Box>

            <Box sx={{mt: 2}}>
                <SelectFieldComponent
                    label="Einfaches Dropdown"
                    value={undefined}
                    options={[
                        {
                            label: 'Option 1',
                            value: '1',
                        },
                        {
                            label: 'Option 2',
                            value: '2',
                        },
                        {
                            label: 'Option 3',
                            value: '3',
                        },
                    ]}
                    onChange={() => {
                    }}
                    error="Fehler des Dropdowns (Einfachauswahl)"
                />
            </Box>
        </Box>
    ),
    [ElementType.Spacer]: (
        <Box>
            <Typography>
                Das Abstands-Element fügt einen vertikalen optischen Platzhalter zwischen den darüber und darunter
                liegenden Elementen ein. Es hilft Ihnen dabei, verschiedene Abfragen und/oder Informationen innerhalb
                eines Abschnittes visuell deutlicher voneinander zu trennen.
            </Typography>
        </Box>
    ),
    [ElementType.Table]: (
        <Box>
            <Typography>
                Das Tabellen-Element ermöglicht Ihnen das Entgegennehmen von Eingaben in tabellarischer Form.
                Die Spalten und deren abgefragte Datenformate sind durch Sie einstellbar.
                Ebenso können Sie Einstellungen zur Anzahl der Zeilen treffen.
            </Typography>
        </Box>
    ),
    [ElementType.Text]: (
        <Box>
            <Typography>
                Das Text-Element ermöglicht Ihnen, Texteingaben von Nutzer:innen entgegenzunehmen.
                Es beinhaltet zudem vielfältige Möglichkeiten für die Validierung der getätigten Eingaben.
            </Typography>

            <Divider sx={{my: 4}}>
                Beispiele
            </Divider>

            <Box sx={{mt: 2}}>
                <TextFieldComponent
                    label="Einfache Texteingabe"
                    value="Das ist eine einfache Texteingabe"
                    onChange={() => {
                    }}
                    hint="Hinweis des Text-Elements"
                />
            </Box>

            <Box sx={{mt: 2}}>
                <TextFieldComponent
                    label="Mehrzeilige Texteingabe"
                    multiline={true}
                    value={'Das ist eine\nmehrzeilige Texteingabe'}
                    onChange={() => {
                    }}
                    required
                />
            </Box>

            <Box sx={{mt: 2}}>
                <TextFieldComponent
                    label="Texteingabe (Max. 20 Zeichen)"
                    value={'Maximal 20 Zeichen'}
                    onChange={() => {
                    }}
                    required
                    maxCharacters={20}
                />
            </Box>

            <Box sx={{mt: 2}}>
                <TextFieldComponent
                    label="Fehlerhafte Texteingabe"
                    value="Das ist eine fehlerhafte Texteingabe"
                    onChange={() => {
                    }}
                    error="Fehler des Text-Elements"
                />
            </Box>
        </Box>
    ),
    [ElementType.Time]: (
        <Box>
            <Typography>
                Das Uhrzeit-Element dient Ihnen dazu, Eingaben der Uhrzeit (0-24 Uhr) von Ihren Nutzer:innen entgegenzunehmen.
                Es beinhaltet zudem vielfältige Möglichkeiten für die Validierung der getätigten Eingaben.
            </Typography>
        </Box>
    ),
    [ElementType.FileUpload]: (
        <Box>
            <Typography>
                Das Anlagen-Element ermöglicht es Ihnen, hochzuladende Dokumente von Ihren Nutzer:innen
                entgegenzunehmen. Welche Art von Anlagen (Dateiendungen) erlaubt sind und in welcher Anzahl,
                können Sie festlegen.
            </Typography>

            <Typography sx={{mt: 2}}>
                Bitte beachten Sie, dass aus Gründen der technischen Limitierungen von Folgesystemen die maximale
                Dateigröße pro Datei unveränderlich auf 10MB beschränkt ist.
            </Typography>

            <Typography sx={{mt: 2}}>
                Die maximale Gesamtgröße aller zu übertragenden Dateien ist abhängig von den in der verwendeten
                Schnittstelle hinterlegten Einstellungen.
            </Typography>
        </Box>
    ),
    [ElementType.IntroductionStep]: null,
    [ElementType.SummaryStep]: null,
    [ElementType.SubmitStep]: null,
    [ElementType.SubmittedStep]: null,
};

export function ElementInfoTab({
    type,
    onClose,
}: {type: ElementType, onClose: () => void}) {
    return (
        <DialogContent tabIndex={0}>
            <Box
                sx={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                }}
            >
                <h3>{getElementNameForType(type)}</h3>

                <Tooltip title="Schließen">
                    <IconButton
                        onClick={onClose}
                        size="small"
                    >
                        <CloseOutlinedIcon/>
                    </IconButton>
                </Tooltip>
            </Box>

            {
                elementDescriptions[type]
            }
        </DialogContent>
    );
}
