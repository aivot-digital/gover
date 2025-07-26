import React, {useEffect } from 'react';
import {useTheme} from '@mui/material';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectSystemConfigValue} from '../../slices/system-config-slice';
import {SystemConfigKeys} from '../../data/system-config-keys';
import {AssetsApiService} from '../../modules/assets/assets-api-service';

interface MetaElementProps {
    title?: string;
    titlePrefix?: string;
}

export function MetaElement({title, titlePrefix}: MetaElementProps) {
    const theme = useTheme();

    const favicon = useAppSelector(selectSystemConfigValue(SystemConfigKeys.system.favicon));

    useEffect(() => {
        const root = document.documentElement;
        root.style.setProperty('--gover-theme-primary', theme.palette.primary.main);
        root.style.setProperty('--gover-theme-primary-dark', theme.palette.primary.dark);
        root.style.setProperty('--gover-theme-secondary', theme.palette.secondary.main);
    }, [theme]);

    return (
        <>
            <meta charSet="utf-8"/>
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
            {favicon ? (
                <link rel="icon"
                      href={AssetsApiService.useAssetLink(favicon)}/>
            ) : [
                <link key="apple-touch-icon"
                      rel="apple-touch-icon"
                      sizes="180x180"
                      href={`${import.meta.env.BASE_URL}/apple-touch-icon.png`}/>,
                <link key="icon-32x32"
                      rel="icon"
                      type="image/png"
                      sizes="32x32"
                      href={`${import.meta.env.BASE_URL}/favicon-32x32.png`}/>,
                <link key="icon-16x16"
                      rel="icon"
                      type="image/png"
                      sizes="16x16"
                      href={`${import.meta.env.BASE_URL}/favicon-16x16.png`}/>
            ]}
        </>
    );
}
