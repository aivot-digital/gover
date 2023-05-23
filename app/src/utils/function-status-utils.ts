import {AnyElement} from '../models/elements/any-element';
import {isStringNotNullOrEmpty} from "./string-utils";
import {isAnyInputElement} from "../models/elements/form/input/any-input-element";

export function getFunctionStatus(comp: AnyElement): null | 'open' | 'fulfilled' | 'unnecessary' {
    if (comp.isVisible != null) {
        const funcExists = isStringNotNullOrEmpty(comp.isVisible.code) || comp.isVisible.conditionSet != null;
        if (isStringNotNullOrEmpty(comp.isVisible.requirements)) {
            return funcExists ? 'fulfilled' : 'open';
        } else if (funcExists) {
            return 'unnecessary';
        }
    }

    if (comp.patchElement != null) {
        const funcExists = isStringNotNullOrEmpty(comp.patchElement.code);
        if (isStringNotNullOrEmpty(comp.patchElement.requirements)) {
            return funcExists ? 'fulfilled' : 'open';
        } else if (funcExists) {
            return 'unnecessary';
        }
    }

    if (isAnyInputElement(comp)) {
        if (comp.validate != null) {
            const funcExists = isStringNotNullOrEmpty(comp.validate.code) || comp.validate.conditionSet != null;
            if (isStringNotNullOrEmpty(comp.validate.requirements)) {
                return funcExists ? 'fulfilled' : 'open';
            } else if (funcExists) {
                return 'unnecessary';
            }
        }

        if (comp.computeValue != null) {
            const funcExists = isStringNotNullOrEmpty(comp.computeValue.requirements);
            if (isStringNotNullOrEmpty(comp.computeValue.requirements)) {
                return funcExists ? 'fulfilled' : 'open';
            } else if (funcExists) {
                return 'unnecessary';
            }
        }
    }

    return null;
}
