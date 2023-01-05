import {BaseInputElementValidator} from '../../validators/base-input-element-validator';
import {TableFieldElement} from '../../models/elements/form-elements/input-elements/table-field-element';
import {isNullOrEmpty} from '../../utils/is-null-or-empty';

export class TableFieldComponentValidator extends BaseInputElementValidator<{[key: string]: string}[], TableFieldElement> {
    protected checkEmpty(comp: TableFieldElement, value: {[key: string]: string}[]): boolean {
        return value.length === 0 || value.some(row => (comp.fields ?? []).some(field => !field.optional && !field.disabled && isNullOrEmpty(row[field.label])));
    }

    protected getEmptyErrorText(comp: TableFieldElement): string {
        return `Dies ist eine Pflichtangabe. Bitte fügen Sie mindestens ${comp.minimumRequiredRows === 1 ? 'eine Zeile' : `${comp.minimumRequiredRows} Zeilen`} hinzu.`;
    }

    protected makeSpecificErrors(comp: TableFieldElement, value: {[key: string]: string}[] | undefined, userInput: any): string | null {
        if (value != null) {
            if (value.some(row => (comp.fields ?? []).some(field => !field.optional && !field.disabled && isNullOrEmpty(row[field.label])))) {
                return 'Bitte füllen Sie alle fehlenden Pflichtfelder in den von Ihnen angegebenen Zeilen aus.';
            }
            if (comp.minimumRequiredRows != null && comp.minimumRequiredRows > 0 && value.length < comp.minimumRequiredRows) {
                if (comp.minimumRequiredRows === 1) {
                    return 'Bitte füllen Sie mindestens eine Zeile aus.';
                } else {
                    return `Bitte füllen Sie mindestens ${comp.minimumRequiredRows} Zeilen aus.`;
                }
            }
        }
        return null;
    }
}
