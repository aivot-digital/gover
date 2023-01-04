import {Helmet} from 'react-helmet';
import React from 'react';
import ProjectPackage from '../../../package.json';
import gitInfo from '../../git-info.json';
import {useTheme} from '@mui/material';
import {SystemAssetsService} from '../../services/system-assets.service';

interface MetaElementProps {
    title?: string;
}

export function MetaElement(props: MetaElementProps) {
    const theme = useTheme();

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
                href={SystemAssetsService.getFaviconLink()}
            />
            <meta charSet="utf-8"/>
            {
                props.title &&
                <title>{props.title}</title>
            }
        </Helmet>
    );
}
