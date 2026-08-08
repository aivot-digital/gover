import {Box, CircularProgress, Dialog, DialogContent, DialogTitle, Divider, IconButton, InputBase, List, ListItem, ListItemIcon, ListItemText, Pagination, Skeleton, Typography} from '@mui/material';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {selectShowSearchDialog, setShowSearchDialog} from '../../../slices/shell-slice';
import {useEffect, useState} from 'react';
import {SearchItemService} from '../../../modules/search/search-item-service';
import {Page} from '../../../models/dtos/page';
import {SearchItemResponseDto} from '../../../modules/search/dtos/search-item-response-dto';
import {createSearchItemLink} from '../../../modules/search/utils/create-search-item-link';
import {OriginTableIcons, OriginTableLabels} from '../../../modules/search/data/origin-table';
import HelpClinic from '@aivot/mui-material-symbols-400-n25-outlined/HelpClinic';
import {Link} from 'react-router-dom';
import {isStringNotNullOrEmpty, isStringNullOrEmpty} from '../../../utils/string-utils';
import Chip from '@mui/material/Chip';
import {ServerEntityType} from '../data/server-entity-type';
import Search from '@aivot/mui-material-symbols-400-n25-outlined/Search';
import Close from '@aivot/mui-material-symbols-400-n25-outlined/Close';
import Lightbulb2 from '@aivot/mui-material-symbols-400-n25-outlined/Lightbulb2';
import {withAsyncWrapper} from '../../../utils/with-async-wrapper';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {useRetainedDialogValue} from '../../../hooks/use-retained-dialog-value';

const RECENT_ITEMS_SKELETON_COUNT = 5;

