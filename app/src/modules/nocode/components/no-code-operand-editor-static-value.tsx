import Article from '@aivot/mui-material-symbols-400-outlined/dist/article/Article';
import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';
import Functions from '@aivot/mui-material-symbols-400-outlined/dist/functions/Functions';
import {Autocomplete, Box, InputAdornment, TextField, createFilterOptions} from '@mui/material';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';
import {renderIconButton} from '../../../components/text-field/text-field-component';
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

const staticValueOptionFilter = createFilterOptions<SelectFieldComponentOption>({
    stringify: (option) => `${option.label} ${option.value}`,
});

export function NoCodeOperandEditorStaticValue(props: NoCodeOperandEditorStaticValueProps) {
    const {
        options: originalOptions,
        desiredType,
    } = props;

    const suggestionOptions: SelectFieldComponentOption[] | undefined = useMemo(() => {
        if (originalOptions && originalOptions.length > 0) {
            return originalOptions.map((opt) => ({
                label: opt.label,
                value: opt.value,
            }));
        }

        return undefined;
    }, [originalOptions]);

    if (suggestionOptions != null) {
        return (
            <SuggestedStaticValue {...props} options={suggestionOptions} />
        );
    }

    if (desiredType === NoCodeDataType.Boolean) {
        return (
            <SelectStaticValue {...props} options={BOOL_DEFAULT_OPTIONS} />
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

function getStaticValueLabel(label: string): string {
    return `${label ?? ''} — (Fester Wert)`;
}

function updateStaticValue(props: NoCodeOperandEditorStaticValueProps, value: string | undefined) {
    props.onChange({
        ...props.value,
        value: value != null && value.length > 0 ? value : undefined,
    });
}

function getStaticValueActions(props: NoCodeOperandEditorStaticValueProps) {
    return [
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
    ];
}

function TextStaticValue(props: NoCodeOperandEditorStaticValueProps) {
    return (
        <TextFieldComponent
            label={getStaticValueLabel(props.label)}
            hint={props.hint}
            value={props.value.value}
            onChange={(val) => {
                updateStaticValue(props, val);
            }}
            startIcon={<Article />}
            endAction={getStaticValueActions(props)}
            muiPassTroughProps={{
                margin: 'none',
            }}
        />
    );
}

function SuggestedStaticValue(props: NoCodeOperandEditorStaticValueProps & { options: SelectFieldComponentOption[] }) {
    const selectedValue = useMemo(() => {
        const currentValue = props.value.value;

        if (currentValue == null || currentValue.length === 0) {
            return undefined;
        }

        return props.options.find((option) => option.value === currentValue) ?? currentValue;
    }, [props.options, props.value.value]);

    return (
        <Autocomplete<SelectFieldComponentOption, false, true, true>
            freeSolo
            disableClearable
            options={props.options}
            filterOptions={staticValueOptionFilter}
            value={selectedValue}
            getOptionLabel={(option) => (
                typeof option === 'string' ? option : option.value
            )}
            isOptionEqualToValue={(option, value) => (
                typeof value !== 'string' && option.value === value.value
            )}
            onInputChange={(_, value, reason) => {
                if (reason === 'input' || reason === 'clear') {
                    updateStaticValue(props, value);
                }
            }}
            onChange={(_, value) => {
                updateStaticValue(
                    props,
                    typeof value === 'string' ? value : value?.value,
                );
            }}
            renderOption={(renderProps, option) => (
                <Box
                    component="li"
                    {...renderProps}
                    sx={{display: 'block !important'}}
                >
                    <Box
                        component="span"
                        sx={{fontWeight: 500}}
                    >
                        {option.label}
                    </Box>
                    {
                        option.label !== option.value &&
                        <Box
                            component="span"
                            sx={{
                                color: 'text.secondary',
                                ml: 1,
                            }}
                        >
                            {option.value}
                        </Box>
                    }
                </Box>
            )}
            renderInput={(params) => (
                <TextField
                    {...params}
                    label={getStaticValueLabel(props.label)}
                    helperText={props.hint}
                    margin="none"
                    fullWidth
                    InputLabelProps={{
                        ...params.InputLabelProps,
                        title: getStaticValueLabel(props.label),
                    }}
                    InputProps={{
                        ...params.InputProps,
                        startAdornment: (
                            <>
                                <InputAdornment position="start">
                                    <Article />
                                </InputAdornment>
                                {params.InputProps.startAdornment}
                            </>
                        ),
                        endAdornment: (
                            <>
                                <InputAdornment
                                    position="end"
                                    sx={{mr: 1}}
                                >
                                    {getStaticValueActions(props).map(renderIconButton)}
                                </InputAdornment>
                                {params.InputProps.endAdornment}
                            </>
                        ),
                    }}
                />
            )}
        />
    );
}

function SelectStaticValue(props: NoCodeOperandEditorStaticValueProps & { options: SelectFieldComponentOption[] }) {
    return (
        <SelectFieldComponent
            label={getStaticValueLabel(props.label)}
            hint={props.hint}
            value={props.value.value ?? undefined}
            onChange={(val) => {
                updateStaticValue(props, val);
            }}
            startIcon={<Article />}
            endAction={getStaticValueActions(props)}
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
            label={getStaticValueLabel(props.label)}
            hint={props.hint}
            value={props.value.value ?? undefined}
            onChange={(val) => {
                updateStaticValue(props, val);
            }}
            startIcon={<Article />}
            endAction={getStaticValueActions(props)}
            muiPassTroughProps={{
                margin: 'none',
            }}
            mode={DateFieldComponentModelMode.Day}
        />
    );
}
