import {NoCodeExpression} from '../../../../models/functions/no-code-expression';
import {NoCodeOperatorDetailsDTO} from '../../../../models/dtos/no-code-operator-details-dto';
import {Box, Tooltip, Typography} from '@mui/material';
import {Actions} from '../../../actions/actions';
import SwapHorizIcon from '@mui/icons-material/SwapHoriz';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import SwitchAccessShortcutAddIcon from '@mui/icons-material/SwitchAccessShortcutAdd';
import ScienceOutlinedIcon from '@mui/icons-material/ScienceOutlined';
import HelpOutlineOutlinedIcon from '@mui/icons-material/HelpOutlineOutlined';
import React from 'react';

interface ExpressionHeaderProps {
    expression: NoCodeExpression;
    operator: NoCodeOperatorDetailsDTO;
    onChange: (expression: NoCodeExpression) => void;
    onShowSelect: () => void;
    onAddAbove: () => void;
    onShowInfo: () => void;
    testedExpression?: NoCodeExpression;
    onTest: () => void;
}

export function ExpressionHeader(props: ExpressionHeaderProps) {
    return (
        <Box
            sx={{
                display: 'flex',
                alignItems: 'center',
            }}
        >
            <Typography>
                {props.operator.label}
            </Typography>

            {
                props.testedExpression === props.expression &&
                <Tooltip
                    title="Das Testergebnis bezieht sich auf diesen Ausdruck."
                    placement="right"
                >
                    <ScienceOutlinedIcon
                        sx={{
                            marginLeft: 1,
                        }}
                        color="primary"
                    />
                </Tooltip>
            }

            <Actions
                sx={{
                    ml: 'auto',
                }}
                actions={[
                    {
                        icon: <ScienceOutlinedIcon />,
                        tooltip: 'Ausdruck testen',
                        onClick: props.onTest,
                    },
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
                        icon: <DeleteOutlineIcon />,
                        tooltip: 'Operator löschen',
                        onClick: () => {
                            props.onChange({
                                ...props.expression,
                                operatorIdentifier: '',
                                operands: [],
                            });
                        },
                    },
                ]}
            />
        </Box>
    );
}