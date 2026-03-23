import {type RootElement} from '../models/elements/root-element';
import {type AnyElement} from '../models/elements/any-element';
import {ElementType} from '../data/element-type/element-type';
import ShortUniqueId from 'short-unique-id';

const uid = new ShortUniqueId();

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
    [ElementType.FormLayout]: 'rt',
    [ElementType.Step]: 'st',
    [ElementType.Alert]: 'al',
    [ElementType.GroupLayout]: 'gp',
    [ElementType.Checkbox]: 'cx',
    [ElementType.Date]: 'dt',
    [ElementType.Headline]: 'hd',
    [ElementType.MultiCheckbox]: 'mx',
    [ElementType.Number]: 'nm',
    [ElementType.ReplicatingContainer]: 'rp',
    [ElementType.RichText]: 'rx',
    [ElementType.Radio]: 'rd',
    [ElementType.Select]: 'sl',
    [ElementType.Spacer]: 'sp',
    [ElementType.Table]: 'tb',
    [ElementType.Text]: 'tx',
    [ElementType.Time]: 'ti',
    [ElementType.IntroductionStep]: 'in',
    [ElementType.SubmitStep]: 'sb',
    [ElementType.SummaryStep]: 'sm',
    [ElementType.Image]: 'im',
    [ElementType.SubmittedStep]: 'sx',
    [ElementType.FileUpload]: 'fu',
    [ElementType.DialogLayout]: 'da',
    [ElementType.StepperLayout]: 'sr',
    [ElementType.ConfigLayout]: 'cl',
    [ElementType.FunctionInput]: 'fi',
    [ElementType.CodeInput]: 'ci',
    [ElementType.RichTextInput]: 'ri',
    [ElementType.UiDefinitionInput]: 'ui',
    [ElementType.IdentityInput]: 'ii',
    [ElementType.TabLayout]: 'tl',
};

export function generateElementIdForType(type: ElementType): string {
    return generateElementId(prefixMap[type]);
}

function generateElementId(prefix: string): string {
    const _uid = uid.rnd(10);
    return prefix + '_' + _uid;
}

export function generateId(length: number = 10) {
    return uid.rnd(length);
}