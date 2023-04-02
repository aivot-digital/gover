import {BaseInputElement} from './base-input-element';
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
}
