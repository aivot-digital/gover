import React, {useEffect, useState} from 'react';
import {Box, Button, Card, CardContent, Divider, List, ListItem, ListItemButton, Skeleton, Typography} from '@mui/material';
import ChevronRight from '@aivot/mui-material-symbols-400-outlined/dist/chevron-right/ChevronRight';
import {useApi} from '../../../hooks/use-api';
import {withAsyncWrapper} from '../../../utils/with-async-wrapper';
import {Page} from '../../../models/dtos/page';
import {Link} from 'react-router-dom';
import {format} from 'date-fns';
import {getFormStatus} from '../../forms/components/form-status-chip-group';
import {ModuleIcons} from '../../../shells/staff/data/module-icons';
import {FormEntity} from '../../forms/entities/form-entity';
import {FormApiService} from '../../forms/services/form-api-service';

const fetchSize = 4;

export function DashboardFormsPanel() {
    const [forms, setForms] = useState<FormEntity[] | null>(null);
    const [loading, setLoading] = useState(true);
    const api = useApi();

    useEffect(() => {
        withAsyncWrapper<void, Page<FormEntity>>({
            main: () =>
                new FormApiService()
                    .list(0, fetchSize, 'updated', 'DESC', {
                        isDrafted: true,
                    }),
            desiredMinRuntime: 600,
        }).then((page) => {
            setForms(page.content);
            setLoading(false);
        });
    }, []);

    const renderFormStatus = (form: FormEntity): string => {
        const {isDrafted, isPublished, isRevoked} = getFormStatus(form);

        if (isPublished) return 'Veröffentlicht';
        if (isDrafted) return 'Entwurf';
        if (isRevoked) return 'Zurückgezogen';
        return 'Unbekannter Status';
    };

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
                        Online-Formulare
                    </Typography>

                    <Typography
                        variant="body2"
                        color="text.secondary"
                        sx={{mt: 1, mb: 2, maxWidth: 400}}
                    >
                        Hier sehen Sie eine Übersicht der zuletzt bearbeiteten Formulare.
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
                                {i < fetchSize - 1 && <Divider component="li" />}
                            </React.Fragment>
                        ))
                        : forms?.length
                            ? forms.map((form, index) => (
                                <React.Fragment key={form.id}>
                                    <ListItem disablePadding>
                                        <ListItemButton
                                            component={Link}
                                            to={`/forms/${form.id}/${form.draftedVersion ?? form.publishedVersion ?? ''}`}
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
                                                        title={form.internalTitle}
                                                        sx={{
                                                            overflow: 'hidden',
                                                            textOverflow: 'ellipsis',
                                                            whiteSpace: 'nowrap',
                                                            display: 'block',
                                                        }}
                                                    >
                                                        {form.internalTitle}
                                                    </Typography>

                                                    <Typography
                                                        variant="body2"
                                                        color="text.secondary"
                                                        fontSize="0.875rem"
                                                        noWrap
                                                    >
                                                        {renderFormStatus(form)} &bull; Zuletzt bearbeitet am{' '}
                                                        {format(new Date(form.updated), 'dd.MM.yyyy — HH:mm')} Uhr
                                                    </Typography>
                                                </Box>

                                                <ChevronRight
                                                    aria-hidden
                                                    sx={{fontSize: '3rem', color: 'rgba(0,0,0,.2)'}}
                                                />
                                            </Box>
                                        </ListItemButton>
                                    </ListItem>

                                    {index < forms.length - 1 && <Divider component="li" />}
                                </React.Fragment>
                            ))
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
                                                {i < fetchSize - 1 && <Divider component="li" />}
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
                                            In den Organisationseinheiten, denen Sie angehören, sind (noch) keine Formulare vorhanden.
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
                        startIcon={ModuleIcons.forms}
                        component={Link}
                        to="/forms"
                    >
                        Formulare verwalten
                    </Button>
                </List>
            </CardContent>
        </Card>
    );
}
