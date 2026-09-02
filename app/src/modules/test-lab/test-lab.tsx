import {PageWrapper} from '../../components/page-wrapper/page-wrapper';
import {Box, Button, Divider, Paper, Stack, Tab, Tabs, Typography} from '@mui/material';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {addSnackbarMessage, setErrorMessage, setLoadingMessage, SnackbarSeverity, SnackbarType} from '../../slices/shell-slice';
import {HintTooltip} from '../../components/hint-tooltip/hint-tooltip';
import {ModuleIcons} from '../../shells/staff/data/module-icons';
import {GenericPageHeader} from '../../components/generic-page-header/generic-page-header';
import {useState} from 'react';
import {TextFieldComponent} from '../../components/text-field/text-field-component';
import {FieldLayoutGallery} from './field-layout-gallery';

type TestLabArea = 'components' | 'system-states';

interface TestAction {
    label: string;
    onClick: () => void;
    hint: string;
}

interface TestActionGroup {
    id: string;
    title: string;
    actions: TestAction[];
}

export function TestLab() {
    const dispatch = useAppDispatch();
    const [input, setInput] = useState<string | undefined>();
    const [activeArea, setActiveArea] = useState<TestLabArea>('components');

    const actionGroups: TestActionGroup[] = [
        {
            id: 'progress',
            title: 'Shell-Fortschritt',
            actions: [
                {
                    label: 'Fortschritt starten',
                    onClick: () => {
                        dispatch(setLoadingMessage({
                            message: input ?? 'Fortschritt aus dem Testlabor',
                            blocking: false,
                            estimatedTime: 5000,
                        }));
                    },
                    hint: 'Startet einen nicht blockierenden Fortschritt mit einer geschätzten Dauer von fünf Sekunden.',
                },
                {
                    label: 'Blockierenden Fortschritt starten',
                    onClick: () => {
                        dispatch(setLoadingMessage({
                            message: input ?? 'Blockierender Fortschritt aus dem Testlabor',
                            blocking: true,
                            estimatedTime: 5000,
                        }));
                        setTimeout(() => {
                            dispatch(setLoadingMessage(undefined));
                        }, 10000);
                    },
                    hint: 'Startet einen blockierenden Fortschritt und beendet ihn nach zehn Sekunden automatisch.',
                },
                {
                    label: 'Fortschritt beenden',
                    onClick: () => {
                        dispatch(setLoadingMessage(undefined));
                    },
                    hint: 'Beendet den aktuell angezeigten Shell-Fortschritt.',
                },
            ],
        },
        {
            id: 'errors',
            title: 'Fehlerseiten',
            actions: [500, 404, 403].map((status) => ({
                label: `Fehler ${status} auslösen`,
                onClick: () => {
                    dispatch(setErrorMessage({
                        message: input ?? `Testfehler aus dem Testlabor (Status ${status}).`,
                        status,
                    }));
                },
                hint: `Zeigt eine Testfehlermeldung mit dem Statuscode ${status} an.`,
            })),
        },
        {
            id: 'notifications',
            title: 'Benachrichtigungen',
            actions: [
                {
                    label: 'Automatisch ausblenden',
                    severity: SnackbarSeverity.Success,
                    type: SnackbarType.AutoHiding,
                    hint: 'Zeigt eine Benachrichtigung an, die automatisch wieder ausgeblendet wird.',
                },
                {
                    label: 'Dauerhaft und schließbar',
                    severity: SnackbarSeverity.Info,
                    type: SnackbarType.Dismissable,
                    hint: 'Zeigt eine dauerhafte Benachrichtigung an, die manuell geschlossen werden kann.',
                },
                {
                    label: 'Dauerhaft und nicht schließbar',
                    severity: SnackbarSeverity.Warning,
                    type: SnackbarType.Permanent,
                    hint: 'Zeigt eine dauerhafte Benachrichtigung ohne Schließen-Aktion an.',
                },
                {
                    label: 'Ladezustand anzeigen',
                    severity: SnackbarSeverity.Error,
                    type: SnackbarType.Loading,
                    hint: 'Zeigt eine Benachrichtigung mit Ladezustand an.',
                },
            ].map(({label, severity, type, hint}) => ({
                label,
                onClick: () => {
                    dispatch(addSnackbarMessage({
                        key: `test-lab-snackbar-${Date.now()}`,
                        message: input ?? 'Testbenachrichtigung aus dem Testlabor.',
                        severity,
                        type,
                    }));
                },
                hint,
            })),
        },
    ];

    return (
        <PageWrapper
            title="Testlabor"
        >
            <Box sx={{minWidth: 0, maxWidth: '100%'}}>
                <GenericPageHeader
                    icon={ModuleIcons.testLab}
                    title="Testlabor"
                />

                <Paper
                    sx={{mt: 2.75, mb: 6, minWidth: 0, maxWidth: '100%'}}
                >
                    <Box sx={{borderBottom: 1, borderColor: 'divider'}}>
                        <Tabs
                            value={activeArea}
                            onChange={(_, area: TestLabArea) => setActiveArea(area)}
                            aria-label="Bereiche des Testlabors"
                        >
                            <Tab
                                id="test-lab-tab-components"
                                value="components"
                                label="Komponenten"
                                aria-controls="test-lab-panel-components"
                            />
                            <Tab
                                id="test-lab-tab-system-states"
                                value="system-states"
                                label="Systemzustände"
                                aria-controls="test-lab-panel-system-states"
                            />
                        </Tabs>
                    </Box>

                    <Box
                        id="test-lab-panel-components"
                        role="tabpanel"
                        aria-labelledby="test-lab-tab-components"
                        hidden={activeArea !== 'components'}
                        sx={{p: {xs: 2, sm: 3, md: 4}, minWidth: 0, maxWidth: '100%'}}
                    >
                        <FieldLayoutGallery/>
                    </Box>

                    <Box
                        id="test-lab-panel-system-states"
                        role="tabpanel"
                        aria-labelledby="test-lab-tab-system-states"
                        hidden={activeArea !== 'system-states'}
                        sx={{p: {xs: 2, sm: 3, md: 4}, minWidth: 0, maxWidth: '100%'}}
                    >
                        <Box
                            component="section"
                            aria-labelledby="test-lab-system-states-title"
                        >
                            <Typography
                                id="test-lab-system-states-title"
                                component="h2"
                                variant="h5"
                                sx={{mb: 3}}
                            >
                                Systemzustände
                            </Typography>

                            <TextFieldComponent
                                label="Nachricht"
                                placeholder="Optionale benutzerdefinierte Nachricht"
                                onChange={(value) => setInput(value ?? undefined)}
                                value={input}
                                margin="none"
                            />

                            <Stack
                                spacing={3}
                                divider={<Divider flexItem/>}
                                sx={{mt: 3}}
                            >
                                {actionGroups.map((group) => (
                                    <Box
                                        key={group.id}
                                        component="section"
                                        aria-labelledby={`test-lab-${group.id}-title`}
                                    >
                                        <Typography
                                            id={`test-lab-${group.id}-title`}
                                            component="h3"
                                            variant="subtitle1"
                                            sx={{mb: 1.5}}
                                        >
                                            {group.title}
                                        </Typography>

                                        <Box sx={{display: 'flex', gap: 1, flexWrap: 'wrap'}}>
                                            {group.actions.map((action) => (
                                                <HintTooltip
                                                    key={action.label}
                                                    title={action.hint}
                                                    arrow
                                                >
                                                    <Button
                                                        onClick={action.onClick}
                                                        variant="outlined"
                                                    >
                                                        {action.label}
                                                    </Button>
                                                </HintTooltip>
                                            ))}
                                        </Box>
                                    </Box>
                                ))}
                            </Stack>
                        </Box>
                    </Box>
                </Paper>
            </Box>
        </PageWrapper>
    );
}
