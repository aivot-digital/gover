import React, {useCallback, useEffect, useMemo, useState} from 'react';
import {type ReplicatingContainerLayout} from '../../models/elements/form/layout/replicating-container-layout';
import {ViewDispatcherComponent} from '../view-dispatcher.component';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import FormHelperText from '@mui/material/FormHelperText';
import FormLabel from '@mui/material/FormLabel';
import Grid from '@mui/material/Grid';
import Skeleton from '@mui/material/Skeleton';
import Typography from '@mui/material/Typography';
import {stringOrDefault} from '../../utils/string-utils';
import {type BaseViewProps} from '../../views/base-view';
import AddCircleOutlineOutlinedIcon from '@mui/icons-material/AddCircleOutlineOutlined';
import DeleteForeverOutlinedIcon from '@mui/icons-material/DeleteForeverOutlined';
import {ConfirmDialog} from '../../dialogs/confirm-dialog/confirm-dialog';
import {hasDerivableAspects} from '../../utils/has-derivable-aspects';
import {flattenElements} from '../../utils/flatten-elements';
import {type ElementData} from '../../models/element-data';

export function ReplicatingContainerView(props: BaseViewProps<ReplicatingContainerLayout, ElementData[]>) {
    const [confirmDelete, setConfirmDelete] = useState<() => void>();

    const {
        rootElement,
        element,
        value,
        setValue,
        onBlur,
        allElements,
        isDeriving,
        isBusy,
        elementData,
        onElementDataChange,
        onElementBlur,
        disableVisibility,
        mode,
        errors,
        scrollContainerRef,
        derivationTriggerIdQueue,
    } = props;

    const {
        children,
    } = element;

    const isDisabled = useMemo(() => {
        if (element.disabled) {
            return true;
        }

        if (isBusy) {
            return true;
        }

        if (isDeriving && hasDerivableAspects(element)) {
            return true;
        }

        return false;
    }, [element, isBusy, isDeriving]);

    const minRequiredSets = element.required === true ? (element.minimumRequiredSets ?? 1) : 0;

    useEffect(() => {
        if (minRequiredSets > 0 && (value == null || value.length < minRequiredSets)) {
            const forcedChildren = Array.from({length: minRequiredSets}, () => ({} as ElementData));
            setValue(forcedChildren);
        }
    }, [setValue, value, element]);

    const handleAdd = useCallback(() => {
        const updatedValue: ElementData[] = [
            ...(value ?? []),
            {},
        ];

        setValue(updatedValue, [`${element.id}.${updatedValue.length - 1}`]);
    }, [element, setValue, value]);

    const handleDelete = useCallback((_: ElementData, index: number) => {
        const newValue = (value ?? [])
            .filter((_: ElementData, i: number) => i !== index);

        const allChildIds = flattenElements(element, false)
            .map(child => child.id);

        setValue(newValue.length === 0 ? undefined : newValue, allChildIds);
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
            {derivationTriggerIdQueue}
            {
                value?.map((val: ElementData, valueIndex: number) => derivationTriggerIdQueue.includes(`${element.id}.${valueIndex}`) ? ( /* TODO: Fix skeletons */
                    // Skeleton
                    (<Box
                        key={valueIndex}
                        sx={{
                            my: 2,
                            px: 3,
                            pt: 2.4,
                            pb: 3.4,
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
                            <Skeleton
                                variant="text"
                                width="30%"
                                height={32}
                            />
                            <Skeleton
                                variant="rectangular"
                                width={170}
                                height={36}
                                sx={{ml: 'auto'}}
                            />
                        </Box>
                        <Grid
                            container
                            spacing={2}
                            sx={{mt: 0}}
                        >
                            {
                                Array.from({length: (element.children?.length ?? 2)}).map((_, i) => (
                                    <Grid key={i} size={12}>
                                        <Skeleton
                                            variant="rectangular"
                                            height={56}
                                            sx={{borderRadius: '4px'}}
                                        />
                                    </Grid>
                                ))
                            }
                        </Grid>
                    </Box>)
                ) : (
                    <Box
                        key={valueIndex}
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
                                (element.disabled !== true) &&
                                (minRequiredSets === 0 || valueIndex >= minRequiredSets) &&
                                <Box
                                    sx={{
                                        ml: 'auto',
                                    }}
                                >
                                    <Button
                                        color="error"
                                        size="small"
                                        endIcon={
                                            <DeleteForeverOutlinedIcon
                                                sx={{
                                                    marginTop: '-4px',
                                                }}
                                            />
                                        }
                                        onClick={() => {
                                            setConfirmDelete(() => () => handleDelete(val, valueIndex));
                                        }}
                                        disabled={isDisabled || (minRequiredSets > 0 && (value ?? []).length <= minRequiredSets)}
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
                            sx={{mt: 0}}
                        >
                            {
                                (element.children ?? []).map((child, childIndex) => (
                                    <ViewDispatcherComponent
                                        rootElement={rootElement}
                                        allElements={allElements}
                                        key={childIndex}
                                        element={child}
                                        isBusy={isBusy}
                                        isDeriving={props.isDeriving}
                                        mode={props.mode}
                                        elementData={val}
                                        onElementDataChange={(data, triggeringElementIds) => {
                                            const newValue = (value ?? [])
                                                .map((v, i) => i === valueIndex ? data : v);
                                            setValue(newValue, triggeringElementIds);
                                        }}
                                        onElementBlur={(data, triggeringElementIds) => {
                                            const newValue = (value ?? [])
                                                .map((v, i) => i === valueIndex ? data : v);
                                            onBlur(newValue, triggeringElementIds);
                                        }}
                                        disableVisibility={disableVisibility}
                                        scrollContainerRef={scrollContainerRef}
                                        derivationTriggerIdQueue={derivationTriggerIdQueue}
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
                        disabled={isDisabled || (element.maximumSets != null && element.maximumSets > 0 && (value ?? []).length >= element.maximumSets)}
                    >
                        {
                            stringOrDefault(element.addLabel, 'Datensatz hinzufügen')
                        }
                    </Button>
                </div>
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

