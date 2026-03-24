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

    const minItemsError = (
        element.maxItems != null &&
        element.minItems != null &&
        element.minItems > element.maxItems
    );

    const maxItemsError = (
        element.maxItems != null &&
        element.minItems != null &&
        element.maxItems < element.minItems
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
                    hint="Lassen Sie das Feld leer, wenn keine Obergrenze gelten soll."
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
                    hint="Die Liste unterstützt bei der Eingabe und kann weiterhin frei ergänzt werden."
                    noItemsHint="Derzeit sind keine Vorschläge hinterlegt."
                    value={suggestions}
                    onChange={(items) => {
                        const originalSuggestions = element.suggestions ?? [];
                        const normalizedSuggestions = (items ?? [])
                            .map((entry, index) => {
                                const originalValue = (originalSuggestions[index] ?? '');
                                const normalizedLabel = entry.label ?? '';
                                const normalizedValue = entry.value ?? '';

                                if (normalizedLabel.trim().length === 0 && normalizedValue.trim().length === 0) {
                                    return '';
                                }

                                if (normalizedLabel === normalizedValue) {
                                    return normalizedLabel;
                                }

                                // Keep both editor columns usable although only one string is stored.
                                if (normalizedLabel === originalValue && normalizedValue.trim().length > 0) {
                                    return normalizedValue;
                                }

                                if (normalizedValue === originalValue && normalizedLabel.trim().length > 0) {
                                    return normalizedLabel;
                                }

                                if (normalizedLabel.trim().length > 0) {
                                    return normalizedLabel;
                                }

                                return normalizedValue;
                            });

                        onPatch({
                            suggestions: normalizedSuggestions,
                        });
                    }}
                    allowEmpty={true}
                    disabled={!editable}
                    variant="outlined"
                    keyLabel="Wert"
                    labelLabel="Anzeige"
                />
            </Grid>
        </Grid>
    );
}
