import {ElementType} from '../data/element-type/element-type';
import {getElementNameForType} from '../data/element-type/element-names';
import {type AnyElement} from '../models/elements/any-element';
import {isStringNotNullOrEmpty, stringOrDefault} from './string-utils';

export function generateInternalComponentTitle(component: AnyElement | null | undefined): string {
    return generateComponentTitle(component, false);
}

export function generateComponentTitle(component: AnyElement | null | undefined, publicFacing: boolean = false): string {
    if (component == null) {
        return '';
    }

    if (!publicFacing && component.name != null && isStringNotNullOrEmpty(component.name)) {
        return component.name;
    }

    const defaultElementDescriptor = getElementNameForType(component.type);
    const mapPointPreviewSuffix = ' (Technische Preview)';

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
        case ElementType.DomainAndUserSelect:
        case ElementType.AssignmentContext:
        case ElementType.DataModelSelect:
        case ElementType.DataObjectSelect:
        case ElementType.ProcessDataKeyInput:
        case ElementType.SecretSelectInput:
        case ElementType.NoCodeInput:
        case ElementType.UiDefinitionInput:
        case ElementType.HtmlTemplateInput:
        case ElementType.StoragePathSelector:
        case ElementType.IdentityConfigElement:
        case ElementType.PaymentConfigElement:
        case ElementType.RichTextInput:
        case ElementType.ReplicatingContainer:
            return stringOrDefault(component.label, defaultElementDescriptor);
        case ElementType.MapPoint: {
            const title = stringOrDefault(component.label, defaultElementDescriptor);
            return title.toLowerCase().includes('technische preview') ? title : `${title}${mapPointPreviewSuffix}`;
        }
        case ElementType.ProcessAttachmentDisplay:
        case ElementType.LinkButton:
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
