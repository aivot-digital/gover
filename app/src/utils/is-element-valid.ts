import {Dispatch} from '@reduxjs/toolkit';
import {isElementVisible} from './is-element-visible';
import {BaseValidator} from '../validators/base-validator';
import {ValidatorMap} from '../components/validator.map';
import {addError} from '../slices/customer-input-errors-slice';
import {ElementType} from '../data/element-type/element-type';
import {AnyInputElement, isAnyInputElement} from '../models/elements/form/input/any-input-element';
import {AnyElement} from '../models/elements/any-element';
import {Logger} from "../hooks/use-logging";
import {generateComponentPatch} from "./generate-component-patch";
import {isAnyElementWithChildren} from "../models/elements/any-element-with-children";

export function isElementValid($debug: Logger, dispatch: Dispatch<any>, _comp: AnyElement, userInput: any, idPrefix?: string): boolean {
    const id = idPrefix != null ? (idPrefix + _comp.id) : _comp.id;

    const comp = {
        ..._comp,
        ...generateComponentPatch(id, _comp, userInput),
    }

    $debug.start(`Validating Element ${id}`);

    if (!isElementVisible(id, comp, userInput)) {
        $debug.log(`Element ${id} is not visible and needs no validation`);
        $debug.end();
        return true;
    }

    let isValid = true;

    if (isAnyInputElement(comp)) {
        const validator: BaseValidator<AnyInputElement> | null = ValidatorMap[comp.type];
        if (validator != null) {
            const error = validator.makeErrors(id, comp, userInput);
            if (error != null) {
                isValid = false;
                dispatch(addError({key: id, error}));
            }
        }
    }

    if (isAnyElementWithChildren(comp)) {
        isValid = comp
            .children
            .map((child: AnyElement) => {
                if (comp.type === ElementType.ReplicatingContainer) {
                    const values: string[] | null = userInput[id];
                    return (values ?? []).map(val =>
                        isElementValid($debug, dispatch, child, userInput, `${id}_${val}_`)
                    ).every(val => val);
                } else {
                    return isElementValid($debug, dispatch, child, userInput, idPrefix);
                }
            })
            .every(val => val);
    }

    $debug.log(`Element ${comp.id} is ${isValid ? '' : 'in'}valid`);
    $debug.end();

    return isValid;
}
