import {AlertComponent} from '../alert/alert-component';
import {useMemo} from 'react';
import {AnyElement} from '../../models/elements/any-element';
import {Link, Typography} from '@mui/material';
import {SummaryAttachmentsTooLargeKey} from '../summary/summary.component.view';
import {ElementType} from '../../data/element-type/element-type';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import {AuthoredElementValues, DerivedRuntimeElementData} from '../../models/element-data';
import {isAnyElementWithChildren} from '../../models/elements/any-element-with-children';
import {generateComponentTitle} from '../../utils/generate-component-title';
import {IdentityCustomerInputKey} from '../../modules/identity/constants/identity-customer-input-key';
import {resolveElementState, resolveReplicatingContainerItemDerivedData} from '../../utils/element-data-utils';

const noLinkKeys = [SummaryAttachmentsTooLargeKey];

const additionErrorKeys: Record<string, string> = {
    [IdentityCustomerInputKey]: 'Nutzerkonto',
    [SummaryAttachmentsTooLargeKey]: 'Hinzugefügte Anlagen',
};

interface ErrorAlertProps {
    element: AnyElement;
    authoredElementValues: AuthoredElementValues;
    derivedData: DerivedRuntimeElementData;
}

interface CollectedError {
    id: string;
    label: string;
    error: string;
}

export function collectErrors(
    element: AnyElement,
    authoredElementValues: AuthoredElementValues,
    derivedData: DerivedRuntimeElementData,
): CollectedError[] {
    const errs = _collectErrors(element, authoredElementValues, derivedData);

    for (const key of Object.keys(additionErrorKeys)) {
        const err = derivedData.elementStates[key]?.error;
        if (err != null && err.length > 0) {
            errs.push({
                id: key,
                label: additionErrorKeys[key],
                error: err,
            });
        }
    }

    return errs;
}

export function _collectErrors(
    element: AnyElement,
    authoredElementValues: AuthoredElementValues,
    derivedData: DerivedRuntimeElementData,
): CollectedError[] {
    const elementState = resolveElementState(element, derivedData);

    if (elementState == null) {
        return [];
    }

    const col: CollectedError[] = [];

    if (elementState.error != null) {
        col.push({
            id: element.id,
            label: generateComponentTitle(element),
            error: elementState.error,
        });
    }

    if (isAnyElementWithChildren(element) && element.children != null) {
        if (element.type === ElementType.ReplicatingContainer) {
            const childElementValues = authoredElementValues[element.id];

            if (Array.isArray(childElementValues)) {
                for (let index = 0; index < childElementValues.length; index++) {
                    const currentChildElementValues = childElementValues[index];
                    if (currentChildElementValues == null || typeof currentChildElementValues !== 'object') {
                        continue;
                    }

                    const childDerivedData = resolveReplicatingContainerItemDerivedData(element, derivedData, index);
                    for (const child of element.children) {
                        const childErrors = collectErrors(child, currentChildElementValues as AuthoredElementValues, childDerivedData);
                        if (childErrors.length > 0) {
                            col.push(...childErrors);
                        }
                    }
                }
            }
        } else if (isAnyElementWithChildren(element)) {
            for (const child of element.children) {
                const childErrors = collectErrors(child, authoredElementValues, derivedData);
                if (childErrors.length > 0) {
                    col.push(...childErrors);
                }
            }
        }
    }

    return col;
}

export function ErrorAlert(props: ErrorAlertProps) {
    const {
        element,
        authoredElementValues,
        derivedData,
    } = props;

    const errors: CollectedError[] = useMemo(() => {
        return collectErrors(element, authoredElementValues, derivedData);
    }, [element, authoredElementValues, derivedData]);

    if (errors.length === 0) {
        return null;
    }

    return (
        <AlertComponent
            title="Dieser Abschnitt enthält fehlerhafte oder fehlende Angaben"
            color="error"
        >
            <Typography>
                Bitte korrigieren Sie Ihre Angaben und fahren Sie fort, damit diese erneut überprüft werden.
            </Typography>

            <ul
                style={{
                    paddingInlineStart: '20px',
                }}
            >
                {
                    errors
                        .map(({id, label, error}) => {
                            const isLinked = !noLinkKeys.includes(id);

                            return (
                                <li key={id}>
                                    {
                                        isLinked ? (
                                            <Link
                                                color="inherit"
                                                onClick={() => {
                                                    const target = document.getElementById(id);
                                                    if (target) {
                                                        target.scrollIntoView({behavior: 'smooth', block: 'start'});
                                                        target.focus?.();
                                                    }
                                                }}
                                                title="Klicken Sie hier, um zu dieser Angabe zu springen"
                                                sx={{cursor: 'pointer', fontWeight: '600'}}
                                            >
                                                {label}
                                                <EditOutlinedIcon
                                                    fontSize="small"
                                                    sx={{transform: 'translateY(4px)'}}
                                                />
                                            </Link>
                                        ) : (
                                            <Typography
                                                component="span"
                                                sx={{fontWeight: '600'}}
                                            >
                                                {label}
                                            </Typography>
                                        )
                                    }
                                    : {error}
                                </li>
                            );
                        })
                }
            </ul>
        </AlertComponent>
    );
}
