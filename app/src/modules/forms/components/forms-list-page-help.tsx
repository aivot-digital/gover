import React from 'react';
import Typography from '@mui/material/Typography';

export function FormsListPageHelp() {
    return (
        <>
            <Typography
                variant="body1"
                component="p"
                marginBottom={2}
            >
                In dieser Übersicht sehen Sie alle Formulare, die als Referenzen auf Formulareingang-Prozesselemente in
                sämtlichen Prozessen dargestellt werden, auf die Sie Zugriff haben. Ein Formular entspricht dabei einem
                Formulareingang-Prozesselement innerhalb eines Prozesses.
            </Typography>
            <Typography
                variant="body1"
                component="p"
                marginBottom={2}
            >
                Neue Formulare fügen Sie hinzu, indem Sie in Ihren Prozessen ein neues Formulareingang-Prozesselement
                anlegen. In dieser Liste können Sie alle Formulare nach ihren Formulareingang-Prozesselementen einsehen,
                den Editor für das jeweilige Formular öffnen, direkt zum zugehörigen Prozess navigieren oder die
                Einstellungen des jeweiligen Formulareingang-Prozesselement-Knotens aufrufen.
            </Typography>
            <Typography
                variant="body1"
                component="p"
            >
                Nutzen Sie diese Übersicht, um schnell auf alle Formulare zuzugreifen, Änderungen vorzunehmen oder die
                Struktur Ihrer Prozesse zu verwalten. So behalten Sie stets den Überblick über alle Formulare und deren
                Einsatz in Ihren Prozessen.
            </Typography>
        </>
    );
}
