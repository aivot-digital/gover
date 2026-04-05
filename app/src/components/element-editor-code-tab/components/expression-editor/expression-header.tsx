import {NoCodeExpression} from '../../../../models/functions/no-code-expression';
import {NoCodeOperatorDetailsDTO} from '../../../../models/dtos/no-code-operator-details-dto';
import {Box, Typography} from '@mui/material';
import {Actions} from '../../../actions/actions';
import SwapHorizIcon from '@mui/icons-material/SwapHoriz';
import SwitchAccessShortcutAddIcon from '@mui/icons-material/SwitchAccessShortcutAdd';
import HelpOutlineOutlinedIcon from '@mui/icons-material/HelpOutlineOutlined';
import React from 'react';
import Functions from '@aivot/mui-material-symbols-400-outlined/dist/functions/Functions';
import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';

interface ExpressionHeaderProps {
    expression: NoCodeExpression;
    operator: NoCodeOperatorDetailsDTO;
    onChange: (expression: NoCodeExpression | null | undefined) => void;
    onShowSelect: () => void;
    onAddAbove: () => void;
    onShowInfo: () => void;
}

export function ExpressionHeader(props: ExpressionHeaderProps) {
    return (
        <Box
            sx={{
                display: 'flex',
                alignItems: 'center',
                gap: 1,
            }}
        >
            <Box
                sx={{
                    bgcolor: 'background.default',
                    py: 0.5,
                    px: 1,
                    borderRadius: 1,
                    display: 'flex',
                    alignItems: 'center',
                    gap: 0.5,
                }}
            >
                <Functions
                    style={{
                        fontSize: '90%',
                    }}
                />

                <Typography
                    fontSize="80%"
                >
                    {props.operator.label}
                </Typography>
            </Box>

            <Typography
                variant="caption"
                color="textSecondary"
            >
                {props.operator.abstractDescription}
            </Typography>

            <Actions
                sx={{
                    ml: 'auto',
                }}
                dense={true}
                actions={[
                    {
                        icon: <SwitchAccessShortcutAddIcon />,
                        tooltip: 'Ausdruck oberhalb einfügen',
                        onClick: props.onAddAbove,
                    },
                    {
                        icon: <HelpOutlineOutlinedIcon />,
                        tooltip: 'Informationen zum Operator',
                        onClick: props.onShowInfo,
                    },
                    {
                        icon: <SwapHorizIcon />,
                        tooltip: 'Operator tauschen',
                        onClick: props.onShowSelect,
                    },
                    {
                        icon: <Delete />,
                        tooltip: 'Operator löschen',
                        onClick: () => {
                            props.onChange(null);
                        },
                    },
                ]}
            />
        </Box>
    );
}