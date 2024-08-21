import {BaseInputElementValidator} from '../../validators/base-input-element-validator';
import {MultiCheckboxFieldElement} from '../../models/elements/form/input/multi-checkbox-field-element';
import {humanizeNumber, pluralize} from "../../utils/huminization-utils";

export class MultiCheckboxFieldComponentValidator extends BaseInputElementValidator<string[], MultiCheckboxFieldElement> {
    protected checkEmpty(comp: MultiCheckboxFieldElement, value: string[]): boolean {
        return value == null || value.length === 0;
    }

    protected getEmptyErrorText(comp: MultiCheckboxFieldElement): string {
        const minRequired = comp.minimumRequiredOptions ?? 1;

        return `Dies ist eine Pflichtangabe. Bitte wählen Sie mindestens ${humanizeNumber(minRequired)} ${pluralize(minRequired, 'Option', 'Optionen')} aus.`;
    }

    protected makeSpecificErrors(comp: MultiCheckboxFieldElement, value: string[] | undefined, userInput: any): string | null {
        if (value != null && comp.minimumRequiredOptions != null && value.length < comp.minimumRequiredOptions) {
            return `Dies ist eine Pflichtangabe. Bitte wählen Sie mindestens ${humanizeNumber(comp.minimumRequiredOptions)} ${pluralize(comp.minimumRequiredOptions, 'Option', 'Optionen')} aus.`;
        }
        return null;
    }
}
