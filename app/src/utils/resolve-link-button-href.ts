// Keep editor feedback and runtime link activation on the same supported URL formats.
export function resolveLinkButtonHref(href: string | null | undefined): string | null {
    const normalizedHref = href?.trim();
    if (normalizedHref == null || normalizedHref.length === 0) {
        return null;
    }

    if (/^https?:\/\//i.test(normalizedHref)) {
        try {
            new URL(normalizedHref);
            return normalizedHref;
        } catch {
            return null;
        }
    }

    if (
        normalizedHref.startsWith('/') && !normalizedHref.startsWith('//') ||
        normalizedHref.startsWith('#') ||
        /^(mailto|tel):\S/i.test(normalizedHref)
    ) {
        return normalizedHref;
    }

    return null;
}

