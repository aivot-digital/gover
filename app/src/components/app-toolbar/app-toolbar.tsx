import React from 'react';
import { type AppToolbarProps } from './app-toolbar-props';
import { AppBar, Box, IconButton, Toolbar, Tooltip, Typography } from '@mui/material';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faArrowLeft, faHome } from '@fortawesome/pro-light-svg-icons';
import { Link, useNavigate } from 'react-router-dom';
import { getPath } from '../../apps/staff-app-routes';

export function AppToolbar(props: AppToolbarProps): JSX.Element {
    const navigate = useNavigate();

    return (
        <>
            <AppBar position="fixed">
                <Toolbar color="primary">
                    <Tooltip title="Zur Startseite">
                        <IconButton
                            size="small"
                            edge="start"
                            component={ Link }
                            to={ getPath('applicationList') }
                            color="inherit"
                        >
                            <FontAwesomeIcon icon={ faHome }/>
                        </IconButton>
                    </Tooltip>


                    <Tooltip title="Schritt Zurück">
                        <IconButton
                            size="small"
                            edge="start"
                            onClick={ () => {
                                navigate(-1);
                            } }
                            color="inherit"
                            sx={ {
                                ml: 1,
                            } }
                        >
                            <FontAwesomeIcon icon={ faArrowLeft }/>
                        </IconButton>
                    </Tooltip>

                    <Typography
                        variant="h6"
                        component="div"
                        sx={ {
                            flexGrow: 1,
                            ml: 2,
                        } }
                    >
                        { props.title }
                    </Typography>

                    <Box
                        sx={ {
                            display: 'flex',
                            alignItems: 'center',
                        } }
                    >
                        {
                            props.actions?.map((action, index) => typeof action === 'string' ?
                                (
                                    <Box
                                        key={ action }
                                        sx={ {
                                            ml: 2,
                                            width: '1px',
                                            height: '2em',
                                            backgroundColor: 'white',
                                        } }
                                    >
                                    </Box>
                                ) :
                                (
                                    <Tooltip
                                        key={ index }
                                        title={ action.tooltip }
                                        arrow
                                    >
                                        <IconButton
                                            color="inherit"
                                            sx={ {
                                                ml: 2,
                                            } }
                                            onClick={ 'onClick' in action ? action.onClick : undefined }
                                            component={ 'onClick' in action ? 'button' : 'a' }
                                            href={ 'href' in action ? action.href : undefined }
                                            target={ 'href' in action ? '_blank' : undefined }
                                        >
                                            <FontAwesomeIcon
                                                icon={ action.icon }
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
                !(props.noPlaceholder ?? false) &&
                <Box
                    sx={ {
                        height: '4rem',
                    } }
                />
            }
        </>
    );
}
