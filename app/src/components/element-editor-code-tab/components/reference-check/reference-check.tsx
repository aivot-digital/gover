import {Box, Paper, Typography} from '@mui/material';
import {AnyElement} from '../../../../models/elements/any-element';
import {ConditionSet} from '../../../../models/functions/conditions/condition-set';
import {isNoCodeExpression, isNoCodeReference, NoCodeExpression} from '../../../../models/functions/no-code-expression';
import {useAppSelector} from '../../../../hooks/use-app-selector';
import {selectAllElements} from '../../../../slices/app-slice';
import {useMemo} from 'react';
import {isStringNotNullOrEmpty} from '../../../../utils/string-utils';
import {generateComponentTitle} from '../../../../utils/generate-component-title';
import {getElementIcon} from '../../../../data/element-type/element-icons';
import {Hint} from '../../../hint/hint';

const JavascriptEngine = {
    JS_CONTEXT_OBJECT_NAME: 'ctx',
};

const BaseElementDerivationContext = {
    INPUT_VALUES_JS_CONTEXT_OBJECT_NAME: 'inputValues',
    COMPUTED_VALUES_JS_CONTEXT_OBJECT_NAME: 'computedValues',
    VALUES_JS_CONTEXT_OBJECT_NAME: 'values',
    VISIBILITIES_JS_CONTEXT_OBJECT_NAME: 'visibilities',
    ERRORS_JS_CONTEXT_OBJECT_NAME: 'errors',
    OVERRIDES_JS_CONTEXT_OBJECT_NAME: 'overrides',
};


interface ReferenceCheckProps {
    element: AnyElement;
    lowCodeOld: (string | undefined)[];
    lowCode: (string | undefined)[];
    noCodeOld: (ConditionSet | undefined)[];
    noCode: (NoCodeExpression | null | undefined)[];
}

export function ReferenceCheck(props: ReferenceCheckProps) {
    const {
        element,
        lowCodeOld,
        lowCode,
        noCodeOld,
        noCode,
    } = props;

    const allElements = useAppSelector(selectAllElements);

    const referencedElements = useMemo(() => determineReferencedElements(
        element,
        allElements,
        lowCodeOld,
        lowCode,
        noCodeOld,
        noCode,
    ), [element,
        allElements,
        lowCodeOld,
        lowCode,
        noCodeOld,
        noCode]);

    return (
        <Box
            sx={{
                mt: 4,
            }}
        >
            <Typography
                variant="h6"
                component="div"
            >
                Referenzierte Elemente
            </Typography>

            {
                referencedElements.length > 0 ? (
                    <Box
                        sx={{
                            display: 'flex',
                            flexWrap: 'wrap',
                            mt: 1,
                            gap: 1,
                        }}
                    >
                        {
                            referencedElements.map(({element: refElement, isForwardReference}) => {
                                const Icon = getElementIcon(refElement);
                                return (
                                    <Paper
                                        variant="outlined"
                                        sx={{
                                            borderColor: isForwardReference ? 'error.main' : 'success.main',
                                            display: 'flex',
                                            alignItems: 'center',
                                            px: 2,
                                            py: 0.5,
                                        }}
                                    >
                                        <Icon
                                            color={isForwardReference ? 'error' : 'success'}
                                        />

                                        <Box
                                            sx={{
                                                ml: 1,
                                            }}
                                        >
                                            <Typography variant="caption">
                                                {refElement.id}
                                            </Typography>
                                            <Typography>
                                                {generateComponentTitle(refElement)}
                                            </Typography>
                                        </Box>

                                        {
                                            isForwardReference && (
                                                <Hint
                                                    summary="Unerlaubte Vorwärtsreferenz erkannt"
                                                    detailsTitle="Unerlaubte Vorwärtsreferenz"
                                                    details={<>
                                                        <Typography
                                                            variant="body1"
                                                            component="div"
                                                        >
                                                            Es darf nur auf Elemente verwiesen werden, die im Formular vor dem aktuellen Element positioniert sind.
                                                            Grund dafür ist, dass spätere Elemente zum Zeitpunkt der Berechnung noch nicht bekannt sind. Verweise auf solche „zukünftigen“ Elemente – sogenannte Vorwärtsreferenzen – können zu unerwartetem Verhalten führen und die Nachvollziehbarkeit der Logik erschweren.
                                                        </Typography>
                                                        <Typography
                                                            variant="body1"
                                                            component="div"
                                                            sx={{mt: 2}}
                                                        >
                                                            Darüber hinaus kann eine Vorwärtsreferenz auch für die Nutzenden des Formulars verwirrend sein:
                                                            Wenn sich das referenzierte Element außerhalb des sichtbaren Bereichs befindet, ist oft unklar, wodurch eine Veränderung ausgelöst wurde oder woher bestimmte Werte stammen.
                                                        </Typography>
                                                        <Typography
                                                            variant="body1"
                                                            component="div"
                                                            sx={{mt: 2}}
                                                        >
                                                            Um solche Probleme zu vermeiden, sollten und dürfen Referenzen ausschließlich auf bereits vorhandene, vorher platzierte Elemente erfolgen.
                                                        </Typography>
                                                    </>}
                                                    sx={{ml: 1}}
                                                    isError={true}
                                                />
                                            )
                                        }
                                    </Paper>
                                );
                            })
                        }
                    </Box>
                ) : (
                    <Typography
                        variant="body2"
                        color="text.secondary"
                    >
                        Keine referenzierten Elemente gefunden.
                    </Typography>
                )
            }
        </Box>
    );
}

