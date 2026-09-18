import React, {type FormEvent, useCallback, useEffect, useMemo, useState} from 'react';
import {
    Box,
    Button,
    CircularProgress,
    Paper,
    Stack,
    Tab,
    Tabs,
    TextField,
    Typography,
} from '@mui/material';
import ForwardToInbox from '@aivot/mui-material-symbols-400-n25-outlined/ForwardToInbox';
import SendOutlined from '@aivot/mui-material-symbols-400-n25-outlined/Send';
import CheckCircle from '@aivot/mui-material-symbols-400-n25-outlined/CheckCircle';
import Warning from '@aivot/mui-material-symbols-400-n25-outlined/Warning';
import Dns from '@aivot/mui-material-symbols-400-n25-outlined/Dns';
import Security from '@aivot/mui-material-symbols-400-n25-outlined/Security';
import Lock from '@aivot/mui-material-symbols-400-n25-outlined/Lock';
import Person from '@aivot/mui-material-symbols-400-n25-outlined/Person';
import Password from '@aivot/mui-material-symbols-400-n25-outlined/Password';
import OutgoingMail from '@aivot/mui-material-symbols-400-n25-outlined/OutgoingMail';
import {useLocation, useNavigate} from 'react-router-dom';
import {PageWrapper} from '../../../components/page-wrapper/page-wrapper';
import {GenericPageHeader} from '../../../components/generic-page-header/generic-page-header';
import {StatusTable} from '../../../components/status-table/status-table';
import {type StatusTablePropsItem} from '../../../components/status-table/status-table-props';
import {AlertComponent} from '../../../components/alert/alert-component';
import {DisabledTooltip} from '../../../components/disabled-tooltip/disabled-tooltip';
import {MailProcessingNotice} from '../../../components/mail-processing-notice/mail-processing-notice';
import {Permission} from '../../../data/permissions/permission';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {selectUser} from '../../../slices/user-slice';
import {useRequireSystemPermission} from '../../permissions/hooks/use-permissions';
import {
    MailApiService,
    type MailConfigurationResponseDTO,
    type TestMailResponseDTO,
} from '../mail-api-service';

interface DisplayedTestResult extends TestMailResponseDTO {
    targetMail: string;
}

const mailApiService = new MailApiService();

function renderOptionalValue(value: string | number | null): React.ReactNode {
    return value == null || String(value).trim().length === 0
        ? (
            <Typography component="span" sx={{
                color: "text.secondary"
            }}>
                <i>Nicht konfiguriert</i>
            </Typography>
        )
        : String(value);
}

function formatSmtpServer(host: string | null, port: number | null): string | null {
    if (host == null || host.trim().length === 0) {
        return null;
    }

    const normalizedHost = host.includes(':') && !host.startsWith('[')
        ? `[${host}]`
        : host;

    return port == null ? normalizedHost : `${normalizedHost}:${port}`;
}

function getErrorMessage(error: unknown): string {
    if (
        error != null &&
        typeof error === 'object' &&
        'message' in error &&
        typeof error.message === 'string' &&
        error.message.trim().length > 0
    ) {
        return error.message;
    }
    return 'Bei der Kommunikation mit dem Server ist ein unbekannter Fehler aufgetreten.';
}

