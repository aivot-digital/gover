import {useApi} from '../../../../hooks/use-api';
import React, {ReactNode, useEffect, useMemo, useState} from 'react';
import {NoCodeApiService} from '../../../../services/no-code-api-service';
import {NoCodeOperatorDetailsDTO} from '../../../../models/dtos/no-code-operator-details-dto';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {showErrorSnackbar} from '../../../../slices/snackbar-slice';
import {ExpressionEditorWrapperProps} from './expression-editor-wrapper-props';
import {ExpressionEditor} from '../expression-editor/expression-editor';
import {ElementWithParents, flattenElementsWithParents} from '../../../../utils/flatten-elements';
import {ElementType} from '../../../../data/element-type/element-type';
import {Alert, AlertTitle, Box, CircularProgress, Typography} from '@mui/material';
import {isAnyInputElement} from '../../../../models/elements/form/input/any-input-element';
import {isNoCodeExpression, isNoCodeReference, NoCodeExpression} from '../../../../models/functions/no-code-expression';
import ScienceOutlinedIcon from '@mui/icons-material/ScienceOutlined';
import {TextFieldComponent} from '../../../text-field/text-field-component';
import {generateComponentTitle} from '../../../../utils/generate-component-title';
import {NumberFieldComponent} from '../../../number-field/number-field-component';
import {DateFieldComponent} from '../../../date-field/date-field-component';
import {DateFieldComponentModelMode} from '../../../../models/elements/form/input/date-field-element';
import {CheckboxFieldComponent} from '../../../checkbox-field/checkbox-field-component';
import {SelectFieldComponent} from '../../../select-field/select-field-component';
import {SelectFieldElementOption} from '../../../../models/elements/form/input/select-field-element';
import {SelectFieldComponentOption} from '../../../select-field/select-field-component-option';
import {RadioFieldComponent} from '../../../radio-field/radio-field-component';
import {RadioFieldElementOption} from '../../../../models/elements/form/input/radio-field-element';
import {TimeFieldComponent} from '../../../time-field/time-field-component';
import {MultiCheckboxComponent} from '../../../multi-checkbox-field/multi-checkbox-component';
import {MultiCheckboxFieldElementOption} from '../../../../models/elements/form/input/multi-checkbox-field-element';
import {FileUploadComponent} from '../../../file-upload-field/file-upload-component';
import {Collapse} from '../../../collapse/collapse';
import {TableFieldComponent} from '../../../table-field/table-field-component';

function extractReferencedElementIds(expression: NoCodeExpression): string[] {
    const ids: string[] = [];

    for (const op of expression.operands) {
        if (isNoCodeReference(op)) {
            if (!ids.includes(op.elementId)) {
                ids.push(op.elementId);
            }
        } else if (isNoCodeExpression(op)) {
            extractReferencedElementIds(op).forEach((id) => {
                if (!ids.includes(id)) {
                    ids.push(id);
                }
            });
        }
    }

    return ids;
}

