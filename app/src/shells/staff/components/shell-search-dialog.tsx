import {Box, Dialog, DialogContent, List, ListItem, ListItemIcon, ListItemText, Typography} from '@mui/material';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
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
import {Loader} from '../../../components/loader/loader';
import {isStringNotNullOrEmpty, isStringNullOrEmpty} from '../../../utils/string-utils';
import Chip from '@mui/material/Chip';
import {SearchInput} from '../../../components/search-input-2/search-input';
import {selectEntityHistory} from '../../../slices/entity-history-slice';
import {ServerEntityType} from '../data/server-entity-type';

export function ShellSearchDialog() {
    const dispatch = useAppDispatch();
    const show = useAppSelector(selectShowSearchDialog);
    const entityHistory = useAppSelector(selectEntityHistory);

    const [search, setSearch] = useState('');
    const [isBusy, setIsBusy] = useState(false);
    const [searchResults, setSearchResults] = useState<Page<SearchItemResponseDto>>();

    const handleClose = () => {
        setTimeout(() => {
            setSearch('');
        }, 250);
        dispatch(setShowSearchDialog(false));
    };

    useEffect(() => {
        if (search.length === 0) {
            setSearchResults(undefined);
            return;
        }

        setIsBusy(true);

        new SearchItemService()
            .getSearchItems(search)
            .then(setSearchResults)
            .finally(() => {
                setIsBusy(false);
            });
    }, [search]);

    return (
        <Dialog
            open={show}
            onClose={handleClose}
            fullWidth={true}
            maxWidth="md"
        >
            <DialogTitleWithClose onClose={handleClose}>
                Suche
            </DialogTitleWithClose>
            <DialogContent>
                <SearchInput
                    value={search}
                    onChange={setSearch}
                    placeholder="Suche"
                    autoFocus={true}
                />

                <Box
                    sx={{
                        position: 'relative',
                        minHeight: '32rem',
                        maxHeight: '75vh',
                    }}
                >
                    {
                        searchResults != null &&
                        searchResults.content.length > 0 &&
                        <List>
                            {searchResults.content.map(item => (
                                <SearchDialogListItem
                                    key={item.id + item.originTable}
                                    id={item.id}
                                    type={item.originTable}
                                    link={createSearchItemLink(item)}
                                    search={search}
                                    label={item.label}
                                />
                            ))}
                        </List>
                    }

                    {
                        isStringNotNullOrEmpty(search) &&
                        searchResults != null &&
                        searchResults.content.length === 0 &&
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
                            Keine Ergebnisse gefunden. Probieren Sie es mit einem anderen Suchbegriff.
                        </Typography>
                    }

                    {
                        isStringNullOrEmpty(search) &&
                        entityHistory.length > 0 &&
                        <>
                            <Typography
                                variant="h6"
                                sx={{
                                    mt: 2,
                                }}
                            >
                                Kürzlich angesehene Objekte
                            </Typography>
                            <List>
                                {
                                    entityHistory.map(item => (
                                        <SearchDialogListItem
                                            key={item.link}
                                            id={item.title}
                                            type={item.type}
                                            link={item.link}
                                            search={search}
                                            label={item.title}
                                        />
                                    ))
                                }
                            </List>
                        </>
                    }

                    {
                        isStringNullOrEmpty(search) &&
                        entityHistory.length === 0 &&
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
                            Bitte geben Sie einen Suchbegriff ein.
                        </Typography>
                    }

                    <Box
                        sx={{
                            position: 'absolute',
                            top: 0,
                            left: 0,
                            right: 0,
                            bottom: 0,
                            pointerEvents: 'none',
                            display: isBusy ? 'block' : 'none',
                            backgroundColor: 'rgba(255, 255, 255, 0.7)',
                            backdropFilter: 'blur(3px)',
                        }}
                    >
                        <Box
                            sx={{
                                position: 'absolute',
                                top: '50%',
                                left: '50%',
                                transform: 'translate(-50%, -50%)',
                                width: '50%',
                            }}
                        >
                            <Loader message="Suche Objekte" />
                        </Box>
                    </Box>
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
    const {
        id,
        type,
        link,
        search,
        label,
    } = props;

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
            }}
        >
            <ListItemIcon>
                {OriginTableIcons[type] ?? <HelpClinic />}
            </ListItemIcon>
            <ListItemText
                primary={label}
                secondary={(isStringNotNullOrEmpty(search) && type === ServerEntityType.DataObjectItems) ? `Hinweis: Das Datenobjekt beinhaltet den Wert „${search}“` : null}
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
                sx={{
                    ml: 1,
                }}
                label={OriginTableLabels[type] ?? 'Unbekannt'}
            />
        </ListItem>
    );
}