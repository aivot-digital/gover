import {FunctionComponent, useState} from 'react';
import {
    Box,
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    List,
    ButtonBase,
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
import {FormFieldTokens} from '../../theming/form-field-tokens';
import ErrorIcon from '@aivot/mui-material-symbols-400-n25-outlined/Error';

export type DialogListPropsDialogContentComponent<T> = FunctionComponent<{
    item: T;
    onChange: (item: T) => void;
    readOnly?: boolean;
    busy?: boolean;
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
    // Read-only lists keep their detail dialog available; busy only suspends mutations temporarily.
    readOnly?: boolean;
    busy?: boolean;
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
        readOnly,
        busy,
        hasError,
    } = props;

    const isReadOnly = readOnly === true;
    const isBusy = busy === true;
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
        if (openForItem == null || isReadOnly || isBusy) {
            return;
        }

        onDialogSave(openForItem.edited, openForItem.original);

        closeDialog();
    };

    const handleCancel = async () => {
        if (openForItem == null) {
            return;
        }

        if (!isReadOnly && !deepEquals(openForItem.original, openForItem.edited)) {
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
        if (isReadOnly || isBusy) {
            return;
        }

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
            <List
                disablePadding
                data-dialog-list
                sx={{
                    minHeight: FormFieldTokens.controlWithSecondaryTextMinHeight,
                    overflow: 'hidden',
                    border: '1px solid',
                    borderColor: 'divider',
                    borderRadius: 1,
                    backgroundColor: 'background.paper',
                }}
            >
                {
                    items.map((item, index) => {
                        const itemHasError = hasError?.(item) === true;

                        return (
                            <Box
                                component="li"
                                key={getId(item)}
                                data-dialog-list-item
                                sx={(theme) => ({
                                    display: 'grid',
                                    gridTemplateColumns: 'minmax(0, 1fr) auto',
                                    alignItems: 'stretch',
                                    minHeight: FormFieldTokens.groupedControlRowMinHeight,
                                    borderTop: index === 0 ? 0 : '1px solid',
                                    borderColor: 'divider',
                                    boxShadow: itemHasError
                                        ? `inset 3px 0 ${theme.palette.error.main}`
                                        : undefined,
                                    '&:hover': {
                                        backgroundColor: alpha(itemHasError ? theme.palette.error.main : theme.palette.primary.main, 0.04),
                                    },
                                })}
                            >
                                <ButtonBase
                                    aria-haspopup="dialog"
                                    aria-invalid={itemHasError || undefined}
                                    onClick={() => {
                                        handleDialogOpen(item);
                                    }}
                                    sx={{
                                        minWidth: 0,
                                        display: 'flex',
                                        alignItems: 'stretch',
                                        justifyContent: 'flex-start',
                                        px: 1.5,
                                        py: 0.5,
                                        textAlign: 'left',
                                        '&.Mui-focusVisible': {
                                            outline: '2px solid',
                                            outlineColor: 'primary.main',
                                            outlineOffset: '-2px',
                                        },
                                    }}
                                >
                                    <Box
                                        sx={{
                                            minWidth: 0,
                                            flex: 1,
                                            display: 'flex',
                                            flexDirection: 'column',
                                            justifyContent: 'center',
                                            gap: 0.25,
                                        }}
                                    >
                                        <Typography
                                            title={title(item)}
                                            sx={{
                                                overflow: 'hidden',
                                                textOverflow: 'ellipsis',
                                                whiteSpace: 'nowrap',
                                                fontSize: '1rem',
                                                lineHeight: 1.25,
                                            }}
                                        >
                                            {title(item)}
                                        </Typography>

                                        {subTitle != null && (
                                            <Typography
                                                variant="caption"
                                                title={subTitle(item)}
                                                sx={{
                                                    overflow: 'hidden',
                                                    textOverflow: 'ellipsis',
                                                    whiteSpace: 'nowrap',
                                                    color: isReadOnly || isBusy ? 'text.disabled' : 'text.secondary',
                                                    fontSize: '0.75rem',
                                                    lineHeight: 1.2,
                                                }}
                                            >
                                                {subTitle(item)}
                                            </Typography>
                                        )}
                                    </Box>

                                    {itemHasError && (
                                        <Tooltip title="Fehler in diesem Eintrag" arrow>
                                            <Box
                                                component="span"
                                                role="img"
                                                aria-label="Fehler in diesem Eintrag"
                                                sx={{
                                                    display: 'inline-flex',
                                                    alignItems: 'center',
                                                    ml: 1,
                                                    color: 'error.main',
                                                }}
                                            >
                                                <ErrorIcon fontSize="small"/>
                                            </Box>
                                        </Tooltip>
                                    )}
                                </ButtonBase>

                                <Actions
                                    dense
                                    isBusy={isBusy}
                                    sx={{pr: 0.75}}
                                    actions={
                                        isReadOnly
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
                            </Box>
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
                    {isReadOnly ? dialogViewTitle ?? dialogTitle : dialogTitle}
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
                            readOnly={isReadOnly}
                            busy={isBusy}
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
                        !isReadOnly &&
                        <Button
                            variant="contained"
                            onClick={handleDialogSave}
                            disabled={isBusy}
                        >
                            Übernehmen
                        </Button>
                    }

                    <Button
                        sx={{
                            ml: isReadOnly ? 0 : 'auto',
                        }}
                        onClick={handleCancel}
                    >
                        {isReadOnly ? 'Schließen' : 'Abbrechen'}
                    </Button>
                </DialogActions>
            </Dialog>
        </>
    );
}
