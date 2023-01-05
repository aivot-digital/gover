import React from 'react';
import {AppToolbarProps} from './app-toolbar-props';
import {AppBar, Box, IconButton, Toolbar, Tooltip, Typography} from '@mui/material';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {faArrowLeft} from '@fortawesome/pro-light-svg-icons';
import {Link} from 'react-router-dom';

export function AppToolbar({title, parentPath, actions, noPlaceholder}: AppToolbarProps) {
    return (
        <>
            <AppBar position="fixed">
                <Toolbar color="primary">
                    <IconButton
                        size="small"
                        edge="start"
                        component={Link}
                        to={parentPath}
                        color="inherit"
                    >
                        <FontAwesomeIcon icon={faArrowLeft}/>
                    </IconButton>
                    <Typography
                        variant="h6"
                        component="div"
                        sx={{
                            flexGrow: 1,
                            ml: 2,
                        }}
                    >
                        {title}
                    </Typography>

                    <Box sx={{display: 'flex', alignItems: 'center'}}>
                        {
                            actions?.map((action, index) => typeof action === 'string' ? (
                                <Box
                                    key={action}
                                    sx={{
                                        ml: 2,
                                        width: '1px',
                                        height: '2em',
                                        backgroundColor: 'white',
                                    }}
                                >
                                </Box>
                            ) : (
                                <Tooltip
                                    key={index}
                                    title={action.tooltip}
                                    arrow
                                >
                                    <IconButton
                                        color="inherit"
                                        sx={{ml: 2}}
                                        onClick={'onClick' in action ? action.onClick : undefined}
                                        component={'onClick' in action ? 'button' : 'a'}
                                        href={'href' in action ? action.href : undefined}
                                        target={'href' in action ? '_blank' : undefined}
                                    >
                                        <FontAwesomeIcon
                                            icon={action.icon}
                                            fixedWidth
                                        />
                                    </IconButton>
                                </Tooltip>
                            ))
                        }
                    </Box>
                </Toolbar>
            </AppBar>
            {
                !noPlaceholder &&
                <Box sx={{height: '4rem'}}/>
            }
        </>
    );
}
