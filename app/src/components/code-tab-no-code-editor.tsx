import {type AnyElement} from '../models/elements/any-element';
import {flattenElements, flattenElementsWithParents} from '../utils/flatten-elements';
import {CodeTabConditionSetEditor} from './code-tab-condition-set-editor';
import React from 'react';
import {ConditionSetOperator} from '../data/condition-set-operator';
import Evaluators from '../evaluators';
import {type Function} from '../models/functions/function';
import {type RootElement} from '../models/elements/root-element';
import {type StepElement} from '../models/elements/steps/step-element';
import {type GroupLayout} from '../models/elements/form/layout/group-layout';
import {ElementType} from '../data/element-type/element-type';
import {type ConditionSet} from '../models/functions/conditions/condition-set';
import {type ReplicatingContainerLayout} from '../models/elements/form/layout/replicating-container-layout';

interface CodeTabNoCodeEditorProps {
    parents: Array<RootElement | StepElement | GroupLayout | ReplicatingContainerLayout>;
    element: AnyElement;
    func: Function;
    onChange: (func: Function) => void;
    shouldReturnString: boolean;
    editable: boolean;
}

export function CodeTabNoCodeEditor({
                                        parents,
                                        element,
                                        func,
                                        onChange,
                                        shouldReturnString,
                                        editable,
                                    }: CodeTabNoCodeEditorProps): JSX.Element {
    let parent: AnyElement | null = null;
    for (const par of [...parents].reverse()) {
        if (par.type === ElementType.ReplicatingContainer) {
            parent = par;
            break;
        }
    }
    if (parent == null) {
        parent = parents[0];
    }

    const allElements = (parent != null ? flattenElementsWithParents(parent, [], true) : [])
        .filter((e) => Evaluators[e.element.type] != null);

    const handleChange = (cs: ConditionSet) => {
        if ((cs.conditions == null || cs.conditions.length === 0) && (cs.conditionsSets == null || cs.conditionsSets.length === 0)) {
            onChange({
                ...func,
                conditionSet: undefined,
            });
        } else {
            onChange({
                ...func,
                conditionSet: cs,
            });
        }
    };

    return (
        <CodeTabConditionSetEditor
            element={element}
            allElements={allElements}
            conditionSet={func.conditionSet ?? {
                operator: ConditionSetOperator.Any,
            }}
            onChange={handleChange}
            shouldReturnString={shouldReturnString}
            editable={editable}
        />
    );
}
