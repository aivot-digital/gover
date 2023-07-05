import {SelectAssetDialogProps} from "./select-asset-dialog-props";
import {Box, Dialog, DialogContent, List, ListItem, ListItemButton, ListItemText} from "@mui/material";
import {PropsWithChildren, useEffect, useState} from "react";
import {DialogTitleWithClose} from "../../components/static-components/dialog-title-with-close/dialog-title-with-close";
import {SearchInput} from "../../components/search-input/search-input";
import {AssetService} from "../../services/asset-service";
import {AlertComponent} from "../../components/alert/alert-component";
import {Link} from "react-router-dom";

export function SelectAssetDialog(props: PropsWithChildren<SelectAssetDialogProps>) {
    const [search, setSearch] = useState('');
    const [assets, setAssets] = useState<string[]>();

    useEffect(() => {
        AssetService
            .list()
            .then(setAssets);
    }, []);

    const handleClose = () => {
        setSearch('');
        props.onCancel();
    };

    const handleSelect = (asset: string) => {
        setSearch('');
        props.onSelect(asset);
    }

    const filteredAssets = assets == null ? undefined : assets.filter(ass => ass.toLowerCase().includes(search.toLowerCase()));

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


            <DialogContent>
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
                        filteredAssets.map(usr => (
                            <ListItem
                                key={usr}
                            >
                                <ListItemButton
                                    onClick={() => handleSelect(usr)}
                                >
                                    <ListItemText primary={usr}/>
                                </ListItemButton>
                            </ListItem>
                        ))
                    }
                </List>
            </DialogContent>
        </Dialog>
    );
}