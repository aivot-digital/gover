import {type BaseEditorProps} from './base-editor';
import {type ChipInputFieldElement} from '../models/elements/form/input/chip-input-field-element';
import {Grid} from '@mui/material';
import {TextFieldComponent} from '../components/text-field/text-field-component';
import {NumberFieldComponent} from '../components/number-field/number-field-component';
import {CheckboxFieldComponent} from '../components/checkbox-field/checkbox-field-component';
import {ChipInputFieldComponent} from '../components/chip-input-field/chip-input-field-component';
import {OptionsSourceType} from '../models/elements/form/input/options-source-type';
import {SelectFieldComponent} from '../components/select-field/select-field-component';
import {CodeListSelectField} from '../modules/code-lists/components/code-list-select-field';
import {MultiCheckboxComponent} from '../components/multi-checkbox-field/multi-checkbox-component';
import {ChipInputPasteSeparatorOptions, DefaultChipInputPasteSeparators} from '../data/chip-input-paste-separators';
import {ElementEditorSectionHeader} from '../components/element-editor-section-header/element-editor-section-header';

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
    // Empty rows saved by the former table editor are not actual suggestions.
    const suggestions = (element.suggestions ?? []).filter((suggestion) => suggestion.trim().length > 0);
    const effectiveMaxItems = element.maxItems != null && element.maxItems > 0 ? element.maxItems : undefined;

    const effectiveMinItems = element.minItems != null && element.minItems > 0
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
                    hint="Ein Platzhalter zeigt ein Beispiel für einen Eintrag."
                    disabled={!editable}
                />
            </Grid>

            <Grid size={{xs: 12, lg: 6}}>
                <NumberFieldComponent
                    label="Maximale Zeichenanzahl je Eintrag"
                    value={element.maxItemLength ?? undefined}
                    onChange={maxItemLength => onPatch({maxItemLength})}
                    minValue={0}
                    hint="Geben Sie 0 oder nichts ein, um die Länge nicht zu begrenzen."
                    disabled={!editable}
                />
            </Grid>

            <Grid size={{xs: 12, lg: 6}}>
                <NumberFieldComponent
                    label="Minimale Anzahl"
                    value={element.minItems ?? undefined}
                    onChange={minItems => onPatch({minItems})}
                    minValue={0}
                    hint="Gilt, sobald Einträge vorhanden sind. Optionale Felder dürfen leer bleiben. 0 oder keine Angabe bedeutet keine Mindestanzahl."
                    error={minItemsError ? 'Die Mindestanzahl darf nicht größer als die Maximalanzahl sein.' : undefined}
                    disabled={!editable}
                />
            </Grid>

            <Grid
                size={{
                    xs: 12,
                    lg: 6,
                }}
            >
                <NumberFieldComponent
                    label="Maximale Anzahl"
                    value={element.maxItems ?? undefined}
                    minValue={0}
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

            <Grid size={{xs: 12}}>
                <MultiCheckboxComponent
                    label="Trennzeichen beim Einfügen"
                    value={element.pasteSeparators ?? DefaultChipInputPasteSeparators}
                    options={ChipInputPasteSeparatorOptions}
                    onChange={pasteSeparators => onPatch({pasteSeparators: pasteSeparators ?? []})}
                    hint="Text aus der Zwischenablage wird an diesen Zeichen in einzelne Einträge aufgeteilt. Ohne Auswahl bleibt der eingefügte Text ein Eintrag."
                    displayInline
                    disabled={!editable}
                />
            </Grid>

            <Grid
                size={{
                    xs: 12,
                }}
            >
                <CheckboxFieldComponent
                    label="Doppelte Einträge erlauben"
                    value={element.allowDuplicates ?? undefined}
                    onChange={(checked) => {
                        onPatch({
                            allowDuplicates: checked,
                        });
                    }}
                    hint="Erlaubt, denselben Text mehrfach manuell einzugeben."
                    disabled={!editable}
                />
            </Grid>

            <Grid size={{xs: 12}}>
                <ElementEditorSectionHeader title="Vorschläge zur Texteingabe" variant="h6">
                    Hinterlegen Sie häufige Eingaben manuell oder wählen Sie eine System-Codeliste.
                    Die Vorschläge werden beim Tippen gefiltert und können übernommen werden.
                    Eigene Texte bleiben möglich.
                </ElementEditorSectionHeader>
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
                        lg: 6,
                    }}
                >
                    <ChipInputFieldComponent
                        label="Vorschläge"
                        hint="Geben Sie Vorschläge einzeln ein oder fügen Sie mehrere Zeilen aus der Zwischenablage ein."
                        value={suggestions}
                        onChange={(suggestions) => onPatch({suggestions: suggestions ?? []})}
                        disabled={!editable}
                    />
                </Grid>
            }
        </Grid>
    );
}
