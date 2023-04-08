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