export function ShellSearchDialog() {
    const dispatch = useAppDispatch();
    const show = useAppSelector(selectShowSearchDialog);

    const [search, setSearch] = useState('');
    const [debouncedSearch, setDebouncedSearch] = useState(search);
    const [isSearchBusy, setIsSearchBusy] = useState(false);
    const [isRecentItemsBusy, setIsRecentItemsBusy] = useState(false);
    const [searchResults, setSearchResults] = useState<Page<SearchItemResponseDto> | undefined>(undefined);
    const [recentItems, setRecentItems] = useState<SearchItemResponseDto[]>([]);
    const [page, setPage] = useState(0);
    const [size] = useState(12);

    // Debounce input before querying the backend.
    useEffect(() => {
        const t = window.setTimeout(() => setDebouncedSearch(search), 400);
        return () => window.clearTimeout(t);
    }, [search]);

    const handleClose = () => {
        setTimeout(() => {
            setSearch('');
            setPage(0);
        }, 250);
        dispatch(setShowSearchDialog(false));
    };

    useEffect(() => {
        if (!show || debouncedSearch.length === 0) {
            setSearchResults(undefined);
            setIsSearchBusy(false);
            return;
        }

        const ac = new AbortController();
        let canceled = false;

        withAsyncWrapper({
            desiredMinRuntime: 400,
            signal: ac.signal,
            runtimeCallback: (running) => setIsSearchBusy(running),
            main: async (_before, signal) => {
                return await new SearchItemService().getSearchItems(
                    debouncedSearch,
                    page,
                    size,
                    signal,
                );
            },
            after: async (res) => {
                if (!canceled) setSearchResults(res);
            },
        }).catch(() => {
        });

        return () => {
            canceled = true;
            ac.abort();
        };
    }, [debouncedSearch, page, show]);

    useEffect(() => {
        if (!show) {
            setRecentItems([]);
            setIsRecentItemsBusy(false);
            return;
        }

        if (debouncedSearch.length > 0) {
            setIsRecentItemsBusy(false);
            return;
        }

        const ac = new AbortController();
        let canceled = false;
        setRecentItems([]);

        withAsyncWrapper({
            desiredMinRuntime: 400,
            signal: ac.signal,
            runtimeCallback: (running) => setIsRecentItemsBusy(running),
            main: async (_before, signal) => {
                return await new SearchItemService().getRecentSearchItems(10, signal);
            },
        }).then((res) => {
            // Apply the result after withAsyncWrapper resolves so the skeleton respects the minimum runtime.
            if (!canceled) setRecentItems(res);
        }).catch(() => {
            if (!canceled) setRecentItems([]);
        });

        return () => {
            canceled = true;
            ac.abort();
        };
    }, [debouncedSearch, show]);

    const renderSearch = useRetainedDialogValue(show, search);
    const renderDebouncedSearch = useRetainedDialogValue(show, debouncedSearch);
    const renderSearchResults = useRetainedDialogValue(show, searchResults);
    const renderRecentItems = useRetainedDialogValue(show, recentItems);
    const renderIsRecentItemsBusy = useRetainedDialogValue(show, isRecentItemsBusy);
    const renderPage = useRetainedDialogValue(show, page);

    const results = renderSearchResults?.content ?? [];
    const totalElements = renderSearchResults?.page.totalElements ?? 0;
    const totalPages = renderSearchResults?.page.totalPages ?? 0;
    const showRecentItemsSection = isStringNullOrEmpty(renderDebouncedSearch) &&
        (renderRecentItems.length > 0 || renderIsRecentItemsBusy);

    const handlePageChange = (_event: React.ChangeEvent<unknown>, newPage: number) => {
        // MUI pages are 1-based, while the backend expects 0-based page indexes.
        setPage(newPage - 1);
    };

    return (
        <Dialog
            open={show}
            onClose={handleClose}
            fullWidth={true}
            maxWidth="sm"
        >
            <DialogTitle sx={{py: 1}}>
                <span style={{display: 'none'}}>Suche</span>
            </DialogTitle>
            <DialogContent>
                <Box
                    sx={{
                        display: 'flex',
                        alignItems: 'center',
                        px: 1,
                        py: 0.75,
                        mb: 2,
                    }}
                >
                    {isSearchBusy ? (
                        <Box
                            sx={{
                                width: '2.1875rem',
                                height: '2.1875rem',
                                display: 'flex',
                                flexShrink: 0,
                                alignItems: 'center',
                                justifyContent: 'center',
                                mr: 2,
                            }}
                        >
                            <CircularProgress
                                size={28}
                                thickness={4}
                                sx={{color: 'primary.main'}}
                            />
                        </Box>
                    ) : (
                        <Search sx={{color: 'primary.main', mr: 2, fontSize: '2.1875rem'}} />
                    )}
                    <InputBase
                        placeholder="Suche…"
                        value={renderSearch}
                        onChange={(e) => {
                            setSearch(e.target.value);
                            setPage(0);
                        }}
                        fullWidth
                        autoFocus
                        sx={{
                            fontSize: '1.5rem',
                            color: 'text.primary',
                        }}
                    />
                    <IconButton onClick={handleClose}>
                        <Close />
                    </IconButton>
                </Box>

                <Box
                    sx={{
                        position: 'relative',
                        minHeight: '35rem',
                        maxHeight: '40vh',
                    }}
                >

                    {isStringNullOrEmpty(renderDebouncedSearch) && (
                        <Box
                            sx={{
                                display: 'flex',
                                alignItems: 'center',
                                gap: 1.5,
                                px: 2,
                                py: 1.25,
                                mb: 3.5,
                                borderRadius: 2,
                                bgcolor: 'action.hover',
                            }}
                        >
                            <Lightbulb2 sx={{color: 'text.secondary'}} />
                            <Typography
                                variant="body1"
                                color="text.secondary"
                            >
                                <strong>Tipp:</strong>{' '}
                                Durchsuchen Sie schnell &amp; einfach Formulare, Prozesse und
                                Vorgangsdaten mit unserer Komfort-Suchfunktion.
                            </Typography>
                        </Box>
                    )}

                    <Divider sx={{mx: -3}} />

                    {/* Search results */}
                    {renderSearchResults && results.length > 0 && (
                        <>
                            <Box sx={{
                                display: 'flex',
                                flexDirection: 'column',
                                justifyContent: 'space-between',
                                minHeight: '35rem'
                            }}>
                                <List sx={{'& .MuiListItem-root:last-of-type': {borderBottom: 'none'}}}>
                                    {results.map((item, idx) => (
                                        <SearchDialogListItem
                                            key={`${renderPage}-${item.originTable}-${item.id}-${idx}`}
                                            id={item.id}
                                            type={item.originTable}
                                            link={createSearchItemLink(item)}
                                            search={renderDebouncedSearch}
                                            label={item.label}
                                            handleClose={handleClose}
                                        />
                                    ))}
                                </List>

                                {/* Pagination and result count */}
                                <Box
                                    sx={{
                                        display: 'flex',
                                        flexDirection: 'row',
                                        alignItems: 'center',
                                        justifyContent: 'space-between',
                                    }}
                                >
                                    {totalElements > 0 && (
                                        <Typography
                                            variant="body2"
                                            color="text.secondary"
                                            sx={{mt: 2, mb: 1, fontSize: '0.875rem'}}
                                        >
                                            Ergebnisse {`${renderPage * size + 1}–${Math.min((renderPage + 1) * size, totalElements)} von ${totalElements}`}
                                        </Typography>
                                    )}
                                    <Pagination
                                        count={totalPages}
                                        page={renderPage + 1}
                                        color="primary"
                                        size="small"
                                        onChange={handlePageChange}
                                        sx={{mt: 2, mb: 1}}
                                    />
                                </Box>
                            </Box>
                        </>
                    )}


                    {/* No results */}
                    {isStringNotNullOrEmpty(renderDebouncedSearch) &&
                        renderSearchResults != null &&
                        results.length === 0 && (
                            <Typography
                                variant="body1"
                                sx={{
                                    position: 'absolute',
                                    top: '50%',
                                    left: '50%',
                                    transform: 'translate(-50%, -50%)',
                                    width: '50%',
                                    textAlign: 'center',
                                }}
                            >
                                <b>Keine Ergebnisse gefunden.</b>
                                <br />
                                Bitte versuchen Sie es mit einem anderen Suchbegriff.
                            </Typography>
                        )}

                    {/* Recent items */}
                    {showRecentItemsSection && (
                        <>
                            <Typography
                                variant="h6"
                                sx={{mt: 2.5}}
                            >
                                Zuletzt verwendet
                            </Typography>
                            <List sx={{'& .MuiListItem-root:last-of-type': {borderBottom: 'none'}}}>
                                {renderRecentItems.length === 0 && renderIsRecentItemsBusy ? (
                                    Array.from({length: RECENT_ITEMS_SKELETON_COUNT}).map((_, idx) => (
                                        <SearchDialogListItemSkeleton key={idx} />
                                    ))
                                ) : (
                                    renderRecentItems.map((item, idx) => (
                                        <SearchDialogListItem
                                            key={`${item.originTable}-${item.id}-${idx}`}
                                            id={item.id}
                                            type={item.originTable}
                                            link={createSearchItemLink(item)}
                                            search={renderDebouncedSearch}
                                            label={item.label}
                                            handleClose={handleClose}
                                        />
                                    ))
                                )}
                            </List>
                        </>
                    )}

                    {/* Empty State */}
                    {isStringNullOrEmpty(renderDebouncedSearch) && renderRecentItems.length === 0 && !renderIsRecentItemsBusy && (
                        <Typography
                            variant="body1"
                            sx={{
                                position: 'absolute',
                                top: '50%',
                                left: '50%',
                                transform: 'translate(-50%, -50%)',
                                width: '50%',
                                textAlign: 'center',
                                color: 'text.secondary',
                            }}
                        >
                            Bitte geben Sie einen Suchbegriff ein, um nach Formularen, Prozessen oder Vorgangsdaten zu suchen.
                        </Typography>
                    )}
                </Box>
            </DialogContent>
        </Dialog>
    );
}

