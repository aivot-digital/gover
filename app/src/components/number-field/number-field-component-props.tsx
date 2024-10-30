export interface NumberFieldComponentProps {
    label: string;
    placeholder?: string;
    decimalPlaces?: number;
    hint?: string;
    error?: string;
    suffix?: string;
    required?: boolean;
    disabled?: boolean;
    value?: number;
    onChange: (val: number | undefined) => void;
    minValue?: number;
    maxValue?: number;
}
