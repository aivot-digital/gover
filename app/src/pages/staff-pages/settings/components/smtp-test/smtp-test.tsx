import React, {type FormEvent, useState} from 'react';
import {Alert, AlertTitle, Box, Button, CircularProgress, TextField, Typography} from '@mui/material';
import {useApi} from "../../../../../hooks/use-api";
import {useSystemApi} from "../../../../../hooks/use-system-api";
import SendOutlinedIcon from '@mui/icons-material/SendOutlined';

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
            <Typography
                sx={{
                    maxWidth: 900,
                }}
            >
                Hier können Sie den Versand von E-Mails durch das System testen.
                Geben Sie dazu eine E-Mail-Adresse als Ziel ein und klicken auf „E-Mail-Versand testen“.
                Sollten beim Versand Probleme auftreten, werden diese hier angezeigt.
            </Typography>

            {
                isSending &&
                <Alert
                    severity="info"
                    sx={{mt: 3}}
                >
                    <AlertTitle>
                        Teste E-Mail-Versand
                    </AlertTitle>

                    <Box
                        sx={{
                            display: 'flex',
                            alignItems: 'center',
                        }}
                    >
                        <CircularProgress
                            color="info"
                            size="2em"
                        />

                        <Typography sx={{ml: 2}}>
                            Der Test der Verbindung wird durchgeführt. Schließen Sie diese Seite nicht. Der Test kann bis zu
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
                    sx={{mt: 3}}
                >
                    <AlertTitle>
                        Test erfolgreich!
                    </AlertTitle>

                    Der Test des E-Mail-Versands war erfolgreich.
                    Sie sollten gleich eine E-Mail im Postfach der angegebenen Adresse vorfinden.
                </Alert>
            }

            {
                emailTestResult != null &&
                emailTestResult !== true &&
                <Alert
                    severity="error"
                    sx={{mt: 3}}
                >
                    <AlertTitle>
                        Versand fehlgeschlagen!
                    </AlertTitle>

                    <Typography>
                        Beim Versand der Test-E-Mail ist ein Fehler aufgetreten.
                        Mehr Informationen können Sie dem Fehlerbericht entnehmen.
                    </Typography>

                    <Typography
                        variant="subtitle2"
                        sx={{mt: 1.6}}
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
                        mt: 1.6,
                    }}
                >
                    <TextField
                        label="E-Mail-Adresse (Ziel)"
                        type="email"
                        placeholder="name@bad-musterstadt.de"
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
                        color="primary"
                        variant="contained"
                        startIcon={<SendOutlinedIcon
                            sx={{
                                marginTop: '-2px',
                            }}
                        />}
                        sx={{
                            mt: 4,
                        }}
                        disabled={isSending}
                    >
                        E-Mail-Versand testen
                    </Button>
                </Box>
            </form>
        </>
    );
}
