import {type ReactNode} from 'react';
import {Typography} from '@mui/material';
import {type NoCodeOperand} from '../../../models/functions/no-code-expression';

export type NoCodeOperandKind = NoCodeOperand['type'];

export interface NoCodeOperandKindHelpEntry {
    title: string;
    details: ReactNode;
}

/** Explains each kind of operand, both when choosing one and in the hints of the pill editor. */
export const NoCodeOperandKindHelp: Record<NoCodeOperandKind, NoCodeOperandKindHelpEntry> = {
    NoCodeStaticValue: {
        title: 'Wert selbst festlegen',
        details: (
            <>
                <Typography gutterBottom>
                    Einen Wert direkt eingeben oder aus verfügbaren Optionen auswählen.
                </Typography>
                <Typography>
                    Der festgelegte Wert wird bei jeder Ausführung unverändert verwendet.
                </Typography>
            </>
        ),
    },
    NoCodeReference: {
        title: 'Aus Formularelement übernehmen',
        details: (
            <>
                <Typography gutterBottom>
                    Den aktuellen eingegebenen oder berechneten Wert eines Formularelements verwenden.
                </Typography>
                <Typography>
                    Sie können nur Elemente auswählen, die sich im selben Kontext befinden und die oberhalb des
                    aktuellen Elements liegen.
                </Typography>
            </>
        ),
    },
    NoCodeProcessDataReference: {
        title: 'Vorgangsdaten',
        details: (
            <>
                <Typography gutterBottom>
                    Mit der Option „Vorgangsdaten” können Sie auf Daten aus dem Prozesskontext zugreifen.
                </Typography>
                <Typography gutterBottom>
                    Diese Option greift auf den Vorgangsdaten-Kontext (`$`) zu.
                </Typography>
                <Typography>
                    Über einen optionalen Pfad können Sie gezielt verschachtelte Werte auslesen.
                </Typography>
            </>
        ),
    },
    NoCodeInstanceDataReference: {
        title: 'Geschützte Vorgangsdaten',
        details: (
            <>
                <Typography gutterBottom>
                    Diese Option greift auf geschützte Vorgangsdaten (`$$`) zu.
                </Typography>
                <Typography>
                    Über einen optionalen Pfad können Sie gezielt verschachtelte Werte auslesen.
                </Typography>
            </>
        ),
    },
    NoCodeNodeDataReference: {
        title: 'Elementdaten',
        details: (
            <>
                <Typography gutterBottom>
                    Diese Option greift auf Elementdaten (`_`) zu.
                </Typography>
                <Typography>
                    Für Elementdaten muss zusätzlich der Datenschlüssel des Prozesselementes angegeben werden.
                </Typography>
            </>
        ),
    },
    NoCodeExpression: {
        title: 'Mit Funktion ermitteln',
        details: (
            <Typography gutterBottom>
                Das Ergebnis einer Funktion verwenden. Die Funktion ermittelt den Wert bei der Ausführung.
            </Typography>
        ),
    },
};
