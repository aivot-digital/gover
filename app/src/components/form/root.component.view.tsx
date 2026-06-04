import React, {useMemo, useRef, useState} from 'react';
import Box from '@mui/material/Box';
import Container from '@mui/material/Container';
import Stepper from '@mui/material/Stepper';
import {type FormLayoutElement} from '../../models/elements/form-layout-element';
import Chip from '@mui/material/Chip';
import Tooltip from '@mui/material/Tooltip';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {useAppSelector} from '../../hooks/use-app-selector';
import {nextStep, previousStep, selectCurrentStep, selectUpcomingStepDirection} from '../../slices/stepper-slice';
import {ElementType} from '../../data/element-type/element-type';
import {type FileUploadElementItem} from '../../models/elements/form/input/file-upload-element';
import {type BaseViewProps} from '../../views/base-view';
import GppGoodOutlinedIcon from '@mui/icons-material/GppGoodOutlined';
import {CustomStep} from '../custom-step/custom-step';
import {Api} from '../../hooks/use-api';
import {AuthoredElementValues, DerivedRuntimeElementData, hasAnyErrorRecursively} from '../../models/element-data';
import {ErrorAlert} from '../error-alert/error-alert';
import {walkAuthoredElementValues} from '../../utils/element-data-utils';
import {FormEntity} from '../../modules/forms/entities/form-entity';
import {FormVersionEntity} from '../../modules/forms/entities/form-version-entity';
import {FormApiService} from '../../modules/forms/services/form-api-service';
import {extractVisibleFormSteps, type VisibleFormStepElement} from '../../utils/visible-form-steps';
import {Stack, Typography, useTheme} from '@mui/material';
import {useRootStructureActionsContext} from './root-structure-actions-context';
import {UiDefinitionEmptyState} from '../ui-definition-empty-state/ui-definition-empty-state';
import {useViewDispatcherContext, ViewDispatcherMode} from '../view-dispatcher/view-dispatcher.context';
import {ViewDispatcherComponent} from '../view-dispatcher/view-dispatcher.component';

export const SUBMIT_EVENT = 'submit';
export const PRE_SUBMIT_EVENT = 'pre-submit';

function extractCurrentStep(currentStep: number, allVisibleSteps: VisibleFormStepElement[]) {
    if (currentStep < 0 || currentStep >= allVisibleSteps.length) {
        return null;
    }
    return allVisibleSteps[currentStep];
}

