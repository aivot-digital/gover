import {BaseValidator} from './base-validator';
import {isElementVisible} from '../utils/is-element-visible';
import {AnyInputElement} from "../models/elements/form/input/any-input-element";
import {evaluateFunction} from "../utils/evaluate-function";
import {CustomerInput} from "../models/customer-input";

export abstract class BaseInputElementValidator<T, M extends AnyInputElement> extends BaseValidator<M> {
    makeErrors(id: string, comp: M, customerInput: CustomerInput): string | null {
        const isVisible = isElementVisible(id, comp, customerInput);
        if (!isVisible) {
            return null;
        }

        const value: T | undefined = customerInput[id]; // TODO: Compute value ?? comp.computeValue;

        if (comp.required && (value == null || this.checkEmpty(comp, value))) {
            return this.getEmptyErrorText(comp);
        }

        const specificError = this.makeSpecificErrors(comp, value, customerInput);
        if (specificError != null) {
            return specificError;
        }

        let error: string | null = null;
        try {
            error = evaluateFunction<string>(comp.validate, customerInput, comp, id);
        } catch (err) {
            console.error('Failed to run validator of ID ' + id, err);
        }

        return error;
    }

    protected abstract makeSpecificErrors(comp: M, value: T | undefined, userInput: any): string | null;

    protected abstract checkEmpty(comp: M, value: T): boolean;

    protected abstract getEmptyErrorText(comp: M): string;
}
