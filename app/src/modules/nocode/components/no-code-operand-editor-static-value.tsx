import Article from '@aivot/mui-material-symbols-400-outlined/dist/article/Article';
import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';
import Functions from '@aivot/mui-material-symbols-400-outlined/dist/functions/Functions';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';
import {NoCodeStaticValue} from '../../../models/functions/no-code-expression';
import {NoCodeParameterOption} from '../../../models/dtos/no-code-operator-details-dto';
import {SelectFieldComponent} from '../../../components/select-field/select-field-component';
import {NoCodeDataType} from '../../../data/no-code-data-type';
import {useMemo} from 'react';
import {SelectFieldComponentOption} from '../../../components/select-field/select-field-component-option';
import {DateFieldComponent} from '../../../components/date-field/date-field-component';
import {DateFieldComponentModelMode} from '../../../models/elements/form/input/date-field-element';

interface NoCodeOperandEditorStaticValueProps {
    label: string;
    hint?: string;
    value: NoCodeStaticValue;
    onChange: (value: NoCodeStaticValue | undefined) => void;
    options?: NoCodeParameterOption[];
    desiredType: NoCodeDataType;
    onAddEnclosingExpression: () => void;
}

export const BOOL_DEFAULT_OPTIONS: NoCodeParameterOption[] = [
    {label: 'Wahr', value: 'true'},
    {label: 'Falsch', value: 'false'},
];

export function NoCodeOperandEditorStaticValue(props: NoCodeOperandEditorStaticValueProps) {
    const {
        options: originalOptions,
        desiredType,
    } = props;

    const options: SelectFieldComponentOption[] | undefined = useMemo(() => {
        if (originalOptions && originalOptions.length > 0) {
            return originalOptions.map((opt) => ({
                label: opt.label,
                value: opt.value,
            }));
        }

        if (desiredType === NoCodeDataType.Boolean) {
            return BOOL_DEFAULT_OPTIONS;
        }

        return undefined;
    }, [originalOptions, desiredType]);

    if (options != null) {
        return (
            <SelectStaticValue {...props} options={options} />
        );
    }

    if (desiredType === NoCodeDataType.Date) {
        return (
            <DateStaticValue {...props} />
        );
    }

    return (
        <TextStaticValue {...props} />
    );
}

function TextStaticValue(props: NoCodeOperandEditorStaticValueProps) {
    return (
        <TextFieldComponent
            label={`${props.label ?? ''} — (Fester Wert)`}
            hint={props.hint}
            value={props.value.value}
            onChange={(val) => {
                props.onChange({
                    ...props.value,
                    value: val,
                });
            }}
            startIcon={<Article />}
            endAction={[
                {
                    icon: <Delete />,
                    tooltip: 'Diesen festen Wert löschen',
                    onClick: () => {
                        props.onChange(undefined);
                    },
                },
                {
                    tooltip: 'Diesen festen Wert mit einem Ausdruck verknüpfen',
                    icon: <Functions />,
                    onClick: props.onAddEnclosingExpression,
                },
            ]}
            muiPassTroughProps={{
                margin: 'none',
            }}
        />
    );
}

function SelectStaticValue(props: NoCodeOperandEditorStaticValueProps & { options: SelectFieldComponentOption[] }) {
    return (
        <SelectFieldComponent
            label={`${props.label ?? ''} — (Fester Wert)`}
            hint={props.hint}
            value={props.value.value ?? undefined}
            onChange={(val) => {
                props.onChange({
                    ...props.value,
                    value: val,
                });
            }}
            startIcon={<Article />}
            endAction={[
                {
                    icon: <Delete />,
                    tooltip: 'Diesen festen Wert löschen',
                    onClick: () => {
                        props.onChange(undefined);
                    },
                },
                {
                    tooltip: 'Diesen festen Wert mit einem Ausdruck verknüpfen',
                    icon: <Functions />,
                    onClick: props.onAddEnclosingExpression,
                },
            ]}
            muiPassTroughProps={{
                margin: 'none',
            }}
            options={props.options}
        />
    );
}


function DateStaticValue(props: NoCodeOperandEditorStaticValueProps) {
    return (
        <DateFieldComponent
            label={`${props.label ?? ''} — (Fester Wert)`}
            hint={props.hint}
            value={props.value.value ?? undefined}
            onChange={(val) => {
                props.onChange({
                    ...props.value,
                    value: val,
                });
            }}
            startIcon={<Article />}
            endAction={[
                {
                    icon: <Delete />,
                    tooltip: 'Diesen festen Wert löschen',
                    onClick: () => {
                        props.onChange(undefined);
                    },
                },
                {
                    tooltip: 'Diesen festen Wert mit einem Ausdruck verknüpfen',
                    icon: <Functions />,
                    onClick: props.onAddEnclosingExpression,
                },
            ]}
            muiPassTroughProps={{
                margin: 'none',
            }}
            mode={DateFieldComponentModelMode.Day}
        />
    );
}