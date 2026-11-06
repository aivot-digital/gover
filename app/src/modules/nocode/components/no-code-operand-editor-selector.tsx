import {Box, Button, ButtonGroup, Typography} from '@mui/material';
import Article from '@aivot/mui-material-symbols-400-n25-outlined/Article';
import DatabaseSearch from '@aivot/mui-material-symbols-400-n25-outlined/DatabaseSearch';
import Help from '@aivot/mui-material-symbols-400-n25-outlined/Help';
import MyLocation from '@aivot/mui-material-symbols-400-n25-outlined/MyLocation';
import {NoCodeOperand} from '../../../models/functions/no-code-expression';
import {useState} from 'react';
import {InfoDialog} from '../../../dialogs/info-dialog/info-dialog';
import {HintTooltip} from '../../../components/hint-tooltip/hint-tooltip';
import {NoCodeDataType} from '../../../data/no-code-data-type';
import {SelectOperatorDialog} from '../../../dialogs/select-operator-dialog/select-operator-dialog';
import {NoCodeOperatorDetailsDTO} from '../../../models/dtos/no-code-operator-details-dto';
import {NoCodeOperandEditorContextType} from './no-code-operand-editor';
import {NoCodeOperandKind, NoCodeOperandKindHelp} from '../data/no-code-operand-kind-help';

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

    const [help, setHelp] = useState<NoCodeOperandKind | null>(null);

    const [showOperatorSelector, setShowOperatorSelector] = useState(false);

    return (
        <>
            <Box
                data-no-code-selector
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
                        onClick={() => setHelp('NoCodeStaticValue')}
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
                            onClick={() => setHelp('NoCodeReference')}
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
                            onClick={() => setHelp('NoCodeProcessDataReference')}
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
                            onClick={() => setHelp('NoCodeInstanceDataReference')}
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
                            onClick={() => setHelp('NoCodeNodeDataReference')}
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
                        onClick={() => setHelp('NoCodeExpression')}
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

            {
                // One dialog per kind keeps the content while a dialog fades out.
                (Object.keys(NoCodeOperandKindHelp) as NoCodeOperandKind[]).map((kind) => (
                    <InfoDialog
                        key={kind}
                        title={NoCodeOperandKindHelp[kind].title}
                        severity="info"
                        open={help === kind}
                        onClose={() => setHelp(null)}
                    >
                        {NoCodeOperandKindHelp[kind].details}
                    </InfoDialog>
                ))
            }

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
