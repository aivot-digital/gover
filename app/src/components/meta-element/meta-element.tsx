import React, {useMemo} from 'react';

interface MetaElementProps {
    faviconUrl?: string | null;
    title?: string;
    titlePrefix?: string;
}

export function MetaElement({faviconUrl, title, titlePrefix}: MetaElementProps) {
    const favicon = useMemo(() => {
        const configuredFavicon = faviconUrl === undefined ? AppConfig.faviconUrl : faviconUrl;
        const baseUrl = import.meta.env.BASE_URL;
        const prosunaFaviconUrl = `${baseUrl}${baseUrl.endsWith('/') ? '' : '/'}favicon.svg`;

        return configuredFavicon ?? prosunaFaviconUrl;
    }, [faviconUrl]);

    return (
        <>
            <meta charSet="utf-8" />
            <title>
                {(titlePrefix ?? 'Prosuna') + (title ? ` - ${title}` : '')}
            </title>
            <meta
                name="robots"
                content="noindex, nofollow"
            />
            <meta
                name="generator"
                content="Prosuna – Die quelloffene Plattform für Ende-zu-Ende digitalisierte Verwaltungsprozesse. (prosuna.de)"
            />

            <link
                rel="icon"
                href={favicon}
            />
        </>
    );
}