interface ShellSearchDialogProps {
    search: string;
    id: string;
    type: ServerEntityType;
    link: string;
    label: string;
    handleClose: () => void;
}

function SearchDialogListItem(props: ShellSearchDialogProps) {
    const {type, link, search, label, handleClose} = props;

    return (
        <ListItem
            component={Link}
            to={link}
            onClick={handleClose}
            dense={true}
            sx={{
                borderBottom: '1px solid',
                borderColor: 'divider',
                px: 0.25,
                color: 'inherit',
                '&:hover': {backgroundColor: 'action.hover'},
            }}
        >
            <ListItemIcon sx={{color: 'primary.main', minWidth: '2.5rem', textAlign: 'center'}}>
                {OriginTableIcons[type] ?? <HelpClinic />}
            </ListItemIcon>
            <ListItemText
                primary={label}
                secondary={
                    isStringNotNullOrEmpty(search) && type === ServerEntityType.DataObjectItems
                        ? `Das Datenobjekt beinhaltet den Wert „${search}“`
                        : null
                }
                slotProps={{
                    primary: {
                        sx: {
                            whiteSpace: 'nowrap',
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                        },
                    },
                }}
            />
            <Chip
                size="small"
                sx={{ml: 2}}
                label={OriginTableLabels[type] ?? 'Unbekannt'}
            />
        </ListItem>
    );
}

function SearchDialogListItemSkeleton() {
    return (
        <ListItem
            dense={true}
            sx={{
                borderBottom: '1px solid',
                borderColor: 'divider',
                px: 0.25,
            }}
        >
            <ListItemIcon sx={{minWidth: '2.5rem', textAlign: 'center'}}>
                <Skeleton
                    variant="circular"
                    width={24}
                    height={24}
                />
            </ListItemIcon>
            <ListItemText
                primary={
                    <Skeleton
                        variant="text"
                        width="72%"
                        height={24}
                    />
                }
            />
            <Skeleton
                variant="rounded"
                width={92}
                height={24}
                sx={{ml: 2, borderRadius: 4}}
            />
        </ListItem>
    );
}
