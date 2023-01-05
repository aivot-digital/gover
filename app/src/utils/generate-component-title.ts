import {ElementType} from '../data/element-type/element-type';
import {RootElement} from '../models/elements/root-element';
import {StepElement} from '../models/elements/step-elements/step-element';
import {HeadlineElement} from '../models/elements/form-elements/content-elements/headline-element';
import {ElementNames} from '../data/element-type/element-names';
import {AlertElement} from '../models/elements/form-elements/content-elements/alert-element';
import {SpacerElement} from '../models/elements/form-elements/content-elements/spacer-element';
import {stringOrDefault} from './string-or-default';
import {
    ReplicatingContainerElement
} from '../models/elements/form-elements/layout-elements/replicating-container-element';
import {isNullOrEmpty} from './is-null-or-empty';
import {AnyElement} from '../models/elements/any-element';
import {AnyInputElement} from '../models/elements/form-elements/input-elements/any-input-element';

export function generateComponentTitle(component: AnyElement) {
    const elementName = ElementNames[component.type];
    switch (component.type) {
        case ElementType.Root:
            return stringOrDefault((component as RootElement).title, 'Unbenannter Antrag');
        case ElementType.Step:
            return stringOrDefault((component as StepElement).title, 'Unbenannter Abschnitt');
        case ElementType.Alert:
            return stringOrDefault((component as AlertElement).title, elementName);
        case ElementType.Container:
            return ElementNames[component.type];
        case ElementType.Headline:
            return stringOrDefault((component as HeadlineElement).content, elementName);
        case ElementType.ReplicatingContainer:
            return stringOrDefault((component as ReplicatingContainerElement).label, elementName);
        case ElementType.Richtext:
            return ElementNames[component.type];
        case ElementType.Image:
            return component.alt;
        case ElementType.Spacer:
            const height = (component as SpacerElement).height;
            return isNullOrEmpty(height) ? `${elementName} (${height}px)` : elementName;
        case ElementType.Date:
        case ElementType.Table:
        case ElementType.Radio:
        case ElementType.MultiCheckbox:
        case ElementType.Checkbox:
        case ElementType.Select:
        case ElementType.Time:
        case ElementType.Number:
        case ElementType.Text:
            return stringOrDefault((component as AnyInputElement).label, elementName);
        default:
            return stringOrDefault(elementName, 'Unbekanntes Element');
    }
}
