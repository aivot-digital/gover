import {Box, Button, Typography, useTheme} from '@mui/material';
import React, {useMemo} from 'react';
import {IdentityProviderIcon} from '../identity-provider-icon/identity-provider-icon';
import {IdentityProviderType} from '../../enums/identity-provider-type';
import {IdentityProvidersApiService} from '../../identity-providers-api-service';

export interface IdentityButtonProps {
    isAuthenticated: boolean;
    relatedProcessNodeId: number;
    identityId: string;
    identityProviderKey: string;
    identityProviderAssetKey: string | null;
    additionalScopes: string[];
    identityProviderName: string;
    identityProviderType: IdentityProviderType;
}

export function IdentityButton(props: IdentityButtonProps) {
    const theme = useTheme();

    const {
        isAuthenticated,
        relatedProcessNodeId,
        identityId,
        identityProviderKey,
        identityProviderAssetKey,
        identityProviderName,
        identityProviderType,
        additionalScopes,
    } = props;

    const startUri = useMemo(() => {
        const searchParams = new URLSearchParams(window.location.search);
        for (const key of (additionalScopes ?? [])) {
            searchParams.set('additionalScopes', key);
        }
        searchParams.set('origin', window.location.href);

        return IdentityProvidersApiService
            .createLink(identityProviderKey, identityId, relatedProcessNodeId, additionalScopes, window.location.href);
    }, [identityProviderKey, additionalScopes]);

    const successColorWithOpacity = useMemo(() => {
        const successColor = theme.palette.success.main; // Greift auf die Haupt-"success"-Farbe zu
        return `rgba(${parseInt(successColor.slice(1, 3), 16)}, ${parseInt(successColor.slice(3, 5), 16)}, ${parseInt(successColor.slice(5, 7), 16)}, 0.04)`;
    }, [theme]);

    return (
        <Button
            variant="outlined"
            color={
                isAuthenticated
                    ? 'success'
                    : 'primary'
            }
            fullWidth
            component={isAuthenticated ? 'div' : 'a'}
            href={startUri}
            sx={{
                textTransform: 'none',
                p: 1.5,
                mt: 2,
                backgroundColor: isAuthenticated ? successColorWithOpacity : 'inherit',
                justifyContent: 'start',
                flexDirection: {
                    xs: 'column',
                    md: 'row',
                },
            }}
            disabled={isAuthenticated}
        >
            <Box
                sx={{
                    opacity: isAuthenticated ? 0.6 : 1,
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
                    name={identityProviderName}
                    type={identityProviderType}
                    iconAssetKey={identityProviderAssetKey}
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
                {isAuthenticated ? (
                    <>Sie haben sich erfolgreich mit dem Nutzerkonto <b>„{identityProviderName}“</b> angemeldet.</>
                ) : (
                    <>Mit Nutzerkonto <b>„{identityProviderName}“</b> anmelden</>
                )}
            </Typography>
        </Button>
    );
}
