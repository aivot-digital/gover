
export interface TableFieldComponentColumnDefinition {
    label: string;
    datatype: 'string' | 'number';
    placeholder?: string;
    decimalPlaces?: number;
    optional?: boolean;
    disabled?: boolean;
}

export type TableRowValue = Record<string, string | undefined | null>;

export interface TableFieldComponentProps {
    label: string;
    fields: TableFieldComponentColumnDefinition[];
    value?: TableRowValue[];
    onChange: (value: TableRowValue[] | undefined) => void;
    required?: boolean;
    disabled?: boolean;
    hint?: string;
    error?: string;
    maximumRows?: number;
    minimumRows?: number;
}