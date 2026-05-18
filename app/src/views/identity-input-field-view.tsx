import {Alert, Box, Button, Stack, Typography} from '@mui/material';
import {useEffect, useMemo, useState} from 'react';
import {useParams, useSearchParams} from 'react-router-dom';
import {BaseViewProps} from './base-view';
import {
    IdentityInputFieldElement,
    IdentityInputFieldElementItem,
    IdentityInputFieldOption,
} from '../models/elements/form/input/identity-input-field-element';
import {FormTriggerApiService} from '../modules/forms/services/form-trigger-api-service';
import {FormTriggerIdentityDetailsDTO} from '../modules/forms/dtos/form-trigger-identity-details-dto';
import {IdentityProviderIcon} from '../modules/identity/components/identity-provider-icon/identity-provider-icon';
import {IdentityProvidersApiService} from '../modules/identity/identity-providers-api-service';
import {TextFieldComponent} from '../components/text-field/text-field-component';
import {useViewDispatcherContext} from '../components/view-dispatcher/view-dispatcher.context';
import {flattenElementsWithParents} from '../utils/flatten-elements';
import {isSectionElementType} from '../models/elements/steps/step-element';
import {useAppSelector} from '../hooks/use-app-selector';
import {selectCurrentStep} from '../slices/stepper-slice';
import {validateEmail} from '../utils/validate-email';
import {hasDerivableAspects} from '../utils/has-derivable-aspects';
import {TestClaimSearchParam} from '../modules/forms/constants/form-trigger-search-params';
import {IdentityIdQueryParam} from '../modules/identity/constants/identity-id-query-param';
import {IdentityStateQueryParam} from '../modules/identity/constants/identity-state-query-param';
import {cleanAuthoredElementValues} from '../utils/element-data-utils';
import {
    createIdentityInputMailValue,
    extractIdentityInputMailValue,
    storePendingIdentityInputAuthContext,
} from '../utils/identity-input-field-utils';

