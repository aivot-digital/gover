import {RootElement} from '../models/elements/root-element';
import {AnyElement} from '../models/elements/any-element';

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
        // @ts-ignore TODO
        occurrences += comp.children.reduce((acc: number, child: AnyElement) => acc + countIdOccurrences(child, id), 0);
    }

    return occurrences;
}
