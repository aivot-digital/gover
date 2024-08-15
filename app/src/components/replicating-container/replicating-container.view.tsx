import React, {useCallback, useEffect, useMemo, useState} from 'react';
import {type ReplicatingContainerLayout} from '../../models/elements/form/layout/replicating-container-layout';
import {ViewDispatcherComponent} from '../view-dispatcher.component';
import {Box, Button, Dialog, DialogContent, FormHelperText, FormLabel, Grid, IconButton, Typography} from '@mui/material';
import {stringOrDefault} from '../../utils/string-utils';
import {generateElementIdForReplicatingContainerChild} from '../../utils/id-utils';
import {type BaseViewProps} from '../../views/base-view';
import AddCircleOutlineOutlinedIcon from '@mui/icons-material/AddCircleOutlineOutlined';
import DeleteForeverOutlinedIcon from '@mui/icons-material/DeleteForeverOutlined';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {useAppSelector} from '../../hooks/use-app-selector';
import {showDialog} from '../../slices/app-slice';
import OpenInNewOutlinedIcon from '@mui/icons-material/OpenInNewOutlined';
import {DialogTitleWithClose} from '../dialog-title-with-close/dialog-title-with-close';
import {selectBooleanSystemConfigValue} from '../../slices/system-config-slice';
import {SystemConfigKeys} from '../../data/system-config-keys';
import {ConfirmDialog} from "../../dialogs/confirm-dialog/confirm-dialog";

export function ReplicatingContainerView(props: BaseViewProps<ReplicatingContainerLayout, string[]>): JSX.Element {
    const dispatch = useAppDispatch();
    const dialog = useAppSelector(state => state.app.showDialog);
    const experimentalFeaturesAdditionalDialogs = useAppSelector(selectBooleanSystemConfigValue(SystemConfigKeys.experimentalFeatures.additionalDialogs));
    const [confirmDelete, setConfirmDelete] = useState<() => void>();

    const {
        element,
        value,
        setValue,
        allElements,
    } = props;

    const itemAsDialog = useMemo(() => {
        if (value != null && dialog != null && value.includes(dialog)) {
            return dialog;
        }
        return undefined;
    }, [value, dialog]);

    const minRequiredSets = element.required === true ? (element.minimumRequiredSets ?? 1) : 0;

    useEffect(() => {
        if (minRequiredSets > 0 && (value == null || value.length < minRequiredSets)) {
            setValue(Array.from({length: minRequiredSets}, () => generateElementIdForReplicatingContainerChild()));
        }
    }, [setValue, value, element]);

    const handleAdd = useCallback(() => {
        setValue([...(value ?? []), generateElementIdForReplicatingContainerChild()]);
    }, [setValue, value]);

    const handleDelete = useCallback((id: string) => {
        setValue((value ?? []).filter((val: string) => val !== id));
        setConfirmDelete(undefined);
    }, [setValue, value]);

    return (
        <Box sx={{mt: 2}}>
            {
                (element.label != null) &&
                <FormLabel>
                    {element.label}
                </FormLabel>
            }

            {
                (element.hint != null) &&
                <FormHelperText>
                    {element.hint}
                </FormHelperText>
            }

            {
                value?.map((val: string, valueIndex: number) => (
                    <Box
                        key={val}
                        sx={{
                            my: 2,
                            px: 3,
                            py: 2.4,
                            border: '1px solid #D4D4D4',
                        }}
                    >
                        <Box
                            sx={{
                                display: 'flex',
                                alignItems: 'center',
                                flexWrap: 'wrap',
                                rowGap: '10px',
                            }}
                        >
                            <Typography
                                component={'p'}
                                variant="h6"
                                sx={{fontSize: '1.125rem'}}
                            >
                                {
                                    (element.headlineTemplate ?? '').replace('#', (valueIndex + 1).toFixed())
                                }
                            </Typography>

                            {
                                experimentalFeaturesAdditionalDialogs &&
                                <IconButton
                                    onClick={() => {
                                        dispatch(showDialog(val));
                                    }}
                                    sx={{
                                        ml: 1,
                                    }}
                                    size="small"
                                >
                                    <OpenInNewOutlinedIcon />
                                </IconButton>
                            }

                            {
                                (element.disabled !== true) &&
                                (minRequiredSets === 0 || valueIndex >= minRequiredSets) &&
                                <Box
                                    sx={{ml: 'auto'}}
                                >
                                    <Button
                                        color="error"
                                        size={'small'}
                                        endIcon={<DeleteForeverOutlinedIcon
                                            sx={{marginTop: '-4px'}}
                                        />}
                                        onClick={() => {
                                            setConfirmDelete(() => () => handleDelete(val));
                                        }}
                                        disabled={minRequiredSets > 0 && (value ?? []).length <= minRequiredSets}
                                    >
                                        {
                                            stringOrDefault(element.removeLabel, 'Datensatz löschen')
                                        }
                                    </Button>
                                </Box>
                            }
                        </Box>
                        <Grid
                            container
                            spacing={2}
                        >
                            {
                                (element.children ?? []).map((child, childIndex) => (
                                    <ViewDispatcherComponent
                                        allElements={allElements}
                                        key={childIndex}
                                        element={child}
                                        idPrefix={`${element.id}_${val}_`}
                                    />
                                ))
                            }
                        </Grid>
                    </Box>
                ))
            }
            {
                (element.disabled !== true) &&
                <div>
                    <Button
                        startIcon={<AddCircleOutlineOutlinedIcon
                            sx={{marginTop: '-2px'}}
                        />}
                        sx={{
                            mt: 2,
                            mb: 3,
                        }}
                        onClick={handleAdd}
                        variant={'outlined'}
                        disabled={element.maximumSets != null && element.maximumSets > 0 && (value ?? []).length >= element.maximumSets}
                    >
                        {
                            stringOrDefault(element.addLabel, 'Datensatz hinzufügen')
                        }
                    </Button>
                </div>
            }

            {
                itemAsDialog != null &&
                <Dialog
                    open={true}
                    disableEscapeKeyDown
                    fullWidth
                    maxWidth="lg"
                >
                    <DialogTitleWithClose
                        onClose={() => {
                            dispatch(showDialog(undefined));
                        }}
                    >
                        {
                            (element.headlineTemplate ?? '').replace('#', ((value ?? []).indexOf(itemAsDialog) + 1).toFixed())
                        }
                    </DialogTitleWithClose>
                    <DialogContent tabIndex={0}>
                        {
                            (element.children ?? []).map((child, childIndex) => (
                                <ViewDispatcherComponent
                                    allElements={allElements}
                                    key={childIndex}
                                    element={child}
                                    idPrefix={`${element.id}_${itemAsDialog}_`}
                                />
                            ))
                        }
                    </DialogContent>
                </Dialog>
            }

            <ConfirmDialog
                title="Möchten Sie diesen Datensatz wirklich löschen?"
                onConfirm={confirmDelete}
                onCancel={() => setConfirmDelete(undefined)}
            >
                Dieser Vorgang kann nicht rückgängig gemacht werden. Wenn Sie die Daten löschen, müssen Sie diese bei Bedarf erneut eingeben. Möchten Sie den Datensatz wirklich löschen?
            </ConfirmDialog>
        </Box>
    );
}

