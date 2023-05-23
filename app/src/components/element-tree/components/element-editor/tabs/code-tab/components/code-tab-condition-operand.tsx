import {
    ConditionOperandReference,
    isConditionOperandReference
} from "../../../../../../../models/functions/conditions/condition-operand-reference";
import {
    ConditionOperandValue,
    isConditionOperandValue
} from "../../../../../../../models/functions/conditions/condition-operand-value";
import {AnyElement} from "../../../../../../../models/elements/any-element";
import {Box, IconButton, MenuItem, TextField, Tooltip} from "@mui/material";
import {stringOrDefault} from "../../../../../../../utils/string-utils";
import {generateComponentTitle} from "../../../../../../../utils/generate-component-title";
import {FontAwesomeIcon} from "@fortawesome/react-fontawesome";
import {faRefresh} from "@fortawesome/pro-light-svg-icons";
import React from "react";
import {ElementType} from "../../../../../../../data/element-type/element-type";

interface CodeTabConditionOperandProps {
    operand?: ConditionOperandReference | ConditionOperandValue;
    allElements: AnyElement[];
    onChange: (op: ConditionOperandReference | ConditionOperandValue) => void;
    options?: string[];
    matchingType?: ElementType;
}

export function CodeTabConditionOperand({operand, allElements, onChange, options, matchingType}: CodeTabConditionOperandProps) {
    let helperText = null;
    switch (matchingType) {
        case ElementType.Time:
            helperText = 'Bitte im Format HH:MM eingeben.';
            break;
        case ElementType.Date:
            helperText = 'Bitte im Format TT.MM.JJJJ eingeben.';
            break;
    }

    return (
        <Box
            sx={{
                display: 'flex',
                alignItems: 'center',
            }}
        >
            {
                (
                    operand == null ||
                    isConditionOperandReference(operand)
                ) &&
                <TextField
                    select
                    label="Element-Referenz"
                    value={operand?.id ?? ''}
                    onChange={event => onChange({
                        id: event.target.value,
                    })}
                >
                    {
                        allElements.map(elem => (
                            <MenuItem
                                key={elem.id}
                                value={elem.id}
                            >
                                {stringOrDefault(elem.name, generateComponentTitle(elem))}
                            </MenuItem>
                        ))
                    }
                </TextField>
            }

            {
                isConditionOperandValue(operand) &&
                <TextField
                    label="Wert"
                    select={options != null}
                    value={operand.value}
                    onChange={event => onChange({
                        value: event.target.value,
                    })}
                    helperText={helperText}
                >
                    {
                        options != null &&
                        options.map(opt => (
                            <MenuItem
                                key={opt}
                                value={opt}
                            >
                                {opt}
                            </MenuItem>
                        ))
                    }
                </TextField>
            }

            <Box sx={{ml: 1}}>
                <Tooltip title={operand == null || isConditionOperandReference(operand) ? 'In Wert ändern' : 'In Referenz ändern'}>
                    <IconButton
                        onClick={() => onChange(operand == null || isConditionOperandReference(operand) ? {value: ''} : {id: ''})}
                    >
                        <FontAwesomeIcon
                            size="sm"
                            icon={faRefresh}
                        />
                    </IconButton>
                </Tooltip>
            </Box>
        </Box>
    );
}
