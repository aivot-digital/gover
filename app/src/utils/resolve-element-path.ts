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
    return _resolveElementPath(version, splitDiffPath(path)).filter((element) => !Array.isArray(element));
}

export function splitDiffPath(path: string): string[] {
    if (path.length === 0 || path === '/') {
        return [];
    }

    if (path.includes('/')) {
        return path.split('/').filter((segment) => segment.length > 0);
    }

    const segments: string[] = [];
    let currentSegment = '';

    for (let i = 0; i < path.length; i++) {
        const currentChar = path[i];

        if (currentChar === '.') {
            if (currentSegment.length > 0) {
                segments.push(currentSegment);
                currentSegment = '';
            }
            continue;
        }

        if (currentChar === '[') {
            if (currentSegment.length > 0) {
                segments.push(currentSegment);
                currentSegment = '';
            }

            const closingBracketIndex = path.indexOf(']', i);
            if (closingBracketIndex === -1) {
                break;
            }

            segments.push(path.slice(i + 1, closingBracketIndex));
            i = closingBracketIndex;
            continue;
        }

        currentSegment += currentChar;
    }

    if (currentSegment.length > 0) {
        segments.push(currentSegment);
    }

    return segments;
}

export function getLeafDiffPathSegment(path: string): string {
    const pathSegments = splitDiffPath(path);
    return pathSegments[pathSegments.length - 1] ?? '';
}

function _resolveElementPath(currentElement: FormVersionEntity | AnyElement, path: string[]): (FormVersionEntity | AnyElement | DeletedElementReference)[] {
    if (path.length <= 1) {
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
