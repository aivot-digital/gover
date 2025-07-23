export function checkVersion(version?: string): string[] {
    const errors: string[] = [];

    if (version == null) {
        return [
            'Bitte geben Sie eine Versionsnummer an.',
        ];
    }

    const versionParts = version.split('.');
    if (versionParts.length !== 3) {
        errors.push('Die Version muss aus drei Teilen bestehen, die jeweils durch einen Punkt getrennt sind. Zum Beispiel: X.Y.Z');
    }

    if (versionParts.some(p => isNaN(parseInt(p)))) {
        errors.push('Bitte verwenden Sie nur Zahlen die Version zu beschreiben. Zum Beispiel: 4.12.3');
    }

    return errors;
}

export function compareVersions(versionA: string, versionB: string): number {
    const parseVersion = (v: string): number[] => {
        return v
            .split('.')
            .map(part => parseInt(part, 10))
            .filter(n => !isNaN(n));
    };

    const vA = parseVersion(versionA);
    const vB = parseVersion(versionB);

    const maxLength = Math.max(vA.length, vB.length);

    for (let i = 0; i < maxLength; i++) {
        const a = vA[i] ?? 0;
        const b = vB[i] ?? 0;

        if (a < b) return 1;
        if (a > b) return -1;
    }

    return 0;
}

