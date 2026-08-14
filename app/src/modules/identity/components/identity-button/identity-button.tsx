import {Box, Button, Typography, useTheme} from '@mui/material';
import {alpha} from '@mui/material/styles';
import {useMemo} from 'react';
import ArrowForward from '@aivot/mui-material-symbols-400-n25-outlined/ArrowForward';
import CheckCircle from '@aivot/mui-material-symbols-400-n25-outlined/CheckCircle';
import {Chip} from '../../../../components/chip/chip';
import {IdentityProviderType} from '../../enums/identity-provider-type';
import {IdentityProvidersApiService} from '../../identity-providers-api-service';
import {IdentityProviderIcon} from '../identity-provider-icon/identity-provider-icon';

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
        return IdentityProvidersApiService.createLink(
            identityProviderKey,
            identityId,
            relatedProcessNodeId,
            additionalScopes,
            window.location.href,
        );
    }, [additionalScopes, identityId, identityProviderKey, relatedProcessNodeId]);

    const content = (
        <>
            <IdentityProviderIcon
                name={identityProviderName}
                type={identityProviderType}
                iconAssetKey={identityProviderAssetKey}
            />

            <Box
                sx={{
                    minWidth: 0,
                    flex: 1,
                    textAlign: {
                        xs: 'center',
                        sm: 'left',
                    },
                }}
            >
                <Typography
                    variant="caption"
                    color="text.secondary"
                    component="div"
                >
                    Nutzerkonto
                </Typography>
                <Typography
                    color="text.primary"
                    sx={{fontWeight: 600}}
                >
                    {
                        isAuthenticated
                            ? <>Mit „{identityProviderName}“ angemeldet</>
                            : <>Mit „{identityProviderName}“ anmelden</>
                    }
                </Typography>
            </Box>
        </>
    );

    if (isAuthenticated) {
        return (
            <Box
                role="status"
                sx={{
                    width: '100%',
                    minHeight: 88,
                    mt: 2,
                    p: 1.5,
                    display: 'flex',
                    flexDirection: {
                        xs: 'column',
                        sm: 'row',
                    },
                    alignItems: 'center',
                    gap: 2,
                    border: '1px solid',
                    borderColor: alpha(theme.palette.success.main, theme.palette.mode === 'dark' ? 0.55 : 0.4),
                    borderRadius: 1,
                    backgroundColor: alpha(theme.palette.success.main, theme.palette.mode === 'dark' ? 0.08 : 0.04),
                }}
            >
                {content}
                <Chip
                    mode="soft"
                    color="success"
                    size="small"
                    icon={<CheckCircle/>}
                    label="Angemeldet"
                    sx={{flexShrink: 0}}
                />
            </Box>
        );
    }

    return (
        <Button
            variant="outlined"
            color="primary"
            fullWidth
            component="a"
            href={startUri}
            sx={{
                minHeight: 88,
                mt: 2,
                p: 1.5,
                display: 'flex',
                flexDirection: {
                    xs: 'column',
                    sm: 'row',
                },
                alignItems: 'center',
                justifyContent: 'flex-start',
                gap: 2,
                textTransform: 'none',
            }}
        >
            {content}
            <ArrowForward sx={{flexShrink: 0}}/>
        </Button>
    );
}
