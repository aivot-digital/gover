import {
    isNoCodeExpression,
    isNoCodeInstanceDataReference,
    isNoCodeNodeDataReference,
    isNoCodeProcessDataReference,
    isNoCodeReference,
    isNoCodeStaticValue,
    NoCodeExpression,
    NoCodeOperand, NoCodeOperandError,
} from '../../../models/functions/no-code-expression';
import {
    NoCodeOperatorDetailsDTO,
    NoCodeParameter,
    NoCodeParameterOption,
    resolveNoCodeSignature,
} from '../../../models/dtos/no-code-operator-details-dto';
import {ElementWithParents} from '../../../utils/flatten-elements';
import {useState} from 'react';
import {SelectOperatorDialog} from '../../../dialogs/select-operator-dialog/select-operator-dialog';
import {NoCodeDataType} from '../../../data/no-code-data-type';
import {NoCodeOperandEditorSelector} from './no-code-operand-editor-selector';
import {NoCodeOperandEditorStaticValue} from './no-code-operand-editor-static-value';
import {NoCodeOperandEditorReference} from './no-code-operand-editor-reference';
import {NoCodeOperandEditorExpression} from './no-code-operand-editor-expression';
import {NoCodeOperandEditorProcessDataReference} from './no-code-operand-editor-process-data-reference';

export type NoCodeOperandEditorContextType = 'BOTH' | 'FORM' | 'PROCESS';

interface NoCodeOperandEditorProps {
    parameter: NoCodeParameter;
    suggestions?: NoCodeParameterOption[];
    operand: NoCodeOperand | undefined | null;
    onChange: (operand: NoCodeOperand | undefined | null) => void;
    allOperators: NoCodeOperatorDetailsDTO[];
    allElements: ElementWithParents[];
    contextType?: NoCodeOperandEditorContextType;
    operandError?: NoCodeOperandError;
}


export function NoCodeOperandEditor(props: NoCodeOperandEditorProps) {
    const {
        operand,
        onChange,
        allOperators,
        allElements,
        parameter,
        contextType = 'BOTH',
        operandError,
    } = props;

    const [showEnclosingOperatorPicker, setShowEnclosingOperatorPicker] = useState(false);

    return (
        <>
            {
                operand == null &&
                <NoCodeOperandEditorSelector
                    allOperators={allOperators}
                    label={parameter.label}
                    hint={parameter.description}
                    desiredType={parameter.type}
                    onChange={onChange}
                    contextType={contextType}
                />
            }

            {
                isNoCodeStaticValue(operand) &&
                <NoCodeOperandEditorStaticValue
                    label={parameter.label}
                    hint={parameter.description ?? undefined}
                    value={operand}
                    onChange={onChange}
                    desiredType={parameter.type}
                    onAddEnclosingExpression={() => {
                        setShowEnclosingOperatorPicker(true);
                    }}
                    options={parameter.options.length > 0 ? parameter.options : props.suggestions}
                    optionsAreSuggestions={parameter.options.length === 0}
                    operandError={operandError}
                />
            }

            {
                isNoCodeReference(operand) &&
                <NoCodeOperandEditorReference
                    allElements={allElements}
                    label={parameter.label}
                    hint={parameter.description ?? undefined}
                    value={operand}
                    onChange={onChange}
                    desiredType={parameter.type}
                    onAddEnclosingExpression={() => {
                        setShowEnclosingOperatorPicker(true);
                    }}
                    operandError={operandError}
                />
            }

            {
                (isNoCodeProcessDataReference(operand) || isNoCodeInstanceDataReference(operand) || isNoCodeNodeDataReference(operand)) &&
                <NoCodeOperandEditorProcessDataReference
                    label={parameter.label}
                    hint={parameter.description ?? undefined}
                    value={operand}
                    onChange={onChange}
                    onAddEnclosingExpression={() => {
                        setShowEnclosingOperatorPicker(true);
                    }}
                    operandError={operandError}
                />
            }

            {
                isNoCodeExpression(operand) &&
                <NoCodeOperandEditorExpression
                    allElements={allElements}
                    allOperators={allOperators}
                    label={parameter.label}
                    value={operand}
                    onChange={onChange}
                    contextType={contextType}
                    onAddEnclosingExpression={() => {
                        setShowEnclosingOperatorPicker(true);
                    }}
                    operandError={operandError}
                />
            }

            <SelectOperatorDialog
                open={showEnclosingOperatorPicker}
                operators={allOperators.filter((op) => op.signatures.some((signature) => signature.parameters.length > 0))}
                onSelect={(op) => {
                    setShowEnclosingOperatorPicker(false);
                    onChange({
                        type: 'NoCodeExpression',
                        operatorIdentifier: op.identifier,
                        operands: resolveNoCodeSignature(op, {operandCount: 1}).parameters.map((_, index) => index === 0 ? operand ?? null : null),
                    });

                }}
                onClose={() => {
                    setShowEnclosingOperatorPicker(false);
                }}
                desiredReturnType={NoCodeDataType.Runtime /* TODO */}
            />
        </>
    );
}
