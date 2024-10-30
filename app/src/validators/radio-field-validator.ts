import {BaseInputElementValidator} from './base-input-element-validator';
import {type RadioFieldElement} from '../models/elements/form/input/radio-field-element';

export class RadioFieldValidator extends BaseInputElementValidator<string, RadioFieldElement> {
    protected checkEmpty(comp: RadioFieldElement, value: string): boolean {
        return false;
    }

    protected getEmptyErrorText(comp: RadioFieldElement): string {
        return 'Dies ist eine Pflichtangabe. Bitte treffen Sie eine entsprechende Auswahl.';
    }

    protected makeSpecificErrors(comp: RadioFieldElement, value: string | undefined, userInput: any): string | null {
        if (value != null) {
            const options = (comp.options ?? []).map((option) => typeof option === 'string' ? option : option.value);
            if (value !== '' && !options.includes(value)) {
                return 'Bitte wählen Sie eine der vorgegebenen Optionen aus.';
            }
        }
        return null;
    }
}
