export function parseGermanNumber(val: string): number {
    return parseFloat(val.replaceAll('.', '').replaceAll(',', '.'));
};
