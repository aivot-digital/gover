import {type AnyElement} from '../models/elements/any-element';
import {type Condition} from '../models/functions/conditions/condition';
import {Box, IconButton, Tooltip, Typography} from '@mui/material';
import {type ConditionOperator, ConditionOperatorIsUnary, ConditionOperatorLabel, getConditionOperatorHint} from '../data/condition-operator';
import React from 'react';
import {ElementType} from '../data/element-type/element-type';
import Evaluators from '../evaluators';
import {SelectFieldComponent} from './select-field/select-field-component';
import {generateComponentTitle} from '../utils/generate-component-title';
import {TextFieldComponent} from './text-field/text-field-component';
import {NumberFieldComponent} from './number-field/number-field-component';
import CachedOutlinedIcon from '@mui/icons-material/CachedOutlined';
import DeleteForeverOutlinedIcon from '@mui/icons-material/DeleteForeverOutlined';
import {isStringArray} from '../utils/is-string-array';
import {ElementWithParents, generateElementNameWithParent} from '../utils/flatten-elements';

interface CodeTabConditionProps {
    allElements: ElementWithParents[];
    cond: Condition;
    index: number;
    onDelete: () => void;
    onChange: (cond: Condition) => void;
    editable: boolean;
}

export function CodeTabCondition({
                                     allElements,
                                     cond,
                                     index,
                                     onDelete,
                                     onChange,
                                     editable,
                                 }: CodeTabConditionProps): JSX.Element {
    const referencedElement = allElements.find((e) => e.element.id === cond.reference);

    const evaluator = referencedElement != null ? Evaluators[referencedElement.element.type] : null;
    const availableOperators: ConditionOperator[] = (evaluator != null) ? Object.keys(evaluator) as unknown as ConditionOperator[] : [];

    let availableValueOptions = null;
    if (referencedElement != null && cond.value != null) {
        switch (referencedElement.element.type) {
            case ElementType.Radio:
            case ElementType.Select:
            case ElementType.MultiCheckbox:
                availableValueOptions = [...(referencedElement.element.options ?? [])];
                break;
            case ElementType.Checkbox:
                availableValueOptions = ['Ja (True)', 'Nein (False)'];
                break;
        }
    }

    const isUnaryOperator = cond.operator != null ? ConditionOperatorIsUnary[cond.operator] : true;
    const valueHelperText = getConditionOperatorHint(referencedElement?.element.type, cond.operator);

    return (
        <Box>
            <Box
                sx={{
                    display: 'flex',
                    alignItems: 'center',
                }}
            >
                {
                    editable &&
                    <IconButton
                        size="small"
                        color="error"
                        onClick={onDelete}
                    >
                        <DeleteForeverOutlinedIcon
                            fontSize="small"
                        />
                    </IconButton>
                }

                <Typography
                    variant="caption"
                    sx={{
                        ml: 2,
                    }}
                >
                    {index + 1}. Bedingung
                </Typography>
            </Box>

            <Box
                sx={{
                    display: 'flex',
                }}
            >
                <Box
                    sx={{
                        flex: 2,
                        maxWidth: '300px',
                    }}
                >
                    <SelectFieldComponent
                        label="Element-Referenz"
                        required
                        value={cond.reference}
                        onChange={(val) => {
                            onChange({
                                ...cond,
                                reference: val ?? '',
                            });
                        }}
                        options={allElements.map(({element, parents}) => ({
                            label: generateComponentTitle(element),
                            subLabel: generateElementNameWithParent({element, parents}),
                            value: element.id,
                        }))}
                        disabled={!editable}
                    />
                </Box>

                <Box
                    sx={{
                        flex: 1,
                        mx: 2,
                    }}
                >
                    {
                        availableOperators != null &&
                        availableOperators.length > 0 &&
                        <SelectFieldComponent
                            label="Operator"
                            required
                            value={cond.operator?.toString() ?? ''}
                            onChange={(val) => {
                                onChange({
                                    ...cond,
                                    operator: val != null ? parseInt(val) as ConditionOperator : undefined,
                                });
                            }}
                            options={availableOperators.map((op) => ({
                                value: op.toString(),
                                label: ConditionOperatorLabel[op],
                            }))}
                            disabled={!editable}
                        />
                    }
                </Box>

                {
                    !isUnaryOperator &&
                    <Box
                        sx={{
                            flex: 2,
                            display: 'flex',
                            alignItems: 'center',
                        }}
                    >
                        {
                            availableOperators != null &&
                            availableOperators.length > 0 &&
                            <>
                                <Box sx={{flex: 1, maxWidth: '300px'}}>
                                    {
                                        cond.value != null &&
                                        (
                                            availableValueOptions === null ||
                                            availableValueOptions.length === 0
                                        ) &&
                                        referencedElement != null &&
                                        (
                                            referencedElement.element.type === ElementType.Number ||
                                            referencedElement.element.type === ElementType.ReplicatingContainer ?
                                                (
                                                    <NumberFieldComponent
                                                        label="Wert"
                                                        required
                                                        value={cond.value !== '' ? parseFloat(cond.value) : undefined}
                                                        onChange={(val) => {
                                                            onChange({
                                                                ...cond,
                                                                value: val != null ? val.toString() : '',
                                                            });
                                                        }}
                                                        decimalPlaces={referencedElement.element.type === ElementType.Number ? referencedElement.element.decimalPlaces : 0}
                                                        hint={valueHelperText ?? undefined}
                                                        disabled={!editable}
                                                    />
                                                ) :
                                                (
                                                    <TextFieldComponent
                                                        label="Wert"
                                                        required
                                                        value={cond.value}
                                                        onChange={(val) => {
                                                            onChange({
                                                                ...cond,
                                                                value: val ?? '',
                                                            });
                                                        }}
                                                        hint={valueHelperText ?? undefined}
                                                        disabled={!editable}
                                                    />
                                                )
                                        )
                                    }

                                    {
                                        cond.value != null &&
                                        (
                                            availableValueOptions !== null &&
                                            availableValueOptions.length > 0
                                        ) &&
                                        <SelectFieldComponent
                                            label="Wert"
                                            required
                                            value={cond.value}
                                            onChange={(val) => {
                                                onChange({
                                                    ...cond,
                                                    value: val ?? '',
                                                });
                                            }}
                                            hint={valueHelperText ?? undefined}
                                            options={
                                                isStringArray(availableValueOptions) ?
                                                    availableValueOptions.map((opt) => ({
                                                        label: opt,
                                                        value: opt,
                                                    })) :
                                                    availableValueOptions as any
                                            }
                                            disabled={!editable}
                                        />
                                    }

                                    {
                                        cond.target != null &&
                                        <SelectFieldComponent
                                            label="Element-Referenz"
                                            required
                                            value={cond.target}
                                            onChange={(val) => {
                                                onChange({
                                                    ...cond,
                                                    target: val ?? '',
                                                });
                                            }}
                                            options={
                                                allElements
                                                    .filter((elem) => elem.element.type === referencedElement?.element.type)
                                                    .map((elem) => ({
                                                        value: elem.element.id,
                                                        label: generateComponentTitle(elem.element),
                                                    }))
                                            }
                                            disabled={!editable}
                                        />
                                    }
                                </Box>

                                {
                                    editable &&
                                    <Box
                                        sx={{
                                            ml: 1,
                                        }}
                                    >
                                        <Tooltip title={cond.value == null ? 'In Wert ändern' : 'In Referenz ändern'}>
                                            <IconButton
                                                onClick={() => {
                                                    onChange({
                                                        ...cond,
                                                        target: cond.value == null ? undefined : '',
                                                        value: cond.value == null ? '' : undefined,
                                                    });
                                                }}
                                            >
                                                <CachedOutlinedIcon
                                                    fontSize={'small'}
                                                />
                                            </IconButton>
                                        </Tooltip>
                                    </Box>
                                }
                            </>
                        }
                    </Box>
                }
            </Box>
        </Box>
    );
}
