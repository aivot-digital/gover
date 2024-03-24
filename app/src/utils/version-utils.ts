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
    const vAParts = versionA.split('.').map(v => parseInt(v));
    const vBParts = versionB.split('.').map(v => parseInt(v));

    if (vAParts.length !== 3) {
        return 1;
    }
    if (vBParts.length !== 3) {
        return -1;
    }

    const [majorA, minorA, patchA] = vAParts;
    const [majorB, minorB, patchB] = vBParts;

    if (majorA < majorB) {
        return 1;
    }
    if (majorA > majorB) {
        return -1;
    }

    if (minorA < minorB) {
        return 1;
    }
    if (minorA > minorB) {
        return -1;
    }

    if (patchA < patchB) {
        return 1;
    }
    if (patchA > patchB) {
        return -1;
    }

    return 0;
}