function generateElementForElementId(allElements: ElementWithParents[], values: Record<string, any>, onChange: (id: string, value: any) => void, elementId: string): ReactNode {

    const element = allElements.find((e) => e.element.id === elementId);
    if (element != null) {
        switch (element.element.type) {
            case ElementType.Text:
                return (
                    <TextFieldComponent
                        label={generateComponentTitle(element.element)}
                        value={values[element.element.id]}
                        onChange={(value) => {
                            onChange(element.element.id, value);
                        }}
                    />
                );
            case ElementType.Number:
                return (
                    <NumberFieldComponent
                        label={generateComponentTitle(element.element)}
                        value={values[element.element.id]}
                        onChange={(value) => {
                            onChange(element.element.id, value);
                        }}
                    />
                );
            case ElementType.Date:
                return (
                    <DateFieldComponent
                        label={generateComponentTitle(element.element)}
                        mode={element.element.mode ?? DateFieldComponentModelMode.Day}
                        value={values[element.element.id]}
                        onChange={(value) => {
                            onChange(element.element.id, value);
                        }}
                    />
                );
            case ElementType.Checkbox:
                return (
                    <CheckboxFieldComponent
                        label={generateComponentTitle(element.element)}
                        value={values[element.element.id]}
                        onChange={(value) => {
                            onChange(element.element.id, value);
                        }}
                    />
                );
            case ElementType.Select:
                let options: SelectFieldComponentOption[] = [];

                if (element.element.options != null) {
                    options = element.element.options.map((o: SelectFieldElementOption | string) => {
                        if (typeof o === 'string') {
                            return {
                                label: o,
                                value: o,
                            };
                        }
                        return {
                            label: o.label,
                            value: o.value,
                        };
                    });
                }

                return (
                    <SelectFieldComponent
                        label={generateComponentTitle(element.element)}
                        options={options}
                        value={values[element.element.id]}
                        onChange={(value) => {
                            onChange(element.element.id, value);
                        }}
                    />
                );
            case ElementType.Radio:
                let radioOptions: SelectFieldComponentOption[] = [];

                if (element.element.options != null) {
                    radioOptions = element.element.options.map((o: RadioFieldElementOption | string) => {
                        if (typeof o === 'string') {
                            return {
                                label: o,
                                value: o,
                            };
                        }
                        return {
                            label: o.label,
                            value: o.value,
                        };
                    });
                }

                return (
                    <RadioFieldComponent
                        label={generateComponentTitle(element.element)}
                        options={radioOptions}
                        value={values[element.element.id]}
                        onChange={(value) => {
                            onChange(element.element.id, value);
                        }}
                    />
                );
            case ElementType.FileUpload:
                return (
                    <FileUploadComponent
                        id={element.element.id}
                        label={generateComponentTitle(element.element)}
                        value={values[element.element.id]}
                        onChange={(value) => {
                            onChange(element.element.id, value);
                        }}
                    />
                );
            case ElementType.ReplicatingContainer:
                return (
                    <NumberFieldComponent
                        label={`Anzahl Datensätze ${generateComponentTitle(element.element)}`}
                        value={(values[element.element.id] ?? []).length}
                        onChange={(value) => {
                            if (value == null) {
                                onChange(element.element.id, undefined);
                            } else {
                                onChange(element.element.id, new Array(value).fill(''));
                            }
                        }}
                    />
                );
            case ElementType.Table:
                return (
                    <TableFieldComponent
                        label={generateComponentTitle(element.element)}
                        fields={element.element.fields ?? []}
                        value={values[element.element.id]}
                        onChange={(value) => {
                            onChange(element.element.id, value);
                        }}
                    />
                );
            case ElementType.MultiCheckbox:
                let multiCheckboxOptions: SelectFieldComponentOption[] = [];

                if (element.element.options != null) {
                    multiCheckboxOptions = element.element.options.map((o: MultiCheckboxFieldElementOption | string) => {
                        if (typeof o === 'string') {
                            return {
                                label: o,
                                value: o,
                            };
                        }
                        return {
                            label: o.label,
                            value: o.value,
                        };
                    });
                }

                return (
                    <MultiCheckboxComponent
                        label={generateComponentTitle(element.element)}
                        options={multiCheckboxOptions}
                        value={values[element.element.id]}
                        onChange={(value) => {
                            onChange(element.element.id, value);
                        }}
                    />
                );
            case ElementType.Time:
                return (
                    <TimeFieldComponent
                        label={generateComponentTitle(element.element)}
                        value={values[element.element.id]}
                        onChange={(value) => {
                            onChange(element.element.id, value);
                        }}
                    />
                );
        }
    }

    return null;
}

export function ExpressionEditorWrapper(props: ExpressionEditorWrapperProps) {
    const api = useApi();
    const dispatch = useAppDispatch();

    const [operators, setOperators] = useState<NoCodeOperatorDetailsDTO[]>([]);
    const [testPayload, setTestPayload] = useState<Record<string, any>>({});
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
        for (const par of [...props.parents].reverse()) {
            if (par.type === ElementType.ReplicatingContainer) {
                return par;
            }
        }
        return props.parents[0];
    }, [props.parents]);

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
        return extractReferencedElementIds(props.expression);
    }, [props.expression]);

    const hasTestDataValues = useMemo(() => {
        return referencedElementIds.some(id => testPayload[id] != null && testPayload[id] !== '');
    }, [referencedElementIds, testPayload]);

    const testDataElements = useMemo(() => {
        return referencedElementIds
            .map(id => generateElementForElementId(
                allElements,
                testPayload,
                (id, value) => {
                    setTestPayload({
                        ...testPayload,
                        [id]: value,
                    });
                },
                id,
            ))
            .filter(e => e != null);
    }, [allElements, testPayload, setTestPayload, referencedElementIds]);

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
                <Box sx={{
                    display: "flex",
                    justifyContent: "center",
                    alignItems: "center",
                    height: "150px",
                    padding: 2,
                    borderRadius: 1,
                    border: '1px solid #e0e0e0',
                }}>
                    <CircularProgress />
                    <Typography variant="body1" sx={{ ml: 3 }}>
                        Daten werden geladen, bitte warten...
                    </Typography>
                </Box>
            ) : (
                <>
                    <ExpressionEditor
                        allOperators={operators}
                        expression={props.expression}
                        onChange={props.onChange}
                        onTest={handleTest}
                        testedExpression={testResult?.expression}
                        editable={isBusyTesting ? false : props.editable}
                        allElements={allElements}
                        desiredReturnType={props.desiredReturnType}
                        hint={props.hint}
                        error={props.error}
                        label={props.label}
                    />

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
                        testDataElements.length > 0 &&
                        <Collapse
                            label="Testdaten"
                            openTooltip="Testdaten anzeigen"
                            closeTooltip="Testdaten verbergen"
                            badge={hasTestDataValues}
                        >
                            {testDataElements}
                        </Collapse>
                    }
                </>
            )}
        </>
    );
}
