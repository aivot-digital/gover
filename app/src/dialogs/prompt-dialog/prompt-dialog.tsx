import { useState } from "react";
import { Button, Dialog, DialogActions, DialogContent, TextField, Typography } from "@mui/material";
import { DialogTitleWithClose } from "../../components/dialog-title-with-close/dialog-title-with-close";

export interface PromptDialogProps {
    title: string;
    message?: string;
    inputLabel?: string;
    inputPlaceholder?: string;
    confirmButtonText?: string;
    cancelButtonText?: string;
    onConfirm: (value: string) => void;
    onCancel: () => void;
    defaultValue?: string;
}

export function PromptDialog(props: PromptDialogProps) {
    const [inputValue, setInputValue] = useState(props.defaultValue ?? "");

    return (
        <Dialog
            open
            onClose={props.onCancel}
            maxWidth="xs"
        >
            <DialogTitleWithClose onClose={props.onCancel}>
                {props.title}
            </DialogTitleWithClose>

            <DialogContent tabIndex={0}>
                {props.message && (
                    <Typography variant="body2" sx={{ mb: 2 }}>
                        {props.message}
                    </Typography>
                )}

                <TextField
                    sx={{ mt: 2 }}
                    fullWidth
                    label={props.inputLabel || "Eingabe"}
                    placeholder={props.inputPlaceholder}
                    value={inputValue}
                    onChange={(e) => setInputValue(e.target.value)}
                    autoFocus
                />
            </DialogContent>

            <DialogActions sx={{ justifyContent: 'flex-start' }}>
                <Button
                    onClick={() => props.onConfirm(inputValue)}
                    variant="contained"
                    color="primary"
                >
                    {props.confirmButtonText || "OK"}
                </Button>

                <Button
                    onClick={props.onCancel}
                    variant="outlined"
                >
                    {props.cancelButtonText || "Abbrechen"}
                </Button>
            </DialogActions>
        </Dialog>
    );
}
