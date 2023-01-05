import {BaseInputElementValidator} from '../../validators/base-input-element-validator';
import {NumberFieldElement} from '../../models/elements/form-elements/input-elements/number-field-element';

export class NumberFieldComponentValidator extends BaseInputElementValidator<number, NumberFieldElement> {
    protected checkEmpty(comp: NumberFieldElement, value: number): boolean {
        return isNaN(value);
    }

    protected getEmptyErrorText(comp: NumberFieldElement): string {
        return 'Dies ist eine Pflichtangabe. Bitte füllen Sie das Feld entsprechend aus.';
    }

    protected makeSpecificErrors(comp: NumberFieldElement, value: number | undefined, userInput: any): string | null {
        return null;
    }
}
