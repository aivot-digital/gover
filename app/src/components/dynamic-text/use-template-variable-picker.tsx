import {type ReactNode, type RefObject, useMemo, useState} from 'react';
import DataObject from '@aivot/mui-material-symbols-400-n25-outlined/DataObject';
import {
    getInputModeVariableCategoryLabel,
    getInputModeVariableReference,
    type InputModeVariable,
    VariablePickerDialog,
} from '../input-mode-field/input-mode-field';
import {getInputVariableReference, InputVariableSource} from '../../models/input-mode';
import {useViewDispatcherContext} from '../view-dispatcher/view-dispatcher.context';
import type {EndAction} from '../text-field/text-field-component-props';
import {
    type DynamicTextInputMethods,
    type DynamicTextVariableMetadata,
    getDynamicTextInsertionReference,
} from './dynamic-text-metadata';

const TEMPLATE_VARIABLE_SOURCES: InputVariableSource[] = [
    InputVariableSource.ElementMetadata,
    InputVariableSource.ProcessData,
    InputVariableSource.ProtectedProcessData,
];

export interface TemplateVariablePicker {
    variableMetadata: DynamicTextVariableMetadata[];
    endAction: EndAction;
    pickerDialog: ReactNode;
}

/**
 * Offers process variables to template fields that are not input-mode fields, such as payment texts and template
 * slots. Values of Markdown inputs are inserted as their text unless the author keeps their formatting.
 */
export function useTemplateVariablePicker(
    inputRef: RefObject<DynamicTextInputMethods | null>,
    options: {offerFormattedInsertion?: boolean; readOnly: boolean},
): TemplateVariablePicker {
    const {inputModeVariables} = useViewDispatcherContext();
    const [pickerOpen, setPickerOpen] = useState(false);

    const variables = useMemo<InputModeVariable[]>(() => (
        (inputModeVariables ?? [])
            .filter((variable) => TEMPLATE_VARIABLE_SOURCES.includes(variable.source))
    ), [inputModeVariables]);
    const variableMetadata = useMemo(() => variables.map((variable) => ({
        reference: getInputModeVariableReference(variable),
        label: variable.label,
        category: getInputModeVariableCategoryLabel(variable.source),
        origin: typeof variable.origin === 'string' ? variable.origin : variable.origin?.name ?? undefined,
        description: variable.description ?? undefined,
    })), [variables]);

    return {
        variableMetadata,
        endAction: {
            icon: <DataObject/>,
            tooltip: 'Variable referenzieren',
            onClick: () => setPickerOpen(true),
        },
        pickerDialog: (
            <VariablePickerDialog
                open={pickerOpen}
                variables={variables}
                allowedSources={TEMPLATE_VARIABLE_SOURCES}
                selectedReference={null}
                allowClear={false}
                offerFormattedInsertion={options.offerFormattedInsertion}
                readOnly={options.readOnly}
                title="Variable referenzieren"
                onClose={() => setPickerOpen(false)}
                onClear={() => undefined}
                onSelect={(reference, _suggestion, insertion) => {
                    setPickerOpen(false);

                    const insertReference = () => inputRef.current?.insertVariableReference(
                        getDynamicTextInsertionReference(getInputVariableReference(reference), reference.markdown, insertion),
                        insertion,
                    );
                    if (typeof requestAnimationFrame === 'function') {
                        requestAnimationFrame(insertReference);
                    } else {
                        insertReference();
                    }
                }}
            />
        ),
    };
}
