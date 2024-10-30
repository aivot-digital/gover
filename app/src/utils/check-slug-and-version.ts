import {type Form as Application, FormListProjection} from '../models/entities/form';

export function checkSlugAndVersion(applications: Application[] | FormListProjection[], slug?: string, version?: string): {
    slugError?: string;
    versionError?: string;
} {
    const errors: {
        slugError?: string;
        versionError?: string;
    } = {};

    const trimmedSlug = slug?.trim() ?? '';
    const trimmedVersion = version?.trim() ?? '';

    if (trimmedSlug.length < 3) {
        errors.slugError = 'Das URL-Element des Formulars muss aus mindestens 3 Zeichen bestehen.';
    }

    if (trimmedSlug.length > 96) {
        errors.slugError = 'Das URL-Element des Formulars darf höchstens 96 Zeichen lang sein.';
    }

    if (!/^[a-z0-9-]+$/.test(trimmedSlug)) {
        errors.slugError = 'Das URL-Element des Formulars kann nur aus Kleinbuchstaben (ohne Umlaute), Zahlen und Bindestrichen bestehen.';
    }

    if (applications != null && applications?.some((app) => app.slug === trimmedSlug && app.version === trimmedVersion)) {
        errors.slugError = 'Es existiert bereits ein Formular mit diesem URL-Element und dieser Version.';
        errors.versionError = 'Es existiert bereits ein Formular mit diesem URL-Element und dieser Version.';
    }

    return errors;
}
