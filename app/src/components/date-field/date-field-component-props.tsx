import {DateFieldComponentModelMode} from "../../models/elements/form/input/date-field-element";
import {SxProps, Theme} from "@mui/material";

export interface DateFieldComponentProps {
    label: string;
    error?: string;
    autocomplete?: string;
    hint?: string;
    required?: boolean;
    disabled?: boolean;
    busy?: boolean;
    value?: string;
    minDate?: Date;
    maxDate?: Date;
    mode: DateFieldComponentModelMode;
    onChange: (val: string | undefined) => void;
    onBlur?: (val: string | undefined) => void;
    sx?: SxProps<Theme>;
    bufferInputUntilBlur?: boolean;
    debounce?: number;
}
