import {Box} from '@mui/material';
import {OperandTypeSelector} from './operand-type-selector';
import {isNoCodeExpression, isNoCodeReference, isNoCodeStaticValue, NoCodeOperand} from '../../../../models/functions/no-code-expression';
import {ExpressionEditor} from './expression-editor';
import {TextFieldComponent} from '../../../text-field/text-field-component';
import {SelectFieldComponent} from '../../../select-field/select-field-component';
import {NoCodeOperatorDetailsDTO, NoCodeParameter} from '../../../../models/dtos/no-code-operator-details-dto';
import {OperandTypeIcon} from './operand-type-icon';
import Close from '@aivot/mui-material-symbols-400-outlined/dist/close/Close';
import DatabaseSearch from '@aivot/mui-material-symbols-400-outlined/dist/database-search/DatabaseSearch';
import React, {useState} from 'react';
import {ElementWithParents} from '../../../../utils/flatten-elements';
import {SelectElementDialog} from '../../../../dialogs/select-element-dialog/select-element-dialog';
import {NoCodeDataTypeLabels} from '../../../../data/no-code-data-type';
import {generateComponentTitle} from '../../../../utils/generate-component-title';

interface OperandEditorProps {
    allElements: ElementWithParents[];
    allOperators: NoCodeOperatorDetailsDTO[];
    parameter: NoCodeParameter;
    operand: NoCodeOperand | null | undefined;
    onChange: (operand: NoCodeOperand | null | undefined) => void;
    disabled: boolean;
    isTopLevelOperand: boolean;
    isLastOperand: boolean;
}

export function OperandEditor(props: OperandEditorProps) {
    const {
        allElements,
        allOperators,
        parameter,
        operand,
        onChange,
        disabled,
        isTopLevelOperand,
        isLastOperand,
    } = props;

    const [showElementSelectDialog, setShowElementSelectDialog] = useState(false);

    return (
        <Box
            sx={{
                display: 'flex',
                alignItems: 'center',
            }}
        >
            {
                !isTopLevelOperand &&
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
                            backgroundColor: isLastOperand ? 'none' : 'black',
                        }}
                    />
                </Box>
            }

            {
                operand == null &&
                <OperandTypeSelector
                    onChange={onChange}
                    optional={false}
                    label={isTopLevelOperand ? undefined : parameter?.label}
                    hint={parameter?.description ?? '' /* TODO */}
                />
            }

            {
                operand != null &&
                <>
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
                                value={operand.value ?? undefined}
                                onChange={(value) => {
                                    onChange({
                                        type: 'NoCodeStaticValue',
                                        value: value ?? '',
                                    });
                                }}
                                disabled={disabled}
                                startIcon={OperandTypeIcon.value}
                                endAction={disabled ? undefined : [{
                                    tooltip: 'Löschen',
                                    icon: <Close />,
                                    onClick: () => {
                                        onChange(null);
                                    },
                                }]}
                            />
                        }
                        {
                            isNoCodeStaticValue(operand) &&
                            (parameter.options ?? []).length > 0 &&
                            <SelectFieldComponent
                                label={parameter.label}
                                value={operand.value ?? undefined}
                                options={parameter.options ?? []}
                                onChange={(value) => {
                                    onChange({
                                        type: 'NoCodeStaticValue',
                                        value: value ?? '',
                                    });
                                }}
                                disabled={disabled}
                                startIcon={OperandTypeIcon.value}
                                endAction={disabled ? undefined : [{
                                    tooltip: 'Löschen',
                                    icon: <Close />,
                                    onClick: () => {
                                        onChange(null);
                                    },
                                }]}
                            />
                        }

                        {
                            isNoCodeReference(operand) && (
                                <TextFieldComponent
                                    label={parameter.label}
                                    required
                                    readonly
                                    value={generateComponentTitle(allElements.find(e => e.element.id === operand.elementId)?.element)}
                                    onChange={() => {
                                    }}
                                    disabled={disabled}
                                    startIcon={OperandTypeIcon.reference}
                                    endAction={disabled ? undefined : [
                                        {
                                            tooltip: 'Löschen',
                                            icon: <Close />,
                                            onClick: () => {
                                                onChange(null);
                                            },
                                        },
                                        {
                                            tooltip: 'Referenz auswählen',
                                            icon: <DatabaseSearch />,
                                            onClick: () => {
                                                setShowElementSelectDialog(true);
                                            },
                                        },
                                    ]}
                                />
                            )
                        }

                        {
                            isNoCodeExpression(operand) && (
                                <ExpressionEditor
                                    allElements={allElements}
                                    allOperators={allOperators}
                                    expression={operand}
                                    onChange={onChange}
                                    editable={!disabled}
                                    desiredReturnType={parameter.type}
                                    label={`${parameter.label} - ${NoCodeDataTypeLabels[parameter.type]}`}
                                />
                            )
                        }
                    </Box>
                </>
            }

            <SelectElementDialog
                allElements={allElements}
                desiredType={parameter.type}
                open={showElementSelectDialog}
                onSelect={(element) => {
                    onChange({
                        type: 'NoCodeReference',
                        elementId: element.id,
                    });
                    setShowElementSelectDialog(false);
                }}
                onClose={() => {
                    setShowElementSelectDialog(false);
                }}
            />
        </Box>
    );
}
