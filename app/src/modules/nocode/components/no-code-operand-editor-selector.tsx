import {Box, Button, ButtonGroup, Typography} from '@mui/material';
import Article from '@aivot/mui-material-symbols-400-outlined/dist/article/Article';
import DatabaseSearch from '@aivot/mui-material-symbols-400-outlined/dist/database-search/DatabaseSearch';
import Help from '@aivot/mui-material-symbols-400-outlined/dist/help/Help';
import MyLocation from '@aivot/mui-material-symbols-400-outlined/dist/my-location/MyLocation';
import {NoCodeOperand} from '../../../models/functions/no-code-expression';
import {useState} from 'react';
import {InfoDialog} from '../../../dialogs/info-dialog/info-dialog';
import {HintTooltip} from '../../../components/hint-tooltip/hint-tooltip';
import {NoCodeDataType} from '../../../data/no-code-data-type';
import {SelectOperatorDialog} from '../../../dialogs/select-operator-dialog/select-operator-dialog';
import {NoCodeOperatorDetailsDTO} from '../../../models/dtos/no-code-operator-details-dto';
import {NoCodeOperandEditorContextType} from './no-code-operand-editor';

interface NoCodeOperandEditorSelectorProps {
    allOperators: NoCodeOperatorDetailsDTO[];
    label: string;
    hint?: string | null;
    onChange: (value: NoCodeOperand) => void;
    contextType?: NoCodeOperandEditorContextType;
}

