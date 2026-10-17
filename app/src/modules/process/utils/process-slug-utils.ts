import {slugify} from '../../../utils/slugify';

export const PROCESS_SLUG_MAX_LENGTH = 128;

export function normalizeProcessSlugInput(value: string | null | undefined): string | null {
    const slug = slugify(value ?? '', PROCESS_SLUG_MAX_LENGTH)
        .replace(/_/g, '-')
        .replace(/--+/g, '-');

    return slug.length > 0 ? slug : null;
}

export function validateProcessSlug(value: string | null | undefined): string | undefined {
    const trimmedValue = value?.trim() ?? '';

    if (trimmedValue.length === 0) {
        return 'Bitte geben Sie einen URL-Namespace für den Prozess an.';
    }

    if (trimmedValue.length < 3) {
        return 'Der URL-Namespace muss mindestens 3 Zeichen lang sein.';
    }

    if (trimmedValue.length > PROCESS_SLUG_MAX_LENGTH) {
        return `Der URL-Namespace darf maximal ${PROCESS_SLUG_MAX_LENGTH} Zeichen lang sein.`;
    }

    if (!/^[a-z0-9-]+$/.test(trimmedValue)) {
        return 'Der URL-Namespace darf nur aus Kleinbuchstaben, Zahlen und Bindestrichen bestehen.';
    }

    return undefined;
}
