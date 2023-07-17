import {BaseInputElementValidator} from '../../validators/base-input-element-validator';
import {RadioFieldElement} from '../../models/elements/form/input/radio-field-element';

export class RadioFieldComponentValidator extends BaseInputElementValidator<string, RadioFieldElement> {
    protected checkEmpty(comp: RadioFieldElement, value: string): boolean {
        return false;
    }

    protected getEmptyErrorText(comp: RadioFieldElement): string {
        return 'Dies ist eine Pflichtangabe. Bitte treffen Sie eine entsprechende Auswahl.';
    }

    protected makeSpecificErrors(comp: RadioFieldElement, value: string | undefined, userInput: any): string | null {
        if (value != null) {
            if (!(comp.options ?? []).includes(value)) {
                return 'Bitte wählen Sie eine der vorgegebenen Optionen aus.';
            }
        }
        return null;
    }
}
