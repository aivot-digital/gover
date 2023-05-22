import {AnyElement} from "../../../../../../../models/elements/any-element";
import {FunctionNoCode} from "../../../../../../../models/functions/function-no-code";
import {useAppSelector} from "../../../../../../../hooks/use-app-selector";
import {selectLoadedApplication} from "../../../../../../../slices/app-slice";
import {flattenElements} from "../../../../../../../utils/flatten-elements";
import {isAnyInputElement} from "../../../../../../../models/elements/form/input/any-input-element";
import {CodeTabConditionSetEditor} from "./code-tab-condition-set-editor";
import React from "react";
import {ElementType} from "../../../../../../../data/element-type/element-type";
import {ConditionSetOperator} from "../../../../../../../data/condition-set-operator";

interface CodeTabNoCodeEditorProps {
    element: AnyElement;
    func: FunctionNoCode;
    onChange: (func: FunctionNoCode) => void;
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
        .filter(e =>
            isAnyInputElement(e) &&
            e.type !== ElementType.Table &&
            e.type !== ElementType.MultiCheckbox &&
            e.type !== ElementType.ReplicatingContainer &&
            e.type !== ElementType.FileUpload
        );

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
