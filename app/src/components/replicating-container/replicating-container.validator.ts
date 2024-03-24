import {BaseInputElementValidator} from '../../validators/base-input-element-validator';
import {type ReplicatingContainerLayout} from '../../models/elements/form/layout/replicating-container-layout';

export class ReplicatingContainerValidator extends BaseInputElementValidator<string[], ReplicatingContainerLayout> {
    protected checkEmpty(comp: ReplicatingContainerLayout, value: string[]): boolean {
        return value == null || value.length === 0;
    }

    protected getEmptyErrorText(comp: ReplicatingContainerLayout): string {
        return `Dies ist eine Pflichtangabe. Bitte fügen Sie mindestens ${comp.minimumRequiredSets === 1 ? 'einen Datensatz' : `${comp.minimumRequiredSets} Datensätze`} hinzu.`;
    }

    protected makeSpecificErrors(comp: ReplicatingContainerLayout, value: string[] | undefined, userInput: any): string | null {
        return null;
    }
}
