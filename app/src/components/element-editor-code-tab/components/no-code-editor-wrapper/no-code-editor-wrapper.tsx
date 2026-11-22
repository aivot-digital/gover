import {useApi} from '../../../../hooks/use-api';
import React, {useEffect, useMemo, useState} from 'react';
import {NoCodeApiService} from '../../../../services/no-code-api-service';
import {NoCodeOperatorDetailsDTO} from '../../../../models/dtos/no-code-operator-details-dto';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {showErrorSnackbar} from '../../../../slices/snackbar-slice';
import {Alert, Box, Button, CircularProgress, Typography} from '@mui/material';
import {NoCodeOperand, NoCodeOperandError} from '../../../../models/functions/no-code-expression';
import {FormLayoutElement as RootElement} from '../../../../models/elements/form-layout-element';
import {StepElement} from '../../../../models/elements/steps/step-element';
import {GroupLayout} from '../../../../models/elements/form/layout/group-layout';
import {ReplicatingContainerLayout} from '../../../../models/elements/form/layout/replicating-container-layout';
import {NoCodeDataType} from '../../../../data/no-code-data-type';
import {
    NoCodeOperandEditorContextType,
} from '../../../../modules/nocode/components/no-code-operand-editor';
import {NoCodeEditor} from '../../../../modules/nocode/components/no-code-editor';
import {AnyElement} from '../../../../models/elements/any-element';

import {noCodeEditorElements} from '../../../../modules/nocode/utils/no-code-editor-elements';
import {validateNoCode} from '../../../../modules/nocode/utils/validate-no-code';

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
    required?: boolean;
    validationRequest?: number;
    focusOnValidation?: boolean;
    toolbarContainer?: HTMLElement | null;
    emptyDescription?: string;
    scrollContainerRef?: React.RefObject<HTMLDivElement | null>;
    onAvailabilityChange?: (available: boolean) => void;
    onValidityChange?: (valid: boolean) => void;
}

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

    const api = useApi();
    const dispatch = useAppDispatch();

    const [operators, setOperators] = useState<NoCodeOperatorDetailsDTO[]>([]);
    const [isBusy, setIsBusy] = useState<boolean>(true);
    const [loadFailed, setLoadFailed] = useState(false);
    const [loadAttempt, setLoadAttempt] = useState(0);

    useEffect(() => {
        let active = true;
        setIsBusy(true);
        setLoadFailed(false);
        new NoCodeApiService(api)
            .getNoCodeOperators()
            .then((loaded) => {
                if (active) setOperators(loaded);
            })
            .catch(err => {
                if (!active) return;
                setLoadFailed(true);
                console.error(err);
                dispatch(showErrorSnackbar('Funktionen für No-Code-Ausdrücke konnten nicht geladen werden.'));
            })
            .finally(() => {
                if (active) setIsBusy(false);
            });
        return () => {
            active = false;
        };
    }, [loadAttempt]);

    const allElements = useMemo(() => noCodeEditorElements(parents), [parents]);

    const validation = useMemo(() => validateNoCode(noCode, operators, allElements, props.required ?? false),
        [noCode, operators, allElements, props.required]);
    useEffect(() => {
        props.onAvailabilityChange?.(!isBusy && !loadFailed);
    }, [isBusy, loadFailed, props.onAvailabilityChange]);
    const valid = !isBusy && !loadFailed && validation.isValid;
    useEffect(() => {
        props.onValidityChange?.(valid);
    }, [valid, validation, props.onValidityChange]);

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
                    <CircularProgress/>
                    <Typography
                        variant="body1"
                        sx={{ml: 3}}
                    >
                        Daten werden geladen, bitte warten...
                    </Typography>
                </Box>
            ) : loadFailed ? <Alert severity="error"
                                    action={<Button onClick={() => setLoadAttempt((attempt) => attempt + 1)}>Erneut
                                                                                                             laden</Button>}>
                Die Funktionen konnten nicht geladen werden. Der Ausdruck kann noch nicht geprüft werden.
            </Alert> : (
                <>
                    <NoCodeEditor
                        validationRequest={props.validationRequest}
                        focusOnValidation={props.focusOnValidation}
                        toolbarContainer={props.toolbarContainer}
                        emptyDescription={props.emptyDescription}
                        scrollContainerRef={props.scrollContainerRef}
                        parameter={{
                            label: label ?? '',
                            description: hint ?? '',
                            type: desiredReturnType,
                            options: [],
                        }}
                        operand={noCode}
                        readOnly={!editable}
                        onChange={(value) => {
                            if (editable) onChange(value);
                        }}
                        allOperators={operators}
                        allElements={allElements}
                        contextType={contextType}
                        operandError={!validation.isValid ? validation : operandError ?? validation}
                    />
                </>
            )}
        </>
    );
}
