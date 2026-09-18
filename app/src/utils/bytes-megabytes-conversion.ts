const BYTES_PER_MB = 1000 * 1000;

export const bytesToMegabytes = (bytes?: number | null) => {
    if (!bytes || bytes === 0) return undefined;
    return bytes / BYTES_PER_MB;
};

export const megabytesToBytes = (mb?: number | null) => {
    if (mb == null) return 0;
    // 0 MB => keine Begrenzung
    if (mb === 0) return 0;
    // runden auf ganze Bytes
    return Math.round(mb * BYTES_PER_MB);
};