export function EmailPage() {
    useRequireSystemPermission(Permission.SYSTEM_CONFIG_UPDATE);

    const location = useLocation();
    const navigate = useNavigate();
    const user = useAppSelector(selectUser);
    const [configuration, setConfiguration] = useState<MailConfigurationResponseDTO>();
    const [configurationError, setConfigurationError] = useState<string>();
    const [isLoadingConfiguration, setIsLoadingConfiguration] = useState(true);
    const [targetMail, setTargetMail] = useState(user?.email ?? '');
    const [isSending, setIsSending] = useState(false);
    const [testResult, setTestResult] = useState<DisplayedTestResult>();
    const currentTab = location.pathname === '/mail/test' ? 1 : 0;

    const loadConfiguration = useCallback(async () => {
        setIsLoadingConfiguration(true);
        setConfigurationError(undefined);
        try {
            setConfiguration(await mailApiService.getConfiguration());
        } catch (error) {
            setConfiguration(undefined);
            setConfigurationError(getErrorMessage(error));
        } finally {
            setIsLoadingConfiguration(false);
        }
    }, []);

    useEffect(() => {
        void loadConfiguration();
    }, [loadConfiguration]);

    const configurationItems = useMemo<StatusTablePropsItem[]>(() => {
        if (configuration == null) {
            return [];
        }

        const sender = configuration.senderAddress == null
            ? null
            : configuration.senderName == null
                ? configuration.senderAddress
                : `${configuration.senderName} <${configuration.senderAddress}>`;

        return [
            {
                label: 'Status',
                icon: configuration.configured
                    ? <CheckCircle color="success"/>
                    : <Warning color="warning"/>,
                children: configuration.configured ? 'Vollständig konfiguriert' : 'Konfiguration unvollständig',
            },
            {
                label: 'SMTP-Server',
                icon: <Dns/>,
                children: renderOptionalValue(formatSmtpServer(configuration.host, configuration.port)),
            },
            {
                label: 'Transportverschlüsselung',
                icon: <Security/>,
                children: configuration.startTlsEnabled ? 'STARTTLS aktiviert' : 'STARTTLS nicht aktiviert',
            },
            {
                label: 'Authentifizierung',
                icon: <Lock/>,
                children: configuration.authenticationEnabled ? 'Aktiviert' : 'Nicht aktiviert',
            },
            ...(configuration.authenticationEnabled
                ? [
                    {
                        label: 'Benutzername',
                        icon: <Person/>,
                        children: renderOptionalValue(configuration.maskedUsername),
                    },
                    {
                        label: 'Passwort',
                        icon: <Password/>,
                        children: configuration.passwordConfigured ? '************' : 'Nicht hinterlegt',
                    },
                ]
                : []),
            {
                label: 'Absender',
                icon: <OutgoingMail/>,
                children: renderOptionalValue(sender),
            },
        ];
    }, [configuration]);

    const handleSubmit = async (event: FormEvent) => {
        event.preventDefault();
        const normalizedTargetMail = targetMail.trim();
        if (!configuration?.configured || normalizedTargetMail.length === 0 || isSending) {
            return;
        }

        setTargetMail(normalizedTargetMail);
        setIsSending(true);
        setTestResult(undefined);
        try {
            const result = await mailApiService.sendTestMail(normalizedTargetMail);
            setTestResult({
                ...result,
                targetMail: normalizedTargetMail,
            });
        } catch (error) {
            setTestResult({
                success: false,
                errorMessage: getErrorMessage(error),
                targetMail: normalizedTargetMail,
            });
        } finally {
            setIsSending(false);
        }
    };

    const testDisabledReason = configuration == null
        ? 'Die E-Mail-Konfiguration wurde noch nicht geladen.'
        : !configuration.configured
            ? 'Die E-Mail-Anbindung ist nicht vollständig konfiguriert.'
            : undefined;

    return (
        <PageWrapper
            title="E-Mail-Anbindung"
            background
        >
            <GenericPageHeader
                title="E-Mail-Anbindung"
                icon={<ForwardToInbox/>}
                helpDialog={{
                    title: 'Hilfe zur E-Mail-Anbindung',
                    tooltip: 'Hilfe anzeigen',
                    content: (
                        <Typography>
                            Die E-Mail-Anbindung wird beim Betrieb der Prosuna-Instanz konfiguriert. Auf dieser Seite
                            können Sie die wirksamen Einstellungen prüfen und eine Test-E-Mail versenden. Änderungen an
                            der Konfiguration müssen durch die technische Administration vorgenommen werden.
                        </Typography>
                    ),
                }}
            />

            <Paper sx={{
                mt: 2.75,
            }}>
                <Box sx={{borderBottom: 1, borderBottomColor: 'divider'}}>
                    <Tabs
                        value={currentTab}
                        onChange={(_, tab: number) => navigate(tab === 0 ? '/mail' : '/mail/test')}
                    >
                        <Tab label="Konfiguration"/>
                        <Tab label="Testen"/>
                    </Tabs>
                </Box>

                <Box sx={{p: 2}}>
                    {isLoadingConfiguration && (
                        <Stack
                            direction="row"
                            spacing={1.5}
                            sx={{
                                alignItems: "center"
                            }}
                        >
                            <CircularProgress size={22}/>
                            <Typography sx={{
                                color: "text.secondary"
                            }}>E-Mail-Konfiguration wird geladen…</Typography>
                        </Stack>
                    )}

                    {configurationError != null && (
                        <AlertComponent
                            color="error"
                            title="E-Mail-Konfiguration konnte nicht geladen werden"
                        >
                            <Typography>{configurationError}</Typography>
                            <Button
                                variant="outlined"
                                onClick={() => void loadConfiguration()}
                                sx={{mt: 2}}
                            >
                                Erneut versuchen
                            </Button>
                        </AlertComponent>
                    )}

                    {configuration != null && currentTab === 0 && (
                        <Box>
                            <Typography
                                variant="h5"
                                component="h2"
                                sx={{mt: 1.5, mb: 1}}
                            >
                                E-Mail-Anbindung konfigurieren
                            </Typography>
                            <Typography sx={{
                                maxWidth: 900,
                                mb: 3,
                            }}>
                                Diese Angaben zeigen die aktuell wirksame Konfiguration der E-Mail-Anbindung.
                            </Typography>

                            <StatusTable
                                items={configurationItems}
                                sx={{mt: 0}}
                                cardVariant="outlined"
                            />

                            {configuration.configurationIssues.length > 0 && (
                                <AlertComponent
                                    color="warning"
                                    title="Konfiguration unvollständig"
                                    sx={{mt: 2}}
                                >
                                    <Box
                                        component="ul"
                                        sx={{
                                            my: 0,
                                            pl: 2.5,
                                        }}
                                    >
                                        {configuration.configurationIssues.map((issue) => (
                                            <li key={issue}>{issue}</li>
                                        ))}
                                    </Box>
                                </AlertComponent>
                            )}

                            <Box sx={{mt: 3, maxWidth: 900}}>
                                <Typography
                                    variant="subtitle1"
                                    component="h3"
                                    sx={{mb: 0.5}}
                                >
                                    Änderungen an der Konfiguration
                                </Typography>
                                <Typography
                                    sx={{mb: 1.5}}
                                >
                                    Die angezeigten Werte werden über die Betriebsumgebung der Prosuna-Instanz
                                    verwaltet. Wenden Sie sich für Änderungen an die technische Administration und
                                    prüfen Sie die Anbindung anschließend im Reiter „Testen“.
                                </Typography>
                            </Box>
                        </Box>
                    )}

                    {configuration != null && currentTab === 1 && (
                        <Box>
                            <Typography
                                variant="h5"
                                component="h2"
                                sx={{mt: 1.5, mb: 1}}
                            >
                                E-Mail-Anbindung testen
                            </Typography>
                            <Typography sx={{maxWidth: 900, mb: 2}}>
                                Senden Sie eine Test-E-Mail, um die Übergabe an den konfigurierten E-Mail-Server zu
                                prüfen.
                            </Typography>

                            <Box
                                component="form"
                                onSubmit={(event) => void handleSubmit(event)}
                            >
                                <TextField
                                    label="Empfängeradresse"
                                    type="email"
                                    value={targetMail}
                                    onChange={(event) => setTargetMail(event.target.value)}
                                    onBlur={() => setTargetMail(targetMail.trim())}
                                    disabled={isSending || !configuration.configured}
                                    required
                                    sx={{
                                        width: '100%',
                                        maxWidth: 420,
                                    }}
                                />

                                <Box sx={{mt: 2}}>
                                    <DisabledTooltip
                                        disabled={testDisabledReason != null}
                                        title={testDisabledReason ?? ''}
                                        wrapperSx={{display: 'inline-flex'}}
                                    >
                                        <Button
                                            type="submit"
                                            variant="contained"
                                            startIcon={isSending
                                                ? <CircularProgress size={18} color="inherit"/>
                                                : <SendOutlined/>}
                                            disabled={isSending || !configuration.configured || targetMail.trim().length === 0}
                                        >
                                            {isSending ? 'Wird übergeben…' : 'Test-E-Mail versenden'}
                                        </Button>
                                    </DisabledTooltip>
                                </Box>
                            </Box>

                            {testResult != null && (
                                <AlertComponent
                                    color={testResult.success ? 'success' : 'error'}
                                    title={testResult.success ? 'Test-E-Mail übergeben' : 'Testversand fehlgeschlagen'}
                                    sx={{mt: 3, maxWidth: 900}}
                                >
                                    {testResult.success ? (
                                        <Typography>
                                            Der konfigurierte E-Mail-Server hat die Test-E-Mail an
                                            {' '}{testResult.targetMail} angenommen. Prüfen Sie nun, ob die Nachricht
                                            im Posteingang oder Spam-Ordner eingegangen ist.
                                        </Typography>
                                    ) : (
                                        <>
                                            <Typography>
                                                Die Test-E-Mail konnte nicht an den konfigurierten E-Mail-Server
                                                übergeben werden.
                                            </Typography>
                                            {testResult.errorMessage != null && (
                                                <Box
                                                    component="code"
                                                    sx={{
                                                        display: 'block',
                                                        mt: 1.5,
                                                        p: 1,
                                                        overflowWrap: 'anywhere',
                                                        border: '1px solid',
                                                        borderColor: 'divider',
                                                        borderRadius: 1,
                                                        backgroundColor: 'action.hover',
                                                        color: 'text.primary',
                                                    }}
                                                >
                                                    {testResult.errorMessage}
                                                </Box>
                                            )}
                                        </>
                                    )}
                                </AlertComponent>
                            )}

                            <Box sx={{maxWidth: 900}}>
                                <MailProcessingNotice/>
                            </Box>
                        </Box>
                    )}
                </Box>
            </Paper>
        </PageWrapper>
    );
}
