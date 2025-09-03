import React from 'react';
import Typography from '@mui/material/Typography';

export function FormsListPageHelp() {
    return (
        <>
            <Typography
                variant="body1"
                paragraph
            >
                Konfigurieren Sie hier die Nutzerkontenanbieter, die in Ihrer Gover-Instanz global verfügbar sein sollen.
                Die angebundenen Nutzerkonten können in Formularen als Authentifizierungsoptionen verwendet werden.
                Unterstützt werden alle Anbieter, die eine OpenID Connect (OIDC) kompatible Schnittstelle bereitstellen.
            </Typography>
            <Typography
                variant="body1"
                paragraph
            >
                <strong>Mögliche Szenarien:</strong>
            </Typography>
            <ul>
                <li>
                    <Typography
                        variant="body1"
                        paragraph
                    >
                        <strong>Direkt OpenID Connect kompatible IDPs</strong>
                        (z.B. BundID, BayernID, Mein Unternehmenskonto, Servicekonto SH, Keycloak, Azure AD):
                        <br />
                        → Sie können den Anbieter direkt anbinden, indem Sie die Verbindungsdaten hier hinterlegen.
                    </Typography>
                </li>
                <li>
                    <Typography
                        variant="body1"
                        paragraph
                    >
                        <strong>Systeme ohne OpenID Connect Unterstützung</strong>
                        (z.B. LDAP/AD, andere IDPs):
                        <br />
                        → Die Anbindung erfolgt über den integrierten Keycloak von Gover. Tragen Sie anschließend die OpenID Connect-Daten des Keycloak-Realms hier ein.
                    </Typography>
                </li>
                <li>
                    <Typography
                        variant="body1"
                        paragraph
                    >
                        <strong>LDAP/AD für Gover-Mitarbeitende:</strong>
                        <br />
                        → Nutzung der User Federation im Staff Realm des Gover-Keycloaks.
                        <br />
                        Diese Nutzerkonten werden nicht über die Funktion "Nutzerkontenanbieter" verwaltet.
                    </Typography>
                </li>
            </ul>
            <Typography
                variant="body1"
                paragraph
            >
                Es wird empfohlen, für jeden Nutzerkontenanbieter sowohl eine produktive als auch eine vorproduktive Anbindung einzurichten, um Tests zu erleichtern.
            </Typography>
            <Typography
                variant="body1"
                paragraph
            >
                Die notwendigen Konfigurationsdaten erhalten Sie in der Dokumentation des Anbieters oder direkt vom Anbieter selbst.
            </Typography>
        </>
    );
}