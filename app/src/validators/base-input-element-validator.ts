import {BaseValidator} from './base-validator';
import {isComponentVisible} from '../utils/is-component-visible';
import {loadValidityFunction} from '../utils/load-function';
import {AnyInputElement} from '../models/elements/form-elements/input-elements/any-input-element';

export abstract class BaseInputElementValidator<T, M extends AnyInputElement> extends BaseValidator<M> {
    makeErrors(id: string, comp: M, userInput: any): string | null {
        const isVisible = isComponentVisible(id, comp, userInput);
        if (!isVisible) {
            return null;
        }

        const value: T | undefined = comp.disabled ? comp.value : (userInput[id] ?? comp.value);

        if (comp.required && (value == null || this.checkEmpty(comp, value))) {
            return this.getEmptyErrorText(comp);
        }

        const specificError = this.makeSpecificErrors(comp, value, userInput);
        if (specificError != null) {
            return specificError;
        }

        const func = loadValidityFunction(comp);
        if (func != null) {
            try {
                const error: string | null = func(userInput, comp, id);
                if (error != null) {
                    return error;
                }
            } catch (err) {
                console.error('Failed to run validator of ID ' + id, err);
            }
        }
        return null;
    }

    protected abstract makeSpecificErrors(comp: M, value: T | undefined, userInput: any): string | null;

    protected abstract checkEmpty(comp: M, value: T): boolean;

    protected abstract getEmptyErrorText(comp: M): string;
}
