import {CheckboxFieldComponent} from '../components/checkbox-field/checkbox-field-component';
import {Grid, Typography} from '@mui/material';
import {getCountries, CountryCode} from 'libphonenumber-js';
import {BaseEditorProps} from './base-editor';
import {PhoneNumberFieldElement, PhoneNumberValidation} from '../models/elements/form/input/phone-number-field-element';
import {SelectFieldComponent} from '../components/select-field/select-field-component';
import {MultiChoiceCombobox} from '../components/multi-checkbox-field/multi-choice-combobox';
import {TextFieldComponent} from '../components/text-field/text-field-component';

const countryNames = new Intl.DisplayNames(['de'], {type: 'region'});
const countries = getCountries().map(value => ({value, label: countryNames.of(value) ?? value}))
    .sort((a, b) => a.label.localeCompare(b.label, 'de'));

export function PhoneNumberFieldEditor({element, editable, onPatch}: BaseEditorProps<PhoneNumberFieldElement>) {
    return <Grid container columnSpacing={4} rowSpacing={2}>
        <Grid size={{xs: 12, lg: 6}}>
            <SelectFieldComponent label="Vorausgewähltes Land" value={element.defaultCountry ?? 'DE'} options={countries}
                onChange={value => onPatch({defaultCountry: value as CountryCode})} required disabled={!editable}
                hint="Legt die Ländervorwahl bei einer leeren Eingabe fest. Andere Länder bleiben auswählbar." />
        </Grid>
        <Grid size={{xs: 12, lg: 6}}>
            <SelectFieldComponent label="Prüfung" value={element.validationMode ?? PhoneNumberValidation.Possible}
                options={[
                    {
                        value: PhoneNumberValidation.Possible,
                        label: 'Plausibilitätsprüfung',
                        subLabel: 'Prüft, ob die Nummernlänge für das gewählte Land möglich ist. Für die meisten Formulare empfohlen.',
                    },
                    {
                        value: PhoneNumberValidation.Strict,
                        label: 'Nummernplanprüfung',
                        subLabel: 'Prüft zusätzlich bekannte Nummernbereiche und Nummernstrukturen. Kann gültige Nummern ablehnen, wenn neue Nummernbereiche noch nicht in den Prüfdaten enthalten sind.',
                    },
                ]}
                onChange={value => onPatch({validationMode: value as PhoneNumberValidation})} required disabled={!editable}
                muiPassTroughProps={{
                    slotProps: {
                        select: {
                            MenuProps: {
                                sx: {
                                    '& .MuiListItemText-secondary': {
                                        whiteSpace: 'normal',
                                    },
                                },
                            },
                        },
                    },
                }}
                hint="Ob die Telefonnummer vergeben oder erreichbar ist, kann nicht geprüft werden." />
        </Grid>
        <Grid size={{xs: 12, lg: 6}}>
            <MultiChoiceCombobox label="Bevorzugte Länder" value={element.preferredCountries ?? ['DE', 'AT', 'CH']}
                options={countries} onChange={value => onPatch({preferredCountries: (value ?? []) as CountryCode[]})}
                disabled={!editable} hint="Diese Länder stehen am Anfang der Länderauswahl. Alle übrigen Länder bleiben verfügbar." />
        </Grid>
        <Grid size={{xs: 12, lg: 6}}>
            <CheckboxFieldComponent label="Automatisches Ausfüllen durch den Browser"
                value={element.autocomplete !== 'off'} disabled={!editable}
                onChange={enabled => onPatch({autocomplete: enabled ? 'tel' : 'off'})}
                hint="Ermöglicht dem Browser, gespeicherte Kontaktdaten vorzuschlagen. Die Unterstützung ist browserabhängig." />
        </Grid>
        <Grid size={{xs: 12, lg: 6}}>
            <TextFieldComponent label="Platzhalter" value={element.placeholder} onChange={placeholder => onPatch({placeholder})} disabled={!editable} />
        </Grid>
        <Grid size={12}>
            <Typography variant="body2" color="text.secondary" sx={{maxWidth: 752}}>
                Direkt wählbare Durchwahlen, zum Beispiel „030 123456-78“, können Sie im Telefonnummer-Feld erfassen.
                Muss nach dem Verbindungsaufbau noch eine Nebenstelle eingegeben werden, etwa „030 123456, danach 7 wählen“,
                erfassen Sie diesen Zusatz bei Bedarf in einem separaten Textfeld.
            </Typography>
        </Grid>
    </Grid>;
}
