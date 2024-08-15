import {BaseInputElement} from '../base-input-element';
import {ElementType} from '../../../../data/element-type/element-type';

export interface FileUploadElement extends BaseInputElement<FileUploadElementItem[], ElementType.FileUpload> {
    extensions?: string[];
    isMultifile?: boolean;
    maxFiles?: number;
    minFiles?: number;
}

export interface FileUploadElementItem {
    name: string;
    uri: string;
    size: number;
}

export function isFileUploadElementItem(obj: any): obj is FileUploadElementItem {
    return obj != null && obj.name != null && obj.uri != null && obj.size != null;
}
