import {loadVisibilityFunction} from './load-function';
import {AnyElement} from '../models/elements/any-element';

export function isComponentVisible(id: string, model: AnyElement, userInput?: any): boolean {
    const func = loadVisibilityFunction(model);
    if (func) {
        try {
            return func(userInput ?? {}, model, id);
        } catch (err) {
            console.error(`Failed to run visibility function ${model.isVisible?.functionName} of ID ${id}`, model, err);
            return false;
        }
    }
    return true;
}
