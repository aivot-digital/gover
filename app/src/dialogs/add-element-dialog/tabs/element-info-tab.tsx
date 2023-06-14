import React from "react";
import {
    Box, Checkbox,
    DialogContent, Divider,
    IconButton,
    List,
    ListItem,
    ListItemButton,
    ListItemIcon,
    ListItemText,
    ListSubheader,
    Tooltip, Typography
} from "@mui/material";
import {FontAwesomeIcon} from "@fortawesome/react-fontawesome";
import {faClose, faInfoCircle} from "@fortawesome/pro-light-svg-icons";
import {generateElementWithDefaultValues} from "../../../utils/generate-element-with-default-values";
import {ElementIcons} from "../../../data/element-type/element-icons";
import {ElementNames} from "../../../data/element-type/element-names";
import {BaseTabProps} from "./base-tab-props";
import {ElementTypesMap} from "../../../data/element-type/element-types-map";
import {ElementType} from "../../../data/element-type/element-type";
import {ElementChildOptions} from "../../../data/element-type/element-child-options";
import {AlertComponent} from "../../../components/alert/alert-component";
import {CheckboxFieldComponent} from "../../../components/checkbox-field/checkbox-field-component";
import {HeadlineComponentView} from "../../../components/headline/headline.component.view";
import {HeadlineComponent} from "../../../components/headline/headline-component";
import {DateFieldComponent} from "../../../components/date-field/date-field-component";
import {DateFieldComponentModelMode} from "../../../models/elements/form/input/date-field-element";
import {MultiCheckboxComponent} from "../../../components/multi-checkbox-field/multi-checkbox-component";
import {NumberFieldComponent} from "../../../components/number-field/number-field-component";
import {RadioFieldComponentView} from "../../../components/radio-field/radio-field.component.view";
import {RadioFieldComponent} from "../../../components/radio-field/radio-field-component";
import {SelectFieldComponent} from "../../../components/select-field/select-field-component";
import {TextFieldComponent} from "../../../components/text-field/text-field-component";

