import React, {useEffect} from 'react';
import {useTheme} from '@mui/material';

interface MetaElementProps {
    title?: string;
    titlePrefix?: string;
}

export function MetaElement({title, titlePrefix}: MetaElementProps) {
    const theme = useTheme();

    useEffect(() => {
        const root = document.documentElement;
        root.style.setProperty('--gover-theme-primary', theme.palette.primary.main);
        root.style.setProperty('--gover-theme-primary-dark', theme.palette.primary.dark);
        root.style.setProperty('--gover-theme-secondary', theme.palette.secondary.main);
    }, [theme]);

    return (
        <>
            <meta charSet="utf-8" />
            <title>
                {(titlePrefix ?? 'Gover') + (title ? ` - ${title}` : '')}
            </title>
            <meta
                name="robots"
                content="noindex, nofollow"
            />
            <meta
                name="generator"
                content="Gover – Die quelloffene Plattform für Ende-zu-Ende digitalisierte Antragsprozesse. (gover.digital)"
            />

            <link
                key="apple-touch-icon"
                rel="apple-touch-icon"
                sizes="180x180"
                href={`${import.meta.env.BASE_URL}/apple-touch-icon.png`}
            />

            <link
                key="icon-32x32"
                rel="icon"
                type="image/png"
                sizes="32x32"
                href={`${import.meta.env.BASE_URL}/favicon-32x32.png`}
            />

            <link
                key="icon-16x16"
                rel="icon"
                type="image/png"
                sizes="16x16"
                href={`${import.meta.env.BASE_URL}/favicon-16x16.png`}
            />

            <link
                rel="icon"
                href="/api/public/system/favicon/"
            />
        </>
    );
}
