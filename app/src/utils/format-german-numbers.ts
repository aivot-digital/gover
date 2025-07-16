export function formatNumToGermanNum(num: number, decimal?: number | null | undefined): string {
    return num.toLocaleString('de', {
        minimumFractionDigits: decimal ?? 0,
        maximumFractionDigits: decimal ?? 0,
    });
}

export function formatNumStringToGermanNum(num?: string | number, decimal?: number | null | undefined): string {
    if (num == null) {
        return '';
    }
    const val = typeof num === 'string' ? parseFloat(num) : num;
    if (isNaN(val)) {
        return '';
    } else {
        return formatNumToGermanNum(val, decimal);
    }
}
