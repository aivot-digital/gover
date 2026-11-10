import {type ProcessNodeEntity} from '../entities/process-node-entity';
import {type GroupLayout} from '../../../models/elements/form/layout/group-layout';
import {keyById, type MergeSpec} from '../../../utils/three-way-merge';
import {flattenElements} from '../../../utils/flatten-elements';
import {getLiteralAuthoredValue} from '../../../models/element-data';
import {
    describeElementPath,
    hasUniqueElementIds,
    isElementTree,
} from '../../elements/utils/form-layout-merge-spec';

const FieldLabels: Record<string, string> = {
    name: 'Name des Elementes',
    description: 'Kurzbeschreibung des Elements',
    dataKey: 'Datenschlüssel des Prozesselements',
    outputMappings: 'Zielpfad in den Vorgangsdaten',
    deadlines: 'Frist',
    requirements: 'Fachliche Anforderungen',
    notes: 'Notizen',
};

/**
 * Merges a process node edited in the node editor with a newer stored state.
 * The configuration layout resolves configuration keys, which are element IDs, to their field labels.
 */
export function createProcessNodeMergeSpec(layout: GroupLayout | null): MergeSpec<ProcessNodeEntity> {
    const layoutElements = layout == null ? [] : flattenElements(layout);

    return {
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
        arrayKey: keyById,
        validate: (node) => Object.values(node.configuration ?? {})
            .map((value) => getLiteralAuthoredValue(value))
            .filter(isElementTree)
            .every(hasUniqueElementIds),
        label: (path, {base, mine, theirs}) => {
            const [field, configurationKey, ...rest] = path;

            if (field == null) {
                return 'Gesamte Konfiguration des Prozesselements';
            }

            if (field !== 'configuration' || configurationKey == null) {
                return FieldLabels[field] ?? field;
            }

            const fieldElement = layoutElements.find((element) => element.id === configurationKey);
            const fieldLabel = (fieldElement as {label?: string | null} | undefined)?.label ?? configurationKey;
            if (rest[0] !== 'value' || rest.length === 1) {
                return fieldLabel;
            }

            // A field can hold a whole form, e.g. the form layout of a form trigger.
            const trees = [mine, theirs, base].map((node) => getLiteralAuthoredValue(node.configuration?.[configurationKey]));
            return describeElementPath(rest.slice(1), trees, fieldLabel);
        },
    };
}
