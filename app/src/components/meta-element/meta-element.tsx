import React, {useMemo} from 'react';

interface MetaElementProps {
    faviconUrl?: string;
    title?: string;
    titlePrefix?: string;
}

export function MetaElement({faviconUrl, title, titlePrefix}: MetaElementProps) {
    const favicon = useMemo(() => {
        return faviconUrl ?? AppConfig.faviconUrl;
    }, [faviconUrl])

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
                key="apple-touch-icon"
                rel="apple-touch-icon"
                sizes="180x180"
                href={`${import.meta.env.BASE_URL}/apple-touch-icon.png`}
            />

            <link
                key="icon-svg"
                rel="icon"
                type="image/svg+xml"
                href={`${import.meta.env.BASE_URL}/favicon.svg`}
            />

            <link
                rel="icon"
                href={favicon}
            />
        </>
    );
}
