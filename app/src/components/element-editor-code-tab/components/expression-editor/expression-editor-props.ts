import {NoCodeOperatorDetailsDTO} from '../../../../models/dtos/no-code-operator-details-dto';
import {NoCodeExpression} from '../../../../models/functions/no-code-expression';
import {ElementWithParents} from '../../../../utils/flatten-elements';
import {NoCodeDataType} from '../../../../data/no-code-data-type';

export interface ExpressionEditorProps {
    allElements: ElementWithParents[];
    allOperators: NoCodeOperatorDetailsDTO[];
    expression: NoCodeExpression;
    onChange: (expression: NoCodeExpression) => void;
    testedExpression?: NoCodeExpression;
    onTest: (expression: NoCodeExpression) => void;
    editable: boolean;
    desiredReturnType: NoCodeDataType;
    label?: string;
    hint?: string;
    error?: string;
}