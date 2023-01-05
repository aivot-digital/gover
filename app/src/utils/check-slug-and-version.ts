import {Application} from '../models/application';

export function checkSlugAndVersion(applications: Application[], slug?: string, version?: string): string[] {
    const errors: string[] = [];

    const trimmedSlug = slug?.trim() ?? '';
    const trimmedVersion = version?.trim() ?? '';

    if (trimmedSlug.length < 3) {
        errors.push('Das URL-Element des Antrages muss aus mindestens 3 Zeichen bestehen.');
    }

    if (!/^[a-z0-9-]+$/.test(trimmedSlug)) {
        errors.push('Das URL-Element des Antrages kann nur aus Kleinbuchstaben, Zahlen und Bindestrichen bestehen.');
    }

    if (applications != null && applications?.some(app => app.slug === trimmedSlug && app.version === trimmedVersion)) {
        errors.push('Es existiert bereits ein Antrag mit diesem URL-Element und dieser Version.');
    }

    return errors;
}
