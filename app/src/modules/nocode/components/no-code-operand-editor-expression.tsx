import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';
import Functions from '@aivot/mui-material-symbols-400-outlined/dist/functions/Functions';
import {isNoCodeExpression, isNoCodeReference, isNoCodeStaticValue, NoCodeExpression, NoCodeOperand, NoCodeReference} from '../../../models/functions/no-code-expression';
import {NoCodeDataType} from '../../../data/no-code-data-type';
import {ElementWithParents} from '../../../utils/flatten-elements';
import {useMemo, useState} from 'react';
import {NoCodeOperatorDetailsDTO, NoCodeParameter, NoCodeParameterOption} from '../../../models/dtos/no-code-operator-details-dto';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import {NoCodeOperandEditor} from './no-code-operand-editor';
import {Actions} from '../../../components/actions/actions';
import Help from '@aivot/mui-material-symbols-400-outlined/dist/help/Help';
import SwapHoriz from '@aivot/mui-material-symbols-400-outlined/dist/swap-horiz/SwapHoriz';
import SwapVert from '@aivot/mui-material-symbols-400-outlined/dist/swap-vert/SwapVert';
import {SelectOperatorDialog} from '../../../dialogs/select-operator-dialog/select-operator-dialog';
import {OperatorInfoDialog} from '../../../dialogs/operator-info-dialog/operator-info-dialog';
import {ReorderDialog} from '../../../dialogs/reorder-dialog/reorder-dialog';
import {generateComponentTitle} from '../../../utils/generate-component-title';
import {isStringNullOrEmpty} from '../../../utils/string-utils';
import {ElementType} from '../../../data/element-type/element-type';
import {BOOL_DEFAULT_OPTIONS} from './no-code-operand-editor-static-value';

interface NoCodeOperandEditorExpressionProps {
    allElements: ElementWithParents[];
    allOperators: NoCodeOperatorDetailsDTO[];
    label: string;
    hint?: string;
    value: NoCodeExpression;
    onChange: (value: NoCodeOperand | undefined) => void;
    desiredType: NoCodeDataType;
    onAddEnclosingExpression: () => void;
}

interface ResolvedParameter {
    parameter: NoCodeParameter;
    operand: NoCodeOperand | undefined | null;
}

