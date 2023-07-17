import React from 'react';
import {Box, Button, IconButton, styled, Tooltip, tooltipClasses, TooltipProps, Typography} from '@mui/material';
import {SearchInput} from '../search-input/search-input';
import {type ListHeaderProps} from './list-header-props';
import {Link} from 'react-router-dom';
import HelpIconOutlined from '@mui/icons-material/HelpOutline';

export function ListHeader({
                               title,
                               search,
                               searchPlaceholder,
                               onSearchChange,
                               actions,
                               hint,
                           }: ListHeaderProps): JSX.Element {
    return (
        <Box
            sx={{
                display: 'flex',
                alignItems: 'center',
            }}
        >
            <Typography variant="h2">
                {title}
            </Typography>

            {
                hint != null &&
                <HintTooltip
                    arrow
                    placement="right"
                    title={
                        <>
                            {hint.text} <a href={hint.moreLink}
                                           target="_blank"
                                           rel="noreferrer">
                            Mehr Informationen
                        </a>
                        </>
                    }
                >
                    <HelpIconOutlined
                        sx={{
                            ml: 1,
                        }}
                    />
                </HintTooltip>
            }

            <Box
                sx={{
                    display: 'flex',
                    ml: 'auto',
                }}
            >
                <SearchInput
                    value={search}
                    onChange={onSearchChange}
                    placeholder={searchPlaceholder}
                />

                {
                    actions.map((act) => (
                        'label' in act ?
                            (
                                <Button
                                    key={act.label}
                                    endIcon={act.icon}
                                    variant="contained"
                                    sx={{ml: 2}}
                                    component={'link' in act ? Link : 'button'}
                                    to={'link' in act ? act.link : undefined}
                                    onClick={'onClick' in act ? act.onClick : undefined}
                                >
                                    {act.label}
                                </Button>
                            ) :
                            (
                                <Tooltip
                                    key={act.tooltip}
                                    title={act.tooltip}
                                >
                                    <IconButton
                                        sx={{ml: 2}}
                                        component={'link' in act ? Link : 'button'}
                                        to={'link' in act ? act.link : undefined}
                                        onClick={'onClick' in act ? act.onClick : undefined}
                                    >
                                        {act.icon}

                                    </IconButton>
                                </Tooltip>
                            )
                    ))
                }
            </Box>
        </Box>
    );
}

const HintTooltip = styled(({ className, ...props }: TooltipProps) => (
    <Tooltip {...props} classes={{ popper: className }} />
))(({ theme }) => ({
    [`& .${tooltipClasses.tooltip}`]: {
        backgroundColor: '#fafafa',
        color: 'black',
        maxWidth: 220,
    },
}));
