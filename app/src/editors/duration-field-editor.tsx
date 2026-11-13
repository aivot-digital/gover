import {Grid} from '@mui/material';
import {BaseEditorProps} from './base-editor';
import {DurationFieldElement, DurationLargestUnit, DurationPrecision} from '../models/elements/form/input/duration-field-element';
import {SelectFieldComponent} from '../components/select-field/select-field-component';
import {DurationFieldComponent} from '../components/duration-field/duration-field-component';

export function DurationFieldEditor({element, editable, onPatch}: BaseEditorProps<DurationFieldElement>) {
    const precision = element.precision ?? DurationPrecision.Minute;
    const largestUnit = element.largestUnit ?? DurationLargestUnit.Hour;
    return (
        <Grid container columnSpacing={4} rowSpacing={2}>
            <Grid size={12} container columnSpacing={4}>
                <Grid size={{xs: 12, lg: 6}}>
                    <SelectFieldComponent
                        label="Größte Einheit"
                        value={largestUnit}
                        options={[
                            {value: DurationLargestUnit.Hour, label: 'Stunden'},
                            {value: DurationLargestUnit.Day, label: 'Tage'},
                        ]}
                        onChange={value => onPatch({largestUnit: value as DurationLargestUnit})}
                        hint="Ein Tag entspricht hier immer 24 Stunden."
                        required
                        disabled={!editable}
                    />
                </Grid>
                <Grid size={{xs: 12, lg: 6}}>
                    <SelectFieldComponent
                        label="Genauigkeit"
                        value={precision}
                        options={[
                            {value: DurationPrecision.Minute, label: 'Minuten'},
                            {value: DurationPrecision.Second, label: 'Sekunden'},
                        ]}
                        onChange={value => onPatch({precision: value as DurationPrecision})}
                        hint="Legt fest, ob die Zeitdauer in ganzen Minuten oder Sekunden eingegeben wird."
                        required
                        disabled={!editable}
                    />
                </Grid>
            </Grid>
            <Grid size={{xs: 12, lg: 6}}>
                <DurationFieldComponent
                    label="Mindestdauer"
                    value={element.minDuration}
                    precision={precision}
                    largestUnit={largestUnit}
                    maxDuration={element.maxDuration}
                    onChange={minDuration => onPatch({minDuration})}
                    disabled={!editable}
                />
            </Grid>
            <Grid size={{xs: 12, lg: 6}}>
                <DurationFieldComponent
                    label="Höchstdauer"
                    value={element.maxDuration}
                    precision={precision}
                    largestUnit={largestUnit}
                    minDuration={element.minDuration}
                    onChange={maxDuration => onPatch({maxDuration})}
                    disabled={!editable}
                />
            </Grid>
        </Grid>
    );
}
