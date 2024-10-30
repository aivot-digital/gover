export interface FileUploadProps {
    extensions?: string[];
    multiple?: boolean;
    onChange: (files: File[]) => void;
    value: File[];
}
