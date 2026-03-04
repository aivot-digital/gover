import {Grid} from '@mui/material';
import {BaseEditorProps} from './base-editor';
import {ElementTreeEntity} from '../components/element-tree/element-tree-entity';
import {CodeInputElement, CodeInputFieldLanguage} from '../models/elements/form/input/code-input-element';
import {SelectFieldComponent} from '../components/select-field/select-field-component';
import {SelectFieldComponentOption} from '../components/select-field/select-field-component-option';
import {NumberFieldComponent} from '../components/number-field/number-field-component';
import {CheckboxFieldComponent} from '../components/checkbox-field/checkbox-field-component';

const languageOptions: SelectFieldComponentOption[] = [
    {
        value: CodeInputFieldLanguage.Javascript,
        label: 'JavaScript',
    },
    {
        value: CodeInputFieldLanguage.Typescript,
        label: 'TypeScript',
    },
    {
        value: CodeInputFieldLanguage.Json,
        label: 'JSON',
    },
    {
        value: CodeInputFieldLanguage.Html,
        label: 'HTML',
    },
    {
        value: CodeInputFieldLanguage.Css,
        label: 'CSS',
    },
    {
        value: CodeInputFieldLanguage.Markdown,
        label: 'Markdown',
    },
];

export function CodeInputFieldEditor(props: BaseEditorProps<CodeInputElement, ElementTreeEntity>) {
    const {
        element,
        editable,
        onPatch,
    } = props;

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
                <SelectFieldComponent
                    label="Sprache"
                    value={element.language ?? CodeInputFieldLanguage.Javascript}
                    onChange={(value) => {
                        onPatch({
                            language: value as CodeInputFieldLanguage | undefined,
                        });
                    }}
                    options={languageOptions}
                    required
                    disabled={!editable}
                    hint="Definiert Syntax-Highlighting und Editor-Unterstützung."
                />
            </Grid>

            <Grid
                size={{
                    xs: 12,
                    lg: 6,
                }}
            >
                <NumberFieldComponent
                    label="Editor-Höhe"
                    value={element.editorHeight ?? 320}
                    onChange={(value) => {
                        const normalizedValue = value == null
                            ? undefined
                            : Math.min(1200, Math.max(200, Math.round(value)));

                        onPatch({
                            editorHeight: normalizedValue,
                        });
                    }}
                    minValue={200}
                    maxValue={1200}
                    decimalPlaces={0}
                    suffix="px"
                    required
                    disabled={!editable}
                    hint="Legt die sichtbare Höhe des Code-Editors in Pixeln fest."
                />
            </Grid>

            <Grid
                size={{
                    xs: 12,
                }}
            >
                <CheckboxFieldComponent
                    label="Zeilenumbruch aktivieren"
                    value={element.wordWrap ?? false}
                    onChange={(checked) => {
                        onPatch({
                            wordWrap: checked,
                        });
                    }}
                    variant="switch"
                    disabled={!editable}
                    hint="Wenn aktiviert, werden lange Zeilen im Editor umgebrochen."
                />
            </Grid>
        </Grid>
    );
}
