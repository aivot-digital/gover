import {BaseInputElementValidator} from '../../validators/base-input-element-validator';
import {MultiCheckboxFieldElement} from '../../models/elements/./form/./input/multi-checkbox-field-element';

export class MultiCheckboxFieldComponentValidator extends BaseInputElementValidator<string[], MultiCheckboxFieldElement> {
    protected checkEmpty(comp: MultiCheckboxFieldElement, value: string[]): boolean {
        return value == null || value.length === 0;
    }

    protected getEmptyErrorText(comp: MultiCheckboxFieldElement): string {
        return `Dies ist eine Pflichtangabe. Bitte wählen Sie mindestens ${comp.minimumRequiredOptions != null && comp.minimumRequiredOptions === 1 ? 'eine Option' : `${comp.minimumRequiredOptions?.toFixed()} Optionen`} aus.`;
    }

    protected makeSpecificErrors(comp: MultiCheckboxFieldElement, value: string[] | undefined, userInput: any): string | null {
        if (value != null && comp.minimumRequiredOptions != null && value.length < comp.minimumRequiredOptions) {
            return `Dies ist eine Pflichtangabe. Bitte wählen Sie mindestens ${comp.minimumRequiredOptions.toFixed()} Optionen aus.`;
        }
        return null;
    }
}