const elementDescriptions: ElementTypesMap<React.ReactNode | null> = {
    [ElementType.Alert]: (
        <Box>
            <Typography>
                Der Hinweis sollte genutzt werden, um Nutzer:innen wichtige Informationen zu vermitteln.
                Hinweise können verschiedene Farben haben um die Gewichtung festzulegen.
                Es gibt Erfolgs-Hinweise, Info-Hinweise, Warn-Hinweise und Fehler-Hinweise.
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
                Mit dem Bild-Element könenn Sie Nutzer:innen Bilder anzeigen.
                Bitte beachten Sie, dass Sie die Bilder auf Ihren eigenen Server hochladen laden müssen und im
                Bild-Element nur die URL zum Bild angeben können.
            </Typography>
        </Box>
    ),
    [ElementType.Container]: (
        <Box>
            <Typography>
                Mit der Gruppierung könenn Sie mehrere Elemente zusammenfassen.
                Gruppierungen können als Vorlagen abgespeichert werden.
            </Typography>

            <Typography sx={{mt: 2}}>
                Gruppierungen sollten immer verwendet werden, um Elemente semantisch zu gruppieren. So erhöhen Sie
                langfristig die Wartbarkeit Ihrer Formulare.
            </Typography>
        </Box>
    ),
    [ElementType.Step]: null,
    [ElementType.Root]: null,
    [ElementType.Checkbox]: (
        <Box>
            <Typography>
                Mit der Checkbox können Sie einfache Bestätigungen von Ihren Nutzer:innen abfragen.
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
                Mit dem Datum können Sie Datumseingaben von Ihren Nutzer:innen abfragen.
                Dabei können Sie aussteuern, ob Ihre Nutzer:innen einen bestimmten Tag, einen Monat oder nur ein Jahr
                eingeben müssen. Sie können außerdem aussteuern, ob das Datum in der Vergangenheit oder Zukunft liegen
                muss.
            </Typography>

            <Divider sx={{my: 4}}>
                Beispiele
            </Divider>

            <Box sx={{mt: 2}}>
                <DateFieldComponent
                    label="Datumsfeld"
                    mode={DateFieldComponentModelMode.Date}
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
                Mit der Überschrift kann ein Abschnitt in sinnvolle Unterbereiche gegliedert werden.
            </Typography>

            <Typography sx={{mt: 2}}>
                Achten Sie darauf, dass kleine Überschriften immer unter normalen Überschriften stehen sollten.
                Diese funktionieren dabei wie Kapitel in einem Abschnitt.
            </Typography>

            <Divider sx={{my: 4}}>
                Beispiele
            </Divider>

            <Box sx={{mt: 2}}>
                <HeadlineComponent
                    content="Normale Überschrift"
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
                Mit der Checkbox (Mehrfachauswahl) können Sie Ihren Nutzer:innen mehrere Auswahlmöglichkeiten bieten aus
                denen diese eine oder mehrere Optionen wählen können. Sie haben außerdem die Möglichkeit eine
                Mindestanzahl an ausgewählten Optionen zu fodern.
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
                Mit dem Zahl-Element können Ihre Nutzer:innen Zahlen eingeben.
                Sie haben die Möglichkeit die geforderten Dezimalstellen zu spezifizieren und eine Einheit anzugeben.
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
                Mit der Strukturierten Listeneingabe können Sie mehrere Elemente zusammenfassen, die dann einen
                Datensatz bilden. Nutzer:innen können dann mehrere dieser Datensätze hinzufügen.
                So können Sie beispielsweise eine Liste der Hunde zur Hundesteueranmeldung abfragen wobei jeder Hund als
                einzelner Datensatz vorliegt und aus den Elementen der Listeneingabe besteht.
            </Typography>

            <Typography sx={{mt: 2}}>
                Sie können für eine Strukturierten Listeneingabe eine Mindest- und Maximalanzahl von Datensätzen
                fordern.
            </Typography>
        </Box>
    ),
    [ElementType.Richtext]: (
        <Box>
            <Typography>
                Mit dem Fließtext-Element können Sie formattierten Text in Ihre Formulare einfügen.
                So können Sie zusätzliche Informationen für Ihre Nutzer:innen aufbereiten und anzeigen.
            </Typography>
        </Box>
    ),
    [ElementType.Radio]: (
        <Box>
            <Typography>
                Mit dem Radio-Button (Einfachauswahl) können Sie Ihren Nutzer:innen mehrere Auswahlmöglichkeiten bieten,
                aus denen diese genau eine Optionen wählen können.
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
                Mit dem Dropdown (Einfachauswahl) können Sie Ihren Nutzer:innen mehrere Auswahlmöglichkeiten bieten,
                aus denen diese genau eine Optionen wählen können.
            </Typography>

            <Divider sx={{my: 4}}>
                Beispiele
            </Divider>

            <Box sx={{mt: 2}}>
                <SelectFieldComponent
                    label="Einfaches Dropdown"
                    value={'2'}
                    options={[
                        {label: 'Option 1', value: '1'},
                        {label: 'Option 2', value: '2'},
                        {label: 'Option 3', value: '3'},
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
                        {label: 'Option 1', value: '1'},
                        {label: 'Option 2', value: '2'},
                        {label: 'Option 3', value: '3'},
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
                        {label: 'Option 1', value: '1'},
                        {label: 'Option 2', value: '2'},
                        {label: 'Option 3', value: '3'},
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
                Mit dem Abstands-Element können Sie ihre Abschnitte besser strukturieren.
                Der Abstand wird vertikal eingefügt und kann so genutzt werden, Bereiche innerhalb eines Abschnitts
                deutlicher von darüberliegenden Elementen zu trennen.
            </Typography>
        </Box>
    ),
    [ElementType.Table]: (
        <Box>
            <Typography>
                Mit dem Tabellen-Element können Sie Tabellendaten von Ihren Nutzer:innen abfragen.
                Sie können die Spalten der Tabelle, sowie den Datentyp (Text/Zahl) der einzelnen Spalten festlegen.
                Darüber hinaus können Sie die Mindest- und Maximalzahl an Zeilen definieren.
            </Typography>
        </Box>
    ),
    [ElementType.Text]: (
        <Box>
            <Typography>
                Mit dem Text-Element können Sie Texteingaben von Ihren Nutzer:innen abfragen.
                Sie haben die Möglichkeit eine Maximalzahl von Zeichen festzusetzen und mehrzeiligen Text zuzulassen.
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
                Mit dem Zeit-Element können Sie Zeitangaben von Ihren Nutzer:innen abfragen.
            </Typography>
        </Box>
    ),
    [ElementType.FileUpload]: (
        <Box>
            <Typography>
                Mit dem Anlagen-Element können Sie Ihren Nutzer:innen die Möglichkeit bieten, dem Formular Dateien
                anzufügen. Sie können außerdem die Mindest- und Maximalzahl von hochzuladenden Anlagen bestimmen, sowie
                die erlaubten Dateieindungen.
            </Typography>

            <Typography sx={{mt: 2}}>
                Bitte beachten Sie, dass aus technischen Limitierungen von Folgesystemen eine maximale Dateigröße von
                10MB festgesetzt ist. Darüber hinaus wird die Summe der größen aller Dateien durch das in der gewählten
                Schnittstelle festgesetzten Maximum für Anlagen limitiert.
            </Typography>
        </Box>
    ),
    [ElementType.IntroductionStep]: null,
    [ElementType.SummaryStep]: null,
    [ElementType.SubmitStep]: null,
    [ElementType.SubmittedStep]: null,
}

export function ElementInfoTab({type, onClose}: { type: ElementType, onClose: () => void }) {
    return (
        <DialogContent>
            <Box
                sx={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                }}
            >
                <h3>{ElementNames[type]}</h3>

                <Tooltip title="Schließen">
                    <IconButton
                        onClick={onClose}
                        size="small"
                    >
                        <FontAwesomeIcon icon={faClose}/>
                    </IconButton>
                </Tooltip>
            </Box>

            {
                elementDescriptions[type]
            }
        </DialogContent>
    );
}