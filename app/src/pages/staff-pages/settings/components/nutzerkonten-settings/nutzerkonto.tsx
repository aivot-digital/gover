import {
    Box, Button,
    Card,
    Collapse,
    Divider,
    FormControlLabel,
    List,
    ListItem,
    ListItemButton,
    ListItemIcon,
    ListItemText,
    Switch,
    Typography,
} from '@mui/material';
import {AlertComponent} from '../../../../../components/alert/alert-component';
import React, {PropsWithChildren, useReducer, useState} from 'react';
import {useApi} from '../../../../../hooks/use-api';
import {useAppDispatch} from '../../../../../hooks/use-app-dispatch';
import {useAppSelector} from '../../../../../hooks/use-app-selector';
import {selectBooleanSystemConfigValue, setSystemConfig} from '../../../../../slices/system-config-slice';
import {useSystemConfigsApi} from '../../../../../hooks/use-system-configs-api';
import ExpandLessOutlinedIcon from '@mui/icons-material/ExpandLessOutlined';
import ExpandMoreOutlinedIcon from '@mui/icons-material/ExpandMoreOutlined';
import {showErrorSnackbar} from '../../../../../slices/snackbar-slice';
import {ConfirmDialog} from '../../../../../dialogs/confirm-dialog/confirm-dialog';

interface NutzerkontoProps {
    configKey: string;
    label: string;
    links: {
        icon: React.ReactNode;
        label: string;
        url: string;
    }[];
}

export function Nutzerkonto(props: PropsWithChildren<NutzerkontoProps>) {
    const api = useApi();
    const dispatch = useAppDispatch();
    const [expanded, toggleExpanded] = useReducer(p => !p, false);
    const [showPublishedFormsExistError, setShowPublishedFormsExistError] = useState(false);
    const [isPatching, setIsPatching] = useState(false);
    const [confirmDisable, setConfirmDisable] = useState<() => void>();

    const isActive = useAppSelector(selectBooleanSystemConfigValue(props.configKey));

    const handleActivate = () => {
        handleToggle(true);
    };

    const handleDeactivate = () => {
        handleToggle(false);
        setConfirmDisable(undefined);
    };

    const handleToggle = (value: boolean) => {
        setIsPatching(true);
        useSystemConfigsApi(api)
            .saveBoolean(props.configKey, value)
            .then((res) => {
                dispatch(setSystemConfig(res));
            })
            .catch((err) => {
                if (err.status === 409) {
                    setShowPublishedFormsExistError(true);
                    if (!expanded) {
                        toggleExpanded();
                    }
                } else {
                    console.error(err);
                    dispatch(showErrorSnackbar('Fehler beim Speichern der Systemkonfiguration'));
                }
            })
            .finally(() => {
                setIsPatching(false);
            });
    };

    return (
        <>
            <Box
                sx={{
                    mb: 2,
                }}
            >
                <Box
                    display="flex"
                    alignItems="center"
                >
                    <FormControlLabel
                        control={
                            <Switch
                                checked={isActive}
                                onChange={(_, value) => {
                                    if (value) {
                                        handleActivate();
                                    } else {
                                        setConfirmDisable(() => handleDeactivate);
                                    }
                                }}
                                inputProps={{'aria-label': 'controlled'}}
                            />
                        }
                        label={
                            <Typography
                                variant="h5"
                            >
                                {props.label}
                            </Typography>
                        }
                        disabled={isPatching}
                    />

                    <Divider
                        sx={{
                            mx: 2,
                            flex: 1,
                        }}
                    />

                    <Button
                        onClick={toggleExpanded}
                    >
                        {expanded ? <>Informationen einklappen <ExpandLessOutlinedIcon /></> : <>Informationen ausklappen <ExpandMoreOutlinedIcon /></>}
                    </Button>
                </Box>

                <Collapse
                    in={expanded}
                    timeout="auto"
                    unmountOnExit
                >
                    <Typography
                        sx={{
                            maxWidth: 900,
                            mt: 2,
                            mb: 0,
                        }}
                    >
                        {props.children}
                    </Typography>

                    {
                        showPublishedFormsExistError && (
                            <AlertComponent
                                color="error"
                                sx={{
                                    mt: 2,
                                    mb: 0,
                                }}
                            >
                                Es existieren veröffentlichte Formulare, die dieses Konto verwenden.
                                Bitte ziehen Sie zuerst die Formulare zurück, bevor Sie das Konto deaktivieren.
                            </AlertComponent>
                        )
                    }

                    <Card variant="outlined" sx={{my: 3}}>
                        <List>
                            {
                                props.links != null &&
                                props.links.map(link => (
                                    <ListItem

                                        disablePadding
                                        key={link.label}
                                    >
                                        <ListItemButton
                                            component="a"
                                            href={link.url}
                                            target="_blank"
                                        >
                                            <ListItemIcon>
                                                {link.icon}
                                            </ListItemIcon>
                                            <ListItemText primary={link.label} />
                                        </ListItemButton>
                                    </ListItem>
                                ))
                            }
                        </List>
                    </Card>
                </Collapse>
            </Box>

            <ConfirmDialog
                title="Konto deaktivieren"
                onConfirm={confirmDisable}
                onCancel={() => setConfirmDisable(undefined)}
            >
                <Typography gutterBottom>
                    Sind Sie sicher, dass Sie dieses Konto wirklich deaktivieren wollen?
                </Typography>
                <Typography gutterBottom>
                    Wenn Sie dieses Konto deaktivieren, wird diese Option in allen aktuell entwickelten Formularen
                    deaktiviert.
                </Typography>
                <Typography gutterBottom>
                    Sie können dieses Konto nur deaktivieren, wenn keine veröffentlichten Formulare dieses Konto verwenden.
                </Typography>
                <Typography>
                    Sie können das Konto jederzeit wieder aktivieren.
                </Typography>
            </ConfirmDialog>
        </>
    );
}