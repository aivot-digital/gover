import {ElementType} from '../data/element-type/element-type';
import {ElementNames} from '../data/element-type/element-names';
import {SpacerElement} from '../models/elements/form/content/spacer-element';
import {AnyElement} from "../models/elements/any-element";
import {isStringNotNullOrEmpty, stringOrDefault} from "./string-utils";


export function generateComponentTitle(component: AnyElement): string {
    if (isStringNotNullOrEmpty(component.name)) {
        return component.name!;
    }

    const defaultElementDescriptor = ElementNames[component.type];

    switch (component.type) {
        case ElementType.Root:
            return stringOrDefault(component.title, 'Unbenannter Antrag');
        case ElementType.Step:
            return stringOrDefault(component.title, 'Unbenannter Abschnitt');
        case ElementType.Alert:
            return stringOrDefault(component.title, defaultElementDescriptor);
        case ElementType.Container:
            return defaultElementDescriptor;
        case ElementType.Headline:
            return stringOrDefault(component.content, defaultElementDescriptor);
        case ElementType.Richtext:
            return defaultElementDescriptor;
        case ElementType.Image:
            return stringOrDefault(component.alt, defaultElementDescriptor);
        case ElementType.Spacer:
            const height = (component as SpacerElement).height;
            return isStringNotNullOrEmpty(height) ? `${defaultElementDescriptor} (${height}px)` : defaultElementDescriptor;
        case ElementType.Date:
        case ElementType.Table:
        case ElementType.Radio:
        case ElementType.MultiCheckbox:
        case ElementType.Checkbox:
        case ElementType.Select:
        case ElementType.Time:
        case ElementType.Number:
        case ElementType.Text:
        case ElementType.FileUpload:
        case ElementType.ReplicatingContainer:
            return stringOrDefault(component.label, defaultElementDescriptor);
        default:
            return stringOrDefault(defaultElementDescriptor, 'Unbekanntes Element');
    }


}
