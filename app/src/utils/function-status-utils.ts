import {type AnyElement} from '../models/elements/any-element';
import {isStringNotNullOrEmpty} from './string-utils';
import {isAnyInputElement} from '../models/elements/form/input/any-input-element';

export type FunctionStatus = 'todo' | 'done' | 'unnecessary';
export interface FunctionStatusItem {
    func: string;
    status: FunctionStatus;
};

export function getFunctionStatus(comp: AnyElement): FunctionStatusItem[] {
    const results: FunctionStatusItem[] = [];

    if (comp.isVisible != null) {
        const funcExists = isStringNotNullOrEmpty(comp.isVisible.code) || comp.isVisible.conditionSet != null;
        if (isStringNotNullOrEmpty(comp.isVisible.requirements)) {
            results.push({
                func: 'Sichtbarkeit',
                status: funcExists ? 'done' : 'todo',
            });
        } else if (funcExists) {
            results.push({
                func: 'Sichtbarkeit',
                status: 'unnecessary',
            });
        }
    }

    if (comp.patchElement != null) {
        const funcExists = isStringNotNullOrEmpty(comp.patchElement.code);
        if (isStringNotNullOrEmpty(comp.patchElement.requirements)) {
            results.push({
                func: 'Sichtbarkeit',
                status: funcExists ? 'done' : 'todo',
            });
        } else if (funcExists) {
            results.push({
                func: 'Sichtbarkeit',
                status: 'unnecessary',
            });
        }
    }

    if (isAnyInputElement(comp)) {
        if (comp.validate != null) {
            const funcExists = isStringNotNullOrEmpty(comp.validate.code) || comp.validate.conditionSet != null;
            if (isStringNotNullOrEmpty(comp.validate.requirements)) {
                results.push({
                    func: 'Sichtbarkeit',
                    status: funcExists ? 'done' : 'todo',
                });
            } else if (funcExists) {
                results.push({
                    func: 'Sichtbarkeit',
                    status: 'unnecessary',
                });
            }
        }

        if (comp.computeValue != null) {
            const funcExists = isStringNotNullOrEmpty(comp.computeValue.code);
            if (isStringNotNullOrEmpty(comp.computeValue.requirements)) {
                results.push({
                    func: 'Sichtbarkeit',
                    status: funcExists ? 'done' : 'todo',
                });
            } else if (funcExists) {
                results.push({
                    func: 'Sichtbarkeit',
                    status: 'unnecessary',
                });
            }
        }
    }

    return results;
}
