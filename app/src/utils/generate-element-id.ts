import {ElementType} from '../data/element-type/element-type';

export function generateElementId(prefix: string = 'e', addRandom?: boolean): string {
    return prefix + (new Date().getTime() * (addRandom ? Math.floor((Math.random() + 0.1) * 100) : 1)).toFixed();
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
