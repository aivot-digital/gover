import {styled} from '@mui/material/styles';
import {InputBase} from '@mui/material';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {faFileMagnifyingGlass} from '@fortawesome/pro-light-svg-icons';
import React from 'react';
import {SearchInputProps} from './search-input-props';

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

export function SearchInput({value, onChange, placeholder}: SearchInputProps) {
    return (
        <Search>
            <SearchIconWrapper>
                <FontAwesomeIcon icon={faFileMagnifyingGlass}/>
            </SearchIconWrapper>
            <StyledInputBase
                placeholder={placeholder}
                inputProps={{
                    'aria-label': 'search',
                }}
                value={value}
                onChange={event => onChange(event.target.value ?? '')}
            />
        </Search>
    );
}
