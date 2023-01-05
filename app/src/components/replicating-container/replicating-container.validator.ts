import { BaseInputElementValidator } from "../../validators/base-input-element-validator";
import {ReplicatingContainerElement} from '../../models/elements/form-elements/layout-elements/replicating-container-element';

export class ReplicatingContainerValidator extends BaseInputElementValidator<string[], ReplicatingContainerElement> {
    protected checkEmpty(comp: ReplicatingContainerElement, value: string[]): boolean {
        return value == null || value.length === 0;
    }

    protected getEmptyErrorText(comp: ReplicatingContainerElement): string {
        return `Dies ist eine Pflichtangabe. Bitte fügen Sie mindestens ${comp.minimumRequiredSets === 1 ? 'einen Datensatz' : `${comp.minimumRequiredSets} Datensätze`} hinzu.`;
    }

    protected makeSpecificErrors(comp: ReplicatingContainerElement, value: string[] | undefined, userInput: any): string | null {
        return null;
    }
}
