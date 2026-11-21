import {AnyElement} from '../../../models/elements/any-element';
import {ElementType} from '../../../data/element-type/element-type';
import {isAnyInputElement} from '../../../models/elements/form/input/any-input-element';
import {flattenElementsWithParents} from '../../../utils/flatten-elements';

export function noCodeEditorElements(parents: AnyElement[]) {
    const scope = [...parents].reverse().find((element) => element.type === ElementType.ReplicatingContainer) ?? parents[0];
    if (scope == null) return [];
    return flattenElementsWithParents(scope, [], true)
        .filter(({element}) => isAnyInputElement(element) &&
            (scope.type !== ElementType.ReplicatingContainer || element.id !== scope.id));
}
