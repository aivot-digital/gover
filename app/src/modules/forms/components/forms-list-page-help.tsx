import React from 'react';
import Typography from '@mui/material/Typography';

export function FormsListPageHelp(): React.ReactElement {
    return (
        <>
            <Typography
                variant="body1"
                component="p"
                sx={{
                    marginBottom: 2
                }}
            >
                Formulare werden als Formulareingänge innerhalb von Prozessen erstellt und gemeinsam mit einer
                Prozessversion veröffentlicht. Sie besitzen daher keinen vom Prozess unabhängigen Lebenszyklus.
            </Typography>
            <Typography
                variant="body1"
                component="p"
                sx={{
                    marginBottom: 2
                }}
            >
                Unter „Veröffentlicht“ finden Sie alle Formulareingänge der aktuell veröffentlichten Prozessversionen,
                auf die Sie Zugriff haben. Die Übersicht enthält auch Formulare, die nicht im öffentlichen
                Formularverzeichnis erscheinen und ausschließlich über ihren Direktlink erreichbar sind.
            </Typography>
            <Typography
                variant="body1"
                component="p"
            >
                Unter „In Bearbeitung“ werden die Formulareingänge der aktuellen Prozessentwürfe angezeigt. Neue
                Formulare legen Sie an, indem Sie einem Prozess einen Formulareingang hinzufügen.
            </Typography>
        </>
    );
}
