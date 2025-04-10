import {ExpressionEditorProps} from './expression-editor-props';
import React, {ReactNode, useMemo, useState} from 'react';
import {Box, Button, FormHelperText, Typography} from '@mui/material';
import {isNoCodeExpression, isNoCodeReference, isNoCodeStaticValue, NoCodeExpression, NoCodeOperand} from '../../../../models/functions/no-code-expression';
import {TextFieldComponent} from '../../../text-field/text-field-component';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import {generateComponentTitle} from '../../../../utils/generate-component-title';
import {generateElementNameWithParent} from '../../../../utils/flatten-elements';
import {SelectFieldComponent} from '../../../select-field/select-field-component';
import {OperandTypeIcon} from './operand-type-icon';
import {ExpressionHeader} from './expression-header';
import {OperatorInfoDialog} from '../../../../dialogs/operator-info-dialog/operator-info-dialog';
import {OperandTypeSelector} from './operand-type-selector';
import {useLogger} from '../../../../hooks/use-logging';
import {SelectElementDialog} from '../../../../dialogs/select-element-dialog/select-element-dialog';
import LocationSearchingIcon from '@mui/icons-material/LocationSearching';
import {SelectOperatorDialog} from '../../../../dialogs/select-operator-dialog/select-operator-dialog';
import {IconButton} from '../../../icon-button/icon-button';
import {NoCodeDataTypeLabels} from '../../../../data/no-code-data-type';