export function NoCodeOperandEditorExpression(props: NoCodeOperandEditorExpressionProps) {
    const {
        allElements,
        allOperators,
        label,
        hint,
        value: operand,
        onChange,
        desiredType,
        onAddEnclosingExpression,
    } = props;

    const [showOperatorSwitcher, setShowOperatorSwitcher] = useState(false);
    const [showReorderParameters, setShowReorderParameters] = useState(false);
    const [showOperatorInfo, setShowOperatorInfo] = useState(false);

    const {
        operatorIdentifier,
        operands: originalOperands,
    } = operand;

    const operands: Array<NoCodeOperand | null> = useMemo(() => {
        return originalOperands ?? [];
    }, [originalOperands]);

    const operator = useMemo(() => {
        return allOperators
            .find((op) => op.identifier === operatorIdentifier);
    }, [allOperators, operatorIdentifier]);

    if (operator == null) {
        return (
            <Typography
                color="error"
            >
                Ungültiger Ausdruckstyp ausgewählt.
            </Typography>
        );
    }

    const {
        parameters,
    } = operator;

    const parameterOptionOverrides: NoCodeParameterOption[] = useMemo(() => {
        const options: NoCodeParameterOption[] = [];

        for (const op of operands) {
            if (!isNoCodeReference(op)) {
                continue;
            }

            const element = allElements
                .find(el => el.element.id === op.elementId)?.element;

            if (element == null) {
                continue;
            }

            switch (element.type) {
                case ElementType.Checkbox:
                    options.push(...BOOL_DEFAULT_OPTIONS);
                    break;
                case ElementType.Radio:
                case ElementType.Select:
                    if (element.options != null) {
                        options.push(...element.options);
                    }
                    break;
            }
        }

        return options;
    }, [operands, allElements]);

    const leadingParameter: ResolvedParameter | undefined = useMemo(() => {
        if (parameters.length >= 2) {
            return {
                parameter: {
                    ...parameters[0],
                    options: parameterOptionOverrides.length > 0 ? parameterOptionOverrides : parameters[0].options,
                },
                operand: operands[0],
            };
        }

        return undefined;
    }, [parameters, operands, parameterOptionOverrides]);

    const trailingParameters: ResolvedParameter[] = useMemo(() => {
        const hasLeadingParameter = leadingParameter != null;

        return parameters
            .slice(hasLeadingParameter ? 1 : 0)
            .map((p, index) => ({
                parameter: {
                    ...p,
                    options: parameterOptionOverrides.length > 0 ? parameterOptionOverrides : p.options,
                },
                operand: operands[index + (hasLeadingParameter ? 1 : 0)],
            }));
    }, [parameters, operands, leadingParameter, parameterOptionOverrides]);

    const combinedParameters = useMemo(() => {
        if (leadingParameter != null) {
            return [leadingParameter, ...trailingParameters];
        } else {
            return trailingParameters;
        }
    }, [leadingParameter, trailingParameters]);

    return (
        <>
            <Box>
                {
                    leadingParameter != null &&
                    <Box
                        sx={{
                            display: 'flex',
                            alignItems: 'stretch',
                            paddingLeft: '0.25rem',
                        }}
                    >
                        <Box
                            sx={{
                                position: 'relative',
                                width: '1.5rem',
                            }}
                        >
                            <Box
                                sx={{
                                    position: 'absolute',
                                    height: '1px',
                                    top: 'calc(50% - 1px)',
                                    width: '100%',
                                    bgcolor: 'grey.400',
                                }}
                            />

                            <Box
                                sx={{
                                    position: 'absolute',
                                    height: '50%',
                                    top: 'calc(50% - 1px)',
                                    width: '1px',
                                    bgcolor: 'grey.400',
                                }}
                            />
                        </Box>

                        <Box
                            sx={{
                                pb: 2,
                                flex: 1,
                            }}
                        >
                            <NoCodeOperandEditor
                                parameter={leadingParameter.parameter}
                                operand={leadingParameter.operand}
                                onChange={(updatedOperand) => {
                                    onChange({
                                        ...operand,
                                        operands: [
                                            updatedOperand ?? null,
                                            ...(operand.operands?.slice(1) ?? []),
                                        ],
                                    });
                                }}
                                allOperators={allOperators}
                                allElements={allElements}
                            />
                        </Box>
                    </Box>
                }

                <Box
                    sx={{
                        display: 'flex',
                        gap: 2,
                    }}
                >
                    <Box
                        sx={{
                            position: 'relative',
                            bgcolor: 'grey.200',
                            pl: 1,
                            pr: 2,
                            py: 1,
                            borderRadius: 1,
                            display: 'flex',
                            alignItems: 'center',
                        }}
                    >
                        {
                            false &&
                            <Typography
                                sx={{
                                    position: 'absolute',
                                    top: -8,
                                    left: 8,
                                    backgroundColor: 'background.paper',
                                    paddingX: 0.75,
                                    color: 'text.secondary',
                                }}
                                variant="caption"
                            >
                                {label} — (Ausdruck)
                            </Typography>
                        }

                        <Functions
                            fontSize="small"
                            sx={{
                                color: 'grey.800',
                            }}
                        />

                        <Typography
                            variant="caption"
                            sx={{
                                ml: 0.5,
                            }}
                        >
                            {operator.label}
                        </Typography>
                    </Box>

                    {
                        operator.abstractDescription != null &&
                        <Typography
                            variant="caption"
                            color="text.secondary"
                            sx={{
                                mt: 1,
                                flex: 1,
                            }}
                        >
                            {operator.abstractDescription}
                        </Typography>
                    }

                    <Actions
                        dense={true}
                        size="small"
                        sx={{
                            ml: 1,
                            color: 'text.secondary',
                        }}
                        color="inherit"
                        actions={[
                            {
                                icon: <Delete />,
                                tooltip: 'Diesen Ausdruck löschen',
                                onClick: () => {
                                    onChange(operands[0] ?? undefined);
                                },
                            },
                            {
                                icon: <SwapHoriz />,
                                tooltip: 'Ausdrucksoperator austauschen',
                                onClick: () => {
                                    setShowOperatorSwitcher(true);
                                },
                            },
                            {
                                icon: <SwapVert />,
                                tooltip: 'Parameterreihenfolge ändern',
                                onClick: () => {
                                    setShowReorderParameters(true);
                                },
                            },
                            {
                                icon: <Help />,
                                tooltip: 'Hilfe zum Ausdruckstyp',
                                onClick: () => {
                                    setShowOperatorInfo(true);
                                },
                            },
                            {
                                icon: <Functions />,
                                tooltip: 'Diesen Ausdruck mit einem anderen Ausdruck verknüpfen',
                                onClick: () => {
                                    onChange({
                                        type: 'NoCodeExpression',
                                        operatorIdentifier: null,
                                        operands: [
                                            operand,
                                        ],
                                    });
                                },
                            },
                        ]}
                    />
                </Box>

                {
                    trailingParameters.map((p, index, all) => (
                        <Box
                            key={index}
                            sx={{
                                display: 'flex',
                                alignItems: 'stretch',
                                paddingLeft: '0.25rem',
                            }}
                        >
                            <Box
                                sx={{
                                    position: 'relative',
                                    width: '1.5rem',
                                }}
                            >
                                <Box
                                    sx={{
                                        position: 'absolute',
                                        height: '50%',
                                        bottom: '50%',
                                        width: '1px',
                                        bgcolor: 'grey.400',
                                    }}
                                />

                                <Box
                                    sx={{
                                        position: 'absolute',
                                        height: '1px',
                                        top: 'calc(50% - 1px)',
                                        width: '100%',
                                        bgcolor: 'grey.400',
                                    }}
                                />

                                {
                                    index < all.length - 1 &&
                                    <Box
                                        sx={{
                                            position: 'absolute',
                                            height: '50%',
                                            bottom: 'calc(50% - 1px)',
                                            width: '1px',
                                            bgcolor: 'grey.400',
                                        }}
                                    />
                                }

                            </Box>

                            <Box
                                sx={{
                                    paddingTop: index === 0 ? 2.5 : 2,
                                    flex: 1,
                                }}
                            >
                                <NoCodeOperandEditor
                                    parameter={p.parameter}
                                    operand={p.operand}
                                    onChange={(updatedOperand) => {
                                        const updatedOperands = operand.operands ? [...operand.operands] : [];
                                        updatedOperands[index + (leadingParameter != null ? 1 : 0)] = updatedOperand ?? null;
                                        onChange({
                                            ...operand,
                                            operands: updatedOperands,
                                        });
                                    }}
                                    allOperators={allOperators}
                                    allElements={allElements}
                                />
                            </Box>
                        </Box>
                    ))
                }
            </Box>

            <SelectOperatorDialog
                open={showOperatorSwitcher}
                operators={allOperators}
                onSelect={(op) => {
                    setShowOperatorSwitcher(false);
                    if (isNoCodeExpression(operand)) {
                        onChange({
                            ...operand,
                            operatorIdentifier: op.identifier,
                        });
                    }
                }}
                onClose={() => {
                    setShowOperatorSwitcher(false);
                }}
                desiredReturnType={NoCodeDataType.Runtime /* TODO */}
            />

            <OperatorInfoDialog
                operator={showOperatorInfo ? operator : undefined}
                onClose={() => setShowOperatorInfo(false)}
            />

            <ReorderDialog
                title="Parameter neu sortieren"
                items={(combinedParameters ?? [])}
                getLabel={(p) => {
                    if (isNoCodeStaticValue(p.operand)) {
                        return {
                            primary: `„${p.operand.value}”`,
                            secondary: p.parameter.label,
                        };
                    }

                    if (isNoCodeReference(p.operand)) {
                        return {
                            primary: getReferenceOperandLabel(p.operand, allElements),
                            secondary: p.parameter.label,
                        };
                    }

                    if (isNoCodeExpression(p.operand)) {
                        return {
                            primary: getExpressionOperandLabel(p.operand, allOperators),
                            secondary: p.parameter.label,
                        };
                    }

                    return {
                        primary: p.parameter.label,
                    };
                }}
                onReorder={(p) => {
                    if (isNoCodeExpression(operand)) {
                        onChange({
                            ...operand,
                            operands: p.map(param => param.operand ?? null),
                        });
                    }
                    setShowReorderParameters(false);
                }}
                onClose={() => setShowReorderParameters(false)}
                open={showReorderParameters}
            />
        </>
    );
}

function getReferenceOperandLabel(operand: NoCodeReference, allElements: ElementWithParents[]): string {
    if (isStringNullOrEmpty(operand.elementId)) {
        return 'Kein Element ausgewählt';
    }

    const element = allElements
        .find(el => el.element.id === operand.elementId);

    return element != null
        ? generateComponentTitle(element.element)
        : `Unbekanntes Element (ID: ${operand.elementId})`;
}

function getExpressionOperandLabel(operand: NoCodeExpression, allOperators: NoCodeOperatorDetailsDTO[]): string {
    if (isStringNullOrEmpty(operand.operatorIdentifier)) {
        return 'Kein Ausdrucksoperator ausgewählt';
    }

    const operator = allOperators
        .find(op => op.identifier === operand.operatorIdentifier);

    if (operator == null) {
        return `Der Ausdruck mit dem unbekannten No-Code Operand (ID: ${operand.operatorIdentifier})`;
    }

    return operator.label;
}