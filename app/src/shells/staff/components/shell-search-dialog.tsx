import {Box, CircularProgress, Dialog, DialogContent, DialogTitle, Divider, IconButton, InputBase, List, ListItem, ListItemIcon, ListItemText, Paper, Typography} from '@mui/material';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {selectShowSearchDialog, setShowSearchDialog} from '../../../slices/shell-slice';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {useEffect, useState} from 'react';
import {SearchItemService} from '../../../modules/search/search-item-service';
import {Page} from '../../../models/dtos/page';
import {SearchItemResponseDto} from '../../../modules/search/dtos/search-item-response-dto';
import {createSearchItemLink} from '../../../modules/search/utils/create-search-item-link';
import {OriginTableIcons, OriginTableLabels} from '../../../modules/search/data/origin-table';
import HelpClinic from '@aivot/mui-material-symbols-400-outlined/dist/help-clinic/HelpClinic';
import {Link} from 'react-router-dom';
import {isStringNotNullOrEmpty, isStringNullOrEmpty} from '../../../utils/string-utils';
import Chip from '@mui/material/Chip';
import {selectEntityHistory} from '../../../slices/entity-history-slice';
import {ServerEntityType} from '../data/server-entity-type';
import Search from '@aivot/mui-material-symbols-400-outlined/dist/search/Search';
import Close from '@aivot/mui-material-symbols-400-outlined/dist/close/Close';
import Lightbulb2 from '@aivot/mui-material-symbols-400-outlined/dist/lightbulb-2/Lightbulb2';
import {withAsyncWrapper} from '../../../utils/with-async-wrapper';

