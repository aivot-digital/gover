import {SelectAssetDialogProps} from "./select-asset-dialog-props";
import {Box, Dialog, DialogContent, List, ListItem, ListItemButton, ListItemText} from "@mui/material";
import {PropsWithChildren, useEffect, useState} from "react";
import {DialogTitleWithClose} from "../../components/dialog-title-with-close/dialog-title-with-close";
import {SearchInput} from "../../components/search-input/search-input";
import {AlertComponent} from "../../components/alert/alert-component";
import {Link} from "react-router-dom";
import {useApi} from "../../hooks/use-api";
import {useAssetsApi} from "../../hooks/use-assets-api";
import {Asset} from "../../models/entities/asset";
import {filterItems} from "../../utils/filter-items";

export function SelectAssetDialog(props: PropsWithChildren<SelectAssetDialogProps>) {
    const api = useApi();
    const [search, setSearch] = useState('');
    const [assets, setAssets] = useState<Asset[]>();

    useEffect(() => {
        if (assets == null) {
            useAssetsApi(api)
                .list(props.mimetype)
                .then(setAssets);
        }
    }, [api, props.mimetype]);

    const handleClose = () => {
        setSearch('');
        props.onCancel();
    };

    const handleSelect = (asset: string) => {
        setSearch('');
        props.onSelect(asset);
    };

    const filteredAssets = filterItems(assets, 'filename', search);

    return (
        <Dialog
            fullWidth
            open={props.show}
            onClose={handleClose}
        >
            <DialogTitleWithClose
                onClose={handleClose}
            >
                {props.title}
            </DialogTitleWithClose>


            <DialogContent tabIndex={0}>
                {
                    props.children != null &&
                    <Box sx={{mb: 2}}>
                        {props.children}
                    </Box>
                }

                <SearchInput
                    value={search}
                    onChange={setSearch}
                    placeholder="Suchen..."
                />

                {
                    assets?.length === 0 &&
                    <AlertComponent
                        title="Keine Anlagen vorhanden"
                        color="info"
                    >
                        Es sind noch keine Anlagen vorhanden.
                        Gehen Sie in die <Link to="/assets">Liste der Anlagen</Link> und fügen Sie neue Anlagen hinzu.
                    </AlertComponent>
                }

                {
                    assets?.length !== 0 &&
                    filteredAssets?.length === 0 &&
                    <AlertComponent
                        title="Keine Anlagen vorhanden"
                        color="info"
                    >
                        Es sind noch keine Anlagen vorhanden die zu Ihrer Suche passen.
                        Gehen Sie in die <Link to="/assets">Liste der Anlagen</Link> und fügen Sie neue Anlagen hinzu.
                    </AlertComponent>
                }

                <List>
                    {
                        filteredAssets != null &&
                        filteredAssets.map(asset => (
                            <ListItem
                                key={asset.key}
                            >
                                <ListItemButton
                                    onClick={() => handleSelect(asset.key)}
                                >
                                    <ListItemText primary={asset.filename}/>
                                </ListItemButton>
                            </ListItem>
                        ))
                    }
                </List>
            </DialogContent>
        </Dialog>
    );
}
