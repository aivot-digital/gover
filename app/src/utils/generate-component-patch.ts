import {loadPatchFunction} from './load-function';
import {AnyElement} from '../models/elements/any-element';

export function generateComponentPatch(id: string, model: AnyElement, userInput?: any): any {
    const func = loadPatchFunction(model);
    if (func) {
        try {
            return func(userInput ?? {}, model, id);
        } catch (err) {
            console.error('Failed to run patch of ID ' + id, err);
            return false;
        }
    }
    return null;
}
