export function slugify(str: string, length: number) {
    return str
        .toLowerCase()  // Convert the string to lowercase letters
        .replace(/\u00e4/g, 'ae')
        .replace(/\u00f6/g, 'oe')
        .replace(/\u00fc/g, 'ue')
        .replace(/\u00df/g, 'ss')
        .normalize('NFKD')  // The normalize() using NFKD method returns the Unicode Normalization Form of a given string.
        .replace(/\s+/g, '-')   // Replace spaces with -
        .replace(/[^\w-]+/g, '')    // Remove all non-word chars
        .replace(/--+/g, '-')  // Replace multiple - with single -
        .substring(0, length);  // Trim - from start of text
}
