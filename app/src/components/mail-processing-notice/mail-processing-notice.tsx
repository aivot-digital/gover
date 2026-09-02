import React from 'react';
import {Box, Link, Typography} from '@mui/material';
import OpenInNew from '@aivot/mui-material-symbols-400-n25-outlined/OpenInNew';

export function MailProcessingNotice() {
    return (
        <Box
            component="section"
            sx={{mt: 5}}
        >
            <Typography
                variant="subtitle1"
                component="h2"
            >
                Hinweis zur E-Mail-Zustellung
            </Typography>
            <Typography>
                Prosuna kann lediglich die <b>erfolgreiche Übergabe</b> der E-Mail an den konfigurierten E-Mail-Server
                bestätigen, nicht die Zustellung selbst. Die tatsächliche Zustellung kann durch nachgelagerte Prüfungen
                wie Graylisting oder Spamfilter deutlich verzögert werden. Prosuna kann den weiteren Zustellstatus nicht
                ermitteln.
            </Typography>
            <Typography sx={{mt: 1, mb: 1.5}}>
                Um Zustellprobleme zu vermeiden, empfehlen wir, die für Prosuna konfigurierte E-Mail-Absenderadresse in
                die <b>Allowlist</b> Ihrer E-Mail-Konfiguration aufzunehmen. Falls Sie keine E-Mails erhalten,
                überprüfen Sie bitte auch Ihren Spam-Ordner oder kontaktieren Sie Ihre E-Mail-Administrator:in.
            </Typography>

            <Typography
                variant="subtitle1"
                component="h3"
                sx={{mt: 3}}
            >
                Weiterführende Zustellprüfung
            </Typography>
            <Typography>
                Für eine genauere Diagnose können Sie unabhängige Prüfdienste verwenden:
            </Typography>
            <Box
                component="ul"
                sx={{
                    mt: 1,
                    mb: 1.5,
                    pl: 3,
                    '& li + li': {
                        mt: 0.75,
                    },
                }}
            >
                <li>
                    <Link
                        href="https://www.mail-tester.com/"
                        target="_blank"
                        rel="noopener noreferrer"
                    >
                        mail-tester.com
                        <OpenInNew
                            aria-hidden="true"
                            sx={{ml: 0.5, fontSize: '1em', verticalAlign: '-0.125em'}}
                        />
                    </Link>
                    {' '}stellt eine Testadresse bereit und bewertet die dorthin gesendete Nachricht u. A.
                    hinsichtlich Spam- und Authentifizierungsmerkmalen. Tragen Sie die Testadresse dafür oben als
                    Empfängeradresse ein.
                </li>
                <li>
                    <Link
                        href="https://mxtoolbox.com/emailhealth"
                        target="_blank"
                        rel="noopener noreferrer"
                    >
                        MXToolbox Email Health
                        <OpenInNew
                            aria-hidden="true"
                            sx={{ml: 0.5, fontSize: '1em', verticalAlign: '-0.125em'}}
                        />
                    </Link>
                    {' '}prüft die Domain-, DNS- und Mailserver-Konfiguration unabhängig von diesem Testversand.
                </li>
            </Box>
            <Typography variant="body2">
                <b>Wichtiger Hinweis:</b> Externe Prüfdienste verarbeiten die an ihre Testadresse gesendete E-Mail
                beziehungsweise die eingegebenen Domaininformationen. Verwenden Sie dafür keine sensiblen oder
                personenbezogenen Inhalte.
            </Typography>
        </Box>
    );
}
