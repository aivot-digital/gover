import {NoCodeExpression} from '../../../../models/functions/no-code-expression';
import {NoCodeDataType} from '../../../../data/no-code-data-type';
import {GroupLayout} from '../../../../models/elements/form/layout/group-layout';
import {StepElement} from '../../../../models/elements/steps/step-element';
import {RootElement} from '../../../../models/elements/root-element';
import {ReplicatingContainerLayout} from '../../../../models/elements/form/layout/replicating-container-layout';

export interface ExpressionEditorWrapperProps {
    parents: Array<RootElement | StepElement | GroupLayout | ReplicatingContainerLayout>;
    expression: NoCodeExpression;
    onChange: (expression: NoCodeExpression) => void;
    editable: boolean;
    desiredReturnType: NoCodeDataType;
    hint?: string;
    error?: string;
    label?: string;
}