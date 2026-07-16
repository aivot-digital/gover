import React, {useEffect, useState} from 'react';
import {
    Box,
    Button,
    Card,
    CardContent,
    Divider,
    List,
    ListItem,
    ListItemButton,
    Skeleton,
    Typography,
} from '@mui/material';
import ChevronRight from '@aivot/mui-material-symbols-400-n25-outlined/ChevronRight';
import {withAsyncWrapper} from '../../../utils/with-async-wrapper';
import {Page} from '../../../models/dtos/page';
import {Link} from 'react-router-dom';
import {ModuleIcons} from '../../../shells/staff/data/module-icons';
import type {ProcessEntity} from '../../process/entities/process-entity';
import {ProcessDefinitionApiService} from '../../process/services/process-definition-api-service';

const fetchSize = 4;

function formatUpdatedAt(value: string): string {
    if (value.trim().length === 0) {
        return 'Unbekannt';
    }

    const normalized = value.replace(/(\.\d{3})\d+/, '$1');
    const date = new Date(normalized);

    if (Number.isNaN(date.getTime())) {
        return 'Unbekannt';
    }

    return `${new Intl.DateTimeFormat('de-DE', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
    }).format(date)} Uhr`;
}

function createProcessLink(process: ProcessEntity): string {
    const editableVersion = process.draftedVersion ?? process.publishedVersion;

    if (editableVersion == null) {
        return '/processes';
    }

    return `/processes/${process.id}/versions/${editableVersion}`;
}

