import {AnyElement} from "../../../../../../../models/elements/any-element";
import {Condition} from "../../../../../../../models/functions/conditions/condition";
import {Box, Button, IconButton, MenuItem, TextField, Tooltip, Typography} from "@mui/material";
import {FontAwesomeIcon} from "@fortawesome/react-fontawesome";
import {faRefresh, faTrashCanXmark} from "@fortawesome/pro-light-svg-icons";
import {
    ConditionOperator, ConditionOperatorHint, ConditionOperatorIsUnary,
    ConditionOperatorLabel,
} from "../../../../../../../data/condition-operator";
import React from "react";
import {CodeTabConditionOperand} from "./code-tab-condition-operand";
import {ElementType} from "../../../../../../../data/element-type/element-type";
import Evaluators from "../../../../../../../evaluators";
import {SelectFieldComponent} from "../../../../../../select-field/select-field-component";
import {stringOrDefault} from "../../../../../../../utils/string-utils";
import {generateComponentTitle} from "../../../../../../../utils/generate-component-title";
import {TextFieldComponent} from "../../../../../../text-field/text-field-component";

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

    const referencedElement = allElements.find(e => e.id === cond.reference);

    const evaluator = referencedElement != null ? Evaluators[referencedElement.type] : null;
    const availableOperators: ConditionOperator[] = evaluator ? Object.keys(evaluator) as unknown as ConditionOperator[] : [];

    let availableValueOptions = null;
    if (referencedElement != null && cond.value != null) {
        switch (referencedElement.type) {
            case ElementType.Radio:
            case ElementType.Select:
            case ElementType.MultiCheckbox:
                availableValueOptions = [...(referencedElement.options ?? [])];
                break;
            case ElementType.Checkbox:
                availableValueOptions = ['Ja (True)', 'Nein (False)'];
                break;
        }
    }

    const isUnaryOperator = cond.operator != null ? ConditionOperatorIsUnary[cond.operator] : true;
    const valueHelperText = referencedElement != null ? ConditionOperatorHint[referencedElement.type] : null;

    return (
        <Box>
            <Box sx={{display: 'flex', alignItems: 'center'}}>
                <IconButton
                    size="small"
                    color="error"
                    onClick={onDelete}
                >
                    <FontAwesomeIcon
                        icon={faTrashCanXmark}
                        size="sm"
                    />
                </IconButton>

                <Typography
                    variant="caption"
                    sx={{ml: 2}}
                >
                    {index + 1}. Bedingung
                </Typography>
            </Box>

            <Box sx={{display: 'flex'}}>
                <Box sx={{flex: 2}}>
                    <SelectFieldComponent
                        label="Element-Referenz"
                        required
                        value={cond.reference}
                        onChange={val => onChange({
                            ...cond,
                            reference: val ?? '',
                        })}
                        options={allElements.map(elem => ({
                            label: stringOrDefault(elem.name, generateComponentTitle(elem)),
                            value: elem.id,
                        }))}
                    />
                </Box>

                <Box sx={{flex: 1, mx: 2}}>
                    {
                        availableOperators != null &&
                        availableOperators.length > 0 &&
                        <SelectFieldComponent
                            label="Operator"
                            required
                            value={cond.operator?.toString() ?? ''}
                            onChange={val => onChange({
                                ...cond,
                                operator: val != null ? parseInt(val) as ConditionOperator : undefined,
                            })}
                            options={availableOperators.map(op => ({
                                value: op.toString(),
                                label: ConditionOperatorLabel[op]
                            }))}
                        />
                    }
                </Box>

                {
                    !isUnaryOperator &&
                    <Box sx={{flex: 2, display: 'flex', alignItems: 'center'}}>
                        {
                            availableOperators != null &&
                            availableOperators.length > 0 &&
                            <>
                                <Box sx={{flex: 1}}>
                                    {
                                        cond.value != null &&
                                        <TextFieldComponent
                                            label="Wert"
                                            required
                                            value={cond.value}
                                            onChange={val => onChange({
                                                ...cond,
                                                value: val ?? '',
                                            })}
                                            hint={valueHelperText ?? undefined}
                                        />
                                    }

                                    {
                                        cond.target != null &&
                                        <SelectFieldComponent
                                            label="Element-Referenz"
                                            required
                                            value={cond.target}
                                            onChange={val => onChange({
                                                ...cond,
                                                target: val ?? '',
                                            })}
                                            options={allElements.map(elem => ({
                                                value: elem.id,
                                                label: stringOrDefault(elem.name, generateComponentTitle(elem)),
                                            }))}
                                        />
                                    }
                                </Box>

                                <Box sx={{ml: 1}}>
                                    <Tooltip title={cond.value == null ? 'In Wert ändern' : 'In Referenz ändern'}>
                                        <IconButton
                                            onClick={() => onChange({
                                                ...cond,
                                                target: cond.value == null ? undefined : '',
                                                value: cond.value == null ? '' : undefined,
                                            })}
                                        >
                                            <FontAwesomeIcon
                                                size="sm"
                                                icon={faRefresh}
                                            />
                                        </IconButton>
                                    </Tooltip>
                                </Box>
                            </>
                        }
                    </Box>
                }
            </Box>
        </Box>
    );
}
