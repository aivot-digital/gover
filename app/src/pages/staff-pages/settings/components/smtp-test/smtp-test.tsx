import React, {type FormEvent, useState} from 'react';
import {Alert, AlertTitle, Box, Button, CircularProgress, TextField, Typography} from '@mui/material';
import {useApi} from "../../../../../hooks/use-api";
import {useSystemApi} from "../../../../../hooks/use-system-api";


export function SmtpTest(): JSX.Element {
    const api = useApi();
    const [targetEmail, setTargetEmail] = useState('');
    const [isSending, setIsSending] = useState(false);
    const [emailTestResult, setEmailTestResult] = useState<true | string>();

    const handleSubmit = (event: FormEvent): void => {
        event.preventDefault();

        setIsSending(true);
        setEmailTestResult(undefined);

        useSystemApi(api)
            .testSmtp(targetEmail)
            .then((res) => {
                if (res.result != null) {
                    setEmailTestResult(res.result);
                } else {
                    setEmailTestResult(true);
                }
            })
            .catch((err) => {
                console.error(err);
                if (err.response?.data?.message != null) {
                    setEmailTestResult(err.response.data.message);
                } else {
                    setEmailTestResult(err.message);
                }
            })
            .finally(() => {
                setIsSending(false);
                setTargetEmail('');
            });
    };

    return (
        <>
            <Typography variant="subtitle1">
                E-Mail-Versand
            </Typography>
            <Typography>
                Hier können Sie den E-Mail-Versand testen.
                Geben Sie dazu eine Zieladresse ein, und klicken Sie auf <strong>E-Mail-Versand Testen</strong>.
                Gover versendet eine Test-E-Mail an die angegebene Adresse.
                Sollten Probleme auftreten, werden diese hier angezeigt.
            </Typography>

            {
                isSending &&
                <Alert
                    severity="info"
                    sx={{mt: 2}}
                >
                    <AlertTitle>
                        Teste E-Mail-Versand
                    </AlertTitle>

                    <Box
                        sx={{
                            mt: 2,
                            display: 'flex',
                            alignItems: 'center',
                        }}
                    >
                        <CircularProgress
                            color="info"
                            size="2em"
                        />

                        <Typography sx={{ml: 2}}>
                            Der Test der verbindung läuft aktuell. Schließen Sie diese Seite nicht. Der Test kann bis zu
                            5 Minuten dauern.
                        </Typography>
                    </Box>
                </Alert>
            }

            {
                emailTestResult != null &&
                emailTestResult === true &&
                <Alert
                    severity="success"
                    sx={{mt: 2}}
                >
                    <AlertTitle>
                        Test erfolgreich!
                    </AlertTitle>

                    Der Test des E-Mail-Versands war erfolgreich.
                    Sie sollten gleich eine E-Mail in Ihrem Postfach vorfinden.
                </Alert>
            }

            {
                emailTestResult != null &&
                emailTestResult !== true &&
                <Alert
                    severity="error"
                    sx={{mt: 2}}
                >
                    <AlertTitle>
                        Versand fehlgeschlagen!
                    </AlertTitle>

                    <Typography>
                        Beim Versand der Test-E-Mail ist ein Fehler aufgetreten.
                        Mehr Informationen entnehmen Sie dem Fehlerbericht.
                    </Typography>

                    <Typography
                        variant="subtitle2"
                        sx={{mt: 2}}
                    >
                        Fehlerbericht
                    </Typography>
                    <Typography component="code">
                        {emailTestResult}
                    </Typography>
                </Alert>
            }

            <form onSubmit={handleSubmit}>
                <Box
                    sx={{
                        display: 'flex',
                        mt: 2,
                        alignItems: 'center',
                    }}
                >
                    <TextField
                        label="Zieladresse"
                        type="email"
                        placeholder="max.muster@mail.de"
                        value={targetEmail}
                        onChange={(event) => {
                            setTargetEmail(event.target.value);
                        }}
                        onBlur={() => {
                            setTargetEmail(targetEmail.trim());
                        }}
                        disabled={isSending}
                        required
                    />

                    <Button
                        type="submit"
                        sx={{
                            ml: 2,
                            flexShrink: 0,
                        }}
                        disabled={isSending}
                    >
                        E-Mail-Versand Testen
                    </Button>
                </Box>
            </form>
        </>
    );
}
