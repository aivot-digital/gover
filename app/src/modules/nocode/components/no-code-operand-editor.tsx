import {isNoCodeExpression, isNoCodeReference, isNoCodeStaticValue, NoCodeExpression, NoCodeOperand, NoCodeReference} from '../../../models/functions/no-code-expression';
import {NoCodeOperatorDetailsDTO, NoCodeParameter} from '../../../models/dtos/no-code-operator-details-dto';
import {Box, Button, ButtonGroup, Typography} from '@mui/material';
import {ElementWithParents} from '../../../utils/flatten-elements';
import {generateComponentTitle} from '../../../utils/generate-component-title';
import {SelectFieldComponent} from '../../../components/select-field/select-field-component';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';
import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';
import Functions from '@aivot/mui-material-symbols-400-outlined/dist/functions/Functions';
import Article from '@aivot/mui-material-symbols-400-outlined/dist/article/Article';
import MyLocation from '@aivot/mui-material-symbols-400-outlined/dist/my-location/MyLocation';
import {Actions} from '../../../components/actions/actions';
import {useState} from 'react';
import {SelectOperatorDialog} from '../../../dialogs/select-operator-dialog/select-operator-dialog';
import {NoCodeDataType} from '../../../data/no-code-data-type';
import Help from '@aivot/mui-material-symbols-400-outlined/dist/help/Help';
import {Hint} from '../../../components/hint/hint';
import {InfoDialog} from '../../../dialogs/info-dialog/info-dialog';
import {HintProps} from '../../../components/hint/hint-props';
import {SelectElementDialog} from '../../../dialogs/select-element-dialog/select-element-dialog';
import DatabaseSearch from '@aivot/mui-material-symbols-400-outlined/dist/database-search/DatabaseSearch';
import SwapHoriz from '@aivot/mui-material-symbols-400-outlined/dist/swap-horiz/SwapHoriz';
import {OperatorInfoDialog} from '../../../dialogs/operator-info-dialog/operator-info-dialog';
import SwapVert from '@aivot/mui-material-symbols-400-outlined/dist/swap-vert/SwapVert';
import {ReorderDialog} from '../../../dialogs/reorder-dialog/reorder-dialog';

interface NoCodeOperandEditorProps {
    label: string;
    hint: HintProps | undefined | null;
    operand: NoCodeOperand | undefined | null;
    onChange: (operand: NoCodeOperand | undefined | null) => void;
    allOperators: NoCodeOperatorDetailsDTO[];
    allElements: ElementWithParents[];
}

interface ResolvedParameter {
    parameter: NoCodeParameter;
    operand: NoCodeOperand | undefined | null;
}

