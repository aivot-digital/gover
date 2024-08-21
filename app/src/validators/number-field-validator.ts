import {BaseInputElementValidator} from './base-input-element-validator';
import {type NumberFieldElement} from '../models/elements/form/input/number-field-element';
import {formatNumToGermanNum} from '../utils/format-german-numbers';

export class NumberFieldValidator extends BaseInputElementValidator<number, NumberFieldElement> {
    protected checkEmpty(comp: NumberFieldElement, value: number): boolean {
        return isNaN(value);
    }

    protected getEmptyErrorText(comp: NumberFieldElement): string {
        return 'Dies ist eine Pflichtangabe. Bitte füllen Sie das Feld entsprechend aus.';
    }

    protected makeSpecificErrors(comp: NumberFieldElement, value: number | undefined, userInput: any): string | null {
        if (value != null) {
            const maxvalue = Math.pow(2, 31);

            if (value >= maxvalue) {
                return `Der eingegebene Wert ist zu groß. Bitte geben Sie einen Wert kleiner als ${formatNumToGermanNum(maxvalue, 0)} ein.`;
            }

            const minvalue = maxvalue * -1;
            if (value <= minvalue) {
                return `Der eingegebene Wert ist zu klein. Bitte geben Sie einen Wert größer als ${formatNumToGermanNum(minvalue, 0)} ein.`;
            }
        }

        return null;
    }
}
