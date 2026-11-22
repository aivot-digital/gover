import React from 'react';
import {SelectFieldComponent} from '../../../components/select-field/select-field-component';
import {type CodeListColumnOption} from '../hooks/use-code-list-column-options';

interface CodeListColumnSelectFieldProps {
    label: string;
    hint?: string;
    value: string | null | undefined;
    onChange: (value: string | undefined) => void;
    // The columns of the selected code list, loaded once per editor with useCodeListColumnOptions.
    columnOptions: CodeListColumnOption[];
    columnOptionsLoaded: boolean;
    disabled?: boolean;
    required?: boolean;
}

export function CodeListColumnSelectField(props: CodeListColumnSelectFieldProps) {
    const {
        label,
        hint,
        value,
        onChange,
        columnOptions,
        columnOptionsLoaded,
        disabled,
        required,
    } = props;

    // Renamed or removed column keys are not rewritten in elements, so a stale reference is shown instead of dropped.
    const isMissingColumn = columnOptionsLoaded &&
        value != null &&
        value.length > 0 &&
        !columnOptions.some((option) => option.value === value);

    return (
        <SelectFieldComponent
            label={label}
            hint={hint}
            value={value}
            onChange={(value) => onChange(value ?? undefined)}
            options={columnOptions}
            disabled={disabled}
            required={required}
            error={isMissingColumn ? `Die Spalte „${value}“ ist in der Codeliste nicht vorhanden. Wählen Sie eine andere Spalte aus.` : undefined}
            emptyStatePlaceholder={columnOptionsLoaded ? 'Die Codeliste enthält keine Spalten' : 'Bitte wählen Sie zuerst eine Codeliste aus'}
        />
    );
}
