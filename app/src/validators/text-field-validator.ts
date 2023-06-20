import {BaseInputElementValidator} from "./base-input-element-validator";
import {TextFieldElement} from "../models/elements/form/input/text-field-element";
import {humanizeNumber} from "../utils/huminization-utils";


export class TextFieldValidator extends BaseInputElementValidator<string, TextFieldElement> {
    protected checkEmpty(comp: TextFieldElement, value: string): boolean {
        return value.length === 0;
    }

    protected getEmptyErrorText(comp: TextFieldElement): string {
        return 'Dies ist eine Pflichtangabe. Bitte füllen Sie das Feld entsprechend aus.';
    }

    protected makeSpecificErrors(comp: TextFieldElement, value: string | undefined, userInput: any): string | null {
        if (value != null) {
            if (comp.maxCharacters != null && comp.maxCharacters > 0 && (comp.minCharacters == null || comp.minCharacters === 0)) {
                if (value.length > comp.maxCharacters) {
                    return `Bitte geben Sie weniger als ${comp.maxCharacters} Zeichen ein.`;
                }
            }

            if (comp.minCharacters != null && comp.minCharacters > 0 && (comp.maxCharacters == null || comp.maxCharacters === 0)) {
                if (value.length < comp.minCharacters) {
                    return `Bitte geben Sie mindestens ${comp.minCharacters} Zeichen ein.`;
                }
            }

            if (comp.maxCharacters != null && comp.maxCharacters > 0 && comp.minCharacters != null && comp.minCharacters > 0) {
                if (value.length !== comp.maxCharacters) {
                    return `Bitte geben Sie genau ${comp.maxCharacters} Zeichen ein.`;
                }
            }
        }

        return null;
    }
}