export function IdentityInputFieldView(props: BaseViewProps<IdentityInputFieldElement, IdentityInputFieldElementItem>) {
    const {
        element,
        value,
        setValue,
        errors,
        isBusy: isGloballyDisabled,
        isDeriving,
    } = props;

    const {
        rootElement,
        rootAuthoredElementValues,
    } = useViewDispatcherContext();

    const currentStep = useAppSelector(selectCurrentStep);
    const [searchParams] = useSearchParams();
    const {processAccessKey, formSlug} = useParams<{
        processAccessKey: string;
        formSlug: string;
    }>();

    const [providers, setProviders] = useState<FormTriggerIdentityDetailsDTO[]>([]);
    const [providersError, setProvidersError] = useState<string>();
    const [isLoadingProviders, setIsLoadingProviders] = useState(false);

    const testClaimKey = useMemo(() => searchParams.get(TestClaimSearchParam), [searchParams]);
    const fieldError = useMemo(() => errors?.join(' '), [errors]);
    const currentMailValue = useMemo(() => extractIdentityInputMailValue(value), [value]);

    const providerMap = useMemo(() => {
        return new Map(providers.map((provider) => [provider.key, provider]));
    }, [providers]);

    const resolvedOptions = useMemo(() => {
        return (element.options ?? [])
            .filter((option): option is IdentityInputFieldOption => option.identityProviderKey != null)
            .map((option) => ({
                option,
                provider: providerMap.get(option.identityProviderKey!),
            }));
    }, [element.options, providerMap]);

    const sourceStepId = useMemo(() => {
        const sourceEntry = flattenElementsWithParents(rootElement, [], false)
            .find(({element: candidate}) => candidate.id === element.id);

        const sourceStep = [...(sourceEntry?.parents ?? [])]
            .reverse()
            .find((candidate) => isSectionElementType(candidate.type));

        return sourceStep?.id ?? null;
    }, [rootElement, element.id]);

    const isDisabled = useMemo(() => {
        return Boolean(element.disabled) || isGloballyDisabled;
    }, [element.disabled, isGloballyDisabled]);

    const isFieldBusy = useMemo(() => {
        return (isDeriving && hasDerivableAspects(element)) || isLoadingProviders;
    }, [element, isDeriving, isLoadingProviders]);

    useEffect(() => {
        if (processAccessKey == null || formSlug == null) {
            return;
        }

        let cancelled = false;
        setIsLoadingProviders(true);
        setProvidersError(undefined);

        new FormTriggerApiService()
            .getIdentityProviders(processAccessKey, formSlug, testClaimKey)
            .then((res) => {
                if (!cancelled) {
                    setProviders(res);
                }
            })
            .catch((error) => {
                console.error('Error loading public identity providers:', error);
                if (!cancelled) {
                    setProviders([]);
                    setProvidersError('Die Identifizierungsanbieter konnten nicht geladen werden.');
                }
            })
            .finally(() => {
                if (!cancelled) {
                    setIsLoadingProviders(false);
                }
            });

        return () => {
            cancelled = true;
        };
    }, [formSlug, processAccessKey, testClaimKey]);

    const handleStartIdentityAuth = (option: IdentityInputFieldOption) => {
        if (option.identityProviderKey == null || isDisabled || isFieldBusy) {
            return;
        }

        const returnUrl = new URL(window.location.href);
        returnUrl.searchParams.delete(IdentityIdQueryParam);
        returnUrl.searchParams.delete(IdentityStateQueryParam);

        storePendingIdentityInputAuthContext({
            elementId: element.id,
            stepId: sourceStepId,
            stepIndex: currentStep,
            optionIdentityProviderKey: option.identityProviderKey,
            returnUrl: returnUrl.toString(),
            authoredElementValues: cleanAuthoredElementValues(rootElement, rootAuthoredElementValues),
        });

        window.location.assign(
            IdentityProvidersApiService.createLink(
                option.identityProviderKey,
                option.additionalScopes ?? undefined,
                returnUrl.toString(),
            ),
        );
    };

    const handleMailChange = (nextMail: string | null | undefined) => {
        setValue(createIdentityInputMailValue(nextMail));
    };

    const activeProvider = value?.identityProviderKey != null ? providerMap.get(value.identityProviderKey) : undefined;
    const hasConfiguredIdentityOption = resolvedOptions.length > 0;
    const unavailableProviderCount = resolvedOptions.filter(({provider}) => provider == null).length;
    const mailError = currentMailValue != null && !validateEmail(currentMailValue) ? 'Bitte geben Sie eine gueltige E-Mail-Adresse ein.' : undefined;
    const showFieldLevelError = fieldError != null && (element.allowsMail !== true || currentMailValue == null);

    return (
        <Stack spacing={2}>
            {
                hasConfiguredIdentityOption &&
                <Stack spacing={1.5}>
                    {
                        resolvedOptions.map(({option, provider}, index) => {
                            if (provider == null) {
                                return null;
                            }

                            const isActiveProvider = value?.identityProviderKey === provider.key;

                            return (
                                <Button
                                    key={`${provider.key}-${index}`}
                                    variant={isActiveProvider ? 'contained' : 'outlined'}
                                    color={isActiveProvider ? 'success' : 'primary'}
                                    onClick={() => handleStartIdentityAuth(option)}
                                    disabled={isDisabled || isFieldBusy}
                                    sx={{
                                        justifyContent: 'flex-start',
                                        textTransform: 'none',
                                        py: 1.5,
                                        px: 2,
                                    }}
                                >
                                    <Box
                                        sx={{
                                            display: 'flex',
                                            alignItems: 'center',
                                            justifyContent: 'center',
                                            minWidth: 160,
                                            mr: 2,
                                        }}
                                    >
                                        <IdentityProviderIcon
                                            name={provider.name}
                                            type={provider.type}
                                            iconAssetKey={provider.iconAssetKey}
                                        />
                                    </Box>

                                    <Box
                                        sx={{
                                            textAlign: 'left',
                                        }}
                                    >
                                        <Typography
                                            component="span"
                                            sx={{
                                                display: 'block',
                                                fontWeight: 600,
                                            }}
                                        >
                                            {provider.name}
                                        </Typography>
                                        <Typography
                                            component="span"
                                            variant="body2"
                                            sx={{
                                                display: 'block',
                                                opacity: 0.9,
                                            }}
                                        >
                                            {isActiveProvider ? 'Authentifizierung erneut starten' : 'Mit diesem Nutzerkonto anmelden'}
                                        </Typography>
                                    </Box>
                                </Button>
                            );
                        })
                    }
                </Stack>
            }

            {
                unavailableProviderCount > 0 &&
                <Alert severity="warning">
                    Mindestens ein konfigurierter Identifizierungsanbieter ist derzeit nicht verfuegbar.
                </Alert>
            }

            {
                providersError != null &&
                <Alert severity="error">
                    {providersError}
                </Alert>
            }

            {
                activeProvider != null &&
                <Alert severity="success">
                    Authentifiziert ueber {activeProvider.name}.
                </Alert>
            }

            {
                element.allowsMail === true &&
                <TextFieldComponent
                    label={hasConfiguredIdentityOption ? 'E-Mail-Adresse alternativ angeben' : (element.label ?? 'E-Mail-Adresse')}
                    type="email"
                    value={currentMailValue}
                    onChange={handleMailChange}
                    placeholder="name@beispiel.de"
                    hint={activeProvider != null ? 'Eine Eingabe ersetzt den Identitaetsnachweis fuer dieses Feld.' : element.hint ?? undefined}
                    error={currentMailValue != null ? (mailError ?? fieldError) : undefined}
                    required={element.required ?? undefined}
                    disabled={isDisabled || isFieldBusy}
                />
            }

            {
                !hasConfiguredIdentityOption &&
                element.allowsMail !== true &&
                <Alert severity="info">
                    Fuer dieses Element ist derzeit keine Identifizierungsmethode verfuegbar.
                </Alert>
            }

            {
                showFieldLevelError &&
                <Typography
                    variant="body2"
                    color="error"
                >
                    {fieldError}
                </Typography>
            }
        </Stack>
    );
}
