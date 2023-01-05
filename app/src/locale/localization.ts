//const testingLang = 'la'; // Choose Latin as testing language

export function Localization<T extends {
    [key: string]: string;
}>(strings: {
    de: T;
    [lang: string]: T;
}) {
    const lang = 'de';//(window.navigator.language ?? 'de-DE').substring(0, 2);
    return new Proxy<T & {
        [key: string]: string;
    } & {
        format: (str: string, data: any) => string;
    }>({
        ...(strings[lang] ?? strings.de),
        format(str: string, data: any): string {
            return str.replace(/\{(\w+)}/g, (_, v) => data[v]);
        }
    }, {
        get(target: T, p: string): string {
            if (p === 'format') {
                return target.format;
            }
            /*
            if (lang === testingLang) {
                return '>>LOREM IPSUM<<';
            }
             */
            if (p in target) {
                return target[p];
            } else {
                console.warn(`Warning: accessing undefined string "${p}"`);
                return `__${p.toUpperCase()}__`;
            }
        },
    });
}