function determineReferencedElements(
    element: AnyElement,
    allElements: AnyElement[] | undefined,
    lowCodeOld: (string | undefined)[],
    lowCode: (string | undefined)[],
    noCodeOld: (ConditionSet | undefined)[],
    noCode: (NoCodeExpression | null | undefined)[],
): {
    element: AnyElement;
    isForwardReference: boolean;
}[] {
    if (!allElements || allElements.length === 0) {
        return [];
    }

    let referencedIds: string[] = [];

    if (lowCodeOld.some(c => isStringNotNullOrEmpty(c))) {
        referencedIds = getLowCodeOldReferencedIds(lowCodeOld.filter(isStringNotNullOrEmpty).join('\n'));
    }

    if (lowCode.some(c => isStringNotNullOrEmpty(c))) {
        referencedIds = getLowCodeReferencedIds(lowCode.join('\n'));
    }

    if (noCodeOld.some(c => c != null)) {
        for (const conditionSet of noCodeOld) {
            if (conditionSet != null) {
                const ids = getNoCodeOldReferencedIds(conditionSet);
                referencedIds.push(...ids);
            }
        }
    }

    if (noCode.some(c => c != null)) {
        for (const expression of noCode) {
            if (expression != null) {
                const ids = getNoCodeReferencedIds(expression);
                referencedIds.push(...ids);
            }
        }
    }

    const referencedElementIdsSet = new Set<string>();
    for (const id of referencedIds) {
        if (id && id.trim() !== '') {
            referencedElementIdsSet.add(id);
        }
    }

    const referencedElements: {
        element: AnyElement;
        isForwardReference: boolean;
    }[] = [];

    const indexOfElement = allElements.findIndex(e => e.id === element.id);

    for (const id of Array.from(referencedElementIdsSet)) {
        const elementFound = allElements.find(e => e.id === id);
        const indexOfReference = allElements.findIndex(e => e.id === id);

        if (elementFound) {
            const isForwardReference = indexOfReference > indexOfElement;
            referencedElements.push({element: elementFound, isForwardReference});
        }
    }

    // Logic to determine referenced elements based on the provided parameters
    // This is a placeholder function and should be implemented according to your requirements
    return referencedElements;
}

function getLowCodeOldReferencedIds(code: string): string[] {
    if (!code || code.trim() === '') {
        return [];
    }

    const referencedIds = new Set<string>();

    const explicitReferencePattern = />>>([a-zA-Z0-9_-]+)/g;
    const implicitReferencePattern = /data\.([a-zA-Z0-9_-]+)/g;

    let match: RegExpExecArray | null;

    // Match explicit references
    while ((match = explicitReferencePattern.exec(code)) !== null) {
        referencedIds.add(match[1]);
    }

    // Match implicit references
    while ((match = implicitReferencePattern.exec(code)) !== null) {
        referencedIds.add(match[1]);
    }

    return Array.from(referencedIds);
}

function getLowCodeReferencedIds(code: string): string[] {
    if (!code || code.trim() === '') {
        return [];
    }

    const expliciteReferencePattern = />>>([a-zA-Z0-9_-]+)/g;

    const implicitRegex = `${JavascriptEngine.JS_CONTEXT_OBJECT_NAME}\\.([a-zA-Z0-9_-]+)`;
    const implicitReferencePattern = new RegExp(implicitRegex, 'g');

    const ids = new Set<string>();

    let match: RegExpExecArray | null;

    // Match explicit references
    while ((match = expliciteReferencePattern.exec(code)) !== null) {
        ids.add(match[1]);
    }

    // Match implicit references
    while ((match = implicitReferencePattern.exec(code)) !== null) {
        ids.add(match[2]);
    }

    return Array.from(ids);
}

function getNoCodeOldReferencedIds(conditionSet: ConditionSet): string[] {
    if (!conditionSet) {
        return [];
    }

    const referencedIds = new Set<string>();

    if (conditionSet.conditions != null) {
        for (const condition of conditionSet.conditions) {
            if (condition.reference != null) {
                referencedIds.add(condition.reference);
            }
            if (condition.target != null) {
                referencedIds.add(condition.target);
            }
        }
    }

    if (conditionSet.conditionsSets != null) {
        for (const _conditionSet of conditionSet.conditionsSets) {
            const nestedReferencedIds = getNoCodeOldReferencedIds(_conditionSet);
            for (const id of nestedReferencedIds) {
                referencedIds.add(id);
            }
        }
    }

    return Array.from(referencedIds);
}

function getNoCodeReferencedIds(noCodeExpression: NoCodeExpression): string[] {
    if (!noCodeExpression) {
        return [];
    }

    const referencedIds = new Set<string>();

    if (noCodeExpression.operands != null) {
        for (const operand of noCodeExpression.operands) {
            if (isNoCodeReference(operand) && operand.elementId != null) {
                referencedIds.add(operand.elementId);
            } else if (isNoCodeExpression(operand)) {
                const nestedReferencedIds = getNoCodeReferencedIds(operand);
                for (const id of nestedReferencedIds) {
                    referencedIds.add(id);
                }
            }
        }
    }

    return Array.from(referencedIds);
}
