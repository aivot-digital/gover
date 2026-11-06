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
        title: 'Fester Wert',
        details: (
            <>
                <Typography gutterBottom>
                    Ein „Fester Wert” ermöglicht es Ihnen, einen konstanten Wert direkt in den Ausdruck einzufügen.
                    Dies ist nützlich, wenn Sie mit unveränderlichen Daten arbeiten oder Standardwerte festlegen
                    möchten.
                </Typography>
                <Typography>
                    Nachdem Sie den Wert festgelegt haben, können Sie diesen mit anderen Werten oder Ausdrücken
                    kombinieren, um komplexere Berechnungen oder Logiken zu erstellen.
                </Typography>
            </>
        ),
    },
    NoCodeReference: {
        title: 'Wert eines Elements',
        details: (
            <>
                <Typography gutterBottom>
                    Mit der Option „Wert eines Elements” können Sie auf Werte zugreifen, die in anderen Feldern
                    eingegeben oder berechnet wurden.
                    Dies ist besonders nützlich, um Beziehungen zwischen verschiedenen Datenpunkten herzustellen und
                    dynamische Ausdrücke zu erstellen.
                </Typography>
                <Typography gutterBottom>
                    Sie können nur Elemente auswählen, die sich im selben Kontext befinden und die oberhalb des
                    aktuellen Elements liegen.
                </Typography>
                <Typography>
                    Nachdem Sie ein Element ausgewählt haben, können Sie dessen Wert in Ihrem Ausdruck verwenden und mit
                    anderen Werten oder Ausdrücken kombinieren.
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
                    Diese Option greift auf Geschützte Vorgangsdaten (`$$`) zu.
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
        title: 'Ausdruck',
        details: (
            <Typography gutterBottom>
                Mit der Option „Ausdruck” können Sie einen benutzerdefinierten Ausdruck erstellen, der verschiedene
                Werte mit einem Operator kombiniert.
            </Typography>
        ),
    },
};
