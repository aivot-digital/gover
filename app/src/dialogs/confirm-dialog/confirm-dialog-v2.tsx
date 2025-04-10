import {Button, Dialog, DialogActions, DialogContent} from '@mui/material';
import {DialogTitleWithClose} from '../../components/dialog-title-with-close/dialog-title-with-close';
import {useMemo, useState} from 'react';
import {ConfirmDialogOptions} from '../../hooks/use-confirm-dialog';

interface ConfirmDialogV2Props<T> {
    options?: ConfirmDialogOptions<T>;
}

export function ConfirmDialogV2<T>(props: ConfirmDialogV2Props<T>) {
    const [updatedState, setUpdatedState] = useState<T>();

    const state = useMemo(() => {
        if (props.options == null) {
            return undefined;
        }

        return updatedState ?? props.options.state;
    }, [props.options, updatedState]);

    const handleConfirm = () => {
        if (props.options == null) {
            return;
        }

        if (state == null) {
            return;
        }

        props.options.onConfirm(state);
    };

    const handleCancel = () => {
        if (props.options == null) {
            return;
        }

        props.options.onCancel();
    };

    return (
        <Dialog
            open={props.options != null}
            onClose={props.options?.onCancel}
        >

            <DialogTitleWithClose
                onClose={props.options?.onCancel ?? (() => {
                })}
            >
                {props.options?.title}
            </DialogTitleWithClose>

            <DialogContent tabIndex={0}>
                {
                    props.options != null &&
                    state != null &&
                    props.options.onRender(state, (update: Partial<T>) => {
                        const updatedState = {
                            ...state,
                            ...update,
                        };
                        setUpdatedState(updatedState);
                    })
                }
            </DialogContent>

            <DialogActions
                sx={{
                    justifyContent: 'flex-start',
                }}
            >
                <Button
                    onClick={handleConfirm}
                    variant="contained"
                >
                    Bestätigen
                </Button>

                <Button
                    onClick={handleCancel}
                    variant="outlined"
                >
                    Abbrechen
                </Button>
            </DialogActions>
        </Dialog>
    );
}
