export interface TextFieldComponentProps {
    label: string;
    autocomplete?: string;
    placeholder?: string;
    required?: boolean;
    disabled?: boolean;
    display?: boolean;
    multiline?: boolean;
    value?: string;
    error?: string;
    hint?: string;
    maxCharacters?: number;
    minCharacters?: number;
    softLimitCharacters?: number;
    softLimitCharactersWarning?: string;
    rows?: number;
    type?: string;
    onChange: (val: string | undefined) => void;
    onBlur?: (val: string | undefined) => void;
    endAction?: EndAction | Array<EndAction>;
    pattern?: {
        regex: string;
        message: string;
    };
}

type EndAction = {
    icon: React.ReactNode;
    onClick: () => void;
};
