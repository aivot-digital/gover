export const CASE_NUMBER_TYPE_UUID = 'uuid';
export const CASE_NUMBER_TYPE_TEMPLATE = 'template';

export type CaseNumberType = typeof CASE_NUMBER_TYPE_UUID | typeof CASE_NUMBER_TYPE_TEMPLATE;

export const CASE_NUMBER_TEMPLATE_MAX_LENGTH = 64;

const CASE_NUMBER_MAX_RENDERED_LENGTH = 36;
const CASE_NUMBER_PADDING_MIN = 4;
const CASE_NUMBER_PADDING_MAX = 12;
const CASE_NUMBER_INCREMENT_PATTERN = /^%I\((\d{1,2})\)/;

const PLACEHOLDERS: Array<{
    token: string;
    renderedLength: number;
}> = [
    {
        token: '%YYY',
        renderedLength: 4,
    },
    {
        token: '%Y',
        renderedLength: 2,
    },
    {
        token: '%M',
        renderedLength: 2,
    },
    {
        token: '%D',
        renderedLength: 2,
    },
    {
        token: '%h',
        renderedLength: 2,
    },
    {
        token: '%m',
        renderedLength: 2,
    },
];

export function getCaseNumberType(caseNumberTemplate: string | null | undefined): CaseNumberType {
    return caseNumberTemplate == null ? CASE_NUMBER_TYPE_UUID : CASE_NUMBER_TYPE_TEMPLATE;
}

export function validateCaseNumberTemplate(caseNumberTemplate: string | null | undefined): string | undefined {
    const template = caseNumberTemplate?.trim() ?? '';

    if (template.length === 0) {
        return 'Bitte geben Sie eine Vorgangsschlüssel-Formatvorlage an.';
    }

    if (template.length > CASE_NUMBER_TEMPLATE_MAX_LENGTH) {
        return `Die Vorgangsschlüssel-Formatvorlage darf maximal ${CASE_NUMBER_TEMPLATE_MAX_LENGTH} Zeichen lang sein.`;
    }

    let renderedLength = 0;
    let incrementCount = 0;

    // Keep this parser aligned with the backend CaseNumberGeneratorService.
    // The backend remains the final source of truth, this only gives immediate UI feedback.
    for (let index = 0; index < template.length;) {
        const incrementMatch = template.slice(index).match(CASE_NUMBER_INCREMENT_PATTERN);
        if (incrementMatch != null) {
            incrementCount += 1;

            const padding = Number.parseInt(incrementMatch[1], 10);
            if (padding < CASE_NUMBER_PADDING_MIN || padding > CASE_NUMBER_PADDING_MAX) {
                return `Die Inkrement-Breite muss zwischen ${CASE_NUMBER_PADDING_MIN} und ${CASE_NUMBER_PADDING_MAX} Stellen liegen.`;
            }

            renderedLength += padding;
            index += incrementMatch[0].length;
            continue;
        }

        const placeholder = PLACEHOLDERS.find((item) => template.startsWith(item.token, index));
        if (placeholder != null) {
            renderedLength += placeholder.renderedLength;
            index += placeholder.token.length;
            continue;
        }

        if (template[index] === '%') {
            return 'Die Vorgangsschlüssel-Formatvorlage enthält einen unbekannten Platzhalter.';
        }

        const codePoint = template.codePointAt(index);
        renderedLength += 1;
        index += codePoint == null ? 1 : String.fromCodePoint(codePoint).length;
    }

    if (incrementCount !== 1) {
        return 'Die Vorgangsschlüssel-Formatvorlage muss genau einen Inkrement-Platzhalter im Format %I(n) enthalten.';
    }

    if (renderedLength > CASE_NUMBER_MAX_RENDERED_LENGTH) {
        return `Der erzeugte Vorgangsschlüssel darf maximal ${CASE_NUMBER_MAX_RENDERED_LENGTH} Zeichen lang sein.`;
    }

    return undefined;
}
