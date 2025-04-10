function hexToRgb(hex: string) {
    // Remove the hash at the start if it's there
    hex = hex.replace(/^#/, '');

    // Parse r, g, b values
    let bigint = parseInt(hex, 16);
    let r = (bigint >> 16) & 255;
    let g = (bigint >> 8) & 255;
    let b = bigint & 255;

    return [r, g, b];
}

function luminance(r: number, g: number, b: number) {
    let a = [r, g, b].map(function(v) {
        v /= 255;
        return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
    });
    return a[0] * 0.2126 + a[1] * 0.7152 + a[2] * 0.0722;
}

function toFixedDown(num: number, digits: number) {
    const factor = Math.pow(10, digits);
    return Math.floor(num * factor) / factor;
}

export function calculateContrastRatio(hex1: string, hex2: string) {
    let rgb1 = hexToRgb(hex1);
    let rgb2 = hexToRgb(hex2);

    let lum1 = luminance(rgb1[0], rgb1[1], rgb1[2]);
    let lum2 = luminance(rgb2[0], rgb2[1], rgb2[2]);

    let brightest = Math.max(lum1, lum2);
    let darkest = Math.min(lum1, lum2);

    return toFixedDown((brightest + 0.05) / (darkest + 0.05), 1);
}