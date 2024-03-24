export function checkTitle(title?: string): string[] {
    if (title == null) {
        return ['Bitte geben Sie einen Titel ein.'];
    }

    const errors: string[] = [];
    const trimmedTitle = title.trim();

    if (trimmedTitle.length === 0) {
        errors.push('Bitte geben Sie einen Titel ein.');
    }

    if (trimmedTitle.length > 60) {
        errors.push('Der Titel darf höchstens 60 Zeichen lang sein.');
    }

    return errors;
}