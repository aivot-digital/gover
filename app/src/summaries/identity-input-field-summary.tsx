import {Grid, Typography, useTheme} from '@mui/material';
import {useEffect, useMemo, useState} from 'react';
import {useParams, useSearchParams} from 'react-router-dom';
import {BaseSummaryProps} from './base-summary';
import {
    IdentityInputFieldElement,
    IdentityInputFieldElementItem,
} from '../models/elements/form/input/identity-input-field-element';
import {FormTriggerApiService} from '../modules/forms/services/form-trigger-api-service';
import {FormTriggerIdentityDetailsDTO} from '../modules/forms/dtos/form-trigger-identity-details-dto';
import {TestClaimSearchParam} from '../modules/forms/constants/form-trigger-search-params';
import {extractIdentityInputMailValue} from '../utils/identity-input-field-utils';

export function IdentityInputFieldSummary(props: BaseSummaryProps<IdentityInputFieldElement, IdentityInputFieldElementItem>) {
    const {
        model,
        value,
    } = props;

    const theme = useTheme();
    const [providers, setProviders] = useState<FormTriggerIdentityDetailsDTO[]>([]);

    const mailValue = useMemo(() => extractIdentityInputMailValue(value), [value]);

    useEffect(() => {
        let cancelled = false;

        new FormTriggerApiService()
            .getIdentityProviders()
            .then((res) => {
                if (!cancelled) {
                    setProviders(res);
                }
            })
            .catch((error) => {
                console.error('Error loading identity providers for summary:', error);
                if (!cancelled) {
                    setProviders([]);
                }
            });

        return () => {
            cancelled = true;
        };
    }, []);

    const providerName = useMemo(() => {
        if (value?.identityProviderKey == null) {
            return undefined;
        }

        return providers.find((provider) => provider.key === value.identityProviderKey)?.name;
    }, [providers, value]);

    const content = mailValue ??
        (providerName != null
            ? `Authentifiziert ueber ${providerName}`
            : (value?.identityProviderKey != null ? 'Identität bestätigt' : 'Keine Angabe'));

    return (
        <Grid
            container
            sx={{
                borderBottom: '1px solid #D4D4D4',
                py: 1,
            }}
        >
            <Grid
                sx={{
                    textAlign: 'left',
                    pr: 5,
                    [theme.breakpoints.up('md')]: {
                        textAlign: 'right',
                    },
                }}
                size={{
                    xs: 12,
                    md: 4,
                }}
            >
                <Typography
                    variant="body2"
                    sx={{
                        fontWeight: 'bold',
                        [theme.breakpoints.up('md')]: {
                            fontWeight: 'normal',
                        },
                    }}
                >
                    {model.label}
                </Typography>
            </Grid>
            <Grid
                size={{
                    xs: 12,
                    md: 8,
                }}
            >
                <Typography variant="body2">
                    {content}
                </Typography>
            </Grid>
        </Grid>
    );
}
