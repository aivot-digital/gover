import {Box, Button, ButtonGroup, FormHelperText, Typography} from '@mui/material';
import {OperandTypeIcon} from './operand-type-icon';
import React from 'react';
import {NoCodeExpression, NoCodeOperand} from '../../../../models/functions/no-code-expression';

interface OperandTypeSelectorProps {
    expression: NoCodeExpression;
    onChange: (newOperand: NoCodeOperand | null) => void;
    optional: boolean;
    hint: string;
}

export function OperandTypeSelector(props: OperandTypeSelectorProps) {
    return (
        <Box
            sx={{
                display: 'flex',
                flexDirection: 'column',
                width: '100%',
                paddingY: 2,
            }}
        >
            <ButtonGroup
                fullWidth
            >
                <Button
                    variant="outlined"
                    sx={{
                        borderStyle: props.optional ? 'dashed' : 'solid',
                        opacity: props.optional ? 0.75 : 1,
                    }}
                    onClick={() => {
                        props.onChange({
                            operatorIdentifier: '',
                            operands: [],
                        });
                    }}
                    startIcon={OperandTypeIcon.exp}
                >
                    Ausdruck
                </Button>

                <Button
                    variant="outlined"
                    sx={{
                        borderStyle: props.optional ? 'dashed' : 'solid',
                        opacity: props.optional ? 0.75 : 1,
                    }}
                    onClick={() => {
                        props.onChange({
                            elementId: '',
                        });
                    }}
                    startIcon={OperandTypeIcon.reference}
                >
                    Referenz
                </Button>

                <Button
                    variant="outlined"
                    sx={{
                        borderStyle: props.optional ? 'dashed' : 'solid',
                        opacity: props.optional ? 0.75 : 1,
                    }}
                    onClick={() => {
                        props.onChange({
                            value: '',
                        });
                    }}
                    startIcon={OperandTypeIcon.value}
                >
                    Wert
                </Button>
            </ButtonGroup>

            <FormHelperText>
                {props.hint}
            </FormHelperText>
        </Box>
    );
}