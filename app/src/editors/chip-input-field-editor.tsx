import {type BaseEditorProps} from './base-editor';
import {type ChipInputFieldElement} from '../models/elements/form/input/chip-input-field-element';
import {Grid} from '@mui/material';
import {TextFieldComponent} from '../components/text-field/text-field-component';
import {NumberFieldComponent} from '../components/number-field/number-field-component';
import {CheckboxFieldComponent} from '../components/checkbox-field/checkbox-field-component';
import {OptionListInput} from '../components/option-list-input/option-list-input';
import {OptionsSourceType} from '../models/elements/form/input/options-source-type';
import {SelectFieldComponent} from '../components/select-field/select-field-component';
import {CodeListSelectField} from '../modules/code-lists/components/code-list-select-field';

const optionsSourceOptions = [
    {
        label: 'Manuelle Eingabe',
        value: OptionsSourceType.Manual,
    },
    {
        label: 'System-Codeliste',
        value: OptionsSourceType.CodeList,
    },
];

export function ChipInputFieldEditor(props: BaseEditorProps<ChipInputFieldElement>) {
    const {
        element,
        editable,
        onPatch,
        hasSummaryLayoutParent,
    } = props;

    if (hasSummaryLayoutParent) {
        return null;
    }

    const optionsSource = element.optionsSource ?? OptionsSourceType.Manual;
    const usesManualSuggestions = optionsSource === OptionsSourceType.Manual;
    const effectiveMaxItems = element.maxItems != null && element.maxItems > 0 ? element.maxItems : undefined;

    const effectiveMinItems = element.required === true && element.minItems != null && element.minItems > 0
        ? element.minItems
        : undefined;

    const minItemsError = (
        effectiveMaxItems != null &&
        effectiveMinItems != null &&
        effectiveMinItems > effectiveMaxItems
    );

    const maxItemsError = (
        effectiveMaxItems != null &&
        effectiveMinItems != null &&
        effectiveMaxItems < effectiveMinItems
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

            {
                element.required === true &&
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
            }

            <Grid
                size={{
                    xs: 12,
                    lg: element.required === true ? 3 : 6,
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
                    lg: 6,
                }}
            >
                <SelectFieldComponent
                    label="Vorschläge definieren über"
                    value={optionsSource}
                    onChange={(value) => {
                        const nextSource = (value as OptionsSourceType | null) ?? OptionsSourceType.Manual;

                        onPatch({
                            optionsSource: nextSource,
                            codeListKey: nextSource === OptionsSourceType.CodeList ? element.codeListKey : undefined,
                        });
                    }}
                    options={optionsSourceOptions}
                    disabled={!editable}
                    required
                />
            </Grid>

            {
                !usesManualSuggestions &&
                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                >
                    <CodeListSelectField
                        value={element.codeListKey}
                        hint="Aus der Codeliste werden die Beschriftungen als Vorschläge übernommen. Die technischen Werte werden für Chip-Eingaben nicht verwendet."
                        onChange={(codeListKey) => {
                            onPatch({
                                codeListKey,
                            });
                        }}
                        disabled={!editable}
                        required
                    />
                </Grid>
            }

            {
                usesManualSuggestions &&
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
            }
        </Grid>
    );
}
