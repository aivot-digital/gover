import {Box, Button, ButtonGroup, Typography} from '@mui/material';
import Article from '@aivot/mui-material-symbols-400-outlined/dist/article/Article';
import Help from '@aivot/mui-material-symbols-400-outlined/dist/help/Help';
import MyLocation from '@aivot/mui-material-symbols-400-outlined/dist/my-location/MyLocation';
import {NoCodeOperand} from '../../../models/functions/no-code-expression';
import {useState} from 'react';
import {InfoDialog} from '../../../dialogs/info-dialog/info-dialog';
import {HintTooltip} from '../../../components/hint-tooltip/hint-tooltip';
import {NoCodeDataType} from '../../../data/no-code-data-type';
import {SelectOperatorDialog} from '../../../dialogs/select-operator-dialog/select-operator-dialog';
import {NoCodeOperatorDetailsDTO} from '../../../models/dtos/no-code-operator-details-dto';

interface NoCodeOperandEditorSelectorProps {
    allOperators: NoCodeOperatorDetailsDTO[];
    label: string;
    hint?: string | null;
    onChange: (value: NoCodeOperand) => void;
    desiredType: NoCodeDataType;
}

export function NoCodeOperandEditorSelector(props: NoCodeOperandEditorSelectorProps) {
    const {
        allOperators,
        label,
        hint,
        onChange,
        desiredType,
    } = props;

    const [showStaticValueHelp, setShowStaticValueHelp] = useState(false);
    const [showReferenceHelp, setShowReferenceHelp] = useState(false);
    const [showExpressionHelp, setShowExpressionHelp] = useState(false);

    const [showOperatorSelector, setShowOperatorSelector] = useState(false);

    return (
        <>
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


                <ButtonGroup size="small">
                    <Button
                        startIcon={<MyLocation />}
                        onClick={() => {
                            setShowOperatorSelector(true);
                        }}
                    >
                        Ausdruck
                    </Button>
                    <Button
                        onClick={() => setShowExpressionHelp(true)}
                    >
                        <Help fontSize="small" />
                    </Button>
                </ButtonGroup>

                {
                    hint != null &&
                    <HintTooltip title={hint}>
                        <Help fontSize="small" />
                    </HintTooltip>
                }
            </Box>

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

            <InfoDialog
                title="Ausdruck"
                severity="info"
                open={showExpressionHelp}
                onClose={() => setShowExpressionHelp(false)}
            >
                <Typography gutterBottom>
                    Mit der Option „Ausdruck” können Sie einen benutzerdefinierten Ausdruck erstellen, der verschiedene Werte mit einem Operator kombiniert.
                </Typography>
            </InfoDialog>

            <SelectOperatorDialog
                open={showOperatorSelector}
                operators={allOperators}
                onSelect={(op) => {
                    setShowOperatorSelector(false);
                    onChange({
                        type: 'NoCodeExpression',
                        operatorIdentifier: op.identifier,
                        operands: null,
                    });
                }}
                onClose={() => {
                    setShowOperatorSelector(false);
                }}
                desiredReturnType={desiredType}
            />
        </>
    );
}