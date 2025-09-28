import {Button, Card, CardContent, List, ListItem, ListItemButton, ListItemText, Skeleton, Typography} from '@mui/material';
import {useEffect, useState} from 'react';
import {FormListResponseDTO} from '../../forms/dtos/form-list-response-dto';
import {useApi} from '../../../hooks/use-api';
import {FormsApiService} from '../../forms/forms-api-service';
import {withAsyncWrapper} from '../../../utils/with-async-wrapper';
import {Page} from '../../../models/dtos/page';
import {ModuleIcons} from '../../../shells/staff/data/module-icons';
import { Link } from 'react-router-dom';
import {format} from 'date-fns';
import {useCachedState} from '../../../hooks/use-cached-state';

const fetchSize = 5;

export function DashboardFormsPanel() {
    const [latestForms, setLatestForms] = useCachedState<FormListResponseDTO[] | null>('dsahboard-forms', null);

    const api = useApi();

    useEffect(() => {
        withAsyncWrapper<void, Page<FormListResponseDTO>>({
            main: () => new FormsApiService(api)
                .list(0, fetchSize, 'updated', 'DESC', {
                    isDeveloper: true,
                    isCurrentlyDraftedVersion: true,
                }),
            desiredMinRuntime: 500,
        })
            .then((forms) => {
                setLatestForms(forms.content);
            });
    }, []);

    return (
        <Card
            sx={{
                height: '100%',
            }}
        >
            <CardContent>
                <Typography
                    variant="h5"
                    component="h3"
                >
                    Online-Formulare
                </Typography>

                <Typography
                    variant="body1"
                    sx={{
                        mt: 1,
                        mb: 2,
                    }}
                >
                    Diese Formulare wurden zuletzt aktualisiert:
                </Typography>

                {
                    latestForms == null &&
                    <List
                        dense={true}
                        disablePadding={true}
                    >
                        {
                            new Array(fetchSize).fill(0).map((_, index) => (
                                <ListItem
                                    key={'skel-' + index}
                                    dense={true}
                                    disablePadding={true}
                                    disableGutters={true}
                                >
                                    <Skeleton
                                        height="2rem"
                                        width="100%"
                                    />
                                </ListItem>
                            ))
                        }
                    </List>
                }

                {
                    latestForms != null &&
                    latestForms.length === 0 &&
                    <Typography
                        variant="body2"
                        color="textSecondary"
                        sx={{
                            mt: 2,
                        }}
                    >
                        Es sind noch keine Formulare vorhanden.
                    </Typography>
                }

                {
                    latestForms != null &&
                    latestForms.length > 0 &&
                    <List
                        dense={true}
                        disablePadding={true}
                    >
                        {
                            latestForms.map((form) => (
                                <ListItem
                                    dense={true}
                                    disablePadding={true}
                                    disableGutters={true}
                                    key={form.id}
                                    sx={{
                                        borderBottom: '1px solid #eee',
                                    }}
                                >
                                    <ListItemButton
                                        component={Link}
                                        to={`/forms/${form.id}/${form.draftedVersion}`}
                                    >
                                        <ListItemText
                                            primary={form.internalTitle}
                                            secondary={`Zuletzt bearbeitet: ${format(new Date(form.updated), 'dd.MM.yyyy HH:mm')} Uhr`}
                                        />
                                    </ListItemButton>
                                </ListItem>
                            ))
                        }
                    </List>
                }

                <Button
                    variant="contained"
                    sx={{
                        mt: 2,
                    }}
                    startIcon={ModuleIcons.forms}
                    component={Link}
                    to="/forms"
                >
                    Alle Formulare anzeigen
                </Button>
            </CardContent>
        </Card>
    );
}