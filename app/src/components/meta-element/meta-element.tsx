import {Helmet} from 'react-helmet';
import React from 'react';
import ProjectPackage from '../../../package.json';
import gitInfo from '../../git-info.json';
import {useTheme} from '@mui/material';
import {useAppSelector} from "../../hooks/use-app-selector";
import {selectSystemConfigValue} from "../../slices/system-config-slice";
import {SystemConfigKeys} from "../../data/system-config-keys";
import {AssetService} from "../../services/asset-service";

interface MetaElementProps {
    title?: string;
}

export function MetaElement({title}: MetaElementProps) {
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
                cssText: `:root { ${colorVariables} }`
            }]}
        >
            <meta
                name="robots"
                content="noindex, nofollow"
            />
            <meta
                name="version"
                content={ProjectPackage.version}
            />
            <meta
                name="branch"
                content={gitInfo.gitBranch}
            />
            <meta
                name="commit"
                content={gitInfo.gitCommitHash}
            />
            <link
                rel="icon"
                href={AssetService.getLink(favicon)}
            />
            <meta charSet="utf-8"/>
            {
                title &&
                <title>Gover - {title}</title>
            }
        </Helmet>
    );
}
