import {AnyElement} from "../../../../../../../models/elements/any-element";
import {Condition} from "../../../../../../../models/functions/conditions/condition";
import {Box, Button, MenuItem, TextField, Typography} from "@mui/material";
import {FontAwesomeIcon} from "@fortawesome/react-fontawesome";
import {faTrashCanXmark} from "@fortawesome/pro-light-svg-icons";
import {
    ConditionOperator,
    ConditionOperatorLabel,
    ConditionOperatorLimiter
} from "../../../../../../../data/condition-operator";
import React from "react";
import {CodeTabConditionOperand} from "./code-tab-condition-operand";
import {
    ConditionOperandReference,
    isConditionOperandReference
} from "../../../../../../../models/functions/conditions/condition-operand-reference";
import {ElementType} from "../../../../../../../data/element-type/element-type";
import {isConditionOperandValue} from "../../../../../../../models/functions/conditions/condition-operand-value";

interface CodeTabConditionProps {
    allElements: AnyElement[];
    cond: Condition;
    index: number;
    onDelete: () => void;
    onChange: (cond: Condition) => void;
}

export function CodeTabCondition({
                                     allElements,
                                     cond,
                                     index,
                                     onDelete,
                                     onChange
                                 }: CodeTabConditionProps) {

    let referencedOperandA = null;
    if (isConditionOperandReference(cond.operandA)) {
        const operand: ConditionOperandReference = cond.operandA;
        referencedOperandA = allElements.find(e => e.id === operand.id);
    }

    let referencedOperandB = null;
    if (isConditionOperandReference(cond.operandB)) {
        const operand: ConditionOperandReference = cond.operandB;
        referencedOperandB = allElements.find(e => e.id === operand.id);
    }

    let availableOperators: ConditionOperator[] = [];
    if (referencedOperandA == null && referencedOperandB == null) {
        availableOperators = ConditionOperatorLimiter[ElementType.Text];
    } else if (referencedOperandA != null && referencedOperandB != null) {
        const operatorsA = ConditionOperatorLimiter[referencedOperandA.type];
        const operatorsB = ConditionOperatorLimiter[referencedOperandB.type];
        availableOperators = operatorsA.filter(op => operatorsB.includes(op));
    } else if (referencedOperandA != null) {
        availableOperators = ConditionOperatorLimiter[referencedOperandA.type];
    } else if (referencedOperandB != null) {
        availableOperators = ConditionOperatorLimiter[referencedOperandB.type];
    }

    let availableValueOptions = null;
    if (referencedOperandA != null && isConditionOperandValue(cond.operandB)) {
        switch (referencedOperandA.type) {
            case ElementType.Radio:
            case ElementType.Select:
            case ElementType.MultiCheckbox:
                availableValueOptions = [...(referencedOperandA.options ?? [])];
                break;
            case ElementType.Checkbox:
                availableValueOptions = ['Ja', 'Nein'];
                break;
        }
    }
    if (referencedOperandB != null && isConditionOperandValue(cond.operandA)) {
        switch (referencedOperandB.type) {
            case ElementType.Radio:
            case ElementType.Select:
            case ElementType.MultiCheckbox:
                availableValueOptions = [...(referencedOperandB.options ?? [])];
                break;
            case ElementType.Checkbox:
                availableValueOptions = ['Ja', 'Nein'];
                break;
        }
    }

    return (
        <Box>
            <Box sx={{display: 'flex', justifyContent: 'space-between'}}>
                <Typography variant="caption">
                    {index + 1}. Bedingung
                </Typography>

                <Button
                    size="small"
                    color="error"
                    startIcon={
                        <FontAwesomeIcon icon={faTrashCanXmark}/>
                    }
                    onClick={onDelete}
                >
                    Bedingung löschen
                </Button>
            </Box>

            <Box sx={{display: 'flex'}}>
                <Box sx={{flex: 2}}>
                    <CodeTabConditionOperand
                        operand={cond.operandA}
                        allElements={allElements}
                        onChange={op => onChange({
                            ...cond,
                            operandA: op,
                        })}
                        options={availableValueOptions ?? undefined}
                        matchingType={referencedOperandB?.type}
                    />
                </Box>

                <Box sx={{flex: 1, mx: 2}}>
                    <TextField
                        select
                        label="Operator"
                        value={cond.operator}
                        onChange={event => onChange({
                            ...cond,
                            operator: parseInt(event.target.value),
                        })}
                    >
                        {
                            availableOperators.map(key => (
                                <MenuItem
                                    value={key}
                                    key={key}
                                >
                                    {ConditionOperatorLabel[key]}
                                </MenuItem>
                            ))
                        }
                    </TextField>
                </Box>

                <Box sx={{flex: 2}}>
                    <CodeTabConditionOperand
                        operand={cond.operandB}
                        allElements={allElements}
                        onChange={op => onChange({
                            ...cond,
                            operandB: op,
                        })}
                        options={availableValueOptions ?? undefined}
                        matchingType={referencedOperandA?.type}
                    />
                </Box>
            </Box>
        </Box>
    );
}
