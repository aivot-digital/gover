import {Dispatch} from '@reduxjs/toolkit';
import {isComponentVisible} from './is-component-visible';
import {BaseValidator} from '../validators/base-validator';
import {ValidatorMap} from '../components/validator.map';
import {addError} from '../slices/customer-input-errors-slice';
import {ElementType} from '../data/element-type/element-type';
import {AnyInputElement} from '../models/elements/form-elements/input-elements/any-input-element';
import {AnyElement} from '../models/elements/any-element';
import {isLayoutElement} from '../models/elements/form-elements/layout-elements/base-layout-element';
import {isInputElement} from '../models/elements/form-elements/input-elements/base-input-element';

export function isComponentValid(dispatch: Dispatch<any>, comp: AnyElement, userInput: any, idPrefix?: string): boolean {
    const id = idPrefix != null ? (idPrefix + comp.id) : comp.id;

    if (!isComponentVisible(id, comp, userInput)) {
        return true;
    }

    let isValid = true;

    if (isInputElement(comp)) {
        const validator: BaseValidator<AnyInputElement> | null = ValidatorMap[comp.type];
        if (validator != null) {
            const error = validator.makeErrors(id, comp, userInput);
            if (error != null) {
                isValid = false;
                dispatch(addError({key: id, error}));
            }
        }
    }

    if (isLayoutElement(comp)) {
        isValid = comp
            .children
            .map((child: AnyElement) => {
                if (comp.type === ElementType.ReplicatingContainer) {
                    const values: string[] | null = userInput[id];
                    return (values ?? []).map(val =>
                        isComponentValid(dispatch, child, userInput, `${comp.id}_${val}_`)
                    ).every(val => val);
                } else {
                    return isComponentValid(dispatch, child, userInput, idPrefix);
                }
            })
            .every(val => val);
    }

    if (!isValid) {
        console.debug(`Element ${comp.id} is not valid`);
    }

    return isValid;
}
