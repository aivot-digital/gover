import React from 'react';
import { Box, Button, IconButton, Tooltip, Typography } from '@mui/material';
import { SearchInput } from '../search-input/search-input';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { ListHeaderProps } from './list-header-props';
import { Link } from "react-router-dom";

export function ListHeader ({title, search, searchPlaceholder, onSearchChange, actions}: ListHeaderProps): JSX.Element {
    return (
        <Box
            sx={ {
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center'
            } }
        >
            <Typography variant="h2">
                { title }
            </Typography>

            <Box
                sx={ {
                    display: 'flex',
                } }
            >
                <SearchInput
                    value={ search }
                    onChange={ onSearchChange }
                    placeholder={ searchPlaceholder }
                />

                {
                    actions.map(act => (
                        'label' in act
                            ? (
                                <Button
                                    key={ act.label }
                                    endIcon={  act.icon }
                                    variant="contained"
                                    sx={ {ml: 2} }
                                    component={ 'link' in act ? Link : 'button' }
                                    to={ 'link' in act ? act.link : undefined }
                                    onClick={ 'onClick' in act ? act.onClick : undefined }
                                >
                                    { act.label }
                                </Button>
                            ) : (
                                <Tooltip
                                    key={ act.tooltip }
                                    title={ act.tooltip }
                                >
                                    <IconButton
                                        sx={ {ml: 2} }
                                        component={ 'link' in act ? Link : 'button' }
                                        to={ 'link' in act ? act.link : undefined }
                                        onClick={ 'onClick' in act ? act.onClick : undefined }
                                    >
                                        { act.icon }

                                    </IconButton>
                                </Tooltip>
                            )
                    ))
                }
            </Box>
        </Box>
    );
}
