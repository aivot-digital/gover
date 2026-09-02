import BayernIdLogo from './system-icons/bayern-id-logo.svg?react';
import BundIdLogo from './system-icons/bund-id-logo.svg?react';
import MukLogo from './system-icons/muk-logo.svg?react';
import ShIdLogo from './system-icons/sh-id-logo.svg?react';
import {IdentityProviderType} from '../../enums/identity-provider-type';
import {AssetsApiService} from '../../../assets/assets-api-service';
import {Box} from '@mui/material';
import {useEffect, useState} from 'react';
import AccountCircle from '@aivot/mui-material-symbols-400-n25-outlined/AccountCircle';

interface IdentityProviderIconProps {
    name: string;
    type: IdentityProviderType;
    iconAssetKey?: string | null;
}

export function IdentityProviderIcon(props: IdentityProviderIconProps) {
    const {
        name,
        type,
        iconAssetKey,
    } = props;
    const [customIconFailed, setCustomIconFailed] = useState(false);
    const customIconUrl = iconAssetKey == null || iconAssetKey.trim().length === 0
        ? null
        : AssetsApiService.useAssetLink(iconAssetKey);

    useEffect(() => {
        setCustomIconFailed(false);
    }, [customIconUrl]);

    const renderIcon = () => {
        switch (type) {
            case IdentityProviderType.BundID:
                return <BundIdLogo aria-hidden/>;
            case IdentityProviderType.BayernID:
                return <BayernIdLogo aria-hidden/>;
            case IdentityProviderType.MUK:
                return <MukLogo aria-hidden/>;
            case IdentityProviderType.SHID:
                return <ShIdLogo aria-hidden/>;
            default:
                return customIconUrl != null && !customIconFailed
                    ? <img
                        src={customIconUrl}
                        alt=""
                        onError={() => setCustomIconFailed(true)}
                    />
                    : <AccountCircle aria-hidden/>;
        }
    };

    // Provider logos are external brand assets with unknown transparency and contrast. A stable light canvas keeps
    // the single configured asset legible in both application color modes.
    return (
        <Box
            role="img"
            aria-label={`Logo ${name.trim().length > 0 ? name : 'Nutzerkonto'}`}
            data-testid="identity-provider-logo"
            sx={{
                width: '100%',
                maxWidth: '12rem',
                height: '3.5rem',
                px: 2,
                py: 1,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                overflow: 'hidden',
                color: 'grey.800',
                backgroundColor: 'common.white',
                border: '1px solid rgba(0, 0, 0, 0.16)',
                borderRadius: 1,
                boxShadow: '0 1px 2px rgba(0, 0, 0, 0.08)',
                '& > svg, & > img': {
                    display: 'block',
                    width: 'auto',
                    height: 'auto',
                    maxWidth: '100%',
                    maxHeight: '100%',
                    objectFit: 'contain',
                    filter: 'drop-shadow(0 0 1px rgba(0, 0, 0, 0.45))',
                },
            }}
        >
            {renderIcon()}
        </Box>
    );
}
