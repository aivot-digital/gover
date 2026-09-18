import {Box, Button, Typography} from '@mui/material';
import {OperandTypeIcon} from './operand-type-icon';
import React from 'react';
import {NoCodeOperand} from '../../../../models/functions/no-code-expression';
import {Hint} from '../../../hint/hint';

interface OperandTypeSelectorProps {
    label?: string;
    onChange: (newOperand: NoCodeOperand | null) => void;
    optional: boolean;
    hint: string | undefined | null;
}

export function OperandTypeSelector(props: OperandTypeSelectorProps) {
    const {
        label,
        onChange,
        optional,
        hint,
    } = props;

    return (
        <Box
            sx={{
                display: 'flex',
                flexDirection: 'row',
                alignItems: 'center',
                width: '100%',
                gap: 2,
                pl: 1,
                py: 0.5,
            }}
        >
            {
                label != null &&
                <Typography
                    variant="caption"
                    color="textSecondary"
                    sx={{
                        width: '96px',
                        textOverflow: 'ellipsis',
                        overflow: 'hidden',
                        whiteSpace: 'nowrap',
                    }}
                >
                    {label}&nbsp;*
                </Typography>
            }

            <Button
                variant="outlined"
                size="small"
                sx={{
                    borderStyle: optional ? 'dashed' : 'solid',
                    opacity: optional ? 0.75 : 1,
                }}
                onClick={() => {
                    onChange({
                        type: 'NoCodeExpression',
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
                size="small"
                sx={{
                    borderStyle: optional ? 'dashed' : 'solid',
                    opacity: optional ? 0.75 : 1,
                }}
                onClick={() => {
                    onChange({
                        type: 'NoCodeReference',
                        elementId: '',
                    });
                }}
                startIcon={OperandTypeIcon.reference}
            >
                Referenz
            </Button>

            <Button
                variant="outlined"
                size="small"
                sx={{
                    borderStyle: optional ? 'dashed' : 'solid',
                    opacity: optional ? 0.75 : 1,
                }}
                onClick={() => {
                    onChange({
                        type: 'NoCodeStaticValue',
                        value: '',
                    });
                }}
                startIcon={OperandTypeIcon.value}
            >
                Wert
            </Button>

            {
                hint != null &&
                <Hint
                    label="Hilfe"
                    summary={hint}
                    detailsTitle={label ?? 'Hilfe'}
                    details={hint}
                />
            }
        </Box>
    );
}