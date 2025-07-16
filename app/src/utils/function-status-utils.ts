import {type AnyElement} from '../models/elements/any-element';
import {isStringNotNullOrEmpty} from './string-utils';
import {isAnyInputElement} from '../models/elements/form/input/any-input-element';

export type FunctionStatus = 'todo' | 'done' | 'unnecessary';

export interface FunctionStatusItem {
    func: string;
    status: FunctionStatus;
}

export function getFunctionStatus(comp: AnyElement): FunctionStatusItem[] {
    const results: FunctionStatusItem[] = [];

    if (comp.visibility != null) {
        const vis = comp.visibility;

        const funcExists = (
            isStringNotNullOrEmpty(vis.javascriptCode?.code) ||
            vis.conditionSet != null ||
            vis.expression != null
        );

        if (isStringNotNullOrEmpty(vis.requirements)) {
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

    if (comp.override != null) {
        const override = comp.override;

        const funcExists = (
            isStringNotNullOrEmpty(override.javascriptCode?.code) ||
            override.expression != null
        );

        if (isStringNotNullOrEmpty(override.requirements)) {
            results.push({
                func: 'Struktur',
                status: funcExists ? 'done' : 'todo',
            });
        } else if (funcExists) {
            results.push({
                func: 'Struktur',
                status: 'unnecessary',
            });
        }
    }

    if (isAnyInputElement(comp)) {
        if (comp.validation != null) {
            const val = comp.validation;

            const funcExists = (
                isStringNotNullOrEmpty(val.javascriptCode?.code) ||
                val.conditionSet != null ||
                val.expression != null
            );

            if (isStringNotNullOrEmpty(val.requirements)) {
                results.push({
                    func: 'Validierung',
                    status: funcExists ? 'done' : 'todo',
                });
            } else if (funcExists) {
                results.push({
                    func: 'Validierung',
                    status: 'unnecessary',
                });
            }
        }

        if (comp.value != null) {
            const val = comp.value;

            const funcExists = (
                isStringNotNullOrEmpty(val.javascriptCode?.code) ||
                val.expression != null
            );

            if (isStringNotNullOrEmpty(val.requirements)) {
                results.push({
                    func: 'Wert',
                    status: funcExists ? 'done' : 'todo',
                });
            } else if (funcExists) {
                results.push({
                    func: 'Wert',
                    status: 'unnecessary',
                });
            }
        }
    }

    return results;
}