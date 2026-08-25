import {ElementChildOptions, type ElementDisplayContext} from '../../data/element-type/element-child-options';
import {ElementType} from '../../data/element-type/element-type';
import {getElementNameForType} from '../../data/element-type/element-names';
import {getSingleUseSectionAddDisabledReason} from '../../data/element-type/single-use-section-types';
import {type AnyElement} from '../../models/elements/any-element';
import {isGroupLayout} from '../../models/elements/form/layout/group-layout';
import {
    type ProcessNodeDefinitionMetadataReusableUiDefinition,
    ProcessNodeDefinitionMetadataReusableUiDefinitionKind,
} from '../../modules/process/entities/process-node-definition-metadata';
import {cloneElement} from '../../utils/clone-element';

export interface ReusableUiDefinitionTarget {
    parentType: ElementType;
    parentElement?: AnyElement;
    allParents: AnyElement[];
    limitElementTypes?: ElementType[];
    displayContext: ElementDisplayContext;
}

export interface ReusableUiDefinitionOption {
    definition: ProcessNodeDefinitionMetadataReusableUiDefinition;
    partial: boolean;
    groupDisabledReason?: string;
    flatDisabledReason?: string;
}

export type ReusableUiDefinitionImportMode = 'complete' | 'group' | 'flat';

const partialKinds = new Set<ProcessNodeDefinitionMetadataReusableUiDefinitionKind>([
    ProcessNodeDefinitionMetadataReusableUiDefinitionKind.FormSection,
    ProcessNodeDefinitionMetadataReusableUiDefinitionKind.StepperSection,
    ProcessNodeDefinitionMetadataReusableUiDefinitionKind.Tab,
]);

export function resolveAllowedChildTypes(target: ReusableUiDefinitionTarget): Set<ElementType> {
    let childOptionSet: Set<ElementType> | null = null;

    if (target.allParents.length > 1) {
        if (target.allParents[0].type === ElementType.ConfigLayout) {
            childOptionSet = new Set<ElementType>(
                ElementChildOptions[target.displayContext][ElementType.ConfigLayout] ?? [],
            );
        } else {
            for (const parent of target.allParents.slice(1)) {
                const allowedTypes = ElementChildOptions[target.displayContext][parent.type] ?? [];
                const allowedTypeSet = new Set<ElementType>(allowedTypes);

                if (childOptionSet == null) {
                    childOptionSet = allowedTypeSet;
                } else {
                    childOptionSet = childOptionSet.intersection(allowedTypeSet);
                }
            }
        }
    } else {
        childOptionSet = new Set<ElementType>(
            ElementChildOptions[target.displayContext][target.parentType] ?? [],
        );
    }

    return childOptionSet ?? new Set<ElementType>();
}

export function createReusableUiDefinitionOptions(
    definitions: ProcessNodeDefinitionMetadataReusableUiDefinition[],
    target: ReusableUiDefinitionTarget,
): ReusableUiDefinitionOption[] {
    const allowedChildTypes = resolveAllowedChildTypes(target);

    return definitions.flatMap<ReusableUiDefinitionOption>((definition) => {
        const partial = partialKinds.has(definition.kind);
        const groupDisabledReason = getElementInsertionDisabledReason(
            definition.uiDefinition,
            target,
            allowedChildTypes,
        );

        if (!partial) {
            return groupDisabledReason == null ? [{
                definition,
                partial: false,
            }] : [];
        }

        const flatDisabledReason = getFlatInsertionDisabledReason(
            definition.uiDefinition,
            target,
            allowedChildTypes,
        );

        if (groupDisabledReason != null && flatDisabledReason != null) {
            return [];
        }

        return [{
            definition,
            partial: true,
            groupDisabledReason,
            flatDisabledReason,
        }];
    });
}

export function cloneReusableUiDefinitionForImport(
    option: ReusableUiDefinitionOption,
    mode: ReusableUiDefinitionImportMode,
): AnyElement[] {
    const clonedDefinition = cloneElement(option.definition.uiDefinition, true);

    if (mode !== 'flat') {
        return [clonedDefinition];
    }

    return isGroupLayout(clonedDefinition) ? clonedDefinition.children : [];
}

function getFlatInsertionDisabledReason(
    definition: AnyElement,
    target: ReusableUiDefinitionTarget,
    allowedChildTypes: Set<ElementType>,
): string | undefined {
    if (!isGroupLayout(definition)) {
        return 'Diese Definition besitzt keinen unterstützten Gruppen-Wrapper.';
    }

    if (definition.children.length === 0) {
        return 'Dieser Teilbereich enthält keine Elemente.';
    }

    for (const child of definition.children) {
        const disabledReason = getElementInsertionDisabledReason(child, target, allowedChildTypes);
        if (disabledReason != null) {
            return `Das enthaltene Element „${getElementNameForType(child.type)}“ kann hier nicht flach eingefügt werden. ${disabledReason}`;
        }
    }

    return undefined;
}

function getElementInsertionDisabledReason(
    element: AnyElement,
    target: ReusableUiDefinitionTarget,
    allowedChildTypes: Set<ElementType>,
): string | undefined {
    if (!allowedChildTypes.has(element.type)) {
        return `Der Elementtyp „${getElementNameForType(element.type)}“ ist an dieser Stelle nicht zulässig.`;
    }

    if (target.limitElementTypes != null && !target.limitElementTypes.includes(element.type)) {
        return `Der Elementtyp „${getElementNameForType(element.type)}“ ist für diese Auswahl nicht freigegeben.`;
    }

    return getSingleUseSectionAddDisabledReason(target.parentElement, element.type);
}