export function ExpressionEditor(props: ExpressionEditorProps) {
    const log = useLogger('ExpressionEditor');

    const [operatorSelectDialogOpen, setOperatorSelectDialogOpen] = useState(false);
    const [addAboveDialogOpen, setAddAboveDialogOpen] = useState(false);
    const [elementSelectDialogOpenIndex, setElementSelectDialogOpenIndex] = useState<number>();
    const [operatorInfoDialogOpen, setOperatorInfoDialogOpen] = useState(false);

    const operator = useMemo(() => {
        return props.allOperators.find(o => o.identifier === props.expression.operatorIdentifier);
    }, [props.allOperators, props.expression.operatorIdentifier]);

    const possibleReferences = useMemo(() => {
        log.debug('Calculating possible references');

        return props.allElements.map(({element, parents}) => ({
            label: generateComponentTitle(element),
            subLabel: generateElementNameWithParent({element, parents}),
            value: element.id,
        }));
    }, [props.allElements]);

    return (
        <>
            {
                props.label != null &&
                <Typography
                    variant="caption"
                    sx={{
                        display: 'block',
                        mb: 0.5,
                    }}
                >
                    {props.label}
                </Typography>
            }

            <Box
                sx={{
                    border: '1px solid #e0e0e0',
                    paddingX: 2,
                    paddingTop: 2,
                    paddingBottom: operator == null ? 2 : 0,
                    borderRadius: 1,
                    marginBottom: 1,
                }}
            >
                {
                    operator == null && (
                        <Button
                            onClick={() => {
                                setOperatorSelectDialogOpen(true);
                            }}
                        >
                            Operator auswählen
                        </Button>
                    )
                }

                {
                    operator != null && (
                        <Box>
                            <ExpressionHeader
                                expression={props.expression}
                                operator={operator}
                                onChange={props.onChange}
                                testedExpression={props.testedExpression}
                                onShowSelect={() => {
                                    setOperatorSelectDialogOpen(true);
                                }}
                                onAddAbove={() => {
                                    setAddAboveDialogOpen(true);
                                }}
                                onShowInfo={() => {
                                    setOperatorInfoDialogOpen(true);
                                }}
                                onTest={() => {
                                    props.onTest(props.expression);
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
                                        const operand: NoCodeOperand | undefined | null = props.expression.operands[index];

                                        return (
                                            <Box
                                                key={index}
                                            >
                                                <Box
                                                    sx={{
                                                        display: 'flex',
                                                        alignItems: 'center',
                                                    }}
                                                >
                                                    <Box
                                                        sx={{
                                                            display: 'flex',
                                                            flexDirection: 'column',
                                                            width: '1em',
                                                            alignSelf: 'stretch',
                                                        }}
                                                    >
                                                        <Box
                                                            sx={{
                                                                flex: 1,
                                                                width: '1px',
                                                                backgroundColor: 'black',
                                                            }}
                                                        />
                                                        <Box
                                                            sx={{
                                                                height: '1px',
                                                                backgroundColor: 'black',
                                                                width: '100%',
                                                                margin: 'auto 0',
                                                            }}
                                                        />
                                                        <Box
                                                            sx={{
                                                                flex: 1,
                                                                width: '1px',
                                                                backgroundColor: index < all.length - 1 ? 'black' : 'none',
                                                            }}
                                                        />
                                                    </Box>

                                                    {
                                                        operand == null &&
                                                        <OperandTypeSelector
                                                            expression={props.expression}
                                                            onChange={(newOperand) => {
                                                                const updatedOperands = [];
                                                                for (let i = 0; i < all.length; i++) {
                                                                    if (i === index) {
                                                                        updatedOperands.push(newOperand);
                                                                    } else {
                                                                        updatedOperands.push(props.expression.operands[i] ?? null);
                                                                    }
                                                                }
                                                                props.onChange({
                                                                    ...props.expression,
                                                                    operands: updatedOperands,
                                                                });
                                                            }}
                                                            optional={false}
                                                            hint={parameter.label}
                                                        />
                                                    }

                                                    {
                                                        operand != null &&
                                                        <>
                                                            {getOperandIcon(operand)}

                                                            <Box
                                                                sx={{
                                                                    flex: 1,
                                                                    mr: 0.5,
                                                                    ml: 1,
                                                                }}
                                                            >
                                                                {
                                                                    isNoCodeStaticValue(operand) &&
                                                                    (parameter.options ?? []).length === 0 &&
                                                                    <TextFieldComponent
                                                                        label={parameter.label}
                                                                        value={operand.value}
                                                                        onChange={(value) => {
                                                                            const updatedOperands = [...props.expression.operands];
                                                                            updatedOperands[index] = {
                                                                                value: value ?? '',
                                                                            };
                                                                            props.onChange({
                                                                                ...props.expression,
                                                                                operands: updatedOperands,
                                                                            });
                                                                        }}
                                                                        disabled={!props.editable}
                                                                    />
                                                                }
                                                                {
                                                                    isNoCodeStaticValue(operand) &&
                                                                    (parameter.options ?? []).length > 0 &&
                                                                    <SelectFieldComponent
                                                                        label={parameter.label}
                                                                        value={operand.value}
                                                                        options={parameter.options ?? []}
                                                                        onChange={(value) => {
                                                                            const updatedOperands = [...props.expression.operands];
                                                                            updatedOperands[index] = {
                                                                                value: value ?? '',
                                                                            };
                                                                            props.onChange({
                                                                                ...props.expression,
                                                                                operands: updatedOperands,
                                                                            });
                                                                        }}
                                                                        disabled={!props.editable}
                                                                    />
                                                                }

                                                                {
                                                                    isNoCodeReference(operand) && (
                                                                        <SelectFieldComponent
                                                                            label={parameter.label}
                                                                            required
                                                                            value={operand.elementId}
                                                                            onChange={(val) => {
                                                                                const updatedOperands = [...props.expression.operands];
                                                                                updatedOperands[index] = {
                                                                                    elementId: val ?? '',
                                                                                };
                                                                                props.onChange({
                                                                                    ...props.expression,
                                                                                    operands: updatedOperands,
                                                                                });
                                                                            }}
                                                                            options={possibleReferences}
                                                                            disabled={!props.editable}
                                                                        />
                                                                    )
                                                                }
                                                                {
                                                                    isNoCodeExpression(operand) && (
                                                                        <ExpressionEditor
                                                                            allElements={props.allElements}
                                                                            allOperators={props.allOperators}
                                                                            expression={operand}
                                                                            onChange={(expression) => {
                                                                                const updatedOperands = [...props.expression.operands];
                                                                                updatedOperands[index] = expression;
                                                                                props.onChange({
                                                                                    ...props.expression,
                                                                                    operands: updatedOperands,
                                                                                });
                                                                            }}
                                                                            testedExpression={props.testedExpression}
                                                                            onTest={props.onTest}
                                                                            editable={props.editable}
                                                                            desiredReturnType={parameter.type}
                                                                            hint={parameter.label + NoCodeDataTypeLabels[parameter.type]}
                                                                        />
                                                                    )
                                                                }
                                                            </Box>

                                                            {
                                                                isNoCodeReference(operand) && (
                                                                    <IconButton
                                                                        buttonProps={{
                                                                            onClick: () => {
                                                                                setElementSelectDialogOpenIndex(index);
                                                                            },
                                                                            color: 'inherit',
                                                                        }}
                                                                        tooltipProps={{
                                                                            title: 'Referenz auswählen',
                                                                        }}
                                                                    >
                                                                        <LocationSearchingIcon />
                                                                    </IconButton>
                                                                )
                                                            }

                                                            <IconButton
                                                                buttonProps={{
                                                                    onClick: () => {
                                                                        const updatedOperands = [...props.expression.operands];
                                                                        updatedOperands[index] = null;
                                                                        props.onChange({
                                                                            ...props.expression,
                                                                            operands: updatedOperands,
                                                                        });
                                                                    },
                                                                    color: 'inherit',
                                                                }}
                                                                tooltipProps={{
                                                                    title: 'Parameter entfernen',
                                                                }}
                                                            >
                                                                <DeleteOutlineIcon />
                                                            </IconButton>
                                                        </>
                                                    }
                                                </Box>
                                            </Box>
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
                open={operatorSelectDialogOpen}
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

            <SelectElementDialog
                open={elementSelectDialogOpenIndex != null}
                onSelect={(element) => {
                    if (elementSelectDialogOpenIndex != null) {
                        const updatedOperands = [...props.expression.operands];
                        updatedOperands[elementSelectDialogOpenIndex] = {
                            elementId: element.id ?? '',
                        };
                        props.onChange({
                            ...props.expression,
                            operands: updatedOperands,
                        });
                    }
                    setElementSelectDialogOpenIndex(undefined);
                }}
                onClose={() => {
                    setElementSelectDialogOpenIndex(undefined);
                }}
            />

            <SelectOperatorDialog
                open={addAboveDialogOpen}
                desiredReturnType={props.desiredReturnType}
                operators={props.allOperators}
                onSelect={(operator) => {
                    const newExpression: NoCodeExpression = {
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

function getOperandIcon(operand?: NoCodeOperand): ReactNode {
    if (isNoCodeStaticValue(operand)) {
        return OperandTypeIcon.value;
    }
    if (isNoCodeReference(operand)) {
        return OperandTypeIcon.reference;
    }
    if (isNoCodeExpression(operand)) {
        return OperandTypeIcon.exp;
    }
    return null;
}