export function ShellSearchDialog() {
    const dispatch = useAppDispatch();
    const show = useAppSelector(selectShowSearchDialog);
    const entityHistory = useAppSelector(selectEntityHistory);

    const [search, setSearch] = useState('');
    const [debouncedSearch, setDebouncedSearch] = useState(search);
    const [isBusy, setIsBusy] = useState(false);
    const [searchResults, setSearchResults] = useState<Page<SearchItemResponseDto>>();

    useEffect(() => {
        const t = window.setTimeout(() => setDebouncedSearch(search), 400);
        return () => window.clearTimeout(t);
    }, [search]);

    const handleClose = () => {
        setTimeout(() => {
            setSearch('');
        }, 250);
        dispatch(setShowSearchDialog(false));
    };

    useEffect(() => {
        if (debouncedSearch.length === 0) {
            setSearchResults(undefined);
            setIsBusy(false);
            return;
        }

        const ac = new AbortController();
        let canceled = false;

        withAsyncWrapper({
            desiredMinRuntime: 400,
            signal: ac.signal,
            runtimeCallback: (running) => setIsBusy(running),
            main: async (_before, signal) => {
                return await new SearchItemService().getSearchItems(debouncedSearch);
            },
            after: async (res) => {
                if (!canceled) setSearchResults(res);
            }
        }).catch((err) => {
        });

        return () => {
            canceled = true;
            ac.abort();
        };
    }, [debouncedSearch]);

    const results = searchResults?.content ?? [];
    const cappedResults = results.slice(0, 10);
    const totalElements: number | undefined = (searchResults as any)?.totalElements;
    const hasMoreResults =
        (typeof totalElements === 'number' && totalElements > cappedResults.length) ||
        results.length > cappedResults.length;

    const historyCapped = entityHistory.slice(0, 10);

    return (
        <Dialog open={show} onClose={handleClose} fullWidth={true} maxWidth="sm">
            <DialogTitle sx={{py: 1}}>
                <span style={{display: 'none'}}>Suche</span>
            </DialogTitle>
            <DialogContent>
                <Paper
                    elevation={0}
                    sx={{
                        display: 'flex',
                        alignItems: 'center',
                        pb: 1,
                        mb: 2
                    }}
                >
                    {isBusy ? (
                        <Box
                            sx={{
                                width: '2.1875rem',
                                height: '2.1875rem',
                                display: 'flex',
                                flexShrink: 0,
                                alignItems: 'center',
                                justifyContent: 'center',
                                mr: 2
                            }}
                        >
                            <CircularProgress size={28} thickness={4} sx={{color: 'primary.dark'}} />
                        </Box>
                    ) : (
                        <Search sx={{color: 'primary.dark', mr: 2, fontSize: '2.1875rem'}} />
                    )}
                    <InputBase
                        placeholder="Suche…"
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                        fullWidth
                        autoFocus
                        sx={{
                            fontSize: '1.5rem',
                            color: 'text.primary'
                        }}
                    />
                    <IconButton onClick={handleClose}>
                        <Close />
                    </IconButton>
                </Paper>

                <Box
                    sx={{
                        position: 'relative',
                        minHeight: '35rem',
                        maxHeight: '40vh'
                    }}
                >

                    {isStringNullOrEmpty(debouncedSearch) && (
                        <Box
                            sx={{
                                display: 'flex',
                                alignItems: 'center',
                                gap: 1.5,
                                px: 2,
                                py: 1.25,
                                mb: 3.5,
                                borderRadius: 2,
                                bgcolor: 'rgba(0,0,0,0.05)',
                            }}
                        >
                            <Lightbulb2 sx={{color: 'text.secondary'}} />
                            <Typography variant="body1" color="text.secondary">
                                <strong>Tipp:</strong> Durchsuchen Sie schnell &amp; einfach Formulare, Prozesse und
                                Vorgangsdaten mit unserer Komfort-Suchfunktion.
                            </Typography>
                        </Box>
                    )}

                    <Divider sx={{mx: -3}} />


                    {/* Suchergebnisse */}
                    {searchResults != null && results.length > 0 && (
                        <>
                            <List sx={{'& .MuiListItem-root:last-of-type': {borderBottom: 'none'}}}>
                                {cappedResults.map((item) => (
                                    <SearchDialogListItem
                                        key={item.id + item.originTable}
                                        id={item.id}
                                        type={item.originTable}
                                        link={createSearchItemLink(item)}
                                        search={debouncedSearch}
                                        label={item.label}
                                    />
                                ))}
                            </List>

                            {hasMoreResults && totalElements && (
                                <Typography
                                    variant="body2"
                                    color="text.secondary"
                                    sx={{mt: 1, px: 0.5, textAlign: 'center'}}
                                >
                                    Es wurden {totalElements - 10} weitere Ergebnisse gefunden.<br/>
                                    Bitte präzisieren Sie ggf. die verwendeten Suchbegriffe.
                                </Typography>
                            )}
                        </>
                    )}

                    {/* Keine Ergebnisse */}
                    {isStringNotNullOrEmpty(debouncedSearch) &&
                        searchResults != null &&
                        results.length === 0 && (
                            <Typography
                                variant="body1"
                                sx={{
                                    position: 'absolute',
                                    top: '50%',
                                    left: '50%',
                                    transform: 'translate(-50%, -50%)',
                                    width: '50%',
                                    textAlign: 'center'
                                }}
                            >
                                <b>Keine Ergebnisse gefunden.</b><br /> Bitte versuchen Sie es mit einem anderen Suchbegriff.
                            </Typography>
                        )}

                    {/* Zuletzt verwendet */}
                    {isStringNullOrEmpty(debouncedSearch) && entityHistory.length > 0 && (
                        <>
                            <Typography variant="h6" sx={{mt: 2.5}}>
                                Zuletzt verwendet
                            </Typography>
                            <List sx={{'& .MuiListItem-root:last-of-type': {borderBottom: 'none'}}}>
                                {historyCapped.map((item) => (
                                    <SearchDialogListItem
                                        key={item.link}
                                        id={item.title}
                                        type={item.type}
                                        link={item.link}
                                        search={search}
                                        label={item.title}
                                    />
                                ))}
                            </List>
                        </>
                    )}

                    {/* Empty State */}
                    {isStringNullOrEmpty(search) && entityHistory.length === 0 && (
                        <Typography
                            variant="body1"
                            sx={{
                                position: 'absolute',
                                top: '50%',
                                left: '50%',
                                transform: 'translate(-50%, -50%)',
                                width: '50%',
                                textAlign: 'center',
                                color: 'text.secondary'
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
}

function SearchDialogListItem(props: ShellSearchDialogProps) {
    const {id, type, link, search, label} = props;

    const dispatch = useAppDispatch();

    const handleClose = () => {
        dispatch(setShowSearchDialog(false));
    };

    return (
        <ListItem
            key={id + type}
            component={Link}
            to={link}
            onClick={handleClose}
            dense={true}
            sx={{
                borderBottom: '1px solid #eee',
                px: 0.25,
                color: 'inherit',
                '&:hover': {backgroundColor: '#f9f9f9'}
            }}
        >
            <ListItemIcon sx={{color: 'primary.dark', minWidth: '2.5rem', textAlign: 'center'}}>
                {OriginTableIcons[type] ?? <HelpClinic />}
            </ListItemIcon>
            <ListItemText
                primary={label}
                secondary={
                    isStringNotNullOrEmpty(search) && type === ServerEntityType.DataObjectItems
                        ? `Hinweis: Das Datenobjekt beinhaltet den Wert „${search}“`
                        : null
                }
                slotProps={{
                    primary: {
                        sx: {
                            whiteSpace: 'nowrap',
                            overflow: 'hidden',
                            textOverflow: 'ellipsis'
                        }
                    }
                }}
            />
            <Chip size="small" sx={{ml: 2}} label={OriginTableLabels[type] ?? 'Unbekannt'} />
        </ListItem>
    );
}
