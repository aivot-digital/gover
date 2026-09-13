import ScienceOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Science';
import {Box, Button, Skeleton, Typography} from '@mui/material';
import {useEffect, useState} from 'react';
import {AlertComponent} from '../../../components/alert/alert-component';
import {GenericDetailsSkeleton} from '../../../components/generic-details-page/generic-details-skeleton';
import {useGenericDetailsPageContext} from '../../../components/generic-details-page/generic-details-page-context';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {useChangeBlocker} from '../../../hooks/use-change-blocker-2';
import {
    type AuthoredElementValues,
    type ComputedElementErrors,
    hasAnyErrorRecursivelyInParent,
} from '../../../models/element-data';
import {
    showApiErrorSnackbar,
    showErrorSnackbar,
    showSuccessSnackbar,
} from '../../../slices/snackbar-slice';
import {ElementDerivationContext} from '../../elements/components/element-derivation-context';
import {ElementsApiService} from '../../elements/elements-api-service';
import {CommunicationProvidersApiService} from '../communication-providers-api-service';
import {type CommunicationProvider, type CommunicationTestingLayout} from '../models';
import {type CommunicationProviderAdditionalData} from './communication-provider-details-page-additional-data';

const EMPTY_TEST_INPUTS: AuthoredElementValues = {};

type TestingLayoutState =
    | {status: 'loading'}
    | {status: 'ready'; layout: CommunicationTestingLayout | null}
    | {status: 'error'};

export function CommunicationProviderDetailsPageTest() {
    const dispatch = useAppDispatch();
    const {
        item: provider,
        isNewItem,
    } = useGenericDetailsPageContext<CommunicationProvider, CommunicationProviderAdditionalData>();
    const [layoutState, setLayoutState] = useState<TestingLayoutState>({status: 'loading'});
    const [inputs, setInputs] = useState<AuthoredElementValues>(EMPTY_TEST_INPUTS);
    const [computedErrors, setComputedErrors] = useState<ComputedElementErrors | null>(null);
    const [isTesting, setIsTesting] = useState(false);
    const [loadAttempt, setLoadAttempt] = useState(0);

    useEffect(() => {
        let isActive = true;

        setInputs(EMPTY_TEST_INPUTS);
        setComputedErrors(null);
        setLayoutState({status: 'loading'});

        if (provider == null || provider.id === 0) {
            return () => {
                isActive = false;
            };
        }

        new CommunicationProvidersApiService()
            .getProviderTestingLayout(provider.id)
            .then((layout) => {
                if (isActive) {
                    setLayoutState({status: 'ready', layout});
                }
            })
            .catch((error) => {
                if (!isActive) {
                    return;
                }

                setLayoutState({status: 'error'});
                dispatch(showApiErrorSnackbar(error, 'Testoberfläche konnte nicht geladen werden.'));
            });

        return () => {
            isActive = false;
        };
    }, [dispatch, loadAttempt, provider?.id]);

    const changeBlocker = useChangeBlocker({
        original: EMPTY_TEST_INPUTS,
        edited: inputs,
        customTitle: 'Testeingaben verwerfen?',
        customMessage: 'Ihre eingegebenen Testdaten werden nicht gespeichert. Möchten Sie die Seite wirklich verlassen? Wenn Sie zurückkehren, müssen Sie die Testdaten erneut eingeben.',
        customConfirmButtonText: 'Testeingaben verwerfen',
        useDeepEquals: true,
        isActive: layoutState.status === 'ready',
    });

    if (provider == null) {
        return <GenericDetailsSkeleton/>;
    }

    if (isNewItem === true) {
        return (
            <AlertComponent
                color="info"
                text="Dieser Tab ist erst nach dem Anlegen des Kommunikationsanbieters verfügbar."
            />
        );
    }

    const handleTest = async () => {
        if (layoutState.status !== 'ready' || isTesting) {
            return;
        }

        setIsTesting(true);
        try {
            if (layoutState.layout != null) {
                const derivedData = await new ElementsApiService().derive({
                    element: layoutState.layout,
                    authoredElementValues: inputs,
                    derivationOptions: {
                        skipErrorsForElementIds: [],
                        skipVisibilitiesForElementIds: [],
                        skipOverridesForElementIds: [],
                        skipValuesForElementIds: [],
                    },
                    processExecutionData: {
                        $: {},
                        $$: {},
                        _: {},
                    },
                });

                if (hasAnyErrorRecursivelyInParent(layoutState.layout, derivedData.elementStates)) {
                    setComputedErrors(derivedData.elementStates);
                    dispatch(showErrorSnackbar('Bitte überprüfen Sie Ihre Eingaben.'));
                    return;
                }
            }

            setComputedErrors(null);
            await new CommunicationProvidersApiService().testProvider(provider.id, inputs);
            dispatch(showSuccessSnackbar('Kommunikationsanbieter wurde erfolgreich getestet.'));
        } catch (error) {
            dispatch(showApiErrorSnackbar(error, 'Kommunikationsanbieter konnte nicht getestet werden.'));
        } finally {
            setIsTesting(false);
        }
    };

    return (
        <Box>
            <Typography variant="h5" sx={{mt: 1.5, mb: 1}}>
                Test des Kommunikationsanbieters
            </Typography>

            <Typography sx={{mb: 3, maxWidth: 900}}>
                Führen Sie hier einen Test mit der gespeicherten Konfiguration des Kommunikationsanbieters durch.
                Die Testeingaben werden nicht gespeichert.
            </Typography>

            {layoutState.status === 'loading' && (
                <Skeleton variant="rounded" height={120}/>
            )}

            {layoutState.status === 'error' && (
                <AlertComponent color="error" title="Testoberfläche konnte nicht geladen werden.">
                    <Typography sx={{mb: 1.5}}>
                        Laden Sie die Testoberfläche erneut, bevor Sie den Test durchführen.
                    </Typography>
                    <Button
                        variant="outlined"
                        size="small"
                        onClick={() => setLoadAttempt(current => current + 1)}
                    >
                        Erneut laden
                    </Button>
                </AlertComponent>
            )}

            {layoutState.status === 'ready' && (
                <>
                    {layoutState.layout == null
                        ? (
                            <AlertComponent
                                color="info"
                                text="Für diesen Kommunikationsanbieter sind keine zusätzlichen Testeingaben erforderlich."
                            />
                        )
                        : (
                            <ElementDerivationContext
                                element={layoutState.layout}
                                authoredElementValues={inputs}
                                onAuthoredElementValuesChange={setInputs}
                                computedErrors={computedErrors}
                                disabled={isTesting}
                            />
                        )}

                    <Box sx={{display: 'flex', mt: 2, mb: 2}}>
                        <Button
                            variant="contained"
                            startIcon={<ScienceOutlinedIcon/>}
                            onClick={() => void handleTest()}
                            disabled={isTesting}
                        >
                            Kommunikationsanbieter testen
                        </Button>
                    </Box>
                </>
            )}

            {changeBlocker.dialog}
        </Box>
    );
}
