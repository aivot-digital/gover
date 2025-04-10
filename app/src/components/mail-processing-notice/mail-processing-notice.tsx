import React from "react";
import { Alert, AlertTitle, Typography } from "@mui/material";

export function MailProcessingNotice(): JSX.Element {
    return (
        <Alert severity="warning" sx={{ mt: 2 }}>
            <AlertTitle>Wichtiger Hinweis zum E-Mail-Versand</AlertTitle>
            <Typography>
                Die Gover-Plattform kann lediglich die <b>erfolgreiche Übergabe</b> der E-Mail an den hinterlegten SMTP-Server bestätigen, nicht die Zustellung selbst.
                Eine spätere Nichtzustellung (z. B. durch Bounces oder Spam-Filter) kann nicht automatisiert vom System erkannt werden.
            </Typography>
            <Typography sx={{ mt: 1 }}>
                Um Zustellprobleme zu vermeiden, empfehlen wir, die für Gover konfigurierte E-Mail-Absenderadresse in die <b>Whitelist</b> Ihrer
                E-Mail-Konfiguration aufzunehmen. Falls Sie keine E-Mails erhalten, überprüfen Sie bitte auch Ihren Spam-Ordner oder kontaktieren Sie Ihre
                E-Mail-Administrator:in.
            </Typography>
        </Alert>
    );
}