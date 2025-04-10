import {Box, Button, Grid, IconButton, List, ListItem, ListItemButton, ListItemIcon, ListItemSecondaryAction, ListItemText, Typography, useTheme} from '@mui/material';
import {useEffect, useMemo, useState} from 'react';
import Fuse from 'fuse.js';
import {SearchInput} from '../../components/search-input-2/search-input';
import {SearchBaseDialogTabProps} from './search-base-dialog-tab-props';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import CancelOutlinedIcon from '@mui/icons-material/CancelOutlined';
import {isStringNullOrEmpty} from '../../utils/string-utils';

export function SearchBaseDialogTab<T>(props: SearchBaseDialogTabProps<T>) {
    const theme = useTheme();
    const [search, setSearch] = useState('');
    const [highlightedOption, setHighlightedOption] = useState<T>();

    const {
        options,
        searchKeys,
    } = props;

    useEffect(() => {
        setSearch('');
        setHighlightedOption(undefined);
    }, [props.options]);

    const searchedOptions = useMemo(() => {
        if (search === '') {
            return options;
        }

        const fuse = new Fuse(options, {
            keys: searchKeys as string[],
            minMatchCharLength: 3,
            shouldSort: true,
            threshold: 0.25,
        });
        return fuse
            .search(search)
            .map(result => result.item);

    }, [options, searchKeys, search]);

    return (
        <Grid
            container
            sx={{
                height: '100%',
            }}
        >
            <Grid
                item
                xs={highlightedOption != null ? 6 : 12}
                sx={{
                    paddingTop: 2,
                    paddingLeft: 2,
                    paddingRight: highlightedOption != null ? 0 : 2,
                    borderRight: highlightedOption != null ? `1px solid ${theme.palette.grey['300']}` : undefined,
                    height: '100%',
                    display: 'flex',
                    flexDirection: 'column',
                }}
            >
                <SearchInput
                    value={search}
                    onChange={setSearch}
                    placeholder={props.searchPlaceholder}
                    autoFocus
                    sx={{
                        marginRight: highlightedOption != null ? 2 : 0,
                    }}
                />

                <Box
                    sx={{
                        overflowY: 'auto',
                        marginY: 2,
                        paddingRight: highlightedOption != null ? 2 : 0,
                        flex: 1,
                    }}
                >
                    {
                        searchedOptions.length === 0 &&
                        <Typography
                            align="center"
                            sx={{
                                marginY: 2,
                            }}
                        >
                            {
                                isStringNullOrEmpty(search) ?
                                    props.noOptionsMessage ?? 'Keine Optionen verfügbar.' :
                                    props.noSearchResultsMessage ?? 'Keine Ergebnisse gefunden.'
                            }
                        </Typography>
                    }

                    <List>
                        {
                            searchedOptions.map((option) => (
                                    <ListItem
                                        key={typeof props.getId === 'function' ? props.getId(option) : option[props.getId] as string}
                                        sx={{
                                            background: highlightedOption === option ? theme.palette.grey['200'] : undefined,
                                            padding: 0,
                                        }}
                                        secondaryAction={
                                            props.detailsBuilder != null ? (
                                                <IconButton
                                                    onClick={() => {
                                                        if (highlightedOption === option) {
                                                            setHighlightedOption(undefined);
                                                        } else {
                                                            setHighlightedOption(option);
                                                        }
                                                    }}
                                                >
                                                    {
                                                        highlightedOption === option ?
                                                            <CancelOutlinedIcon /> :
                                                            <InfoOutlinedIcon />
                                                    }
                                                </IconButton>
                                            ) : undefined
                                        }
                                    >
                                        <ListItemButton
                                            onClick={() => {
                                                props.onSelect(option);
                                            }}
                                            sx={{
                                                paddingY: 0,
                                            }}
                                        >
                                            {
                                                props.getIcon != null &&
                                                <ListItemIcon>
                                                    {props.getIcon(option)}
                                                </ListItemIcon>
                                            }
                                            <ListItemText
                                                primary={
                                                    typeof props.primaryTextKey === 'function' ?
                                                        props.primaryTextKey(option) :
                                                        option[props.primaryTextKey] as string
                                                }
                                                secondary={
                                                    props.secondaryTextKey == null ?
                                                        undefined : (
                                                            typeof props.secondaryTextKey === 'function' ?
                                                                props.secondaryTextKey(option) :
                                                                option[props.secondaryTextKey] as string
                                                        )
                                                }
                                            />
                                        </ListItemButton>
                                    </ListItem>
                                ),
                            )
                        }
                    </List>
                </Box>
            </Grid>

            {
                highlightedOption != null &&
                props.detailsBuilder != null &&
                <Grid
                    item
                    xs={6}
                    sx={{
                        height: '100%',
                        display: 'flex',
                        flexDirection: 'column',
                    }}
                >
                    <Box
                        sx={{
                            overflowY: 'auto',
                            paddingY: 2,
                            paddingX: 2,
                            flex: 1,
                        }}
                    >
                        {props.detailsBuilder(highlightedOption)}
                    </Box>

                    <Box
                        sx={{
                            display: 'flex',
                            marginTop: 'auto',
                            borderTop: `1px solid ${theme.palette.grey['300']}`,
                            paddingX: 2,
                            paddingY: 1,
                        }}
                    >
                        <Button
                            variant="contained"
                            onClick={() => {
                                props.onSelect(highlightedOption);
                            }}
                            sx={{marginLeft: 'auto'}}
                        >
                            Auswählen
                        </Button>
                    </Box>
                </Grid>
            }
        </Grid>
    );
}