import {styled} from '@mui/material/styles';
import {InputBase} from '@mui/material';
import React from 'react';
import {SearchInputProps} from './search-input-props';
import SearchOutlinedIcon from '@mui/icons-material/SearchOutlined';

// TODO: DK -> Bitte styling anpassen

const Search = styled('div')(({theme}) => ({
    position: 'relative',
    borderRadius: theme.shape.borderRadius,
    backgroundColor: '#E2E7E2',
    '&:hover': {
        backgroundColor: '#E7ECE7',
    },
    marginLeft: 0,
    width: '100%',
    [theme.breakpoints.up('sm')]: {
        marginLeft: theme.spacing(1),
        width: 'auto',
    },
}));

const SearchIconWrapper = styled('div')(({theme}) => ({
    padding: theme.spacing(0, 2),
    height: '100%',
    position: 'absolute',
    pointerEvents: 'none',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
}));

const StyledInputBase = styled(InputBase)(({theme}) => ({
    color: 'inherit',
    '& .MuiInputBase-input': {
        padding: theme.spacing(1, 1, 1, 0),
        // vertical padding + font size from searchIcon
        paddingLeft: `calc(1em + ${theme.spacing(4)})`,
        transition: theme.transitions.create('width'),
        width: '100%',
        [theme.breakpoints.up('sm')]: {
            width: '16ch',
            '&:focus': {
                width: '24ch',
            },
        },
    },
}));

/**
 * @deprecated Use SearchInput from search-input-2 instead
 * @param props
 * @constructor
 */
export function SearchInput(props: SearchInputProps) {
    return (
        <Search>
            <SearchIconWrapper>
                <SearchOutlinedIcon/>
            </SearchIconWrapper>
            <StyledInputBase
                placeholder={props.placeholder}
                inputProps={{
                    'aria-label': 'Suche',
                }}
                value={props.value}
                onChange={event => {
                    props.onChange(event.target.value ?? '');
                }}
                autoFocus={props.autoFocus}
            />
        </Search>
    );
}
