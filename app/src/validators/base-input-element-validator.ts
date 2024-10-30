import {BaseValidator} from './base-validator';
import {isElementVisible} from '../utils/is-element-visible';
import {type AnyInputElement} from '../models/elements/form/input/any-input-element';
import {evaluateFunction} from '../utils/evaluate-function';
import {type AnyElement} from '../models/elements/any-element';

export abstract class BaseInputElementValidator<T, M extends AnyInputElement> extends BaseValidator<M> {
    makeErrors(allElements: AnyElement[], idPrefix: string | undefined, id: string, comp: M, userInput: any): string | null {
        const isVisible = isElementVisible(idPrefix, allElements, id, comp, userInput);
        if (!isVisible) {
            return null;
        }

        const value: T | undefined = userInput[idPrefix != null ? idPrefix + id : id]; // TODO: Compute value ?? comp.computeValue;

        if (comp.required && (value == null || this.checkEmpty(comp, value))) {
            return this.getEmptyErrorText(comp);
        }

        const specificError = this.makeSpecificErrors(comp, value, userInput);
        if (specificError != null) {
            return specificError;
        }

        let error: string | null = null;
        try {
            error = evaluateFunction(idPrefix, allElements, comp.validate, userInput, comp, id, false);
        } catch (err) {
            console.error('Failed to run validator of ID ' + id, err);
        }

        return error;
    }

    protected abstract makeSpecificErrors(comp: M, value: T | undefined, userInput: any): string | null;

    protected abstract checkEmpty(comp: M, value: T): boolean;

    protected abstract getEmptyErrorText(comp: M): string;
}
