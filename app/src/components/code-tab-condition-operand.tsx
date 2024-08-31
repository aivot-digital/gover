import {type AnyElement} from '../models/elements/any-element';
import {Box, IconButton, Tooltip} from '@mui/material';
import {generateComponentTitle} from '../utils/generate-component-title';
import React from 'react';
import {type ElementType} from '../data/element-type/element-type';
import {getConditionOperatorHint} from '../data/condition-operator';
import {SelectFieldComponent} from './select-field/select-field-component';
import {TextFieldComponent} from './text-field/text-field-component';
import CachedOutlinedIcon from '@mui/icons-material/CachedOutlined';

interface ReferenceProps {
    reference: string;
    onChange: (op: string) => void;
}

interface ValueTargetProps {
    value?: string;
    onChangeValue: (op: string) => void;

    target?: string;
    onChangeTarget: (op: string) => void;
}

type CodeTabConditionOperandProps = {
    allElements: AnyElement[];
    options?: string[];
    referenceType?: ElementType;
} & (ReferenceProps | ValueTargetProps);

export function CodeTabConditionOperand(props: CodeTabConditionOperandProps) {
    const helperText = getConditionOperatorHint(props.referenceType);

    return (
        <Box
            sx={{
                display: 'flex',
                alignItems: 'center',
            }}
        >
            {
                'reference' in props &&
                <SelectFieldComponent
                    label="Element-Referenz"
                    value={props.reference}
                    required
                    onChange={(val) => {props.onChange(val ?? '');}}
                    options={props.allElements.map((elem) => ({
                        label: generateComponentTitle(elem),
                        value: elem.id,
                    }))}
                />
            }

            {
                'target' in props &&
                <SelectFieldComponent
                    label="Element-Referenz"
                    value={props.target}
                    onChange={(val) => {props.onChangeTarget(val ?? '');}}
                    options={props.allElements.map((elem) => ({
                        label: generateComponentTitle(elem),
                        value: elem.id,
                    }))}
                />
            }

            {
                'value' in props &&
                props.options != null &&
                <SelectFieldComponent
                    label="Wert"
                    value={props.value}
                    onChange={(val) => {props.onChangeValue(val ?? '');}}
                    options={props.options.map((opt) => ({
                        label: opt,
                        value: opt,
                    }))}
                />
            }

            {
                'value' in props &&
                props.options == null &&
                <TextFieldComponent
                    label="Wert"
                    value={props.value}
                    onChange={(val) => {props.onChangeValue(val ?? '');}}
                    hint={helperText ?? undefined}
                />
            }

            {
                !('reference' in props) &&
                <Box sx={{ml: 1}}>
                    <Tooltip title={props.value == null ? 'In Wert ändern' : 'In Referenz ändern'}>
                        <IconButton
                            onClick={() => {
                                if (props.value != null) {
                                    props.onChangeTarget('');
                                } else {
                                    props.onChangeValue('');
                                }
                            }}
                        >
                            <CachedOutlinedIcon
                                fontSize={'small'}
                            />
                        </IconButton>
                    </Tooltip>
                </Box>
            }
        </Box>
    );
}
