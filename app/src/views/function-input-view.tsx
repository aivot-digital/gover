import {BaseViewProps} from "./base-view";
import {Box} from "@mui/material";
import {
    FunctionInputElement,
    FunctionInputElementItem,
    FunctionInputElementItemType,
    FunctionInputElementReturnType
} from "../models/elements/form/input/function-input-element";
import {FunctionSelector} from "../components/element-editor-code-tab/components/function-selector/function-selector";
import {CodeEditor} from "../components/code-editor/code-editor";
import {CodeTabConditionSetEditor} from "../components/code-tab-condition-set-editor";
import {ConditionSetOperator} from "../data/condition-set-operator";
import {OperandEditor} from "../components/element-editor-code-tab/components/expression-editor/operand-editor";
import {NoCodeOperand} from "../models/functions/no-code-expression";
import {NoCodeDataType} from "../data/no-code-data-type";

export function FunctionInputView(props: BaseViewProps<FunctionInputElement, FunctionInputElementItem>) {
    const {
        element,
        value,
        setValue,
    } = props;

    if (value?.type == null) {
        return (
            <FunctionSelector
                onSelectFunctionCode={() => {
                    setValue({
                        ...(value ?? {
                            type: null,
                            code: null,
                            conditionSet: null,
                            noCode: null,
                        }),
                        type: FunctionInputElementItemType.Javascript,
                    });
                }}
                onSelectNoCode={() => {
                    setValue({
                        ...(value ?? {
                            type: null,
                            code: null,
                            conditionSet: null,
                            noCode: null,
                        }),
                        type: FunctionInputElementItemType.ConditionSet,
                    });
                }}
                onSelectCloudCode={() => {
                    setValue({
                        ...(value ?? {
                            type: null,
                            code: null,
                            conditionSet: null,
                            noCode: null,
                        }),
                        type: FunctionInputElementItemType.Javascript,
                    });
                }}
                onSelectNoCodeExpression={() => {
                    setValue({
                        ...(value ?? {
                            type: null,
                            code: null,
                            conditionSet: null,
                            noCode: null,
                        }),
                        type: FunctionInputElementItemType.NoCode,
                    });
                }}
                allowNoCode={element.returnType === FunctionInputElementReturnType.BOOLEAN}
                allowExpression={true}
                fullWidth={true}
            />
        );
    }

    switch (value.type) {
        case FunctionInputElementItemType.Javascript:
            return (
                <Box>
                    <CodeEditor
                        value={value.code?.code ?? undefined}
                        onChange={(val) => {
                            setValue({
                                ...value,
                                code: {
                                    ...(value.code ?? {
                                        code: "",
                                    }),
                                    code: val,
                                },
                            });
                        }}
                        actions={[]}
                    />
                </Box>
            );
        case FunctionInputElementItemType.ConditionSet:
            return (
                <Box>
                    <CodeTabConditionSetEditor
                        element={element}
                        allElements={[]}
                        conditionSet={value.conditionSet ?? {
                            conditions: [],
                            conditionsSets: [],
                            operator: ConditionSetOperator.All
                        }}
                        onChange={(val) => {
                            setValue({
                                ...value,
                                conditionSet: val,
                            })
                        }}
                        shouldReturnString={false}
                        editable={true}
                    />
                </Box>
            );
        case FunctionInputElementItemType.NoCode:
            return (
                <Box>
                    <OperandEditor
                        allElements={[]}
                        allOperators={[]}
                        parameter={{
                            type: NoCodeDataType.Boolean,
                            label: 'Wert',
                            options: [],
                            description: null,
                        }}
                        operand={undefined}
                        onChange={function (operand: NoCodeOperand | null | undefined): void {
                            throw new Error("Function not implemented.");
                        }}
                        disabled={false}
                        isTopLevelOperand={false}
                        isLastOperand={false}
                    />
                </Box>
            );
    }
}