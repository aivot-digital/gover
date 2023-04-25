import {Box, Button, CircularProgress, Container, List, ListItem, ListItemText, Paper} from '@mui/material';
import React, {useEffect, useState} from 'react';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {faCloudDownload, faCloudUpload, faEdit, faPlus, faTrashAlt} from '@fortawesome/pro-light-svg-icons';
import {AppFooter} from '../../../components/app-footer/app-footer';
import {MetaElement} from '../../../components/meta-element/meta-element';
import {AppMode} from '../../../data/app-mode';
import {AppToolbar} from '../../../components/app-toolbar/app-toolbar';
import {ListHeader} from '../../../components/list-header/list-header';
import {DataOverviewProps} from './data-overview-props';
import {EmptyDataListPlaceholder} from '../../../components/empty-data-list-placeholder/empty-data-list-placeholder';
import {
    EmptySearchDataListPlaceholder
} from '../../../components/empty-search-data-list-placeholder/empty-search-data-list-placeholder';
import {useAuthGuard} from '../../../hooks/use-auth-guard';
import strings from './data-overview-strings.json';
import {useNavigate} from 'react-router-dom';
import {Localization} from '../../../locale/localization';
import {ImportDialog} from './components/import-dialog/import-dialog';
import {readFile} from '../../../utils/read-file';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showErrorSnackbar} from '../../../slices/snackbar-slice';
import {EditDialog} from './components/edit-dialog/edit-dialog';
import {downloadObjectFile} from "../../../utils/download-utils";

const _ = Localization(strings);

