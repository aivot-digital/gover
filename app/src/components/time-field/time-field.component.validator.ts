import {BaseInputElementValidator} from '../../validators/base-input-element-validator';
import {TimeFieldElement} from '../../models/elements/form/input/time-field-element';

export class TimeFieldComponentValidator extends BaseInputElementValidator<string, TimeFieldElement> {
    protected checkEmpty(comp: TimeFieldElement, value: string): boolean {
        return value.length === 0;
    }

    protected getEmptyErrorText(comp: TimeFieldElement): string {
        return 'Dies ist eine Pflichtangabe. Bitte füllen Sie das Feld entsprechend aus.';
    }

    protected makeSpecificErrors(comp: TimeFieldElement, value: string | undefined, userInput: any): string | null {
        if (value != null) {
            const timestamp = Date.parse(value);
            if (isNaN(timestamp)) {
                return 'Bitte geben Sie eine Uhrzeit im geforderten Format ein.';
            }
        }
        return null;
    }
}
