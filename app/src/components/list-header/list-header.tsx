import React from 'react';
import {Box, Button, IconButton, Tooltip, Typography} from '@mui/material';
import {SearchInput} from '../search-input/search-input';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {ListHeaderProps} from './list-header-props';

export function ListHeader({title, search, searchPlaceholder, onSearchChange, actions}: ListHeaderProps) {
    return (
        <Box
            sx={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center'
            }}
        >
            <Typography variant="h2">
                {title}
            </Typography>

            <div style={{display: 'flex'}}>
                <SearchInput
                    value={search}
                    onChange={onSearchChange}
                    placeholder={searchPlaceholder}
                />

                {
                    actions.map(act => 'label' in act ? (
                        <Button
                            key={act.label}
                            endIcon={act.icon}
                            variant="contained"
                            sx={{ml: 2}}
                            onClick={act.onClick}
                        >
                            {act.label}
                        </Button>
                    ) : (
                        <Tooltip
                            key={act.tooltip}
                            title={act.tooltip}>
                            <IconButton
                                sx={{ml: 2}}
                                onClick={act.onClick}
                            >
                                {act.icon}
                            </IconButton>
                        </Tooltip>
                    ))
                }
            </div>
        </Box>
    );
}
