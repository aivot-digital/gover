import {isNoCodeExpression, isNoCodeReference, isNoCodeStaticValue, NoCodeExpression, NoCodeOperand} from '../../../models/functions/no-code-expression';
import {NoCodeOperatorDetailsDTO, NoCodeParameter} from '../../../models/dtos/no-code-operator-details-dto';
import {ElementWithParents} from '../../../utils/flatten-elements';
import {useState} from 'react';
import {SelectOperatorDialog} from '../../../dialogs/select-operator-dialog/select-operator-dialog';
import {NoCodeDataType} from '../../../data/no-code-data-type';
import {NoCodeOperandEditorSelector} from './no-code-operand-editor-selector';
import {NoCodeOperandEditorStaticValue} from './no-code-operand-editor-static-value';
import {NoCodeOperandEditorReference} from './no-code-operand-editor-reference';
import {NoCodeOperandEditorExpression} from './no-code-operand-editor-expression';

interface NoCodeOperandEditorProps {
    parameter: NoCodeParameter;
    operand: NoCodeOperand | undefined | null;
    onChange: (operand: NoCodeOperand | undefined | null) => void;
    allOperators: NoCodeOperatorDetailsDTO[];
    allElements: ElementWithParents[];
}


export function NoCodeOperandEditor(props: NoCodeOperandEditorProps) {
    const {
        operand,
        onChange,
        allOperators,
        allElements,
        parameter,
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
                    onChange={onChange}
                    desiredType={parameter.type}
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
                    options={parameter.options}
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
                    desiredType={parameter.type}
                    onAddEnclosingExpression={() => {
                        setShowEnclosingOperatorPicker(true);
                    }}
                />
            }

            <SelectOperatorDialog
                open={showEnclosingOperatorPicker}
                operators={allOperators}
                onSelect={(op) => {
                    setShowEnclosingOperatorPicker(false);
                    onChange({
                        type: 'NoCodeExpression',
                        operatorIdentifier: op.identifier,
                        operands: [
                            operand ?? null,
                        ],
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