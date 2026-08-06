import {ElementType} from '../data/element-type/element-type';
import {AnyElement} from '../models/elements/any-element';
import {isAnyElementWithChildren} from '../models/elements/any-element-with-children';
import {generateComponentTitle, generateInternalComponentTitle} from './generate-component-title';
import {ElementWithParents, flattenElementsWithParents} from './flatten-elements';
import {getElementNameForType} from '../data/element-type/element-names';
import {isAnyInputElement} from '../models/elements/form/input/any-input-element';
import {isReplicatingContainerLayout} from '../models/elements/form/layout/replicating-container-layout';
import {OptionsSourceType} from '../models/elements/form/input/options-source-type';

export function createLowCodeContextType(rootElement: AnyElement) {
    const relevantElements = flattenElementsWithParents(rootElement, [], false);
    const elementStateInterfaces = relevantElements
        .map(elementToContextStateInterface)
        .join('\n\n');

    const effectiveValueFields = elementToEffectiveValueFields(rootElement);
    const elementStateFields = elementToElementStateFields(rootElement);

    return `// Element State Types
    ${elementStateInterfaces}

    // Context Type
    declare interface Context {
        effectiveValues: {
            ${effectiveValueFields.join('\n')}
        };
        elementStates: {
            ${elementStateFields.join('\n')}
        };
    }

    // Global Context Variable
    declare var ctx: Context;
    `;
}

function createElementStateInterfaceName(element: AnyElement): string {
    return `${element.id}_state`;
}

function elementToEffectiveValueFields(element: AnyElement): string[] {
    const fields = [];

    if (isAnyInputElement(element)) {
        fields.push(`/** ${generateComponentTitle(element)} */
            ${element.id}: ${elementToValueType(element)} | undefined | null;`);
    }

    if (isAnyElementWithChildren(element) && !isReplicatingContainerLayout(element)) {
        for (const child of element.children ?? []) {
            fields.push(...elementToEffectiveValueFields(child));
        }
    }

    return fields;
}

function elementToElementStateFields(element: AnyElement): string[] {
    const fields = [];

    if (isAnyInputElement(element)) {
        fields.push(`/** ${generateComponentTitle(element)} */
            ${element.id}: ${createElementStateInterfaceName(element)} | undefined;`);
    }

    if (isAnyElementWithChildren(element) && !isReplicatingContainerLayout(element)) {
        for (const child of element.children ?? []) {
            fields.push(...elementToElementStateFields(child));
        }
    }

    return fields;
}

function elementToContextStateInterface({element, parents}: ElementWithParents): string {
    return `
    /**
     * Der Runtime-State für das Element "${generateComponentTitle(element)}".
     * Das Element hat die ID "${element.id}" und den Typ "${getElementNameForType(element.type)}".
     * Der Pfad zu diesem Element lautet: ${parents.map(generateInternalComponentTitle).join(' -> ')}
     */
    declare interface ${createElementStateInterfaceName(element)} {
        /** Gibt an, ob das Element sichtbar ist. */
        visible: boolean | undefined | null;
        /** Die aktuell berechnete Fehlermeldung. */
        error: string | undefined | null;
        /** Die aktuell berechnete Überschreibung des Elementes. */
        override: Record<string, unknown> | undefined | null;
        /** Gibt an, ob der effektive Wert authored oder derived ist. */
        valueSource: 'Authored' | 'Derived' | undefined | null;
        /** Enthält bei strukturierten Listen die States der einzelnen Datensätze. */
        subStates: {id: string | undefined | null; states: Record<string, ${createElementStateInterfaceName(element)}>}[] | undefined | null;
    }`;
}

function elementToValueType(element: AnyElement): string {
    switch (element.type) {
        case ElementType.Checkbox:
            return 'boolean';
        case ElementType.Text:
        case ElementType.Date:
        case ElementType.DateTime:
        case ElementType.Time:
        case ElementType.DataModelSelect:
        case ElementType.DataObjectSelect:
        case ElementType.ProcessDataKeyInput:
        case ElementType.RichTextInput:
            return 'string';
        case ElementType.DateRange:
        case ElementType.TimeRange:
        case ElementType.DateTimeRange:
            return '{start: string | null | undefined; end: string | null | undefined}';
        case ElementType.MapPoint:
            return '{latitude: number | null | undefined; longitude: number | null | undefined; address: string | null | undefined}';
        case ElementType.Radio:
        case ElementType.Select:
            if ((element.optionsSource ?? OptionsSourceType.Manual) === OptionsSourceType.CodeList) {
                return 'string';
            }
            return element.options
                ?.map((option) => `'${typeof option === 'string' ? option : option.value}'`)
                .join(' | ') ?? 'string';
        case ElementType.Number:
            return 'number';
        case ElementType.MultiCheckbox:
        case ElementType.ChipInput:
            return 'string[]';
        case ElementType.DomainAndUserSelect:
            return '{type: \'orgUnit\' | \'team\' | \'user\'; id: string}[]';
        case ElementType.AssignmentContext:
            return '{domainAndUserSelection: {type: \'orgUnit\' | \'team\' | \'user\'; id: string}[] | null | undefined; generalAssigneePreference: \'previousProcessStepAssignee\' | \'uninvolvedUser\' | \'processInstanceAssignee\' | null | undefined; repeatExecutionAssigneePreference: \'previousIterationAssignee\' | \'differentFromPreviousIterationAssignee\' | null | undefined}';
        case ElementType.NoCodeInput:
            return '{noCode: Record<string, unknown> | null}';
        case ElementType.UiDefinitionInput:
            return 'Record<string, unknown>';
        case ElementType.HtmlTemplateInput:
            return '{assetKey: string | null; slots: Record<string, string | null>}';
        case ElementType.StoragePathSelector:
            return '{storageProviderId: number | null | undefined; path: string | null | undefined}';
        case ElementType.IdentityConfigElement:
            return '{identityProviderKey: string | null | undefined; identityAttributes: Record<string, unknown> | null | undefined}';
        case ElementType.PaymentConfigElement:
            return 'Record<string, unknown>';
        case ElementType.FileUpload:
            return '{name: string; uri: string; size: number;}[]';
        case ElementType.IntroductionStep:
        case ElementType.SummaryStep:
            return 'boolean';
        case ElementType.SubmitStep:
            return 'object';
        case ElementType.Table:
            return `{${element.fields?.map((field) => `${field.key}: string | number | null | undefined`).join('; ')}}[]`;
        case ElementType.ReplicatingContainer:
            return '{id: string | null | undefined; values: Record<string, unknown> | null | undefined}[]';
        default:
            return 'never';
    }
}
