import {Box, Button, Typography, useTheme} from '@mui/material';
import React, {useMemo} from 'react';
import {IdentityProviderLink} from '../../models/identity-provider-link';
import {IdentityProviderInfo} from '../../models/identity-provider-info';
import {IdentityProvidersApiService} from '../../identity-providers-api-service';
import {IdentityProviderIcon} from '../identity-provider-icon/identity-provider-icon';
import {IdentityValue} from '../../models/identity-value';
import {LegacySystemIdpKey} from '../../../../data/legacy-system-idp-key';
import {IdentityProviderType} from '../../enums/identity-provider-type';

export interface IdentityButtonProps {
    identityProviderLink: IdentityProviderLink;
    identityProviderInfo: IdentityProviderInfo;
    isBusy: boolean;
    value?: IdentityValue | undefined | null;
}

export function IdentityButton(props: IdentityButtonProps) {
    const theme = useTheme();

    const {
        identityProviderLink,
        identityProviderInfo,
        isBusy,
        value,
    } = props;

    const startUri = useMemo(() => {
        return IdentityProvidersApiService
            .createLink(identityProviderLink.identityProviderKey);
    }, [identityProviderLink]);

    const isSuccessful = useMemo(() => {
        if (value == null) {
            return false;
        }
        return value.identityProviderKey === identityProviderInfo.key;
    }, [identityProviderInfo, value]);

    const successColorWithOpacity = useMemo(() => {
        const successColor = theme.palette.success.main; // Greift auf die Haupt-"success"-Farbe zu
        return `rgba(${parseInt(successColor.slice(1, 3), 16)}, ${parseInt(successColor.slice(3, 5), 16)}, ${parseInt(successColor.slice(5, 7), 16)}, 0.04)`;
    }, [theme]);

    return (
        <Button
            variant="outlined"
            color={
                isSuccessful
                    ? 'success'
                    : 'primary'
            }
            fullWidth
            component={isSuccessful ? 'div' : 'a'}
            href={startUri}
            sx={{
                textTransform: 'none',
                p: 1.5,
                mt: 2,
                backgroundColor: isSuccessful ? successColorWithOpacity : 'inherit',
                justifyContent: 'start',
                flexDirection: {
                    xs: 'column',
                    md: 'row',
                },
            }}
            disabled={(!isSuccessful && value != null) || isBusy}
        >
            <Box
                sx={{
                    opacity: ((!isSuccessful && value != null) || isBusy) ? 0.6 : 1,
                    width: {md: 210},
                    flexShrink: {md: 0},
                    pr: {md: 4},
                    mr: {md: 4},
                    textAlign: {md: 'center'},
                    borderRight: {md: '1px solid #bdbdbd'},
                    display: 'flex',
                    justifyContent: 'center',
                }}
            >
                <IdentityProviderIcon
                    name={identityProviderInfo.name}
                    type={identityProviderInfo.type}
                    iconAssetKey={identityProviderInfo.iconAssetKey}
                />
            </Box>
            <Typography
                color="inherit"
                sx={{
                    mt: {xs: 1, md: 0},
                    maxWidth: {xs: 420, md: '100%'},
                    textAlign: {xs: 'center', md: 'left'},
                }}
            >
                {isSuccessful ? 'Angemeldet' : `Mit ${identityProviderInfo.name} anmelden`}
            </Typography>
        </Button>
    );
}
