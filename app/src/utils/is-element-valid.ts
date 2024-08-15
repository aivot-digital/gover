import {type Dispatch} from '@reduxjs/toolkit';
import {isElementVisible} from './is-element-visible';
import {type BaseValidator} from '../validators/base-validator';
import {ElementType} from '../data/element-type/element-type';
import {type AnyInputElement, isAnyInputElement} from '../models/elements/form/input/any-input-element';
import {type AnyElement} from '../models/elements/any-element';
import {type Logger} from '../hooks/use-logging';
import {generateComponentPatch} from './generate-component-patch';
import {isAnyElementWithChildren} from '../models/elements/any-element-with-children';
import Validators from '../validators';
import {addError} from "../slices/app-slice";

export function isElementValid(
    $debug: Logger,
    idPrefix: string | undefined,
    allElements: AnyElement[],
    dispatch: Dispatch<any>,
    _comp: AnyElement,
    userInput: any,
): boolean {
    const id = idPrefix != null ? (idPrefix + _comp.id) : _comp.id;

    const comp = {
        ..._comp,
        ...generateComponentPatch(idPrefix, allElements, _comp.id, _comp, userInput),
    };

    $debug.start(`Validating Element ${id}`);

    if (!isElementVisible(idPrefix, allElements, _comp.id, comp, userInput)) {
        $debug.log(`Element ${id} is not visible and needs no validation`);
        $debug.end();
        return true;
    }

    let isValid = true;

    if (isAnyInputElement(comp)) {
        const validator: BaseValidator<AnyInputElement> | null = Validators[comp.type];
        if (validator != null) {
            if (idPrefix != null) {
                // idPrefix
            }
            const error = validator.makeErrors(allElements, idPrefix, _comp.id, comp, userInput);
            if (error != null) {
                isValid = false;
                dispatch(addError({
                    key: id,
                    error: error,
                }));
            }
        }
    }

    if (isAnyElementWithChildren(comp)) {
        isValid = isValid && comp
            .children
            .map((child: AnyElement) => {
                if (comp.type === ElementType.ReplicatingContainer) {
                    const values: string[] | null = userInput[id];

                    return (values ?? []).map((val) =>
                        isElementValid($debug, `${id}_${val}_`, allElements, dispatch, child, userInput),
                    ).every((val) => val);
                } else {
                    return isElementValid($debug, idPrefix, allElements, dispatch, child, userInput);
                }
            })
            .every((val) => val);
    }

    $debug.log(`Element ${comp.id} is ${isValid ? '' : 'in'}valid`);
    $debug.end();

    return isValid;
}
