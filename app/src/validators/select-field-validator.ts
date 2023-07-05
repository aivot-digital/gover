import {BaseInputElementValidator} from "./base-input-element-validator";
import {SelectFieldElement} from "../models/elements/form/input/select-field-element";

export class SelectFieldValidator extends BaseInputElementValidator<string, SelectFieldElement> {
    protected checkEmpty(comp: SelectFieldElement, value: string): boolean {
        return false;
    }

    protected getEmptyErrorText(comp: SelectFieldElement): string {
        return 'Dies ist eine Pflichtangabe. Bitte treffen Sie eine entsprechende Auswahl.';
    }

    protected makeSpecificErrors(comp: SelectFieldElement, value: string | undefined, userInput: any): string | null {
        if (value != null) {
            if (value !== '' && !(comp.options ?? []).includes(value)) {
                return 'Bitte wählen Sie eine der vorgegebenen Optionen aus.'
            }
        }
        return null;
    }
}
