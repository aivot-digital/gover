import {ProcessNodeProvider} from "../services/process-node-provider-api-service";
import {Button, Dialog, DialogContent} from "@mui/material";
import {useMemo} from "react";

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
                {
                    filteredNodeProviders
                        .map((provider) => (
                            <Button
                                key={provider.key}
                                onClick={() => {
                                    onSelect(provider);
                                    onClose();
                                }}
                            >
                                {provider.name} (Version {provider.version})
                            </Button>
                        ))
                }
            </DialogContent>
        </Dialog>
    );
}