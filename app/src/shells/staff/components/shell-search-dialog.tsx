import {Box, Dialog, DialogContent, List, ListItem, ListItemIcon, ListItemText, Skeleton, Typography} from '@mui/material';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {selectShowSearchDialog, setShowSearchDialog} from '../../../slices/shell-slice';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {useEffect, useState} from 'react';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';
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
                                <ListItem
                                    key={item.id + item.originTable}
                                    component={Link}
                                    to={createSearchItemLink(item)}
                                    onClick={handleClose}
                                    dense={true}
                                    sx={{
                                        borderBottom: '1px solid #eee',
                                    }}
                                >
                                    <ListItemIcon>
                                        {OriginTableIcons[item.originTable] ?? <HelpClinic />}
                                    </ListItemIcon>
                                    <ListItemText
                                        primary={item.label}
                                        secondary={item.originTable === 'data_object_items' ? `Hinweis: Das Datenobjekt beinhaltet den Wert „${search}“` : null}
                                    />
                                    <Chip
                                        size="small"
                                        label={OriginTableLabels[item.originTable] ?? item.originTable}
                                    />
                                </ListItem>
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
                                {entityHistory.map(item => (
                                    <ListItem
                                        key={item.link}
                                        component={Link}
                                        to={item.link}
                                        onClick={handleClose}
                                        dense={true}
                                        sx={{
                                            borderBottom: '1px solid #eee',
                                        }}
                                    >
                                        <ListItemText
                                            primary={item.title}
                                        />
                                    </ListItem>
                                ))}
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