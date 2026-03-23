import {AnyElement} from '../models/elements/any-element';
import {FormVersionEntity} from '../modules/forms/entities/form-version-entity';


export interface DeletedElementReference {
    deleted_element: true;
    id: string;
}

export function isDeletedElementReference(element: any): element is DeletedElementReference {
    return element != null && (element as DeletedElementReference).deleted_element;
}

export function resolveElementPath(version: FormVersionEntity, path: string): (FormVersionEntity | AnyElement | DeletedElementReference)[] {
    return _resolveElementPath(version, path.split('/').slice(1)).filter((element) => !Array.isArray(element));
}

function _resolveElementPath(currentElement: FormVersionEntity | AnyElement, path: string[]): (FormVersionEntity | AnyElement | DeletedElementReference)[] {
    if (path.length === 1) {
        return [currentElement];
    }

    const nextElement = (currentElement as any)[path[0]];

    if (!nextElement) {
        return [{
            deleted_element: true,
            id: path[0],
        }];
    }

    return [
        currentElement,
        ..._resolveElementPath(nextElement, path.slice(1)),
    ];
}