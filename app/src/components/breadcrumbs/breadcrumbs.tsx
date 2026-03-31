import {Link, SxProps} from '@mui/material';
import {Link as RouterLink} from 'react-router';
import React, {useMemo} from 'react';
import {Breadcrumbs as MuiBreadcrumbs} from '@mui/material';


interface BreadcrumbsProps {
    prefix?: string;
    path: string;
    rootLabel: string;
    sx?: SxProps;
}

export function Breadcrumbs(props: BreadcrumbsProps) {
    const {
        prefix = '',
        path,
        rootLabel,
        sx,
    } = props;

    const paths: {
        link: string;
        label: string;
    }[] = useMemo(() => {
        return path
            .split('/')
            .map((item, index, all) => {
                let path: string;
                let name: string;

                if (index === 0) {
                    path = prefix;
                    name = rootLabel;
                } else {
                    const comb = all
                        .slice(0, index + 1)
                        .join('/');
                    const params = new URLSearchParams();
                    params.set('path', comb);

                    path = `${prefix}?${params.toString()}`;
                    name = item;
                }

                return {
                    link: path,
                    label: name,
                };
            });
    }, [prefix, path]);

    return (
        <MuiBreadcrumbs
            sx={sx}
        >
            {
                paths
                    .map(({link, label}) => (
                        <Link
                            to={link}
                            key={label}
                            component={RouterLink}
                            underline="none"
                        >
                            {label}
                        </Link>
                    ))
            }
        </MuiBreadcrumbs>
    );
}