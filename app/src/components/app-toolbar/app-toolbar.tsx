import React from 'react';
import {type AppToolbarProps} from './app-toolbar-props';
import {AppBar, Box, IconButton, Toolbar, Tooltip, Typography} from '@mui/material';
import {Link, useNavigate} from 'react-router-dom';
import HomeOutlinedIcon from '@mui/icons-material/HomeOutlined';
import ArrowBackOutlinedIcon from '@mui/icons-material/ArrowBackOutlined';
import {getPath} from '../../apps/staff-app-routes';

export function AppToolbar(props: AppToolbarProps) {
    const navigate = useNavigate();

    return (
        <>
            <AppBar position="fixed">
                <Toolbar color="primary">
                    <Tooltip title="Zur Startseite">
                        <IconButton
                            size="small"
                            edge="start"
                            component={Link}
                            to={getPath('applicationList')}
                            color="inherit"
                        >
                            <HomeOutlinedIcon/>
                        </IconButton>
                    </Tooltip>


                    <Tooltip title="Schritt zurück">
                        <IconButton
                            size="small"
                            edge="start"
                            onClick={() => {
                                navigate(-1);
                            }}
                            color="inherit"
                            sx={{
                                ml: 1,
                            }}
                        >
                            <ArrowBackOutlinedIcon/>
                        </IconButton>
                    </Tooltip>

                    <Typography
                        variant="h5"
                        component="div"
                        sx={{
                            flexGrow: 1,
                            ml: 2,
                        }}
                    >
                        {props.title}
                    </Typography>

                    <Box
                        sx={{
                            display: 'flex',
                            alignItems: 'center',
                        }}
                    >
                        {
                            props.actions?.map((action, index) => typeof action === 'string' ?
                                (
                                    <Box
                                        key={action}
                                        sx={{
                                            ml: 2,
                                            width: '1px',
                                            height: '2em',
                                            backgroundColor: 'white',
                                            opacity: '.25',
                                        }}
                                    >
                                    </Box>
                                ) :
                                (
                                    <Tooltip
                                        key={index}
                                        title={action.tooltip}
                                        arrow
                                    >
                                        <IconButton
                                            size="small"
                                            color="inherit"
                                            sx={{
                                                ml: 2,
                                            }}
                                            onClick={'onClick' in action ? action.onClick : undefined}
                                            component={'onClick' in action ? 'button' : 'a'}
                                            href={'href' in action ? action.href : undefined}
                                            target={'href' in action ? '_blank' : undefined}
                                        >
                                            {action.icon}
                                        </IconButton>
                                    </Tooltip>
                                ))
                        }
                    </Box>
                </Toolbar>
            </AppBar>
            {
                !(props.noPlaceholder ?? false) &&
                <Box
                    sx={{
                        height: '4rem',
                    }}
                />
            }
        </>
    );
}
