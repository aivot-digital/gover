import {Helmet} from 'react-helmet';
import React from 'react';
import {useTheme} from '@mui/material';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectSystemConfigValue} from '../../slices/system-config-slice';
import {SystemConfigKeys} from '../../data/system-config-keys';
import {AppConfig} from "../../app-config";
import {AssetsApiService} from '../../modules/assets/assets-api-service';

interface MetaElementProps {
    title?: string;
    titlePrefix?: string;
}

export function MetaElement({title, titlePrefix}: MetaElementProps): JSX.Element {
    const theme = useTheme();

    const favicon = useAppSelector(selectSystemConfigValue(SystemConfigKeys.system.favicon));

    const colorVariables = Object.entries({
        'primary': theme.palette.primary.main,
        'primary-dark': theme.palette.primary.dark,
        'secondary': theme.palette.secondary.main,
    }).map(([key, value]) => `--hw-${key}: ${value}`).join(';');
    return (
        <Helmet
            style={[{
                cssText: `:root { ${colorVariables} }`,
            }]}
        >
            <meta
                name="robots"
                content="noindex, nofollow"
            />
            <meta
                name="version"
                content={AppConfig.version}
            />
            <meta
                name="date"
                content={AppConfig.date}
            />
            {favicon ? (
                <link rel="icon"
                      href={AssetsApiService.useAssetLink(favicon)}/>
            ) : [
                <link key="apple-touch-icon"
                      rel="apple-touch-icon"
                      sizes="180x180"
                      href={`${process.env.PUBLIC_URL}/apple-touch-icon.png`}/>,
                <link key="icon-32x32"
                      rel="icon"
                      type="image/png"
                      sizes="32x32"
                      href={`${process.env.PUBLIC_URL}/favicon-32x32.png`}/>,
                <link key="icon-16x16"
                      rel="icon"
                      type="image/png"
                      sizes="16x16"
                      href={`${process.env.PUBLIC_URL}/favicon-16x16.png`}/>
            ]}
            <meta charSet="utf-8"/>
            {
                title != null &&
                <title>{titlePrefix ? titlePrefix : 'Gover'} - {title}</title>
            }
        </Helmet>
    );
}
