import {AnyElement} from "../../../../../../../models/elements/any-element";
import {useAppSelector} from "../../../../../../../hooks/use-app-selector";
import {selectLoadedApplication} from "../../../../../../../slices/app-slice";
import {flattenElements} from "../../../../../../../utils/flatten-elements";
import {CodeTabConditionSetEditor} from "./code-tab-condition-set-editor";
import React from "react";
import {ConditionSetOperator} from "../../../../../../../data/condition-set-operator";
import Evaluators from "../../../../../../../evaluators";
import {Function} from "../../../../../../../models/functions/function";

interface CodeTabNoCodeEditorProps {
    element: AnyElement;
    func: Function;
    onChange: (func: Function) => void;
    shouldReturnString: boolean;
}

export function CodeTabNoCodeEditor({
                                        element,
                                        func,
                                        onChange,
                                        shouldReturnString,
                                    }: CodeTabNoCodeEditorProps) {
    const application = useAppSelector(selectLoadedApplication);

    const allElements = (application != null ? flattenElements(application.root) : [])
        .filter(e => Evaluators[e.type] != null);

    return (
        <CodeTabConditionSetEditor
            element={element}
            allElements={allElements}
            conditionSet={func.conditionSet ?? {
                operator: ConditionSetOperator.Any,
            }}
            onChange={cs => onChange({
                ...func,
                conditionSet: cs,
            })}
            shouldReturnString={shouldReturnString}
        />
    );
}
