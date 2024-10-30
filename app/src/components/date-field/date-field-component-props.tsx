import {DateFieldComponentModelMode} from "../../models/elements/form/input/date-field-element";

export interface DateFieldComponentProps {
    label: string;
    error?: string;
    autocomplete?: string;
    hint?: string;
    required?: boolean;
    disabled?: boolean;
    value?: string;
    minDate?: Date;
    maxDate?: Date;
    mode: DateFieldComponentModelMode;
    onChange: (val: string | undefined) => void;
}