export function NoCodeOperandEditor(props: NoCodeOperandEditorProps) {
    const {
        label,
        hint,
        operand,
        onChange,
        allOperators,
        allElements,
    } = props;

    const [showOperatorPicker, setShowOperatorPicker] = useState(false);
    const [showOperatorSwitcher, setShowOperatorSwitcher] = useState(false);
    const [showSelectElementDialog, setShowSelectElementDialog] = useState(false);
    const [showReorderParameters, setShowReorderParameters] = useState(false);

    const [showStaticValueHelp, setShowStaticValueHelp] = useState(false);
    const [showReferenceHelp, setShowReferenceHelp] = useState(false);
    const [showExpressionHelp, setShowExpressionHelp] = useState(false);

    const operator = isNoCodeExpression(operand) ? allOperators
        .find((op) => op.identifier === operand.operatorIdentifier) : undefined;

    const leadingParameter: ResolvedParameter | undefined = isNoCodeExpression(operand) && operator != null && operator.parameters.length > 1 ? {
        parameter: operator.parameters[0],
        operand: operand.operands?.[0],
    } : undefined;

    const trailingParameters: ResolvedParameter[] | undefined = isNoCodeExpression(operand) && operator != null && operator.parameters != null ? operator
        .parameters
        .slice(leadingParameter != null ? 1 : 0)
        .map((p, index) => ({
            parameter: p,
            operand: operand.operands?.[index + (leadingParameter != null ? 1 : 0)],
        })) : undefined;

    const combinedParameters = leadingParameter != null && trailingParameters != null
        ? [leadingParameter, ...trailingParameters]
        : trailingParameters;

    return (
        <>
            {
                operand == null &&
                <Box
                    sx={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: 2,
                    }}
                >
                    <Typography>
                        {label}
                    </Typography>

                    <ButtonGroup size="small">
                        <Button
                            startIcon={<Article />}
                            onClick={() => {
                                onChange({
                                    type: 'NoCodeStaticValue',
                                    value: '',
                                });
                            }}
                        >
                            Fester Wert
                        </Button>
                        <Button
                            onClick={() => setShowStaticValueHelp(true)}
                        >
                            <Help fontSize="small" />
                        </Button>
                    </ButtonGroup>

                    <ButtonGroup size="small">
                        <Button
                            startIcon={<MyLocation />}
                            onClick={() => {
                                onChange({
                                    type: 'NoCodeReference',
                                    elementId: '',
                                });
                            }}
                        >
                            Wert eines Elements
                        </Button>
                        <Button
                            onClick={() => setShowReferenceHelp(true)}
                        >
                            <Help fontSize="small" />
                        </Button>
                    </ButtonGroup>

                    {
                        hint != null &&
                        <Hint {...hint} />
                    }
                </Box>
            }

            {
                isNoCodeStaticValue(operand) &&
                <TextFieldComponent
                    label={`${label ?? ''} — (Fester Wert)`}
                    value={operand.value}
                    onChange={(val) => {
                        onChange({
                            ...operand,
                            value: val,
                        });
                    }}
                    startIcon={<Article />}
                    endAction={[
                        {
                            icon: <Delete />,
                            tooltip: 'Diesen festen Wert löschen',
                            onClick: () => {
                                onChange(undefined);
                            },
                        },
                        {
                            tooltip: 'Diesen festen Wert mit einem Ausdruck verknüpfen',
                            icon: <Functions />,
                            onClick: () => {
                                setShowOperatorPicker(true);
                            },
                        },
                    ]}
                    muiPassTroughProps={{
                        margin: 'none',
                    }}
                />
            }

            {
                isNoCodeReference(operand) &&
                <SelectFieldComponent
                    label={`${label ?? ''} — (Wert eines Elementes)`}
                    value={operand.elementId ?? ''}
                    onChange={(val) => {
                        onChange({
                            ...operand,
                            elementId: val,
                        });
                    }}
                    options={allElements.map((e) => ({
                        label: generateComponentTitle(e.element),
                        value: e.element.id,
                    }))}
                    startIcon={<MyLocation />}
                    endAction={[
                        {
                            tooltip: 'Diesen Verweis auf ein Element löschen',
                            icon: <Delete />,
                            onClick: () => {
                                onChange(undefined);
                            },
                        },
                        {
                            tooltip: 'Element für diesen Verweis auswählen',
                            icon: <DatabaseSearch />,
                            onClick: () => {
                                setShowSelectElementDialog(true);
                            },
                        },
                        {
                            tooltip: 'Diesen Verweis mit einem Ausdruck verknüpfen',
                            icon: <Functions />,
                            onClick: () => {
                                setShowOperatorPicker(true);
                            },
                        },
                    ]}
                    muiPassTroughProps={{
                        margin: 'none',
                    }}
                />
            }

            {
                isNoCodeExpression(operand) &&
                operator != null &&
                trailingParameters != null &&
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
                                    label={leadingParameter.parameter.label}
                                    hint={undefined}
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
                                label != null &&
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
                                        onChange(operand?.operands?.[0]);
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
                                        setShowExpressionHelp(true);
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
                                        label={p.parameter.label}
                                        hint={p.parameter.description != null ? {
                                            detailsTitle: `${operator.label} — ${p.parameter.label}`,
                                            summary: p.parameter.description,
                                            details: (
                                                <>
                                                    <Typography>
                                                        {p.parameter.description}
                                                    </Typography>

                                                    <Typography>
                                                        {operator.description}
                                                    </Typography>
                                                </>
                                            ),
                                        } : undefined}
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
            }

            <SelectOperatorDialog
                open={showOperatorPicker}
                operators={allOperators}
                onSelect={(op) => {
                    setShowOperatorPicker(false);
                    onChange({
                        type: 'NoCodeExpression',
                        operatorIdentifier: op.identifier,
                        operands: [
                            operand ?? null,
                        ],
                    });

                }}
                onClose={() => {
                    setShowOperatorPicker(false);
                }}
                desiredReturnType={NoCodeDataType.Runtime /* TODO */}
            />

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

            <SelectElementDialog
                allElements={allElements}
                open={showSelectElementDialog}
                onSelect={(element) => {
                    setShowSelectElementDialog(false);
                    if (isNoCodeReference(operand)) {
                        onChange({
                            ...operand,
                            elementId: element.id,
                        });
                    }
                }}
                onClose={() => setShowSelectElementDialog(false)}
            />

            <InfoDialog
                title="Fester Wert"
                severity="info"
                open={showStaticValueHelp}
                onClose={() => setShowStaticValueHelp(false)}
            >
                <Typography>
                    Ein „Fester Wert” ermöglicht es Ihnen, einen konstanten Wert direkt in den Ausdruck einzufügen.
                    Dies ist nützlich, wenn Sie mit unveränderlichen Daten arbeiten oder Standardwerte festlegen möchten.
                </Typography>
                <Typography>
                    Nachdem Sie den Wert festgelegt haben, können Sie diesen mit anderen Werten oder Ausdrücken kombinieren, um komplexere Berechnungen oder Logiken zu erstellen.
                </Typography>
            </InfoDialog>

            <InfoDialog
                title="Wert eines Elements"
                severity="info"
                open={showReferenceHelp}
                onClose={() => setShowReferenceHelp(false)}
            >
                <Typography gutterBottom>
                    Mit der Option „Wert eines Elements” können Sie auf Werte zugreifen, die in anderen Feldern eingegeben oder berechnet wurden.
                    Dies ist besonders nützlich, um Beziehungen zwischen verschiedenen Datenpunkten herzustellen und dynamische Ausdrücke zu erstellen.
                </Typography>
                <Typography gutterBottom>
                    Sie können nur Elemente auswählen, die sich im selben Kontext befinden und die oberhalb des aktuellen Elements liegen.
                </Typography>
                <Typography>
                    Nachdem Sie ein Element ausgewählt haben, können Sie dessen Wert in Ihrem Ausdruck verwenden und mit anderen Werten oder Ausdrücken kombinieren.
                </Typography>
            </InfoDialog>

            <OperatorInfoDialog
                operator={showExpressionHelp ? operator : undefined}
                onClose={() => setShowExpressionHelp(false)}
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
                    }
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
    const element = allElements
        .find(el => el.element.id === operand.elementId);

    return element != null
        ? generateComponentTitle(element.element)
        : `Unbekanntes Element (ID: ${operand.elementId})`;
}

function getExpressionOperandLabel(operand: NoCodeExpression, allOperators: NoCodeOperatorDetailsDTO[]): string {
    const operator = allOperators
        .find(op => op.identifier === operand.operatorIdentifier);

    if (operator == null) {
        return `Der Ausdruck mit dem unbekannten No-Code Operand (ID: ${operand.operatorIdentifier})`;
    }

    return operator.label;
}