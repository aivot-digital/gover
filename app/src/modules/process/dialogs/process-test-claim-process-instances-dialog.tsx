import Refresh from '@mui/icons-material/Refresh';
import {
    Button,
    Dialog,
    DialogContent,
    Skeleton,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Typography,
} from '@mui/material';
import React, {type ReactNode, useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar} from '../../../slices/snackbar-slice';
import {type ProcessInstanceEntity} from '../entities/process-instance-entity';
import {ProcessInstanceStatusLabels} from '../enums/process-instance-status';
import {ProcessInstanceApiService} from '../services/process-instance-api-service';

interface ProcessTestClaimProcessInstancesDialogProps {
    open: boolean;
    onClose: () => void;
    testClaimId: number | null;
    selectedInstanceId: number | null;
    onSelectInstance: (instanceId: number) => void;
}

function formatStartedAt(value: string | null | undefined): string {
    if (value == null || value.trim().length === 0) {
        return '—';
    }

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return '—';
    }

    return new Intl.DateTimeFormat('de-DE', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        hour12: false,
    }).format(date).replace(',', ' –') + ' Uhr';
}

function getInstanceStateLabel(instance: ProcessInstanceEntity): string {
    if (instance.statusOverride != null && instance.statusOverride.trim().length > 0) {
        return instance.statusOverride;
    }

    return ProcessInstanceStatusLabels[instance.status];
}

export function ProcessTestClaimProcessInstancesDialog(props: ProcessTestClaimProcessInstancesDialogProps): ReactNode {
    const {
        open,
        onClose,
        testClaimId,
        selectedInstanceId,
        onSelectInstance,
    } = props;

    const dispatch = useAppDispatch();
    const [items, setItems] = useState<ProcessInstanceEntity[] | null>(null);
    const [isLoading, setIsLoading] = useState(false);
    const loadRequestRef = useRef(0);

    const loadItems = useCallback(async (): Promise<void> => {
        if (testClaimId == null) {
            setItems([]);
            return;
        }

        const requestId = loadRequestRef.current + 1;
        loadRequestRef.current = requestId;
        setIsLoading(true);

        try {
            const response = await new ProcessInstanceApiService().listAllOrdered(
                'started',
                'DESC',
                {
                    createdForTestClaimId: testClaimId,
                },
            );

            const sortedItems = [...response.content].sort((leftItem, rightItem) => (
                new Date(rightItem.started).getTime() - new Date(leftItem.started).getTime()
            ));

            if (loadRequestRef.current !== requestId) {
                return;
            }

            setItems(sortedItems);
        } catch (error) {
            if (loadRequestRef.current !== requestId) {
                return;
            }

            setItems([]);
            dispatch(showApiErrorSnackbar(error, 'Die Test-Vorgänge konnten nicht geladen werden.'));
        } finally {
            if (loadRequestRef.current === requestId) {
                setIsLoading(false);
            }
        }
    }, [dispatch, testClaimId]);

    useEffect(() => {
        if (!open) {
            loadRequestRef.current += 1;
            setItems(null);
            setIsLoading(false);
            return;
        }

        void loadItems();
    }, [loadItems, open]);

    const title = useMemo(() => {
        if (items == null) {
            return 'Test-Vorgänge';
        }

        return `Test-Vorgänge (${items.length})`;
    }, [items]);

    return (
        <Dialog
            open={open}
            onClose={onClose}
            fullWidth
            maxWidth="md"
        >
            <DialogTitleWithClose
                onClose={onClose}
                actions={[{
                    tooltip: 'Test-Vorgänge neu laden',
                    ariaLabel: 'Test-Vorgänge neu laden',
                    icon: <Refresh/>,
                    onClick: () => {
                        void loadItems();
                    },
                    disabled: testClaimId == null || isLoading,
                }]}
            >
                {title}
            </DialogTitleWithClose>

            <DialogContent>
                {
                    testClaimId == null ?
                        <Typography color="text.secondary">
                            Für diese Ansicht ist kein Testanspruch verfügbar.
                        </Typography> :
                        <TableContainer
                            sx={{
                                maxHeight: 560,
                                overflow: 'auto',
                            }}
                        >
                            <Table
                                size="small"
                                stickyHeader={true}
                            >
                                <TableHead>
                                    <TableRow>
                                        <TableCell>
                                            Gestartet
                                        </TableCell>
                                        <TableCell>
                                            Status
                                        </TableCell>
                                        <TableCell align="right">
                                            Aktion
                                        </TableCell>
                                    </TableRow>
                                </TableHead>

                                <TableBody>
                                    {
                                        items == null &&
                                        Array.from({length: 4}).map((_, index) => (
                                            <TableRow key={index}>
                                                <TableCell>
                                                    <Skeleton width="80%"/>
                                                </TableCell>
                                                <TableCell>
                                                    <Skeleton width="70%"/>
                                                </TableCell>
                                                <TableCell align="right">
                                                    <Skeleton
                                                        width={88}
                                                        sx={{ml: 'auto'}}
                                                    />
                                                </TableCell>
                                            </TableRow>
                                        ))
                                    }

                                    {
                                        items != null &&
                                        items.length === 0 &&
                                        <TableRow>
                                            <TableCell
                                                colSpan={3}
                                                align="center"
                                                sx={{py: 3}}
                                            >
                                                Keine Test-Vorgänge gefunden.
                                            </TableCell>
                                        </TableRow>
                                    }

                                    {
                                        items != null &&
                                        items.map((instance) => {
                                            const isSelected = selectedInstanceId === instance.id;

                                            return (
                                                <TableRow
                                                    key={instance.id}
                                                    hover
                                                    selected={isSelected}
                                                    onClick={() => {
                                                        onSelectInstance(instance.id);
                                                    }}
                                                    sx={{
                                                        cursor: 'pointer',
                                                    }}
                                                >
                                                    <TableCell>
                                                        {formatStartedAt(instance.started)}
                                                    </TableCell>

                                                    <TableCell>
                                                        {getInstanceStateLabel(instance)}
                                                    </TableCell>

                                                    <TableCell align="right">
                                                        <Button
                                                            size="small"
                                                            variant={isSelected ? 'contained' : 'outlined'}
                                                            onClick={(event) => {
                                                                event.stopPropagation();
                                                                onSelectInstance(instance.id);
                                                            }}
                                                        >
                                                            {isSelected ? 'Neu laden' : 'Laden'}
                                                        </Button>
                                                    </TableCell>
                                                </TableRow>
                                            );
                                        })
                                    }
                                </TableBody>
                            </Table>
                        </TableContainer>
                }
            </DialogContent>
        </Dialog>
    );
}
