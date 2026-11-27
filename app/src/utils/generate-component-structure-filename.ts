import {ElementType} from '../data/element-type/element-type';
import {type AnyElement} from '../models/elements/any-element';
import {generateComponentTitle} from './generate-component-title';
import {isStringNotNullOrEmpty, stringOrDefault} from './string-utils';

const COMPONENT_STRUCTURE_FILE_EXTENSION = '.uielement.prosuna.json';

export function generateComponentStructureFilename(component: AnyElement): string {
    return `${resolveComponentStructureFilenameBase(component)}${COMPONENT_STRUCTURE_FILE_EXTENSION}`;
}

function resolveComponentStructureFilenameBase(component: AnyElement): string {
    const configuredTitle = resolveConfiguredComponentTitle(component);
    if (configuredTitle != null) {
        return configuredTitle;
    }

    return stringOrDefault(component.id, generateComponentTitle(component));
}

function resolveConfiguredComponentTitle(component: AnyElement): string | undefined {
    if (isStringNotNullOrEmpty(component.name)) {
        return component.name as string;
    }

    switch (component.type) {
        case ElementType.Step:
            return isStringNotNullOrEmpty(component.title) ? component.title as string : undefined;
        case ElementType.Alert:
            return isStringNotNullOrEmpty(component.title) ? component.title as string : undefined;
        case ElementType.Headline:
            return isStringNotNullOrEmpty(component.content) ? component.content as string : undefined;
        case ElementType.Image:
            return isStringNotNullOrEmpty(component.alt) ? component.alt as string : undefined;
        case ElementType.MapPoint:
            return isStringNotNullOrEmpty(component.label) ? generateComponentTitle(component) : undefined;
        default:
            break;
    }

    if ('label' in component && isStringNotNullOrEmpty(component.label)) {
        return component.label as string;
    }

    return undefined;
}
