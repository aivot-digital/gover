import React, {useMemo, useState} from 'react';
import {Box, FormHelperText, Typography} from '@mui/material';
import {NoCodeExpression, NoCodeOperand} from '../../../../models/functions/no-code-expression';
import {ElementWithParents} from '../../../../utils/flatten-elements';
import {ExpressionHeader} from './expression-header';
import {OperatorInfoDialog} from '../../../../dialogs/operator-info-dialog/operator-info-dialog';
import {SelectOperatorDialog} from '../../../../dialogs/select-operator-dialog/select-operator-dialog';
import {NoCodeDataType} from '../../../../data/no-code-data-type';
import {OperandEditor} from './operand-editor';
import {NoCodeOperatorDetailsDTO} from '../../../../models/dtos/no-code-operator-details-dto';

export interface ExpressionEditorProps {
    allElements: ElementWithParents[];
    allOperators: NoCodeOperatorDetailsDTO[];
    expression: NoCodeExpression;
    onChange: (expression: NoCodeExpression | undefined | null) => void;
    editable: boolean;
    desiredReturnType: NoCodeDataType;
    label?: string;
    hint?: string;
    error?: string;
}

export function ExpressionEditor(props: ExpressionEditorProps) {
    const {
        allElements,
        allOperators,
        expression,
        editable,
    } = props;

    const {
        operatorIdentifier,
    } = expression;

    const [operatorSelectDialogOpen, setOperatorSelectDialogOpen] = useState(false);
    const [addAboveDialogOpen, setAddAboveDialogOpen] = useState(false);
    const [operatorInfoDialogOpen, setOperatorInfoDialogOpen] = useState(false);

    const operator = useMemo(() => {
        return allOperators.find(o => o.identifier === operatorIdentifier);
    }, [allOperators, operatorIdentifier]);

    return (
        <>
            <Box
                sx={{
                    position: 'relative',
                    border: '1px solid #e0e0e0',
                    paddingX: 2,
                    paddingTop: 2,
                    paddingBottom: operator == null ? 2 : 0,
                    borderRadius: 1,
                    marginBottom: 1,
                }}
            >
                {
                    props.label != null &&
                    <Typography
                        variant="caption"
                        sx={{
                            position: 'absolute',
                            top: -8,
                            left: 8,
                            backgroundColor: 'background.paper',
                            paddingX: 0.5,
                        }}
                    >
                        {props.label}
                    </Typography>
                }

                {
                    operator != null && (
                        <Box>
                            <ExpressionHeader
                                expression={props.expression}
                                operator={operator}
                                onChange={props.onChange}
                                onShowSelect={() => {
                                    setOperatorSelectDialogOpen(true);
                                }}
                                onAddAbove={() => {
                                    setAddAboveDialogOpen(true);
                                }}
                                onShowInfo={() => {
                                    setOperatorInfoDialogOpen(true);
                                }}
                            />

                            <Box
                                sx={{
                                    paddingLeft: 1,
                                    marginLeft: 1,
                                    paddingTop: 1,
                                    paddingBottom: 1,
                                }}
                            >
                                {
                                    operator.parameters.map((parameter, index, all) => {
                                        const operand: NoCodeOperand | undefined | null = (props.expression.operands ?? [])[index];
                                        return (
                                            <OperandEditor
                                                key={index}
                                                allElements={allElements}
                                                allOperators={allOperators}
                                                parameter={parameter}
                                                operand={operand}
                                                onChange={(updatedOperand) => {
                                                    const updatedOperands = [...(props.expression.operands ?? [])];
                                                    updatedOperands[index] = updatedOperand ?? null;
                                                    props.onChange({
                                                        ...props.expression,
                                                        operands: updatedOperands,
                                                    });
                                                }}
                                                disabled={!editable}
                                                isTopLevelOperand={false}
                                                isLastOperand={index === all.length - 1}
                                            />
                                        );
                                    })
                                }
                            </Box>
                        </Box>
                    )
                }
            </Box>

            {
                (props.hint != null || props.error != null) && (
                    <FormHelperText
                        error={props.error != null}
                    >
                        {props.error ?? props.hint}
                    </FormHelperText>
                )
            }

            <SelectOperatorDialog
                open={operatorSelectDialogOpen || operator == null}
                desiredReturnType={props.desiredReturnType}
                operators={props.allOperators}
                onSelect={(operator) => {
                    props.onChange({
                        ...props.expression,
                        operatorIdentifier: operator.identifier,
                    });
                    setOperatorSelectDialogOpen(false);
                }}
                onClose={() => {
                    setOperatorSelectDialogOpen(false);
                }}
            />

            <SelectOperatorDialog
                open={addAboveDialogOpen}
                desiredReturnType={props.desiredReturnType}
                operators={props.allOperators}
                onSelect={(operator) => {
                    const newExpression: NoCodeExpression = {
                        type: 'NoCodeExpression',
                        operatorIdentifier: operator.identifier,
                        operands: [
                            props.expression,
                        ],
                    };
                    props.onChange(newExpression);
                    setAddAboveDialogOpen(false);
                }}
                onClose={() => {
                    setAddAboveDialogOpen(false);
                }}
            />

            <OperatorInfoDialog
                operator={operatorInfoDialogOpen ? operator : undefined}
                onClose={() => {
                    setOperatorInfoDialogOpen(false);
                }}
            />
        </>
    );
}