export function DashboardProcessesPanel() {
    const [processes, setProcesses] = useState<ProcessEntity[] | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        withAsyncWrapper<void, Page<ProcessEntity>>({
            main: () =>
                new ProcessDefinitionApiService()
                    .list(0, fetchSize, 'updated', 'DESC'),
            desiredMinRuntime: 600,
        }).then((page) => {
            setProcesses(page.content);
            setLoading(false);
        });
    }, []);

    return (
        <Card sx={{height: '100%', borderRadius: 2, position: 'relative', overflow: 'hidden'}}>
            <CardContent>
                <Box sx={{pt: 0.5, px: 1}}>
                    <Typography
                        variant="h5"
                        component="h3"
                        fontWeight={600}
                        fontSize={'1.5rem'}
                    >
                        Prozesse
                    </Typography>

                    <Typography
                        variant="body2"
                        color="text.secondary"
                        sx={{mt: 1, mb: 2, maxWidth: 400}}
                    >
                        Hier sehen Sie eine Übersicht der zuletzt bearbeiteten Prozesse.
                    </Typography>
                </Box>

                <List disablePadding>
                    {loading
                        ? Array.from({length: fetchSize}).map((_, i) => (
                            <React.Fragment key={i}>
                                <ListItem disablePadding>
                                    <Box
                                        sx={{
                                            display: 'flex',
                                            alignItems: 'center',
                                            justifyContent: 'space-between',
                                            width: '100%',
                                            gap: 2,
                                            py: 2.5625,
                                            px: 1,
                                        }}
                                    >
                                        <Box sx={{flex: 1, minWidth: 0}}>
                                            <Skeleton
                                                variant="text"
                                                height={20}
                                                width="70%"
                                            />
                                            <Skeleton
                                                variant="text"
                                                height={14}
                                                width="50%"
                                                sx={{mt: 0.5}}
                                            />
                                        </Box>

                                        <Box
                                            sx={{
                                                display: 'flex',
                                                alignItems: 'center',
                                                minWidth: 80,
                                                justifyContent: 'flex-end',
                                            }}
                                        >
                                            <Skeleton
                                                variant="circular"
                                                width={40}
                                                height={40}
                                                sx={{opacity: 0.4}}
                                            />
                                        </Box>
                                    </Box>
                                </ListItem>
                                {i < fetchSize - 1 && <Divider component="li"/>}
                            </React.Fragment>
                        ))
                        : processes?.length
                            ? processes.map((processDefinition, index) => {
                                const processName = processDefinition.internalTitle.trim().length > 0 ?
                                    processDefinition.internalTitle :
                                    'Unbenannter Prozess';

                                return (
                                    <React.Fragment key={processDefinition.id}>
                                        <ListItem disablePadding>
                                            <ListItemButton
                                                component={Link}
                                                to={createProcessLink(processDefinition)}
                                                sx={{
                                                    py: 2,
                                                    px: 1,
                                                    borderRadius: 1,
                                                    '&:hover': {bgcolor: 'action.hover'},
                                                    '&.Mui-focusVisible': {
                                                        outline: '2px solid',
                                                        outlineColor: 'primary.main',
                                                    },
                                                }}
                                            >
                                                <Box
                                                    sx={{
                                                        display: 'flex',
                                                        alignItems: 'center',
                                                        justifyContent: 'space-between',
                                                        width: '100%',
                                                        gap: 2,
                                                    }}
                                                >
                                                    <Box sx={{flex: 1, minWidth: 0}}>
                                                        <Typography
                                                            variant="subtitle1"
                                                            fontWeight={700}
                                                            noWrap
                                                            title={processName}
                                                            sx={{
                                                                overflow: 'hidden',
                                                                textOverflow: 'ellipsis',
                                                                whiteSpace: 'nowrap',
                                                                display: 'block',
                                                            }}
                                                        >
                                                            {processName}
                                                        </Typography>

                                                        <Typography
                                                            variant="body2"
                                                            color="text.secondary"
                                                            fontSize="0.875rem"
                                                            noWrap
                                                        >
                                                            {formatUpdatedAt(processDefinition.updated)}
                                                        </Typography>
                                                    </Box>

                                                    <ChevronRight
                                                        aria-hidden
                                                        sx={{fontSize: '3rem', color: 'rgba(0,0,0,.2)'}}
                                                    />
                                                </Box>
                                            </ListItemButton>
                                        </ListItem>

                                        {index < processes.length - 1 && <Divider component="li"/>}
                                    </React.Fragment>
                                );
                            })
                            : (
                                <Box sx={{position: 'relative'}}>
                                    <List disablePadding>
                                        {Array.from({length: fetchSize}).map((_, i) => (
                                            <React.Fragment key={i}>
                                                <ListItem disablePadding>
                                                    <Box
                                                        sx={{
                                                            display: 'flex',
                                                            alignItems: 'center',
                                                            justifyContent: 'space-between',
                                                            width: '100%',
                                                            gap: 2,
                                                            py: 2.5625,
                                                            px: 1,
                                                        }}
                                                    >
                                                        <Box sx={{flex: 1, minWidth: 0}}>
                                                            <Skeleton
                                                                variant="text"
                                                                height={20}
                                                                width="70%"
                                                                animation={false}
                                                            />
                                                            <Skeleton
                                                                variant="text"
                                                                height={14}
                                                                width="50%"
                                                                sx={{mt: 0.5}}
                                                                animation={false}
                                                            />
                                                        </Box>
                                                        <Skeleton
                                                            variant="circular"
                                                            width={40}
                                                            height={40}
                                                            sx={{opacity: 0.3}}
                                                            animation={false}
                                                        />
                                                    </Box>
                                                </ListItem>
                                                {i < fetchSize - 1 && <Divider component="li"/>}
                                            </React.Fragment>
                                        ))}
                                    </List>

                                    <Box
                                        sx={{
                                            position: 'absolute',
                                            inset: 0,
                                            display: 'flex',
                                            alignItems: 'center',
                                            justifyContent: 'center',
                                            bgcolor: 'rgba(255,255,255,0.6)',
                                            textAlign: 'center',
                                            px: 2,
                                        }}
                                    >
                                        <Typography
                                            variant="body2"
                                            color="text.secondary"
                                            sx={{maxWidth: 320}}
                                        >
                                            In den Organisationseinheiten, denen Sie angehören, sind (noch) keine
                                            Prozesse vorhanden.
                                        </Typography>
                                    </Box>
                                </Box>
                            )}

                    <Button
                        variant="contained"
                        sx={{
                            mt: 2,
                            mx: 1,
                        }}
                        startIcon={ModuleIcons.processes}
                        component={Link}
                        to="/processes"
                    >
                        Prozesse verwalten
                    </Button>
                </List>
            </CardContent>
        </Card>
    );
}
