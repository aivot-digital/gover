import {ElementType} from '../data/element-type/element-type';
import {type AnyElement} from '../models/elements/any-element';
import {isAnyElementWithChildren} from '../models/elements/any-element-with-children';
import {SelectFieldPresentation} from '../models/elements/form/input/select-field-presentation';

// Reserved persisted id. It is deliberately not a member of the active ElementType enum.
const LEGACY_RADIO_TYPE = 11;

/** Normalize known element definitions, never arbitrary configuration or submitted data. */
export function normalizeElementDefinition<T extends AnyElement | null | undefined>(element: T): T {
    if (element == null) return element;
    const type: number = element.type;
    if (type === LEGACY_RADIO_TYPE) {
        return {...element, type: ElementType.Select, presentation: SelectFieldPresentation.Radio} as T;
    }

    let normalized = element;
    if (element.type === ElementType.UiDefinitionInput && Number(element.elementType) === LEGACY_RADIO_TYPE) {
        normalized = {...element, elementType: ElementType.Select};
    }
    if (isAnyElementWithChildren(element) && element.children != null) {
        const children = element.children.map(normalizeElementDefinition);
        if (children.some((child, index) => child !== element.children![index])) {
            normalized = {...normalized, children};
        }
    }
    return normalized;
}
