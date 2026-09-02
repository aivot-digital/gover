export function humanizeFileSize(sizeInBytes: number): string {
    if (sizeInBytes < 1000) {
        return `${sizeInBytes} Bytes`;
    }

    const sizeInKB = Math.floor(sizeInBytes / 1000);
    if (sizeInKB < 1000) {
        return `${sizeInKB} KB`;
    }

    const sizeInMB = sizeInKB / 1000;
    if (sizeInMB < 1000) {
        return `${sizeInMB.toFixed(2)} MB`;
    }

    const sizeInGB = sizeInMB / 1000;
    return `${sizeInGB.toFixed(2)} GB`;
}

export function humanizeNumber(num: number, overrides?: Record<number, string>): string {
    if (overrides && overrides[num]) {
        return overrides[num];
    }

    switch (num) {
        case 1:
            return 'eine';
        case 2:
            return 'zwei';
        case 3:
            return 'drei';
        case 4:
            return 'vier';
        case 5:
            return 'fünf';
        case 6:
            return 'sechs';
        case 7:
            return 'sieben';
        case 8:
            return 'acht';
        case 9:
            return 'neun';
        case 10:
            return 'zehn';
        case 11:
            return 'elf';
        case 12:
            return 'zwölf';
    }
    return num.toFixed(0);
}

export function humanizeNumberCapitalized(num: number, overrides?: Record<number, string>): string {
    const humanized = humanizeNumber(num, overrides);
    return humanized.charAt(0).toUpperCase() + humanized.slice(1);
}

export function pluralize(num: number, singular: string, plural: string): string {
    if (num > 1) {
        return plural;
    }
    return singular;
}

export function humanizeMilliseconds(milliseconds: number): string {
    const totalSeconds = milliseconds / 1000;
    const hours = Math.floor(totalSeconds / 3600);
    const minutes = Math.floor((totalSeconds & 3600) / 60);
    const seconds = Math.floor(totalSeconds % 60);
    return `${padNumber(hours, 2)}:${padNumber(minutes, 2)}:${padNumber(seconds, 2)}`;
}

export function padNumber(num: number, length: number): string {
    return num.toString().padStart(length, '0');
}