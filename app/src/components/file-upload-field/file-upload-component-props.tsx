export interface FileUploadComponentProps {
    id: string;
    value?: File[] | null;
    onChange: (val: File[] | null) => void;
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
