import {isValidProcessDataPath} from './process-data-path';

function transliterate(value: string): string {
    // Pasted umlauts may use combining characters; treat both Unicode spellings identically.
    return value.normalize('NFC').replace(/ä/g, 'ae').replace(/ö/g, 'oe').replace(/ü/g, 'ue')
        .replace(/Ä/g, 'Ae').replace(/Ö/g, 'Oe').replace(/Ü/g, 'Ue').replace(/ß/g, 'ss').replace(/ẞ/g, 'SS')
        .normalize('NFKD').replace(/[\u0300-\u036f]/g, '');
}

/** Keep path separators and case intact; whitespace beside separators is already valid path syntax. */
export function normalizeDataKeyInput(value: string): string {
    return transliterate(value)
        .replace(/\s*([.\[\]])\s*/g, '$1')
        .replace(/[\s-]+/g, '_');
}

export function generateDataKey(title: string | null | undefined, maxLength?: number): string | null {
    let key = transliterate(title ?? '').toLowerCase()
        .replace(/[^a-z0-9_$]+/g, '_').replace(/_+/g, '_').replace(/^_+|_+$/g, '');
    if (!key) return null;
    if (/^[0-9]/.test(key)) key = `feld_${key}`;
    return maxLength == null ? key : key.slice(0, maxLength);
}

export function validateDataKeyPath(value: string | null | undefined, required = false, allowWildcards = false): string | undefined {
    if (!value?.trim()) return required ? 'Geben Sie einen Datenschlüssel ein.' : undefined;
    if (isValidProcessDataPath(value, allowWildcards)) return undefined;
    if (!allowWildcards && value.includes('*')) return 'An dieser Stelle sind keine Platzhalter (*) erlaubt.';
    if (/[^a-zA-Z0-9_$.[\]*\s]/.test(value)) {
        return 'Verwenden Sie Buchstaben von A bis Z, Zahlen, Unterstriche oder $. Pfade werden mit Punkten und Array-Indizes wie [0] geschrieben.';
    }
    if (/(?:^|\.)\s*[0-9]/.test(value)) return 'Ein Name im Datenschlüssel darf nicht mit einer Zahl beginnen.';
    if ([...value.matchAll(/\[\s*([0-9]+)\s*\]/g)].some((match) => Number(match[1]) > 2147483647)) {
        return 'Ein Array-Index darf höchstens 2147483647 betragen.';
    }
    return allowWildcards
        ? 'Vervollständigen Sie den Pfad, zum Beispiel person.name, items[0].name oder items[*].name.'
        : 'Vervollständigen Sie den Pfad, zum Beispiel person.name oder items[0].name.';
}

/** Keep this rule aligned with ProcessNodeDataKeyUtils: node keys are single identifiers accessed through _.key. */
export function validateNodeDataKey(value: string | null | undefined): string | undefined {
    if (!value?.trim()) return 'Geben Sie einen Datenschlüssel ein.';
    if (value.length > 32) return 'Der Datenschlüssel darf maximal 32 Zeichen lang sein.';
    if (!/^[a-zA-Z_$][a-zA-Z0-9_$]*$/.test(value)) {
        return 'Verwenden Sie Buchstaben von A bis Z, Zahlen, Unterstriche oder $. Der Datenschlüssel darf nicht mit einer Zahl beginnen und keine Punkte oder Klammern enthalten.';
    }
    return undefined;
}
