import {type RootElement} from '../models/elements/root-element';
import {type AnyElement} from '../models/elements/any-element';
import {ElementType} from '../data/element-type/element-type';

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

const prefixMap: Record<ElementType, string> = {
    [ElementType.Root]: 'root',
    [ElementType.Step]: 'step',
    [ElementType.Alert]: 'alrt',
    [ElementType.Container]: 'grup',
    [ElementType.Checkbox]: 'ckbx',
    [ElementType.Date]: 'date',
    [ElementType.Headline]: 'hdln',
    [ElementType.MultiCheckbox]: 'mucx',
    [ElementType.Number]: 'numb',
    [ElementType.ReplicatingContainer]: 'repc',
    [ElementType.Richtext]: 'ritx',
    [ElementType.Radio]: 'radi',
    [ElementType.Select]: 'selc',
    [ElementType.Spacer]: 'spac',
    [ElementType.Table]: 'tabl',
    [ElementType.Text]: 'text',
    [ElementType.Time]: 'time',
    [ElementType.IntroductionStep]: 'intr',
    [ElementType.SubmitStep]: 'subm',
    [ElementType.SummaryStep]: 'summ',
    [ElementType.Image]: 'imag',
    [ElementType.SubmittedStep]: 'subx',
    [ElementType.FileUpload]: 'fupl',
};

export function generateElementIdForType(type: ElementType): string {
    return generateElementId(prefixMap[type]);
}

export function generateElementIdForReplicatingContainerChild(): string {
    return generateElementId(prefixMap[ElementType.ReplicatingContainer] + '_c');
}

function generateElementId(prefix: string): string {
    return prefix + '_' + new Date().getTime().toFixed() + Math.floor((Math.random() + 0.1) * 1000).toFixed(0);
}

export function makeId(element: AnyElement, idPrefix?: string | null): string {
    return idPrefix != null ? (idPrefix + element.id) : element.id;
}

export function resolveId(id: string, idPrefix?: string | null): string {
    return idPrefix != null ? (idPrefix + id) : id;
}
