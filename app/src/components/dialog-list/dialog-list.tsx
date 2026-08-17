import {FunctionComponent, useState} from 'react';
import {
    Box,
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    List,
    ListItemButton,
    ListItemText,
    Tooltip,
    Typography,
} from '@mui/material';
import {alpha} from '@mui/material/styles';
import {DialogTitleWithClose} from '../dialog-title-with-close/dialog-title-with-close';
import {useConfirm} from '../../providers/confirm-provider';
import {deepEquals} from '../../utils/equality-utils';
import {Actions} from '../actions/actions';
import Edit from '@aivot/mui-material-symbols-400-n25-outlined/Edit';
import Delete from '@aivot/mui-material-symbols-400-n25-outlined/Delete';
import Visibility from '@aivot/mui-material-symbols-400-n25-outlined/Visibility';
import ErrorIcon from '@aivot/mui-material-symbols-400-n25-outlined/Error';

export type DialogListPropsDialogContentComponent<T> = FunctionComponent<{
    item: T;
    onChange: (item: T) => void;
    disabled?: boolean;
}>

interface DialogListProps<T> {
    dialogTitle: string;
    dialogViewTitle?: string;
    items: T[];
    getId: (item: T) => string;
    title: (item: T) => string;
    subTitle?: (item: T) => string;
    dialogContentComponent: DialogListPropsDialogContentComponent<T>;
    onDialogSave: (edited: T, original: T) => void;
    onDelete: (item: T) => void;
    disabled?: boolean;
    hasError?: (item: T) => boolean;
}

export function DialogList<T>(props: DialogListProps<T>) {
    const {
        dialogTitle,
        dialogViewTitle,
        items,
        getId,
        title,
        subTitle,
        dialogContentComponent: DialogContentComponent,
        onDialogSave,
        onDelete,
        disabled,
        hasError,
    } = props;

    const isReadonly = disabled === true;
    const confirm = useConfirm();

    const [showDialog, setShowDialog] = useState(false);

    const [openForItem, setOpenForItem] = useState<{
        edited: T;
        original: T;
    }>();

    const handleDialogOpen = (item: T) => {
        setOpenForItem({
            edited: JSON.parse(JSON.stringify(item)),
            original: item,
        });
        setShowDialog(true);
    };

    const handleDialogSave = () => {
        if (openForItem == null) {
            return;
        }

        onDialogSave(openForItem.edited, openForItem.original);

        closeDialog();
    };

    const handleCancel = async () => {
        if (openForItem == null) {
            return;
        }

        if (!isReadonly && !deepEquals(openForItem.original, openForItem.edited)) {
            const conf = await confirm({
                title: 'Änderungen verwerfen',
                children: (
                    <Typography>
                        Sollen die Änderungen wirklich verworfen werden?
                    </Typography>
                ),
            });

            if (!conf) {
                return;
            }
        }

        closeDialog();
    };

    const closeDialog = () => {
        setShowDialog(false);
        // Reset the item with a timeout to prevent the dialog content from being cleared,
        // before the dialog has fully vanished.
        setTimeout(() => {
            setOpenForItem(undefined);
        }, 200);
    };

    const handleDelete = async (item: T) => {
        const conf = await confirm({
            title: 'Eintrag löschen',
            children: (
                <Typography>
                    Soll der Eintrag wirklich gelöscht werden?
                </Typography>
            ),
        });

        if (!conf) {
            return;
        }

        onDelete(item);
    };

    return (
        <>
            <List disablePadding>
                {
                    items.map((item) => {
                        const itemHasError = hasError?.(item) === true;

                        return (
                            <ListItemButton
                                key={getId(item)}
                                sx={(theme) => ({
                                    mb: 1,
                                    border: '1px solid',
                                    borderColor: itemHasError ? theme.palette.error.main : 'divider',
                                    borderRadius: 1,
                                    '&:hover': {
                                        borderColor: itemHasError ? theme.palette.error.main : theme.palette.primary.main,
                                        backgroundColor: alpha(itemHasError ? theme.palette.error.main : theme.palette.primary.main, 0.04),
                                    },
                                })}
                                onClick={() => {
                                    handleDialogOpen(item);
                                }}
                            >
                                <ListItemText
                                    primary={title(item)}
                                    secondary={subTitle?.(item)}
                                />

                                {
                                    itemHasError &&
                                    <Tooltip
                                        title="Fehler in diesem Eintrag"
                                        arrow
                                    >
                                        <Box
                                            component="span"
                                            role="img"
                                            aria-label="Fehler in diesem Eintrag"
                                            sx={{
                                                display: 'inline-flex',
                                                alignItems: 'center',
                                                mr: 1,
                                                color: 'error.main',
                                            }}
                                        >
                                            <ErrorIcon fontSize="small"/>
                                        </Box>
                                    </Tooltip>
                                }

                                <Actions
                                    actions={
                                        isReadonly
                                            ? [
                                                {
                                                    icon: <Visibility/>,
                                                    tooltip: 'Ansehen',
                                                    onClick: (evt) => {
                                                        evt.preventDefault();
                                                        evt.stopPropagation();
                                                        handleDialogOpen(item);
                                                    },
                                                },
                                            ]
                                            : [
                                                {
                                                    icon: <Edit/>,
                                                    tooltip: 'Bearbeiten',
                                                    onClick: (evt) => {
                                                        evt.preventDefault();
                                                        evt.stopPropagation();
                                                        handleDialogOpen(item);
                                                    },
                                                },
                                                {
                                                    icon: <Delete/>,
                                                    tooltip: 'Eintrag löschen',
                                                    onClick: (evt) => {
                                                        evt.preventDefault();
                                                        evt.stopPropagation();
                                                        handleDelete(item);
                                                    },
                                                },
                                            ]
                                    }
                                />
                            </ListItemButton>
                        );
                    })
                }
            </List>

            <Dialog
                open={showDialog}
                onClose={handleCancel}
                fullWidth={true}
                maxWidth="md"
            >
                <DialogTitleWithClose
                    onClose={handleCancel}
                >
                    {isReadonly ? dialogViewTitle ?? dialogTitle : dialogTitle}
                </DialogTitleWithClose>

                <DialogContent>
                    {
                        openForItem != null &&
                        <DialogContentComponent
                            item={openForItem.edited}
                            onChange={(changed) => {
                                setOpenForItem({
                                    ...openForItem,
                                    edited: changed,
                                });
                            }}
                            disabled={disabled}
                        />
                    }
                </DialogContent>

                <DialogActions
                    sx={{
                        justifyContent: 'flex-start',
                        pt: 2,
                    }}
                >
                    {
                        !isReadonly &&
                        <Button
                            variant="contained"
                            onClick={handleDialogSave}
                        >
                            Übernehmen
                        </Button>
                    }

                    <Button
                        sx={{
                            ml: isReadonly ? 0 : 'auto',
                        }}
                        onClick={handleCancel}
                    >
                        {isReadonly ? 'Schließen' : 'Abbrechen'}
                    </Button>
                </DialogActions>
            </Dialog>
        </>
    );
}
