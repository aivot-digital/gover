import {ReactComponent as BayernIdLogo} from './system-icons/bayern-id-logo.svg';
import {ReactComponent as BundIdLogo} from './system-icons/bund-id-logo.svg';
import {ReactComponent as MukLogo} from './system-icons/muk-logo.svg';
import {ReactComponent as ShIdLogo} from './system-icons/sh-id-logo.svg';
import {IdentityProviderType} from '../../enums/identity-provider-type';
import {AssetsApiService} from '../../../assets/assets-api-service';
import {useMemo} from 'react';

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

    return useMemo(() => {
        switch (type) {
            case IdentityProviderType.BundID:
                return <BundIdLogo
                    title="BundID-Logo"
                    height="2em"
                />;
            case IdentityProviderType.BayernID:
                return <BayernIdLogo
                    title="BayernID-Logo"
                    height="2em"
                />;
            case IdentityProviderType.MUK:
                return <MukLogo
                    title="MUK-Logo"
                    height="2em"
                />;
            case IdentityProviderType.SHID:
                return <ShIdLogo
                    title="Servicekonto Schleswig-Holstein Logo"
                    height="2em"
                />;
            default:
                return <img
                    src={AssetsApiService.useAssetLink(iconAssetKey ?? '')}
                    alt={name}
                    style={{
                        height: '2em',
                    }}
                />;
        }
    }, [type, name, iconAssetKey]);
}

