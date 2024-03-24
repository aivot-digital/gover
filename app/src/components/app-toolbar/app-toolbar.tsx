import React, {useEffect, useRef} from 'react';
import {type AppToolbarProps} from './app-toolbar-props';
import {AppBar, Box, Button, IconButton, styled, Toolbar, Tooltip, Typography} from '@mui/material';
import {Link, useNavigate} from 'react-router-dom';
import HomeOutlinedIcon from '@mui/icons-material/HomeOutlined';
import ArrowBackOutlinedIcon from '@mui/icons-material/ArrowBackOutlined';
import {getPath} from '../../apps/staff-app-routes';


export function AppToolbar(props: AppToolbarProps): JSX.Element {
    const { updateToolbarHeight } = props;
    const navigate = useNavigate();
    const Offset = styled('div')(({ theme }) => theme.mixins.toolbar);

    const offsetRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        const updateHeight = () => {
            if (offsetRef.current && updateToolbarHeight) {
                updateToolbarHeight(offsetRef.current.offsetHeight);
            }
        };

        updateHeight();

        window.addEventListener('resize', updateHeight);

        return () => {
            window.removeEventListener('resize', updateHeight);
        };
    }, [updateToolbarHeight]);

    return (
        <>
            <AppBar position="fixed">
                <Toolbar color="primary">
                    <Tooltip title="Zur Startseite">
                        <IconButton
                            size="small"
                            edge="start"
                            component={Link}
                            to={getPath('moduleSelect')}
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
                                        {
                                            'label' in action ?
                                                (
                                                    <Button
                                                        size="small"
                                                        color="inherit"
                                                        sx={{
                                                            ml: 2,
                                                        }}
                                                        onClick={'onClick' in action ? action.onClick : undefined}
                                                        component={'onClick' in action ? 'button' : 'a'}
                                                        href={'href' in action ? action.href : undefined}
                                                        target={'href' in action ? '_blank' : undefined}
                                                        startIcon={action.icon}
                                                    >
                                                        {action.label}
                                                    </Button>
                                                ) :
                                                (
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
                                                )
                                        }
                                    </Tooltip>
                                ))
                        }
                    </Box>
                </Toolbar>
            </AppBar>
            <Offset ref={offsetRef}/>
        </>
    );
}
