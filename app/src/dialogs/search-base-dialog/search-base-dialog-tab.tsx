import {
    Box,
    Button,
    Grid,
    IconButton,
    List,
    ListItem,
    ListItemButton,
    ListItemIcon,
    ListItemText,
    Tooltip,
    Typography,
    useTheme,
} from '@mui/material';
import {useEffect, useMemo, useState} from 'react';
import Fuse from 'fuse.js';
import {SearchInput} from '../../components/search-input/search-input';
import {SearchBaseDialogTabProps} from './search-base-dialog-tab-props';
import InfoOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Info';
import CancelOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Cancel';
import {isStringNullOrEmpty} from '../../utils/string-utils';
import {useNormalizedReactId} from '../../hooks/use-normalized-react-id';

export function SearchBaseDialogTab<T>(props: SearchBaseDialogTabProps<T>) {
    const theme = useTheme();
    const generatedId = useNormalizedReactId();
    const detailsId = `search-dialog-details-${generatedId}`;
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
                sx={{
                    paddingTop: 2,
                    paddingLeft: 2,
                    paddingRight: highlightedOption != null ? 0 : 2,
                    borderRight: highlightedOption != null ? `1px solid ${theme.palette.grey['300']}` : undefined,
                    height: '100%',
                    display: 'flex',
                    flexDirection: 'column',
                }}
                size={highlightedOption != null ? 6 : 12}>
                <SearchInput
                    value={search}
                    onChange={setSearch}
                    label={props.searchPlaceholder}
                    placeholder="Suchen…"
                    autoFocus
                    size={'small'}
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
                            searchedOptions.map((option) => {
                                const optionLabel = typeof props.primaryTextKey === 'function'
                                    ? props.primaryTextKey(option)
                                    : option[props.primaryTextKey] as string;
                                const detailsExpanded = highlightedOption === option;
                                const detailsActionProps = {
                                    'aria-label': `${optionLabel}: Details ${detailsExpanded ? 'schließen' : 'anzeigen'}`,
                                    'aria-expanded': detailsExpanded,
                                    'aria-controls': detailsExpanded ? detailsId : undefined,
                                    onClick: () => setHighlightedOption(detailsExpanded ? undefined : option),
                                };

                                return (
                                    <ListItem
                                        key={typeof props.getId === 'function' ? props.getId(option) : option[props.getId] as string}
                                        sx={{
                                            background: highlightedOption === option ? theme.palette.grey['200'] : undefined,
                                            padding: 0,
                                            alignItems: 'flex-start',
                                        }}
                                    >
                                        <ListItemButton
                                            onClick={() => {
                                                props.onSelect(option);
                                            }}
                                            sx={{
                                                paddingY: 0,
                                                minWidth: 0,
                                                flex: 1,
                                                alignItems: 'flex-start',
                                            }}
                                        >
                                            {
                                                props.getIcon != null &&
                                                <ListItemIcon>
                                                    {props.getIcon(option)}
                                                </ListItemIcon>
                                            }
                                            <ListItemText
                                                sx={{
                                                    minWidth: 0,
                                                    overflowWrap: 'anywhere',
                                                }}
                                                primary={
                                                    optionLabel
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
                                        {props.detailsBuilder != null && <Box sx={{
                                            flexShrink: 0,
                                            px: 1,
                                            py: 0.5,
                                        }}>
                                            <Tooltip
                                                title={detailsExpanded ? 'Details schließen' : 'Details anzeigen'}
                                                arrow
                                            >
                                                <IconButton
                                                    {...detailsActionProps}
                                                >
                                                    {
                                                        detailsExpanded ?
                                                            <CancelOutlinedIcon/> :
                                                            <InfoOutlinedIcon/>
                                                    }
                                                </IconButton>
                                            </Tooltip>
                                        </Box>}
                                    </ListItem>
                                );
                            })
                        }
                    </List>
                </Box>
            </Grid>
            {
                highlightedOption != null &&
                props.detailsBuilder != null &&
                <Grid
                    id={detailsId}
                    sx={{
                        height: '100%',
                        display: 'flex',
                        flexDirection: 'column',
                    }}
                    size={6}>
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
                            gap: 1,
                        }}
                    >
                        <Button onClick={() => setHighlightedOption(undefined)}>Details schließen</Button>
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
