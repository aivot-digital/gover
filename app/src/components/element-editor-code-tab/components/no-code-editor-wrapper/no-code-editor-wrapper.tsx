import {useApi} from '../../../../hooks/use-api';
import React, {useEffect, useMemo, useState} from 'react';
import {NoCodeApiService} from '../../../../services/no-code-api-service';
import {NoCodeOperatorDetailsDTO} from '../../../../models/dtos/no-code-operator-details-dto';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {showErrorSnackbar} from '../../../../slices/snackbar-slice';
import {flattenElementsWithParents} from '../../../../utils/flatten-elements';
import {ElementType} from '../../../../data/element-type/element-type';
import {Box, CircularProgress, Typography} from '@mui/material';
import {isAnyInputElement} from '../../../../models/elements/form/input/any-input-element';
import {NoCodeOperand, NoCodeOperandError} from '../../../../models/functions/no-code-expression';
import {OperandEditor} from '../expression-editor/operand-editor';
import {FormLayoutElement as RootElement} from '../../../../models/elements/form-layout-element';
import {StepElement} from '../../../../models/elements/steps/step-element';
import {GroupLayout} from '../../../../models/elements/form/layout/group-layout';
import {ReplicatingContainerLayout} from '../../../../models/elements/form/layout/replicating-container-layout';
import {NoCodeDataType} from '../../../../data/no-code-data-type';
import {
    NoCodeOperandEditor,
    NoCodeOperandEditorContextType,
} from '../../../../modules/nocode/components/no-code-operand-editor';
import {PillNoCodeEditor} from '../../../../modules/nocode/components/pill-no-code-editor';
import {AnyElement} from '../../../../models/elements/any-element';

interface NoCodeEditorWrapperProps {
    parents: Array<RootElement | StepElement | GroupLayout | ReplicatingContainerLayout | AnyElement>;
    noCode: NoCodeOperand | null | undefined;
    onChange: (expression: NoCodeOperand | null | undefined) => void;
    editable: boolean;
    desiredReturnType: NoCodeDataType;
    hint?: string;
    error?: string;
    label?: string;
    contextType?: NoCodeOperandEditorContextType;
    operandError?: NoCodeOperandError;
}

const old_editor = localStorage.getItem('old_editor') != null;
// Opt-in preview of the token-based editor; both editors read and write the same NoCodeOperand JSON.
const pillNoCodeEditor = localStorage.getItem('pill-no-code') != null;
const CurrentNoCodeEditor = pillNoCodeEditor ? PillNoCodeEditor : NoCodeOperandEditor;

export function NoCodeEditorWrapper(props: NoCodeEditorWrapperProps) {
    const {
        parents,
        noCode,
        onChange,
        editable,
        hint,
        label,
        desiredReturnType,
        contextType = 'FORM',
        operandError,
    } = props;
    const useNewEditor = !old_editor || contextType === 'PROCESS';

    const api = useApi();
    const dispatch = useAppDispatch();

    const [operators, setOperators] = useState<NoCodeOperatorDetailsDTO[]>([]);
    const [isBusy, setIsBusy] = useState<boolean>(true);

    useEffect(() => {
        setIsBusy(true);
        new NoCodeApiService(api)
            .getNoCodeOperators()
            .then(setOperators)
            .catch(err => {
                console.error(err);
                dispatch(showErrorSnackbar('Operatoren für No-Code-Expressions konnten nicht geladen werden.'));
            })
            .finally(() => setIsBusy(false));
    }, []);

    const parent = useMemo(() => {
        for (const par of [...parents].reverse()) {
            if (par.type === ElementType.ReplicatingContainer) {
                return par;
            }
        }
        return parents[0];
    }, [parents]);

    const allElements = useMemo(() => {
        const allElements = flattenElementsWithParents(parent, [], true)
            .filter((e) => isAnyInputElement(e.element));

        // Check if the parent is a replicating list container. shift it to prevent selectability for children
        if (parent.type === ElementType.ReplicatingContainer) {
            allElements.shift();
        }

        return allElements;
    }, [parent]);

    return (
        <>
            {isBusy ? (
                <Box
                    sx={{
                        display: 'flex',
                        justifyContent: 'center',
                        alignItems: 'center',
                        height: '150px',
                        padding: 2,
                        borderRadius: 1,
                        border: '1px solid #e0e0e0',
                    }}
                >
                    <CircularProgress />
                    <Typography
                        variant="body1"
                        sx={{ml: 3}}
                    >
                        Daten werden geladen, bitte warten...
                    </Typography>
                </Box>
            ) : (
                <>
                    {
                        !useNewEditor &&
                        <OperandEditor
                            allElements={allElements}
                            allOperators={operators}
                            disabled={!editable}
                            operand={noCode}
                            onChange={(noCode) => {
                                onChange(noCode);
                            }}
                            isTopLevelOperand={true}
                            isLastOperand={true}
                            parameter={{
                                label: 'Ausdruck',
                                description: props.hint ?? '',
                                type: NoCodeDataType.Runtime,
                                options: [],
                            }}
                        />
                    }

                    {
                        useNewEditor &&
                        <Box
                            sx={{
                                pointerEvents: editable ? 'auto' : 'none',
                                opacity: editable ? 1 : 0.65,
                            }}
                        >
                            <CurrentNoCodeEditor
                                parameter={{
                                    label: label ?? '',
                                    description: hint ?? '',
                                    type: desiredReturnType,
                                    options: [],
                                }}
                                operand={noCode}
                                onChange={onChange}
                                allOperators={operators}
                                allElements={allElements}
                                contextType={contextType}
                                operandError={operandError}
                            />
                        </Box>
                    }

                </>
            )}
        </>
    );
}
