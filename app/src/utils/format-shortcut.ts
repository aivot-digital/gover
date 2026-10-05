export type Modifier = 'meta' | 'ctrl' | 'shift' | 'alt';

/**
 * Formats a keyboard shortcut label according to platform conventions (Mac vs. Windows/Linux).
 *
 * - On macOS: uses Apple-style symbols (⌘, ⇧, ⌥, ⌃) with a narrow non-breaking space before the key.
 * - On Windows/Linux: uses German key names (Strg, Umschalt, Option, Befehl) with "+" separators.
 *
 * Example:
 *   - macOS → "⌘ K"
 *   - Windows → "Strg+K"
 */
export function formatShortcut(
    mods: Modifier[],
    key: string,
    opts?: {
        macSeparator?: string;
        winSeparator?: string;
    }
) {
    const isMac = /Mac|iPhone|iPad/i.test(navigator.platform);
    const macSep = opts?.macSeparator ?? '\u202F'; // default: narrow non-breaking space
    const winSep = opts?.winSeparator ?? '+';

    // Apple-specific order of modifier symbols for Mac
    const order: Modifier[] = ['ctrl', 'alt', 'shift', 'meta'];
    const sorted = [...mods].sort((a, b) => order.indexOf(a) - order.indexOf(b));

    const prettyKey = prettifyKey(key);

    if (isMac) {
        // Map modifier names to their macOS symbols
        const macMap: Record<Modifier, string> = {
            meta: '⌘',     // Befehl
            shift: '⇧',    // Umschalt
            alt: '⌥',      // Option
            ctrl: '⌃',     // Steuerung
        };
        const cluster = sorted.map(m => macMap[m]).join('');
        return `${cluster}${macSep}${prettyKey}`;
    }

    // On Windows/Linux: use German names with "+" separators
    const winMap: Record<Modifier, string> = {
        meta: 'Strg',
        ctrl: 'Strg',
        shift: 'Umschalt',
        alt: 'Alt',
    };
    return [...sorted.map(m => winMap[m]), prettyKey].join(winSep);
}

/**
 * Converts key identifiers to short, user-friendly display strings.
 * Uses uppercase characters and common arrow / function symbols where appropriate.
 */
function prettifyKey(k: string) {
    const key = k.toLowerCase();
    const map: Record<string, string> = {
        ' ': 'Leertaste',
        escape: 'Esc',
        esc: 'Esc',
        enter: 'Eingabe',
        return: 'Eingabe',
        tab: 'Tab',
        backspace: 'Rücktaste',
        delete: 'Entf',
        arrowup: '↑',
        arrowdown: '↓',
        arrowleft: '←',
        arrowright: '→',
    };
    return (map[key] ?? key).toUpperCase();
}
