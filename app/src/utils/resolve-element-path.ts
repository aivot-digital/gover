import {AnyElement} from '../models/elements/any-element';
import {Form} from '../models/entities/form';


export function resolveElementPath(form: Form, path: string): (Form | AnyElement | 'deleted_element')[] {
    return _resolveElementPath(form, path.split('/').slice(1)).filter((element) => !Array.isArray(element));
}

function _resolveElementPath(currentElement: Form | AnyElement, path: string[]): (Form | AnyElement | 'deleted_element')[] {
    if (path.length === 1) {
        return [currentElement];
    }

    const nextElement = (currentElement as any)[path[0]];

    if (!nextElement) {
        return ['deleted_element'];
    }

    return [
        currentElement,
        ..._resolveElementPath(nextElement, path.slice(1)),
    ];
}