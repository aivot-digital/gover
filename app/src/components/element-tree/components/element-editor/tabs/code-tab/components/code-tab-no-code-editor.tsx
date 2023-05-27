import {AnyElement} from "../../../../../../../models/elements/any-element";
import {flattenElements} from "../../../../../../../utils/flatten-elements";
import {CodeTabConditionSetEditor} from "./code-tab-condition-set-editor";
import React from "react";
import {ConditionSetOperator} from "../../../../../../../data/condition-set-operator";
import Evaluators from "../../../../../../../evaluators";
import {Function} from "../../../../../../../models/functions/function";
import {RootElement} from "../../../../../../../models/elements/root-element";
import {StepElement} from "../../../../../../../models/elements/steps/step-element";
import {GroupLayout} from "../../../../../../../models/elements/form/layout/group-layout";
import {
    ReplicatingContainerLayout
} from "../../../../../../../models/elements/form/layout/replicating-container-layout";
import {ElementType} from "../../../../../../../data/element-type/element-type";
import {ConditionSet} from "../../../../../../../models/functions/conditions/condition-set";

interface CodeTabNoCodeEditorProps {
    parents: (RootElement | StepElement | GroupLayout | ReplicatingContainerLayout)[];
    element: AnyElement;
    func: Function;
    onChange: (func: Function) => void;
    shouldReturnString: boolean;
}

export function CodeTabNoCodeEditor({
                                        parents,
                                        element,
                                        func,
                                        onChange,
                                        shouldReturnString,
                                    }: CodeTabNoCodeEditorProps) {
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

    const allElements = (parent != null ? flattenElements(parent, true) : [])
        .filter(e => Evaluators[e.type] != null);

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
        />
    );
}
