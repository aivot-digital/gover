export function checkVersion(version?: string): string[] {
    const errors: string[] = [];

    const trimmedVersion = version?.trim() ?? '';

    if (trimmedVersion.length < 3) {
        errors.push('Die Version des Antrages muss aus mindestens 3 Zeichen bestehen.');
    }

    return errors;
}
