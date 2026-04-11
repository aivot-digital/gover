import {type BaseEditorProps} from './base-editor';
import {type ChipInputFieldElement} from '../models/elements/form/input/chip-input-field-element';
import {type ElementTreeEntity} from '../components/element-tree/element-tree-entity';
import {Grid} from '@mui/material';
import {TextFieldComponent} from '../components/text-field/text-field-component';
import {NumberFieldComponent} from '../components/number-field/number-field-component';
import {CheckboxFieldComponent} from '../components/checkbox-field/checkbox-field-component';
import {OptionListInput} from '../components/option-list-input/option-list-input';

export function ChipInputFieldEditor(props: BaseEditorProps<ChipInputFieldElement, ElementTreeEntity>) {
    const {
        element,
        editable,
        onPatch,
    } = props;
    const effectiveMaxItems = element.maxItems != null && element.maxItems > 0 ? element.maxItems : undefined;

    const minItemsError = (
        effectiveMaxItems != null &&
        element.minItems != null &&
        element.minItems > effectiveMaxItems
    );

    const maxItemsError = (
        effectiveMaxItems != null &&
        element.minItems != null &&
        effectiveMaxItems < element.minItems
    );

    const suggestions = (element.suggestions ?? []).map((entry) => ({
        label: entry,
        value: entry,
    }));

    return (
        <Grid
            container
            columnSpacing={4}
            rowSpacing={2}
        >
            <Grid
                size={{
                    xs: 12,
                    lg: 6,
                }}
            >
                <TextFieldComponent
                    label="Platzhalter"
                    value={element.placeholder}
                    onChange={(value) => {
                        onPatch({
                            placeholder: value,
                        });
                    }}
                    hint="Ein Platzhalter zeigt beispielhaft, wie Chips eingegeben werden können."
                    disabled={!editable}
                />
            </Grid>

            <Grid
                size={{
                    xs: 12,
                    lg: 3,
                }}
            >
                <NumberFieldComponent
                    label="Minimale Anzahl"
                    value={element.minItems ?? undefined}
                    onChange={(value) => {
                        onPatch({
                            minItems: value,
                        });
                    }}
                    hint="Geben Sie 0 oder nichts ein, um keine Mindestanzahl zu fordern."
                    error={minItemsError ? 'Die Mindestanzahl darf nicht größer als die Maximalanzahl sein.' : undefined}
                    disabled={!editable}
                />
            </Grid>

            <Grid
                size={{
                    xs: 12,
                    lg: 3,
                }}
            >
                <NumberFieldComponent
                    label="Maximale Anzahl"
                    value={element.maxItems ?? undefined}
                    onChange={(value) => {
                        onPatch({
                            maxItems: value,
                        });
                    }}
                    hint="Geben Sie 0 oder nichts ein, um keine Maximalanzahl zu fordern."
                    error={maxItemsError ? 'Die Maximalanzahl darf nicht kleiner als die Mindestanzahl sein.' : undefined}
                    disabled={!editable}
                />
            </Grid>

            <Grid
                size={{
                    xs: 12,
                }}
            >
                <CheckboxFieldComponent
                    label="Doppelte Chips erlauben"
                    value={element.allowDuplicates ?? undefined}
                    onChange={(checked) => {
                        onPatch({
                            allowDuplicates: checked,
                        });
                    }}
                    hint="Wenn deaktiviert, wird jeder Chip nur einmal übernommen."
                    disabled={!editable}
                />
            </Grid>

            <Grid
                size={{
                    xs: 12,
                }}
            >
                <OptionListInput
                    label="Vorschläge"
                    addLabel="Vorschlag hinzufügen"
                    hint="Die Liste unterstützt bei der Eingabe und kann weiterhin frei ergänzt werden. Bei Vorschlägen sind Anzeige und Wert identisch; das Wert-Feld wird automatisch aus der Anzeige übernommen."
                    noItemsHint="Derzeit sind keine Vorschläge hinterlegt."
                    value={suggestions}
                    onChange={(items) => {
                        const normalizedSuggestions = items?.map((entry) => entry.label.trim());

                        onPatch({
                            suggestions: normalizedSuggestions,
                        });
                    }}
                    allowEmpty={true}
                    disabled={!editable}
                    variant="outlined"
                    keyLabel="Wert"
                    labelLabel="Anzeige"
                    disableKeyField={true}
                />
            </Grid>
        </Grid>
    );
}
