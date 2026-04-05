import {useMemo} from 'react';
import {Grid} from '@mui/material';
import {BaseEditorProps} from './base-editor';
import {UiDefinitionInputFieldElement} from '../models/elements/form/input/ui-definition-input-field-element';
import {ElementTreeEntity} from '../components/element-tree/element-tree-entity';
import {SelectFieldComponent} from '../components/select-field/select-field-component';
import {SelectFieldComponentOption} from '../components/select-field/select-field-component-option';
import {ElementType} from '../data/element-type/element-type';
import {getElementNameForType} from '../data/element-type/element-names';

const supportedRootTypes: ElementType[] = [
    ElementType.FormLayout,
    ElementType.Step,
    ElementType.IntroductionStep,
    ElementType.SummaryStep,
    ElementType.SubmitStep,
    ElementType.SubmittedStep,
    ElementType.Alert,
    ElementType.GroupLayout,
    ElementType.Checkbox,
    ElementType.Date,
    ElementType.Headline,
    ElementType.MultiCheckbox,
    ElementType.Number,
    ElementType.ReplicatingContainer,
    ElementType.RichText,
    ElementType.Radio,
    ElementType.Select,
    ElementType.Spacer,
    ElementType.Table,
    ElementType.Text,
    ElementType.Time,
    ElementType.Image,
    ElementType.FileUpload,
    ElementType.CodeInput,
    ElementType.RichTextInput,
    ElementType.UiDefinitionInput,
    ElementType.ChipInput,
    ElementType.DateTime,
    ElementType.DateRange,
    ElementType.TimeRange,
    ElementType.DateTimeRange,
    ElementType.MapPoint,
    ElementType.DomainAndUserSelect,
    ElementType.AssignmentContext,
    ElementType.DataModelSelect,
    ElementType.DataObjectSelect,
    ElementType.NoCodeInput,
];

export function UiDefinitionInputFieldEditor(props: BaseEditorProps<UiDefinitionInputFieldElement, ElementTreeEntity>) {
    const {
        element,
        onPatch,
        editable,
    } = props;

    const rootTypeOptions = useMemo<SelectFieldComponentOption[]>(() => {
        return supportedRootTypes.map((type) => ({
            label: getElementNameForType(type),
            value: type.toString(),
        }));
    }, []);

    return (
        <Grid
            container
            columnSpacing={4}
            rowSpacing={2}
        >
            <Grid size={{xs: 12, lg: 6}}>
                <SelectFieldComponent
                    label="Erwarteter Wurzeltyp"
                    value={element.elementType?.toString() ?? undefined}
                    onChange={(value) => {
                        onPatch({
                            elementType: value == null ? undefined : Number(value) as ElementType,
                        });
                    }}
                    options={rootTypeOptions}
                    disabled={!editable}
                    placeholder="Beliebiger Elementtyp"
                    hint="Optional: schränkt den späteren Editor auf einen bestimmten Starttyp der UI-Definition ein."
                />
            </Grid>
        </Grid>
    );
}
