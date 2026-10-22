import {Grid} from '@mui/material';
import {BaseEditorProps} from './base-editor';
import {EmailFieldElement} from '../models/elements/form/input/email-field-element';
import {CheckboxFieldComponent} from '../components/checkbox-field/checkbox-field-component';
import {TextFieldComponent} from '../components/text-field/text-field-component';

export function EmailFieldEditor({element, editable, onPatch}: BaseEditorProps<EmailFieldElement>) {
    return <Grid container columnSpacing={4} rowSpacing={2}>
        <Grid size={{xs: 12, lg: 6}}>
            <CheckboxFieldComponent
                label="Lokale Domains zulassen"
                value={element.allowLocalDomains ?? false}
                onChange={allowLocalDomains => onPatch({allowLocalDomains})}
                disabled={!editable}
                hint="Erlaubt auch Adressen ohne Punkt im Domainnamen, zum Beispiel name@verwaltung. Aktivieren Sie diese Option nur, wenn solche Adressen beabsichtigt sind und der genutzte Mail-Anbieter E-Mails daran zustellen kann."
            />
        </Grid>
        <Grid size={{lg: 6}}/>
        <Grid size={{xs: 12, lg: 6}}>
            <CheckboxFieldComponent label="Automatisches Ausfüllen durch den Browser"
                value={element.autocomplete !== 'off'} disabled={!editable}
                onChange={enabled => onPatch({autocomplete: enabled ? 'email' : 'off'})}
                hint="Ermöglicht dem Browser, gespeicherte Kontaktdaten vorzuschlagen. Die Unterstützung ist browserabhängig." />
        </Grid>
        <Grid size={{xs: 12, lg: 6}}>
            <TextFieldComponent label="Platzhalter" value={element.placeholder} onChange={placeholder => onPatch({placeholder})} disabled={!editable} />
        </Grid>
    </Grid>;
}
