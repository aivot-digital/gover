import {Button, Dialog, DialogActions, DialogContent} from "@mui/material";
import {DialogTitleWithClose} from "../../components/dialog-title-with-close/dialog-title-with-close";
import {PropsWithChildren} from "react";

interface ConfirmDialogProps {
    title: string;
    onCancel: () => void;
    onConfirm?: () => void;
}

export function ConfirmDialog(props: PropsWithChildren<ConfirmDialogProps>) {
    return (
        <Dialog
            open={props.onConfirm != null}
            onClose={props.onCancel}
        >

            <DialogTitleWithClose
                onClose={props.onCancel}
            >
                {props.title}
            </DialogTitleWithClose>

            <DialogContent tabIndex={0}>
                {props.children}
            </DialogContent>

            <DialogActions sx={{justifyContent: 'flex-start'}}>
                <Button
                    onClick={props.onConfirm}
                    variant="contained">
                    Bestätigen
                </Button>

                <Button
                    onClick={props.onCancel}
                    variant="outlined"
                >
                    Abbrechen
                </Button>
            </DialogActions>
        </Dialog>
    );
}
