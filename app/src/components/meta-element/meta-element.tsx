import React, {useMemo} from 'react';

interface MetaElementProps {
    faviconUrl?: string | null;
    title?: string;
    titlePrefix?: string;
}

export function MetaElement({faviconUrl, title, titlePrefix}: MetaElementProps) {
    const favicon = useMemo(() => {
        return faviconUrl === undefined ? AppConfig.faviconUrl : faviconUrl;
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
                href={favicon ?? 'data:,'}
            />
        </>
    );
}