export function NoCodeOperandEditorSelector(props: NoCodeOperandEditorSelectorProps) {
    const {
        allOperators,
        label,
        hint,
        onChange,
        contextType = 'BOTH',
    } = props;

    const disableFormOptions = contextType === 'PROCESS';
    const disableProcessOptions = contextType === 'FORM';

    const [showStaticValueHelp, setShowStaticValueHelp] = useState(false);
    const [showReferenceHelp, setShowReferenceHelp] = useState(false);
    const [showProcessDataReferenceHelp, setShowProcessDataReferenceHelp] = useState(false);
    const [showInstanceDataReferenceHelp, setShowInstanceDataReferenceHelp] = useState(false);
    const [showNodeDataReferenceHelp, setShowNodeDataReferenceHelp] = useState(false);
    const [showExpressionHelp, setShowExpressionHelp] = useState(false);

    const [showOperatorSelector, setShowOperatorSelector] = useState(false);

    return (
        <>
            <Box
                sx={{
                    display: 'flex',
                    alignItems: 'center',
                    flexWrap: 'wrap',
                    gap: 2,
                }}
            >
                <Typography>
                    {label}
                </Typography>

                <ButtonGroup size="small">
                    <Button
                        startIcon={<Article/>}
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
                        <Help fontSize="small"/>
                    </Button>
                </ButtonGroup>

                {
                    !disableFormOptions &&
                    <ButtonGroup size="small">
                        <Button
                            startIcon={<MyLocation/>}
                            disabled={disableFormOptions}
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
                            disabled={disableFormOptions}
                            onClick={() => setShowReferenceHelp(true)}
                        >
                            <Help fontSize="small"/>
                        </Button>
                    </ButtonGroup>
                }

                {
                    !disableProcessOptions &&
                    <ButtonGroup size="small">
                        <Button
                            startIcon={<DatabaseSearch/>}
                            onClick={() => {
                                onChange({
                                    type: 'NoCodeProcessDataReference',
                                    path: '',
                                });
                            }}
                        >
                            Vorgangsdaten
                        </Button>
                        <Button
                            onClick={() => setShowProcessDataReferenceHelp(true)}
                        >
                            <Help fontSize="small"/>
                        </Button>
                    </ButtonGroup>
                }

                {
                    false &&
                    !disableProcessOptions &&
                    <ButtonGroup size="small">
                        <Button
                            startIcon={<DatabaseSearch/>}
                            onClick={() => {
                                onChange({
                                    type: 'NoCodeInstanceDataReference',
                                    path: '',
                                });
                            }}
                        >
                            Geschützte Vorgangsdaten
                        </Button>
                        <Button
                            onClick={() => setShowInstanceDataReferenceHelp(true)}
                        >
                            <Help fontSize="small"/>
                        </Button>
                    </ButtonGroup>
                }

                {
                    !disableProcessOptions &&
                    <ButtonGroup size="small">
                        <Button
                            startIcon={<DatabaseSearch/>}
                            onClick={() => {
                                onChange({
                                    type: 'NoCodeNodeDataReference',
                                    nodeDataKey: '',
                                    path: '',
                                });
                            }}
                        >
                            Elementdaten
                        </Button>
                        <Button
                            onClick={() => setShowNodeDataReferenceHelp(true)}
                        >
                            <Help fontSize="small"/>
                        </Button>
                    </ButtonGroup>
                }


                <ButtonGroup size="small">
                    <Button
                        startIcon={<MyLocation/>}
                        onClick={() => {
                            setShowOperatorSelector(true);
                        }}
                    >
                        Ausdruck
                    </Button>
                    <Button
                        onClick={() => setShowExpressionHelp(true)}
                    >
                        <Help fontSize="small"/>
                    </Button>
                </ButtonGroup>

                {
                    hint != null &&
                    <HintTooltip title={hint}>
                        <Help fontSize="small"/>
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
                    Dies ist nützlich, wenn Sie mit unveränderlichen Daten arbeiten oder Standardwerte festlegen
                    möchten.
                </Typography>
                <Typography>
                    Nachdem Sie den Wert festgelegt haben, können Sie diesen mit anderen Werten oder Ausdrücken
                    kombinieren, um komplexere Berechnungen oder Logiken zu erstellen.
                </Typography>
            </InfoDialog>

            <InfoDialog
                title="Wert eines Elements"
                severity="info"
                open={showReferenceHelp}
                onClose={() => setShowReferenceHelp(false)}
            >
                <Typography gutterBottom>
                    Mit der Option „Wert eines Elements” können Sie auf Werte zugreifen, die in anderen Feldern
                    eingegeben oder berechnet wurden.
                    Dies ist besonders nützlich, um Beziehungen zwischen verschiedenen Datenpunkten herzustellen und
                    dynamische Ausdrücke zu erstellen.
                </Typography>
                <Typography gutterBottom>
                    Sie können nur Elemente auswählen, die sich im selben Kontext befinden und die oberhalb des
                    aktuellen Elements liegen.
                </Typography>
                <Typography>
                    Nachdem Sie ein Element ausgewählt haben, können Sie dessen Wert in Ihrem Ausdruck verwenden und mit
                    anderen Werten oder Ausdrücken kombinieren.
                </Typography>
            </InfoDialog>

            <InfoDialog
                title="Vorgangsdaten"
                severity="info"
                open={showProcessDataReferenceHelp}
                onClose={() => setShowProcessDataReferenceHelp(false)}
            >
                <Typography gutterBottom>
                    Mit der Option „Vorgangsdaten” können Sie auf Daten aus dem Prozesskontext zugreifen.
                </Typography>
                <Typography gutterBottom>
                    Diese Option greift auf den Vorgangsdaten-Kontext (`$`) zu.
                </Typography>
                <Typography>
                    Über einen optionalen Pfad können Sie gezielt verschachtelte Werte auslesen.
                </Typography>
            </InfoDialog>

            <InfoDialog
                title="Geschützte Vorgangsdaten"
                severity="info"
                open={showInstanceDataReferenceHelp}
                onClose={() => setShowInstanceDataReferenceHelp(false)}
            >
                <Typography gutterBottom>
                    Diese Option greift auf Geschützte Vorgangsdaten (`$$`) zu.
                </Typography>
                <Typography>
                    Über einen optionalen Pfad können Sie gezielt verschachtelte Werte auslesen.
                </Typography>
            </InfoDialog>

            <InfoDialog
                title="Elementdaten"
                severity="info"
                open={showNodeDataReferenceHelp}
                onClose={() => setShowNodeDataReferenceHelp(false)}
            >
                <Typography gutterBottom>
                    Diese Option greift auf Elementdaten (`_`) zu.
                </Typography>
                <Typography>
                    Für Elementdaten muss zusätzlich der Datenschlüssel des Prozesselementes angegeben werden.
                </Typography>
            </InfoDialog>

            <InfoDialog
                title="Ausdruck"
                severity="info"
                open={showExpressionHelp}
                onClose={() => setShowExpressionHelp(false)}
            >
                <Typography gutterBottom>
                    Mit der Option „Ausdruck” können Sie einen benutzerdefinierten Ausdruck erstellen, der verschiedene
                    Werte mit einem Operator kombiniert.
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
                desiredReturnType={NoCodeDataType.Runtime}
            />
        </>
    );
}