export function RootComponentView(props: BaseViewProps<FormLayoutElement, void>) {
    const {
        element,
        authoredElementValues,
        derivedData,
        isBusy,
        isDeriving,
        onDerive,
        onEvent,
        onResetErrors,
    } = props;

    const {
        mode,
        scrollContainerRef,
    } = useViewDispatcherContext();

    const theme = useTheme();
    const dispatch = useAppDispatch();
    const rootStructureActions = useRootStructureActionsContext();

    // TODO: internalize these information
    const form = element;
    const currentStep = useAppSelector(selectCurrentStep);
    const upcomingStepDirection = useAppSelector(selectUpcomingStepDirection);

    // Deconstruct the element to get the steps.
    const {
        children,
    } = element;

    // Collecting all steps including the fixed steps
    const allVisibleSteps = useMemo(() => extractVisibleFormSteps(children, derivedData), [children, derivedData]);

    // Extract the current step based on the current step index and all visible steps
    const currentStepElement = useMemo(() => extractCurrentStep(currentStep, allVisibleSteps), [currentStep, allVisibleSteps]);

    // Determine the total number of steps
    const totalStepCount = useMemo(() => allVisibleSteps.length, [allVisibleSteps]);

    // Create a ref for each step to allow scrolling to the step when it becomes active.
    const stepRefs = useRef(allVisibleSteps.map(() => React.createRef<HTMLDivElement>()));

    const [isBusyNavigating, setIsBusyNavigating] = useState(false);
    const [hasSteppedOnce, setHasSteppedOnce] = useState(false);

    const handleNextStep = async () => {
        // Check if the form is loaded. If not, this handler should not run.
        if (form == null) {
            return;
        }

        if (currentStep === (totalStepCount - 1) && mode === ViewDispatcherMode.Editor) {
            const shouldContinue = await onEvent(authoredElementValues, PRE_SUBMIT_EVENT);
            if (shouldContinue === false) {
                return;
            }
        }

        setIsBusyNavigating(true);

        // Check if the current step is valid
        const derivationData = await onDerive(
            authoredElementValues,
            [currentStepElement?.id ?? ''],
            (children ?? []).filter(step => step.id !== currentStepElement?.id).map(step => step.id),
        );

        const currentPageHasErrors = hasAnyErrorRecursively(derivationData.elementStates);

        if (currentPageHasErrors) {
            setIsBusyNavigating(false);
            setHasSteppedOnce(true);
            return;
        }

        // Check if submit step
        if (currentStep === (totalStepCount - 1)) {
            await onEvent(authoredElementValues, SUBMIT_EVENT);
        }

        // Handle default step
        else {
            dispatch(nextStep());
        }

        setIsBusyNavigating(false);
        setHasSteppedOnce(true);
    };

    const handlePreviousStep = () => {
        dispatch(previousStep());
        onResetErrors();
    };

    return (
        <main role="main">
                <span
                    aria-live="polite"
                    className="visually-hidden"
                >
                    {/* TODO: Waits for final QS by JP
                        isDeriving != null &&
                        isDeriving &&
                        'Berechnungen werden durchgeführt'
                    */}
                    {
                        isDeriving != null &&
                        !isDeriving &&
                        'Berechnungen fertig. Eingabebereit'
                    }
                </span>

            <Container
                sx={{
                    mt: 5,
                    mb: 5,
                    minHeight: '66vh',
                    /* Remove spacing for richtext components that are immediately preceded by a headline component */
                    '& .MuiGrid-item:has(.headline-component-content) + .MuiGrid-item.MuiGrid-grid-md-12:has(.richtext-component-content)': {
                        paddingTop: 0,
                    },
                    '& .MuiGrid-item:has(.headline-component-content) + .MuiGrid-item.MuiGrid-grid-md-12:has(.richtext-component-content) .richtext-component-content': {
                        marginTop: 0,
                    },
                }}
            >

                {
                    totalStepCount === 0 &&
                    (
                        mode === ViewDispatcherMode.Editor ?
                            <UiDefinitionEmptyState
                                target="section"
                                onAdd={() => {
                                    rootStructureActions?.openAddAtRootDialog();
                                }}
                                disabled={!(rootStructureActions?.canAddAtRoot ?? false)}
                            /> :
                            <Stack
                                justifyContent="center"
                                sx={{
                                    height: '40vh',
                                    justifyContent: 'center',
                                    alignItems: 'center',
                                }}
                            >
                                <Typography
                                    textAlign="center"
                                    variant="body1"
                                    component="p"
                                    sx={{
                                        maxWidth: '480px',
                                    }}
                                >
                                    Dieses Formular enthält derzeit keine Abschnitte oder Elemente.
                                </Typography>
                            </Stack>
                    )
                }

                {
                    totalStepCount > 0 &&
                    currentStep < totalStepCount &&
                    <Stepper
                        sx={{
                            mt: 8,
                            mb: 10,
                            ml: '20px',
                            [theme.breakpoints.down('md')]: {
                                mt: 5,
                                mb: 6,
                                ml: 0,
                            },
                        }}
                        activeStep={currentStep}
                        orientation="vertical"
                    >
                        {
                            allVisibleSteps
                                .map((step, index) => (
                                    <CustomStep
                                        key={index}
                                        step={step}
                                        stepIndex={index}
                                        isFirstStep={index === 0}
                                        isLastStep={index === allVisibleSteps.length - 1}
                                        isSubmitStep={step.type === ElementType.SubmitStep}
                                        onNext={handleNextStep}
                                        onPrevious={handlePreviousStep}
                                        active={currentStep === index}
                                        navDirection={upcomingStepDirection}
                                        stepRefs={stepRefs}
                                        scrollContainerRef={scrollContainerRef}
                                        isBusy={isBusy || isBusyNavigating}
                                        isDeriving={isDeriving}
                                    >
                                        <ViewDispatcherComponent
                                            {...props}
                                            element={step}
                                            isBusy={isBusy || isBusyNavigating}
                                            suppressErrors={!hasSteppedOnce}
                                        />

                                        {
                                            hasSteppedOnce &&
                                            <ErrorAlert
                                                element={step}
                                                authoredElementValues={authoredElementValues}
                                                derivedData={derivedData}
                                            />
                                        }
                                    </CustomStep>
                                ))
                        }
                    </Stepper>
                }
            </Container>

            {
                totalStepCount > 0 &&
                currentStep < totalStepCount &&
                <Container
                    sx={{
                        textAlign: 'left',
                        marginTop: 0,
                        mb: 8,
                        [theme.breakpoints.up('md')]: {
                            textAlign: 'right',
                            marginTop: '-80px',
                        },
                    }}
                >
                    <Tooltip
                        title="Ihre Angaben werden lokal auf diesem Gerät zwischengespeichert. Das Löschen Ihrer lokalen Daten oder Cookies kann einen Verlust Ihres Entwurfs zur Folge haben."
                        arrow
                    >
                        <Chip
                            sx={{
                                pl: 1,
                                pr: 1,
                                cursor: 'help',
                            }}
                            icon={<Box
                                component="span"
                                sx={{
                                    color: (theme) => theme.palette.primary.main,
                                    transform: 'translateY(2px)',
                                }}
                            ><GppGoodOutlinedIcon fontSize="small"/></Box>}
                            label="Lokal auf Ihrem Gerät zwischengespeichert"
                            variant="outlined"
                        />
                    </Tooltip>
                </Container>
            }
        </main>
    );
}

/**
 * Returns null if the file sizes are within the limits, returns an error message if the total file size exceeds the maximum allowed size.
 */
async function determineUploadSizeError(element: FormLayoutElement, elementData: AuthoredElementValues, form: FormEntity, version: FormVersionEntity, api: Api): Promise<string | null> {
    let totalFileSize = 0;

    walkAuthoredElementValues(
        element,
        elementData,
        (element, value) => {
            if (element.type === ElementType.FileUpload && value != null) {
                totalFileSize += (value as FileUploadElementItem[])
                    .reduce((acc, item) => acc + item.size, 0);
            }
        },
    );

    if (totalFileSize === 0) {
        // No file uploads, so no size check needed
        return null;
    }

    const {maxFileSize} = await new FormApiService()
        .getMaxFileSize(form.slug, version.version);

    const maxFileSizeBytes = maxFileSize * 1000 * 1000;

    if (totalFileSize > maxFileSizeBytes) {
        return `Die Gesamtgröße der von Ihnen hinzugefügten Anlagen überschreitet das Maximum von ${maxFileSize.toFixed(0)} Megabyte.`;
    }

    return null;
}
