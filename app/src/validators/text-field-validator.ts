import {BaseInputElementValidator} from "./base-input-element-validator";
import {TextFieldElement} from "../models/elements/form/input/text-field-element";


export class TextFieldValidator extends BaseInputElementValidator<string, TextFieldElement> {
    protected checkEmpty(comp: TextFieldElement, value: string): boolean {
        return value.length === 0;
    }

    protected getEmptyErrorText(comp: TextFieldElement): string {
        return 'Dies ist eine Pflichtangabe. Bitte füllen Sie das Feld entsprechend aus.';
    }

    protected makeSpecificErrors(comp: TextFieldElement, value: string | undefined, userInput: any): string | null {
        /*TODO: check for case of length 1 and length greater than 1*/
        if (value != null && comp.maxCharacters != null && comp.maxCharacters > 0 && value.length > comp.maxCharacters) {
            return `Bitte geben Sie weniger als ${comp.maxCharacters} Zeichen ein.`
        }
        return null;
    }
}
