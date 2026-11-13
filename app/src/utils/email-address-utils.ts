export const invalidEmailMessage = 'Bitte geben Sie eine gültige E-Mail-Adresse ein.';

/** One ASCII local part and an IDNA domain; no display names, lists or address literals. */
export function normalizeEmailAddress(value: string, allowLocalDomains = false): string | undefined {
    if (/[\u0000-\u001f\u007f]/.test(value)) return undefined;
    const address = value.trim();
    const parts = address.split('@');
    if (parts.length !== 2) return undefined;
    const [local, domain] = parts;
    if (local.length > 64 || !/^[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+)*$/.test(local)) return undefined;
    // URL is used only for IDNA conversion. Reject URL syntax and address literals first.
    if (!/^[\p{L}\p{M}\p{N}.\-]+$/u.test(domain)) return undefined;
    try {
        // A suffix prevents URL's special IPv4 parsing from changing numeric domains.
        const ascii = new URL(`http://${domain}.invalid`).hostname.slice(0, -8);
        if (!allowLocalDomains && !ascii.includes('.')) return undefined;
        if (!ascii.split('.').every(label => label.length <= 63 && (label.slice(2, 4) !== '--' || label.startsWith('xn--')) && /^[a-z0-9](?:[a-z0-9-]*[a-z0-9])?$/.test(label))) return undefined;
        const normalized = `${local}@${ascii}`;
        return normalized.length <= 254 ? normalized : undefined;
    } catch {
        return undefined;
    }
}

export function validateEmailAddress(value: string | null | undefined, allowLocalDomains = false): string | undefined {
    return value == null || value.trim() === '' || normalizeEmailAddress(value, allowLocalDomains) != null
        ? undefined : invalidEmailMessage;
}
