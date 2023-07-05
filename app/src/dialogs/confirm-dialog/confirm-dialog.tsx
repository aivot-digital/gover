import {Button, Dialog, DialogActions, DialogContent} from "@mui/material";
import {DialogTitleWithClose} from "../../components/static-components/dialog-title-with-close/dialog-title-with-close";
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

            <DialogContent>
                {props.children}
            </DialogContent>

            <DialogActions>
                <Button onClick={props.onConfirm}>
                    Ja
                </Button>

                <Button onClick={props.onCancel}>
                    Nein
                </Button>
            </DialogActions>
        </Dialog>
    );
}