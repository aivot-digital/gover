import {useApi} from '../../../../hooks/use-api';
import React, {useEffect, useMemo, useState} from 'react';
import {NoCodeApiService} from '../../../../services/no-code-api-service';
import {NoCodeOperatorDetailsDTO} from '../../../../models/dtos/no-code-operator-details-dto';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {showErrorSnackbar} from '../../../../slices/snackbar-slice';
import {flattenElementsWithParents} from '../../../../utils/flatten-elements';
import {ElementType} from '../../../../data/element-type/element-type';
import {Alert, AlertTitle, Box, CircularProgress, Typography} from '@mui/material';
import {isAnyInputElement} from '../../../../models/elements/form/input/any-input-element';
import {isNoCodeExpression, isNoCodeReference, NoCodeExpression, NoCodeOperand} from '../../../../models/functions/no-code-expression';
import ScienceOutlinedIcon from '@mui/icons-material/ScienceOutlined';
import {Collapse} from '../../../collapse/collapse';
import {ElementData} from '../../../../models/element-data';
import {ElementDerivationContext} from '../../../../modules/elements/components/element-derivation-context';
import {OperandEditor} from '../expression-editor/operand-editor';
import {RootElement} from '../../../../models/elements/root-element';
import {StepElement} from '../../../../models/elements/steps/step-element';
import {GroupLayout} from '../../../../models/elements/form/layout/group-layout';
import {ReplicatingContainerLayout} from '../../../../models/elements/form/layout/replicating-container-layout';
import {NoCodeDataType} from '../../../../data/no-code-data-type';
import {
    NoCodeOperandEditor,
    NoCodeOperandEditorContextType,
} from '../../../../modules/nocode/components/no-code-operand-editor';
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
}

const new_editor = localStorage.getItem('new_editor') != null;

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
    } = props;
    const useNewEditor = new_editor || contextType === 'PROCESS';

    const api = useApi();
    const dispatch = useAppDispatch();

    const [operators, setOperators] = useState<NoCodeOperatorDetailsDTO[]>([]);
    const [testPayload, setTestPayload] = useState<ElementData>({});
    const [testResult, setTestResult] = useState<{
        expression: NoCodeExpression;
        result: string;
        error: boolean;
    }>();

    const [isBusy, setIsBusy] = useState<boolean>(true);
    const [isBusyTesting, setIsBusyTesting] = useState<boolean>(false);

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

    const referencedElementIds = useMemo(() => {
        if (noCode == null) {
            return [];
        }
        return extractReferencedElementIds(noCode);
    }, [noCode]);

    const referencedElements = useMemo(() => {
        return referencedElementIds
            .map(id => allElements.find(e => e.element.id === id))
            .filter(e => e != null);
    }, [referencedElementIds, allElements]);


    const handleTest = async (expression: NoCodeExpression) => {
        setTestResult(undefined);
        setIsBusyTesting(true);

        const service = new NoCodeApiService(api);
        try {
            const result = await service.evaluateNoCode(expression, testPayload);
            setTestResult({
                expression,
                result: JSON.stringify(result.result, null, 2),
                error: false,
            });
        } catch (e: any) {
            setTestResult({
                expression,
                result: e.message,
                error: true,
            });
        } finally {
            setIsBusyTesting(false);
        }
    };

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
                            <NoCodeOperandEditor
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
                            />
                        </Box>
                    }

                    {
                        testResult != null && (
                            <Alert
                                severity={testResult.error ? 'error' : 'success'}
                                sx={{
                                    marginTop: 4,
                                }}
                                icon={<ScienceOutlinedIcon />}
                                onClose={() => setTestResult(undefined)}
                            >
                                <AlertTitle>
                                    Testergebnis
                                </AlertTitle>

                                {testResult.result}
                            </Alert>
                        )
                    }

                    {
                        false &&
                        referencedElements.length > 0 &&
                        <Collapse
                            label="Testdaten"
                            openTooltip="Testdaten anzeigen"
                            closeTooltip="Testdaten verbergen"
                        >
                            {
                                referencedElements.map((element) => (
                                    <ElementDerivationContext
                                        key={element?.element.id}
                                        element={element?.element}
                                        elementData={testPayload}
                                        onElementDataChange={(change) => {
                                            setTestPayload({
                                                ...testPayload,
                                                ...change,
                                            });
                                        }}
                                    />
                                ))
                            }
                        </Collapse>
                    }
                </>
            )}
        </>
    );
}

function extractReferencedElementIds(op: NoCodeOperand): string[] {
    const ids: string[] = [];

    if (isNoCodeReference(op)) {
        if (!ids.includes(op.elementId!)) {
            ids.push(op.elementId!);
        }
    } else if (isNoCodeExpression(op) && op.operands != null) {
        for (const c of op.operands) {
            if (c == null) {
                continue;
            }

            extractReferencedElementIds(c)
                .forEach((id) => {
                    if (!ids.includes(id)) {
                        ids.push(id);
                    }
                });
        }
    }

    return ids;
}
