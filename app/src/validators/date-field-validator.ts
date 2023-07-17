import {BaseInputElementValidator} from "./base-input-element-validator";
import {DateFieldElement} from "../models/elements/form/input/date-field-element";

export class DateFieldValidator extends BaseInputElementValidator<string, DateFieldElement> {
    protected checkEmpty(comp: DateFieldElement, value: string): boolean {
        return value.length === 0;
    }

    protected getEmptyErrorText(comp: DateFieldElement): string {
        return 'Dies ist eine Pflichtangabe. Bitte füllen Sie das Feld entsprechend aus.';
    }

    protected makeSpecificErrors(comp: DateFieldElement, value: string | undefined, userInput: any): string | null {
        /*TODO: case currently not reachable, error message should include date format of input*/
        if (value != null) {
            const timestamp = Date.parse(value);
            if (isNaN(timestamp)) {
                return 'Bitte geben Sie ein Datum im geforderten Format ein.';
            }
        }

        return null;
    }
}
