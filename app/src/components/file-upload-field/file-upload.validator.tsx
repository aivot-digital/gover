import {BaseInputElementValidator} from '../../validators/base-input-element-validator';
import {
    FileUploadElement,
    FileUploadElementItem
} from "../../models/elements/form-elements/input-elements/file-upload-element";

export class FileUploadValidator extends BaseInputElementValidator<FileUploadElementItem[], FileUploadElement> {
    protected checkEmpty(comp: FileUploadElement, value: FileUploadElementItem[]): boolean {
        return value.length === 0;
    }

    protected getEmptyErrorText(comp: FileUploadElement): string {
        return 'Dies ist eine Pflichtangabe. Bitte fügen Sie die entsprechenden Anlagen hinzu.';
    }

    protected makeSpecificErrors(comp: FileUploadElement, value: FileUploadElementItem[] | undefined, userInput: any): string | null {
        if (value != null) {
            if (comp.minFiles != null && value.length < comp.minFiles) {
                return `Bitte fügen Sie mindestens ${comp.minFiles} Anlagen hinzu.`;
            }

            if (comp.maxFiles != null && value.length > comp.maxFiles) {
                return `Bitte fügen Sie maximal ${comp.maxFiles} Anlagen hinzu.`;
            }
        }
        return null;
    }
}
