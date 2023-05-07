import {AnyElement} from '../models/elements/any-element';
import {isStringNotNullOrEmpty} from "./string-utils";
import {isFunctionCode} from "../models/functions/function-code";
import {isFunctionNoCode} from "../models/functions/function-no-code";
import {isAnyInputElement} from "../models/elements/form/input/any-input-element";

export function getFunctionStatus(comp: AnyElement): null | 'open' | 'fulfilled' | 'unnecessary' {
    if (comp.isVisible != null) {
        const funcExists = isFunctionCode(comp.isVisible) || isFunctionNoCode(comp.isVisible);
        if (isStringNotNullOrEmpty(comp.isVisible.requirements)) {
            return funcExists ? 'fulfilled' : 'open';
        } else if (funcExists) {
            return 'unnecessary';
        }
    }

    if (comp.patchElement != null) {
        const funcExists = isFunctionCode(comp.patchElement);
        if (isStringNotNullOrEmpty(comp.patchElement.requirements)) {
            return funcExists ? 'fulfilled' : 'open';
        } else if (funcExists) {
            return 'unnecessary';
        }
    }

    if (isAnyInputElement(comp)) {
        if (comp.validate != null) {
            const funcExists = isFunctionCode(comp.validate) || isFunctionNoCode(comp.validate);
            if (isStringNotNullOrEmpty(comp.validate.requirements)) {
                return funcExists ? 'fulfilled' : 'open';
            } else if (funcExists) {
                return 'unnecessary';
            }
        }

        if (comp.computeValue != null) {
            const funcExists = isFunctionCode(comp.computeValue);
            if (isStringNotNullOrEmpty(comp.computeValue.requirements)) {
                return funcExists ? 'fulfilled' : 'open';
            } else if (funcExists) {
                return 'unnecessary';
            }
        }
    }

    return null;
}
