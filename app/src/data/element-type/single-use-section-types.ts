import {ElementType} from './element-type';
import {getElementNameForType} from './element-names';
import {type AnyElement} from '../../models/elements/any-element';

const singleUseSectionTypes = new Set<ElementType>([
    ElementType.IntroductionStep,
    ElementType.SummaryStep,
    ElementType.SubmitStep,
]);

export function getSingleUseSectionAddDisabledReason(parentElement: AnyElement | undefined, type: ElementType): string | undefined {
    if (parentElement?.type !== ElementType.FormLayout || !singleUseSectionTypes.has(type)) {
        return undefined;
    }

    const alreadyExists = parentElement.children?.some((child) => child.type === type) === true;
    return alreadyExists ?
        `Der Abschnitt "${getElementNameForType(type)}" kann nur einmal hinzugefügt werden.` :
        undefined;
}
