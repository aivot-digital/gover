import {ElementType} from '../data/element-type/element-type';
import {AnyElement} from '../models/elements/any-element';
import {isAnyElementWithChildren} from '../models/elements/any-element-with-children';
import {isRootElement} from '../models/elements/root-element';
import {generateComponentTitle} from './generate-component-title';
import {ElementWithParents, flattenElementsWithParents} from './flatten-elements';
import {getElementNameForType} from '../data/element-type/element-names';
import {isAnyInputElement} from '../models/elements/form/input/any-input-element';
import {isReplicatingContainerLayout} from '../models/elements/form/layout/replicating-container-layout';


export function createLowCodeContextType(rootElement: AnyElement) {
    // Create element data object interfaces for all elements in the root element.
    const relevantElements = flattenElementsWithParents(rootElement, [], false);

    if (isRootElement(rootElement)) {
        if (rootElement.introductionStep != null) {
            relevantElements.push({
                element: rootElement.introductionStep,
                parents: [rootElement],
            });
        }
        if (rootElement.summaryStep != null) {
            relevantElements.push({
                element: rootElement.summaryStep,
                parents: [rootElement],
            });
        }
        if (rootElement.submitStep != null) {
            relevantElements.push({
                element: rootElement.submitStep,
                parents: [rootElement],
            });
        }
    }

    const elementDataObjectInterfaces = relevantElements
        .map(elementToContextDataObjectInterface)
        .join('\n\n');


    const types = elementToElementDataType(rootElement);

    const typeDef = `// Element Types
    ${elementDataObjectInterfaces}
    
    // Context Type
    declare interface Context {
        ${types.join('\n')}
    }
    
    // Global Context Variable
    declare var ctx: Context;
    `;

    return typeDef;
}

function createElementDataObjectInterfaceName(element: AnyElement): string {
    return `${element.id}_edo`;
}

function elementToElementDataType(element: AnyElement): string[] {
    const fields = [];

    if (isAnyInputElement(element)) {
        fields.push(`/** ${generateComponentTitle(element)}*/
        ${element.id}: ${element.id}_edo;`);
    }

    if (isAnyElementWithChildren(element) && !isReplicatingContainerLayout(element)) {
        for (const child of element.children ?? []) {
            fields.push(
                ...elementToElementDataType(child),
            );
        }
    }

    return fields;
}

function elementToContextDataObjectInterface({element, parents}: ElementWithParents): string {
    const valueType = elementToValueType(element);
    const elementType = elementToTypeDefinition(element);

    return `
    /**
     * Der Datensatz für das Element "${generateComponentTitle(element)}".
     * Das Element hat die ID "${element.id}" und den Typ "${getElementNameForType(element.type)}".
     * Der Pfad zu diesem Element lauter: ${parents.map(generateComponentTitle).join(' -> ')}
     */
    declare interface ${createElementDataObjectInterfaceName(element)} {
        /** Der Typ-Identifikator des Elementes. In diesem Fall ${getElementNameForType(element.type)} */
        $type: ${element.type};
        /** Der eingegebene Wert des Elementes. */
        inputValue: ${valueType} | undefined | null;
        /** Gibt an, ob das Element sichtbar ist. */
        isVisible: boolean | undefined | null;
        /** Gibt an, ob das Element durch ein Nutzerkonto vorausgefüllt ist. */
        isPrefilled: boolean | undefined | null;
        /** Gibt an, ob das Element durch eine Nutzer:in verändert wurde. */
        isDirty: boolean | undefined | null;
        /** Die berechnete Überschreibung des Elementes. */
        computedOverride: ${elementType} | undefined | null;
        /** Der berechnete Wert des Elementes. */
        computedValue: ${valueType} | undefined | null;
        /** Die berechneten Fehler des Elementes. */
        computedErrors: string[] | undefined | null;
    }`;
}

function elementToValueType(element: AnyElement): string {
    switch (element.type) {
        case ElementType.Checkbox:
            return 'boolean';
        case ElementType.Text:
        case ElementType.Date:
        case ElementType.Time:
            return 'string';
        case ElementType.Radio:
        case ElementType.Select:
            return element.options?.map(option => `'${option.value}'`).join(' | ') ?? 'string';
        case ElementType.Number:
            return 'number';
        case ElementType.MultiCheckbox:
            return 'string[]';
        case ElementType.FileUpload:
            return '{name: string; uri: string; size: number;}[]';
        case ElementType.IntroductionStep:
        case ElementType.SummaryStep:
            return 'boolean';
        case ElementType.SubmitStep:
            return 'object';
        case ElementType.Table:
            return `{${element.fields?.map(field => `${field.key}: string | number | null | undefined`).join('; ')}}[]`;
        case ElementType.ReplicatingContainer:
            const children = (element.children ?? []).map(element => `${element.id}: ${createElementDataObjectInterfaceName(element)};`).join('\n');
            return `{${children}}[]`;
        default:
            return 'never';
    }
}

function elementToTypeDefinition(element: AnyElement): string {
    const lines = ['{'];
    for (const e of Object.keys(element)) {
        lines.push(`        ${e}: ${typeof [element.type]};`);
    }
    lines.push('    }');
    return lines.join('\n');
}