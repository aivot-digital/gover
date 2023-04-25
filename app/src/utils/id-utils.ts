import {RootElement} from '../models/elements/root-element';
import {AnyElement} from '../models/elements/any-element';
import {ElementType} from "../data/element-type/element-type";

const idRegex = /^[a-z][a-zA-Z0-9_]*$/;

export function checkId(root: RootElement, id?: string): string | null {
    if (id == null || id.length === 0) {
        return 'Bitte geben Sie eine ID ein.';
    }

    if (!id.match(idRegex)) {
        return 'IDs müssen mit einem Kleinbuchstaben beginnen. Sie dürfen nur aus Kleinbuchstaben, Großbuchstaben, Zahlen und Unterstrichen (_) bestehen.';
    }

    if (countIdOccurrences(root, id) > 1) {
        return 'Es existiert bereits ein Element mit dieser ID.';
    }

    return null;
}

function countIdOccurrences(comp: AnyElement, id: string): number {
    let occurrences = 0;

    if (comp.id === id) {
        occurrences++;
    }

    if ('children' in comp) {
        // @ts-ignore TODO: Fix typing here
        occurrences += comp.children.reduce((acc: number, child: AnyElement) => acc + countIdOccurrences(child, id), 0);
    }

    return occurrences;
}

export function generateElementIdForType(type?: ElementType): string {
    let prefix = 'e';
    switch (type) {
        case ElementType.Root:
            prefix = 'root';
            break;
        case ElementType.Step:
            prefix = 'step';
            break;
        case ElementType.SummaryStep:
            prefix = 'sum';
            break;
        case ElementType.IntroductionStep:
            prefix = 'general';
            break;
        case ElementType.SubmitStep:
            prefix = 'submit';
            break;
        case ElementType.Container:
            prefix = 'cont';
            break;
    }
    return generateElementId(prefix, true);
}

export function regenerateIdsForElement(elem: AnyElement): AnyElement {
    const updatedElem = {
        ...elem,
        id: generateElementIdForType(elem.type),
    };

    if ('children' in updatedElem) {
        updatedElem.children = updatedElem.children.map(child => regenerateIdsForElement(child)) as any;
    }

    return updatedElem;
}

export function generateElementId(prefix: string = 'e', addRandom?: boolean): string {
    return prefix + (new Date().getTime() * (addRandom ? Math.floor((Math.random() + 0.1) * 100) : 1)).toFixed();
}
