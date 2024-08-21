export interface FileUploadComponentProps {
    id: string;
    value?: File[];
    onChange: (val: File[] | undefined) => void;
    error?: string;
    label: string;
    placeholder?: string;
    required?: boolean;
    disabled?: boolean;
    extensions?: string[];
    isMultifile?: boolean;
    maxFiles?: number;
    minFiles?: number;
    hint?: string;
}