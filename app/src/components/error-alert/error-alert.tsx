import {AlertComponent} from '../alert/alert-component';
import {useMemo} from 'react';
import {AnyElement} from '../../models/elements/any-element';
import {Link, Typography} from '@mui/material';
import {PrivacyUserInputKey} from '../general-information/general-information.component.view';
import {SummaryAttachmentsTooLargeKey, SummaryUserInputKey} from '../summary/summary.component.view';
import {SubmitHumanKey} from '../submit/submit.component.view';
import {ElementType} from '../../data/element-type/element-type';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import {IdentityCustomerInputKey} from '../../modules/identity/constants/identity-customer-input-key';
import {ElementDataObject, ElementData} from '../../models/element-data';
import {isAnyElementWithChildren} from '../../models/elements/any-element-with-children';
import {generateComponentTitle} from '../../utils/generate-component-title';

const additionErrorKeys: Record<string, string> = {
    [PrivacyUserInputKey]: 'Datenschutzrechtliche Einwilligung',
    [IdentityCustomerInputKey]: 'Nutzerkonto',
    [SummaryUserInputKey]: 'Zusammenfassung',
    [SummaryAttachmentsTooLargeKey]: 'Hinzugefügte Anlagen',
    [SubmitHumanKey]: 'Verifizierung',
};

const noLinkKeys = [SummaryAttachmentsTooLargeKey];

interface ErrorAlertProps {
    element: AnyElement;
    elementData: ElementData;
}

interface CollectedError {
    id: string;
    label: string;
    error: string;
}

export function collectErrors(element: AnyElement, elementData: ElementData): CollectedError[] {
    const elementObjectData: ElementDataObject | undefined = elementData[element.id];

    if (elementObjectData == null) {
        return [];
    }

    const col: CollectedError[] = [];

    if (elementObjectData.computedErrors != null) {
        elementObjectData.computedErrors.forEach((error) => {
            col.push({
                id: element.id,
                label: generateComponentTitle(element),
                error: error,
            });
        });
    }

    if (isAnyElementWithChildren(element) && element.children != null) {
        if (element.type === ElementType.ReplicatingContainer) {
            const childElementData = elementObjectData.value;

            if (Array.isArray(childElementData)) {
                for (const cElementData of childElementData) {
                    for (const child of element.children) {
                        const childErrors = collectErrors(child, cElementData as ElementData);
                        if (childErrors.length > 0) {
                            col.push(...childErrors);
                        }
                    }
                }
            }
        } else if (isAnyElementWithChildren(element)) {
            for (const child of element.children) {
                const childErrors = collectErrors(child, elementData);
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
        elementData,
    } = props;

    const errors: CollectedError[] = useMemo(() => {
        return collectErrors(element, elementData);
    }, [element, elementData]);

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

            <ul style={{paddingInlineStart: '20px'}}>
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