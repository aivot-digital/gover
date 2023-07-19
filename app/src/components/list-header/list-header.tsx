import React from 'react';
import {Box, Button, IconButton, styled, Tooltip, tooltipClasses, type TooltipProps, Typography} from '@mui/material';
import {SearchInput} from '../search-input/search-input';
import {type ListHeaderProps} from './list-header-props';
import {Link} from 'react-router-dom';
import HelpIconOutlined from '@mui/icons-material/HelpOutline';

export function ListHeader(props: ListHeaderProps): JSX.Element {
    return (
        <Box
            sx={{
                display: 'flex',
                alignItems: 'center',
            }}
        >
            <Typography variant={props.smallTitle === true ? 'h5' : 'h2'}>
                {props.title}
            </Typography>

            {
                props.hint != null &&
                <HintTooltip
                    arrow
                    placement="right"
                    title={
                        <>
                            {props.hint.text} <a href={props.hint.moreLink}
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
                            color: '#a6a6a6',
                            cursor: 'help'
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
                    value={props.search}
                    onChange={props.onSearchChange}
                    placeholder={props.searchPlaceholder}
                />

                {
                    props.actions?.map((act) => (
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
        backgroundColor: '#fff',
        color: '#444',
        maxWidth: 220,
        boxShadow: '0 4px 20px rgba(0, 0, 0, 0.05)',
        padding: '10px 12px',
        border: '1px solid #ccc',
    },
    [`& .${tooltipClasses.tooltip} a`]: {
        color: '#444',
        marginTop: '4px',
        display: 'block',
    },
    [`& .${tooltipClasses.arrow}`]: {
        color: '#fff',
    },
    [`& .${tooltipClasses.arrow}:before`]: {
        border: '1px solid #ccc',
    },
}));
