import React from 'react';
import {Box, Typography} from '@mui/material';

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
                die <b>Whitelist</b> Ihrer E-Mail-Konfiguration aufzunehmen. Falls Sie keine E-Mails erhalten,
                überprüfen Sie bitte auch Ihren Spam-Ordner oder kontaktieren Sie Ihre E-Mail-Administrator:in.
            </Typography>
        </Box>
    );
}
