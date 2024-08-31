import {BaseInputElementValidator} from '../../validators/base-input-element-validator';
import {type FileUploadElement, type FileUploadElementItem} from '../../models/elements/form/input/file-upload-element';
import {isStringNullOrEmpty} from '../../utils/string-utils';

const maxSizeInMegaBytes = 10;
const maxSizeInBytes = maxSizeInMegaBytes * 1000 * 1000; // 10 MB

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

            if (comp.extensions != null && comp.extensions.length > 0) {
                for (const val of value) {
                    const extension = val.name.split('.').pop();
                    if (extension == null || isStringNullOrEmpty(extension) || comp.extensions.every(ex => ex.trim().toLowerCase() != extension.trim().toLowerCase())) {
                        return `Die Anlage „${val.name}“ hat eine unzulässige Dateiendung.`;
                    }
                }
            }

            for (const val of value) {
                if (val.size > maxSizeInBytes) {
                    return `Die Anlage „${val.name}“ überschreitet die maximale Dateigröße pro Datei von ${maxSizeInMegaBytes} MB`;
                }
            }
        }
        return null;
    }
}
