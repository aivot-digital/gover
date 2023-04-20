import {PatchFunction} from '../models/_lib/patch-function';
import {VisibilityFunction} from '../models/_lib/visibility-function';
import {ValidityFunction} from '../models/_lib/validity-function';
import {AnyInputElement} from '../models/elements/./form/./input/any-input-element';
import {AnyElement} from '../models/elements/any-element';
import {isNullOrEmpty} from './is-null-or-empty';

export function loadPatchFunction(model: AnyElement): PatchFunction | null {
    return loadFunction<PatchFunction>(model, model.patchElement?.functionName);
}

export function loadVisibilityFunction(model: AnyElement): VisibilityFunction | null {
    return loadFunction<VisibilityFunction>(model, model.isVisible?.functionName);
}

export function loadValidityFunction(model: AnyInputElement): ValidityFunction | null {
    return loadFunction<ValidityFunction>(model, model.validate?.functionName);
}

function loadFunction<F>(model: AnyElement, functionName?: string | null): F | null {
    if (!isNullOrEmpty(functionName)) {
        const func: F | null = (window as any)[functionName!];
        if (func != null) {
            try {
                return func;
            } catch (e) {
                console.error(`Failed to run function ${functionName} of ID ${model.id}`, model, e);
            }
        } else {
            console.error(`Missing function ${functionName} of ID ${model.id}`, model);
        }
    }
    return null;
}
