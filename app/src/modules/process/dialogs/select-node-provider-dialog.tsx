import {ProcessNodeProvider} from '../services/process-node-provider-api-service';
import {Box, Button, Dialog, DialogContent, List, ListItemAvatar, ListItemButton, ListItemText} from '@mui/material';
import {useMemo} from 'react';
import Chip from '@mui/material/Chip';

interface SelectNodeProviderDialogProps {
    open: boolean;
    nodeProviders: ProcessNodeProvider[];
    onClose: () => void;
    onSelect: (provider: ProcessNodeProvider) => void;
    filter?: (provider: ProcessNodeProvider) => boolean;
}

export function SelectNodeProviderDialog(props: SelectNodeProviderDialogProps) {
    const {
        open,
        nodeProviders,
        onClose,
        onSelect,
        filter,
    } = props;

    const filteredNodeProviders = useMemo(() => {
        if (filter == null) {
            return nodeProviders;
        }

        return nodeProviders.filter(filter);
    }, [nodeProviders, filter]);

    return (
        <Dialog
            open={open}
            onClose={onClose}
            fullWidth
            maxWidth="md"
        >
            <DialogContent>
                <List>
                    {
                        filteredNodeProviders
                            .map((provider) => (
                                <ListItemButton
                                    key={provider.key}
                                    onClick={() => {
                                        onSelect(provider);
                                        onClose();
                                    }}
                                >
                                    <ListItemText
                                        primary={
                                            <Box
                                                sx={{
                                                    display: 'flex',
                                                    alignItems: 'center',
                                                    gap: 1,
                                                }}
                                            >
                                                {provider.name}
                                                <Chip size="small" label={`Version ${provider.version}`} sx={{ml: 'auto'}}/>
                                            </Box>
                                        }
                                        secondary={provider.description}
                                    />
                                </ListItemButton>
                            ))
                    }
                </List>
            </DialogContent>
        </Dialog>
    );
}