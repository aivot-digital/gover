import {type ProcessNodeEntity} from '../entities/process-node-entity';
import {type GroupLayout} from '../../../models/elements/form/layout/group-layout';
import {isFormLayoutElement} from '../../../models/elements/form-layout-element';
import {keyById, type MergeConflictPresentation, type MergeSpec} from '../../../utils/three-way-merge';
import {variableReferenceMergeRules} from '../../../utils/variable-reference-merge';
import {flattenElements} from '../../../utils/flatten-elements';
import {getLiteralAuthoredValue} from '../../../models/element-data';
import {
    describeElementConflict,
    formatElementConflictLabel,
    hasUniqueElementIds,
} from '../../elements/utils/form-layout-merge-spec';
import {describeConfigurationProperty} from '../../elements/utils/element-conflict-labels';
import {isElementTree, mergeElementTreeStructure} from '../../elements/utils/element-tree-structure-merge';
import {isUiDefinitionInputFieldElement} from '../../../models/elements/form/input/ui-definition-input-field-element';

const FieldLabels: Record<string, string> = {
    name: 'Name des Elementes',
    description: 'Kurzbeschreibung des Elements',
    dataKey: 'Datenschlüssel des Prozesselements',
    outputMappings: 'Zielpfad in den Vorgangsdaten',
    deadlines: 'Frist',
    requirements: 'Fachliche Anforderungen',
    notes: 'Notizen',
    configuration: 'Konfiguration',
};

export function getExternallyEditedFields(layout: GroupLayout | null): string[] {
    return (layout == null ? [] : flattenElements(layout))
        .filter((element) => isUiDefinitionInputFieldElement(element) && element.openExternalEditor)
        .map((element) => element.id);
}

/**
 * Merges a process node edited in the node editor with a newer stored state.
 * The configuration layout resolves configuration keys, which are element IDs, to their field labels.
 */
export function createProcessNodeMergeSpec(layout: GroupLayout | null, stored?: ProcessNodeEntity): MergeSpec<ProcessNodeEntity> {
    const layoutElements = layout == null ? [] : flattenElements(layout);
    const externalFields = getExternallyEditedFields(layout);
    const inlineDefinitions = new Set(layoutElements.filter(isUiDefinitionInputFieldElement)
        .filter((field) => !field.openExternalEditor).map((field) => field.id));
    const describeConflict: NonNullable<MergeSpec<ProcessNodeEntity>['describeConflict']> = (path, {base, mine, theirs}) => {
        const [field, configurationKey, ...rest] = path;
        const nodePrefix = ['process-node', mine.id];

        if (field !== 'configuration' || configurationKey == null) {
            let propertyLabel = field == null ? 'Gesamte Konfiguration des Prozesselements' : FieldLabels[field] ?? field;
            if (field === 'deadlines' && configurationKey != null) {
                const definition = [theirs, mine, base].flatMap((node) => node.deadlineDefinitions).find((candidate) => candidate.key === configurationKey);
                const detailLabels: Record<string, string> = {days: 'Frist', strict: 'Aufgabe bei Fristüberschreitung beenden'};
                propertyLabel = [propertyLabel, definition?.label ?? configurationKey, ...rest.map((key) => detailLabels[key] ?? key)].join(' › ');
            } else if (path.length > 1) {
                // Output mapping keys and extensions are authored identifiers, not property names.
                propertyLabel = [propertyLabel, ...path.slice(1)].join(' › ');
            }
            return {group: {id: JSON.stringify([...nodePrefix, 'general']), label: 'Allgemeine Angaben'}, propertyLabel};
        }

        const fieldElement = layoutElements.find((element) => element.id === configurationKey);
        const fieldLabel = (fieldElement as {label?: string | null} | undefined)?.label?.trim() || configurationKey;
        const groupPrefix = [...nodePrefix, 'configuration', configurationKey];
        const values = [mine, theirs, base].map((node) => node.configuration?.[configurationKey]);
        const trees = values.map((value) => getLiteralAuthoredValue(value));

        // A configuration field can contain an entire form. Its root belongs to the field's own group.
        if (rest[0] === 'value' && trees.some(isElementTree)) {
            if (rest.length === 1 && !trees.some(isFormLayoutElement)) {
                return {group: {id: JSON.stringify(groupPrefix), label: fieldLabel}, propertyLabel: 'Gesamte UI-Definition'};
            }
            return describeElementConflict(rest.slice(1), trees, fieldLabel, groupPrefix);
        }

        return {
            group: {id: JSON.stringify(groupPrefix), label: fieldLabel},
            propertyLabel: describeConfigurationProperty(rest, values),
        };
    };

    return {
        ...variableReferenceMergeRules,
        // The server derives these on every save; the lock version must be the stored one, so the save passes its check.
        serverManaged: [
            'lockVersion',
            'created',
            'updated',
            'savedWithErrors',
            'ports',
            'portResolutionProblems',
            'deadlineDefinitions',
        ],
        // These keys are written by a separate editor and omitted by the node editor's PUT.
        normalize: stored == null || externalFields.length === 0 ? undefined : (node) => {
            const configuration = {...node.configuration};
            for (const key of externalFields) {
                if (Object.hasOwn(stored.configuration, key)) {
                    configuration[key] = stored.configuration[key];
                } else {
                    delete configuration[key];
                }
            }
            return {...node, configuration};
        },
        arrayKey: keyById,
        mergeSubtree: (input) => {
            const [field, key, property] = input.path;
            if (input.path.length === 3 && field === 'configuration' && property === 'value' && inlineDefinitions.has(key)) {
                return mergeElementTreeStructure(input);
            }
        },
        validate: (node) => Object.values(node.configuration ?? {})
            .map((value) => getLiteralAuthoredValue(value))
            .filter(isElementTree)
            .every(hasUniqueElementIds),
        describeConflict,
        label: (path, states) => {
            const presentation: MergeConflictPresentation = describeConflict(path, states);
            if (path[0] !== 'configuration' || path[1] == null) return presentation.propertyLabel;
            return presentation.propertyLabel === 'Wert' ? presentation.group.label : formatElementConflictLabel(presentation);
        },
    };
}
