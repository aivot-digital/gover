import React from 'react';
import {type AnyElement} from '../models/elements/any-element';
import {type ConditionSet} from '../models/functions/conditions/condition-set';
import {Box, Button, Divider, IconButton, MenuItem, TextField, Typography} from '@mui/material';
import {ConditionSetOperator} from '../data/condition-set-operator';
import {ConditionOperator} from '../data/condition-operator';
import {CodeTabCondition} from './code-tab-condition';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import DeleteForeverOutlinedIcon from '@mui/icons-material/DeleteForeverOutlined';
import {ElementWithParents} from '../utils/flatten-elements';

interface CodeTabConditionSetEditorProps {
    element: AnyElement;
    allElements: ElementWithParents[];
    conditionSet: ConditionSet;
    onChange: (cs: ConditionSet) => void;
    shouldReturnString: boolean;
    suppressConditionUnmetMessage?: boolean;
    editable: boolean;
}

export function CodeTabConditionSetEditor({
                                              element,
                                              allElements,
                                              conditionSet,
                                              onChange,
                                              shouldReturnString,
                                              suppressConditionUnmetMessage,
                                              editable,
                                          }: CodeTabConditionSetEditorProps): JSX.Element {
    return (
        <Box>
            <Typography>
                {
                    shouldReturnString ?
                        'Diese Funktion gibt einen Fehler zurück, wenn' :
                        'Diese Funktion ist wahr, wenn'
                }
            </Typography>

            <TextField
                select
                fullWidth
                value={conditionSet.operator}
                label="Bedingung"
                onChange={(event) => {
                    onChange({
                        ...conditionSet,
                        operator: parseInt(event.target.value) as ConditionSetOperator,
                        conditionSetUnmetMessage: undefined,
                    });
                }}
                disabled={!editable}
            >
                {
                    shouldReturnString ?
                        (
                            <MenuItem
                                value={ConditionSetOperator.Any}
                            >
                                <u>keine einzige</u>&nbsp;der folgenden Bedingungen wahr ist
                            </MenuItem>
                        ) :
                        (
                            <MenuItem
                                value={ConditionSetOperator.Any}
                            >
                                <u>mindestens eine</u>&nbsp;der folgenden Bedingungen wahr ist
                            </MenuItem>
                        )
                }

                {
                    shouldReturnString ?
                        (
                            <MenuItem
                                value={ConditionSetOperator.All}
                            >
                                <u>nicht alle</u>&nbsp;der folgenden Bedingungen wahr sind
                            </MenuItem>
                        ) :
                        (
                            <MenuItem
                                value={ConditionSetOperator.All}
                            >
                                <u>alle</u>&nbsp;der folgenden Bedingungen wahr sind
                            </MenuItem>
                        )
                }
            </TextField>

            {
                shouldReturnString &&
                conditionSet.operator === ConditionSetOperator.Any &&
                !suppressConditionUnmetMessage &&
                <TextField
                    fullWidth
                    label="Fehlermeldung"
                    value={conditionSet.conditionSetUnmetMessage}
                    onChange={(event) => {
                        onChange({
                            ...conditionSet,
                            conditionSetUnmetMessage: event.target.value,
                        });
                    }}
                    helperText="Dieser Fehler wird angezeigt, wenn die Bedingungsgruppe nicht wahr ist. Lassen Sie dieses Feld leer, um die Standardfehlermeldung anzuzeigen."
                />
            }
            <Box
                sx={{
                    pt: 2,
                    ml: 2,
                    borderLeft: '1px solid #e0e0e0',
                }}
            >
                <Box
                    sx={{
                        ml: 2,
                        p: 2,
                        border: '1px solid #e0e0e0',
                    }}
                >
                    {
                        editable &&
                        <Box
                            sx={{
                                display: 'flex',
                                justifyContent: 'flex-end',
                                mb: 2,
                            }}
                        >
                            <Button
                                startIcon={
                                    <AddOutlinedIcon/>
                                }
                                onClick={() => {
                                    onChange({
                                        ...conditionSet,
                                        conditions: [
                                            ...conditionSet.conditions ?? [],
                                            {
                                                operator: ConditionOperator.Equals,
                                                reference: '',
                                                value: '',
                                                conditionUnmetMessage: '',
                                            },
                                        ],
                                    });
                                }}
                            >
                                Bedingung hinzufügen
                            </Button>
                        </Box>
                    }

                    {
                        (conditionSet.conditions != null) &&
                        conditionSet.conditions.map((cond, index) => (
                            <Box
                                sx={{mb: 3}}
                                key={index}
                            >
                                <CodeTabCondition
                                    allElements={allElements}
                                    cond={cond}
                                    index={index}
                                    onDelete={() => {
                                        onChange({
                                            ...conditionSet,
                                            conditions: conditionSet.conditions!.filter((c) => c !== cond),
                                        });
                                    }}
                                    onChange={(updatedCond) => {
                                        onChange({
                                            ...conditionSet,
                                            conditions: conditionSet.conditions!.map((c) => c !== cond ? c : updatedCond),
                                        });
                                    }}
                                    editable={editable}
                                />
                                {
                                    shouldReturnString &&
                                    conditionSet.operator === ConditionSetOperator.All &&
                                    !suppressConditionUnmetMessage &&
                                    <TextField
                                        label="Fehlermeldung, wenn diese Bedingung nicht wahr ist"
                                        value={cond.conditionUnmetMessage}
                                        onChange={(event) => {
                                            onChange({
                                                ...conditionSet,
                                                conditions: conditionSet.conditions!.map((c) => c !== cond ?
                                                    c :
                                                    {
                                                        ...cond,
                                                        conditionUnmetMessage: event.target.value,
                                                    }),
                                            });
                                        }}
                                        helperText="Diese Fehlermeldung wird angezeigt, wenn die Bedingung nicht wahr ist. Lassen Sie dieses Feld leer, um die Standardfehlermeldung anzuzeigen."
                                        disabled={!editable}
                                    />
                                }
                            </Box>
                        ))
                    }

                    <Divider
                        sx={{
                            my: 2,
                        }}
                    />

                    {
                        editable &&
                        <Box
                            sx={{
                                display: 'flex',
                                justifyContent: 'flex-end',
                                mb: 2,
                            }}
                        >
                            <Button
                                onClick={() => {
                                    onChange({
                                        ...conditionSet,
                                        conditionsSets: [
                                            ...conditionSet.conditionsSets ?? [],
                                            {
                                                operator: ConditionSetOperator.All,
                                                conditions: [],
                                                conditionsSets: [],
                                                conditionSetUnmetMessage: '',
                                            },
                                        ],
                                    });
                                }}
                                startIcon={
                                    <AddOutlinedIcon/>
                                }
                            >
                                Bedingungsgruppe hinzufügen
                            </Button>
                        </Box>
                    }

                    {
                        (conditionSet.conditionsSets != null) &&
                        conditionSet.conditionsSets.map((cs, index) => (
                            <Box
                                sx={{mb: 3}}
                                key={index}
                            >
                                <Box
                                    sx={{
                                        display: 'flex',
                                        alignItems: 'center',
                                        mb: 2,
                                    }}
                                >
                                    {
                                        editable &&
                                        <IconButton
                                            size="small"
                                            color="error"
                                            onClick={() => {
                                                onChange({
                                                    ...conditionSet,
                                                    conditionsSets: conditionSet.conditionsSets!.filter((c) => c !== cs),
                                                });
                                            }}
                                        >
                                            <DeleteForeverOutlinedIcon
                                                fontSize="small"
                                            />
                                        </IconButton>
                                    }

                                    <Typography
                                        variant="caption"
                                        sx={{ml: 2}}
                                    >
                                        {index + 1}. Bedingungsgruppe
                                    </Typography>
                                </Box>

                                <CodeTabConditionSetEditor
                                    element={element}
                                    allElements={allElements}
                                    conditionSet={cs}
                                    onChange={(updatedCs) => {
                                        onChange({
                                            ...conditionSet,
                                            conditionsSets: conditionSet.conditionsSets!.map((c) => c !== cs ? c : updatedCs),
                                        });
                                    }}
                                    shouldReturnString={shouldReturnString}
                                    suppressConditionUnmetMessage={conditionSet.operator === ConditionSetOperator.Any}
                                    editable={editable}
                                />
                            </Box>
                        ))
                    }
                </Box>
            </Box>
        </Box>
    );
}

