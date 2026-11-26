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
                    Sie können Eingabefelder aus demselben Kontext auswählen. Innerhalb wiederholbarer Datensätze
                    ist die Auswahl auf diesen Bereich beschränkt.
                </Typography>
            </>
        ),
    },
    NoCodeProcessDataReference: {
        title: 'Vorgangsdaten',
        details: (
            <>
                <Typography gutterBottom>
                    Verwenden Sie einen Wert aus der Vorgangsdatenebene (<code>$</code>).
                </Typography>
                <Typography>
                    Geben Sie den Datenpfad zum gewünschten Wert an, zum Beispiel <code>person.name</code>.
                </Typography>
            </>
        ),
    },
    NoCodeInstanceDataReference: {
        title: 'Geschützte Vorgangsdaten',
        details: (
            <>
                <Typography gutterBottom>
                    Verwenden Sie eine systemseitig bereitgestellte Information aus der geschützten Vorgangsdatenebene (<code>$$</code>).
                </Typography>
                <Typography>
                    Geben Sie den Datenpfad zum gewünschten Wert an, zum Beispiel <code>caseNumber</code>.
                </Typography>
            </>
        ),
    },
    NoCodeNodeDataReference: {
        title: 'Elementdaten',
        details: (
            <>
                <Typography gutterBottom>
                    Verwenden Sie ein Ergebnis eines Prozesselements aus der Elementdatenebene (<code>_</code>).
                </Typography>
                <Typography>
                    Geben Sie den Datenschlüssel des Prozesselements und den Pfad zum gewünschten Wert an,
                    zum Beispiel <code>pruefung.ergebnis</code>.
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
