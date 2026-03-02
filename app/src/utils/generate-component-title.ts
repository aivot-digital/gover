import {ElementType} from '../data/element-type/element-type';
import {getElementNameForType} from '../data/element-type/element-names';
import {type AnyElement} from '../models/elements/any-element';
import {isStringNotNullOrEmpty, stringOrDefault} from './string-utils';


export function generateComponentTitle(component: AnyElement | null | undefined): string {
    if (component == null) {
        return '';
    }

    if (component.name != null && isStringNotNullOrEmpty(component.name)) {
        return component.name;
    }

    const defaultElementDescriptor = getElementNameForType(component.type);

    switch (component.type) {
        case ElementType.FormLayout:
            return 'Formular';
        case ElementType.Step:
            return stringOrDefault(component.title, 'Unbenannter Abschnitt');
        case ElementType.Alert:
            return stringOrDefault(component.title, defaultElementDescriptor);
        case ElementType.GroupLayout:
            return defaultElementDescriptor;
        case ElementType.Headline:
            return stringOrDefault(component.content, defaultElementDescriptor);
        case ElementType.RichText:
            return defaultElementDescriptor;
        case ElementType.Image:
            return stringOrDefault(component.alt, defaultElementDescriptor);
        case ElementType.Spacer:
            const height = component.height;
            return height != null && isStringNotNullOrEmpty(height) ? `${defaultElementDescriptor} (${height}px)` : defaultElementDescriptor;
        case ElementType.Date:
        case ElementType.DateTime:
        case ElementType.DateRange:
        case ElementType.TimeRange:
        case ElementType.DateTimeRange:
        case ElementType.Table:
        case ElementType.Radio:
        case ElementType.MultiCheckbox:
        case ElementType.Checkbox:
        case ElementType.Select:
        case ElementType.Time:
        case ElementType.Number:
        case ElementType.Text:
        case ElementType.FileUpload:
        case ElementType.ChipInput:
        case ElementType.MapPoint:
        case ElementType.ReplicatingContainer:
            return stringOrDefault(component.label, defaultElementDescriptor);
        default:
            return stringOrDefault(defaultElementDescriptor, 'Unbekanntes Element');
    }
}

export function generateComponentPath(components: AnyElement[]): string {
    return components
        .map((c) => generateComponentTitle(c))
        .join(' › ');
}
