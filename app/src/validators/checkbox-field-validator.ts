import {BaseInputElementValidator} from "./base-input-element-validator";
import {CheckboxFieldElement} from "../models/elements/form/input/checkbox-field-element";

export class CheckboxFieldValidator extends BaseInputElementValidator<boolean, CheckboxFieldElement> {
    protected checkEmpty(comp: CheckboxFieldElement, value: boolean): boolean {
        return !value;
    }

    protected getEmptyErrorText(comp: CheckboxFieldElement): string {
        return 'Bitte haken Sie dieses Feld an.';
    }

    protected makeSpecificErrors(comp: CheckboxFieldElement, value: boolean | undefined, userInput: any): string | null {
        return null;
    }
}
