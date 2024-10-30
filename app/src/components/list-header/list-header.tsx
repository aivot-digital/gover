import React from 'react';
import {Badge, Box, Button, IconButton, Tooltip, Typography} from '@mui/material';
import {SearchInput} from '../search-input/search-input';
import {type ListHeaderProps} from './list-header-props';
import {Link} from 'react-router-dom';
import HelpIconOutlined from '@mui/icons-material/HelpOutline';
import {HintTooltip} from '../hint-tooltip/hint-tooltip';

export function ListHeader(props: ListHeaderProps): JSX.Element {
    return (
        <Box
            sx={{
                display: 'flex',
                alignItems: 'center',
            }}
        >
            <Typography component={'h3'} variant={props.smallTitle === true ? 'h5' : 'h2'}>
                {props.title}
            </Typography>

            {
                props.hint != null &&
                <HintTooltip
                    arrow
                    placement="right"
                    title={
                        <>
                            {props.hint.text} <a
                            href={props.hint.moreLink}
                            target="_blank"
                            rel="noreferrer"
                        >
                            Mehr Informationen
                        </a>
                        </>
                    }
                >
                    <HelpIconOutlined
                        sx={{
                            ml: 1,
                            color: '#a6a6a6',
                            cursor: 'help',
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
                {
                    props.search != null &&
                    <SearchInput
                        value={props.search}
                        onChange={props.onSearchChange}
                        placeholder={props.searchPlaceholder}
                    />
                }

                {
                    props.actions?.map((act) => (
                        'label' in act ?
                            (act.badge != null ? (
                                <Badge {...act.badge} key={act.label}>
                                    <Button
                                        endIcon={act.icon}
                                        variant="contained"
                                        sx={{ml: 2}}
                                        component={'link' in act ? Link : ('href' in act ? 'a' : 'button')}
                                        to={'link' in act ? act.link : undefined}
                                        href={'href' in act ? act.href : undefined}
                                        target={'target' in act ? act.target : undefined}
                                        onClick={'onClick' in act ? act.onClick : undefined}
                                    >
                                        {act.label}
                                    </Button>
                                </Badge>
                            ) : (
                                <Button
                                    key={act.label}
                                    endIcon={act.icon}
                                    variant="contained"
                                    sx={{ml: 2}}
                                    component={'link' in act ? Link : ('href' in act ? 'a' : 'button')}
                                    to={'link' in act ? act.link : undefined}
                                    href={'href' in act ? act.href : undefined}
                                    target={'target' in act ? act.target : undefined}
                                    onClick={'onClick' in act ? act.onClick : undefined}
                                >
                                    {act.label}
                                </Button>
                            )) :
                            (
                                <Tooltip
                                    key={act.tooltip}
                                    title={act.tooltip}
                                >{
                                    act.badge != null ? (
                                        <Badge {...act.badge}>
                                            <IconButton
                                                sx={{ml: 2}}
                                                component={'link' in act ? Link : 'button'}
                                                to={'link' in act ? act.link : undefined}
                                                onClick={'onClick' in act ? act.onClick : undefined}
                                            >
                                                {act.icon}
                                            </IconButton>
                                        </Badge>
                                    ) : (
                                        <IconButton
                                            sx={{ml: 2}}
                                            component={'link' in act ? Link : 'button'}
                                            to={'link' in act ? act.link : undefined}
                                            onClick={'onClick' in act ? act.onClick : undefined}
                                        >
                                            {act.icon}
                                        </IconButton>
                                    )
                                }
                                </Tooltip>
                            )
                    ))
                }
            </Box>
        </Box>
    );
}