export function DataOverview<T extends { id: number }>(props: DataOverviewProps<T>) {
    useAuthGuard();
    const navigate = useNavigate();
    const dispatch = useAppDispatch();

    const [isLoading, setIsLoading] = useState(true);
    const [items, setItems] = useState<T[]>([]);
    const [itemToEdit, setItemToEdit] = useState<T>();
    const [search, setSearch] = useState<string>('');
    const [showImportDialog, setShowImportDialog] = useState(false);

    useEffect(() => {
        props.list().then(res => {
            setItems(res);
            setIsLoading(false);
        });
    }, []);

    const handleItemCreate = () => {
        setIsLoading(true);
        props.create()
            .then(newItem => {
                setItems([...items, newItem]);
                handleItemEdit(newItem)
                setIsLoading(false);
            });
    };

    const handleItemEdit = (item: T) => {
        if (props.linkToEdit != null) {
            navigate(props.linkToEdit(item));
        } else {
            setItemToEdit(item);
        }
    };

    const handleItemEditCancel = () => {
        setItemToEdit(undefined);
    };

    const handleItemSave = (item: T) => {
        props.update(item);
        const updatedItems = [...items];
        const index = updatedItems.findIndex(i => i.id === item.id);
        if (index >= 0) {
            updatedItems[index] = item;
        }
        setItems(updatedItems);
        handleItemEditCancel();
    };

    const handleItemDelete = (deletedItem: T) => {
        const confirmed = window.confirm(_.format(_.deleteMessage, {name: props.toPrimaryString(deletedItem)}));
        if (confirmed) {
            props.destroy(deletedItem);
            const updatedItems = [...items];
            const index = updatedItems.indexOf(deletedItem);
            if (index >= 0) {
                updatedItems.splice(index, 1);
            }
            setItems(updatedItems);
        }
    };

    const handleImport = (files: File[]) => {
        setIsLoading(true);
        Promise.all(files.map(f => readFile<T[]>(f)))
            .then(results => {
                const itemsToImport = results
                    .reduce((acc, val) => acc.concat(val), []);
                return Promise.all(itemsToImport.map(item => {
                    const {id, ...newItem} = item;
                    return props.import(newItem);
                }));
            })
            .then(importedItems => {
                setItems([...items, ...importedItems]);
                setIsLoading(false);
            })
            .catch(err => {
                console.error(err);
                dispatch(showErrorSnackbar(_.importFailedError));
                setIsLoading(false);
            });
    };

    const filteredItems = items
        .filter(item => props.search(search, item))
        .sort(props.sort);

    return (
        <>
            <MetaElement title={props.title}/>

            <AppToolbar
                title={props.title}
                parentPath={'/overview'}
                actions={[
                    {
                        icon: faCloudUpload,
                        tooltip: _.importTooltip,
                        onClick: () => {
                            setShowImportDialog(true);
                        },
                    },
                    {
                        icon: faCloudDownload,
                        tooltip: _.exportTooltip,
                        onClick: () => {
                            const date = new Date().toISOString();
                            downloadObjectFile(`Export ${props.title} ${date}.${props.exportExtension}`, items);
                        },
                    },
                ]}
            />

            <Box sx={{backgroundColor: '#F3F3F3'}}>
                <Container sx={{py: 4, minHeight: '75vh'}}>
                    <Box
                        sx={{
                            mt: 3,
                            mb: 6,
                        }}
                    >
                        <ListHeader
                            title={props.title}
                            search={search}
                            searchPlaceholder={props.searchPlaceholder}
                            onSearchChange={setSearch}
                            actions={[
                                {
                                    label: props.addLabel,
                                    icon: faPlus,
                                    onClick: handleItemCreate
                                },
                            ]}
                        />
                    </Box>
                    <Box sx={{mt: 3, mb: 5}}>
                        {
                            items.length === 0 &&
                            <EmptyDataListPlaceholder
                                helperText={props.noItemsHelperText}
                                addText={props.addLabel}
                                onAdd={handleItemCreate}
                            />
                        }
                        {
                            items.length > 0 &&
                            filteredItems.length === 0 &&
                            <EmptySearchDataListPlaceholder
                                helperText={props.emptySearchHelperText}
                            />
                        }

                        <List>
                            {
                                filteredItems
                                    .map(item => (
                                        <ListItem
                                            component={Paper}
                                            key={item.id}
                                            sx={{mb: 2}}
                                            secondaryAction={
                                                <Box>
                                                    <Button
                                                        onClick={() => handleItemEdit(item)}
                                                        startIcon={
                                                            <FontAwesomeIcon
                                                                icon={faEdit}
                                                                style={{marginTop: '-2px'}}
                                                            />
                                                        }
                                                    >
                                                        {_.editLabel}
                                                    </Button>

                                                    <Button
                                                        onClick={() => handleItemDelete(item)}
                                                        startIcon={
                                                            <FontAwesomeIcon
                                                                icon={faTrashAlt}
                                                                style={{marginTop: '-2px'}}
                                                            />
                                                        }
                                                        sx={{ml: 2}}
                                                    >
                                                        {_.deleteLabel}
                                                    </Button>
                                                </Box>
                                            }
                                        >
                                            <ListItemText
                                                primary={props.toPrimaryString(item)}
                                                secondary={props.toSecondaryString != null ? props.toSecondaryString(item) : undefined}
                                            />
                                        </ListItem>
                                    ))
                            }
                        </List>
                    </Box>
                </Container>
            </Box>

            <AppFooter mode={AppMode.Staff}/>

            <EditDialog
                onClose={() => setItemToEdit(undefined)}
                onSave={handleItemSave}
                fieldsToEdit={props.fieldsToEdit ?? []}
                toPrimaryString={props.toPrimaryString}
                item={itemToEdit}
                open={true}
            />

            <ImportDialog
                onImport={handleImport}
                extension={props.exportExtension}
                onClose={() => setShowImportDialog(false)}
                open={showImportDialog}
            />

            {
                isLoading &&
                <Box
                    sx={{
                        position: 'fixed',
                        top: 0,
                        left: 0,
                        right: 0,
                        bottom: 0,
                        backgroundColor: '#ffffff55',
                    }}
                >
                    <CircularProgress
                        sx={{
                            position: 'absolute',
                            left: '50%',
                            top: '50%',
                            transform: 'translate(-50%, -50%)'
                        }}
                    />
                </Box>
            }
        </>
    );
}
