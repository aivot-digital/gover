import {AlertComponent} from '../alert/alert-component';
import {useAppSelector} from '../../hooks/use-app-selector';
import {useMemo} from 'react';
import {AnyElement} from '../../models/elements/any-element';
import {selectLoadedForm} from '../../slices/app-slice';
import {flattenElements} from '../../utils/flatten-elements';
import {Link, Typography} from '@mui/material';
import {isAnyInputElement} from '../../models/elements/form/input/any-input-element';
import {PrivacyUserInputKey} from '../general-information/general-information.component.view';
import {IdCustomerDataKey} from '../id-input/id-input';
import {SummaryAttachmentsTooLargeKey, SummaryUserInputKey} from '../summary/summary.component.view';
import {SubmitHumanKey} from '../submit/submit.component.view';
import {ElementType} from '../../data/element-type/element-type';
import {CheckboxFieldElement} from '../../models/elements/form/input/checkbox-field-element';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';

interface ErrorItem {
    element: AnyElement;
    error: string;
}

const additionErrorKeys: Record<string, string> = {
    [PrivacyUserInputKey]: 'Datenschutzrechtliche Einwilligung',
    [IdCustomerDataKey]: 'Nutzerkonto',
    [SummaryUserInputKey]: 'Zusammenfassung',
    [SummaryAttachmentsTooLargeKey]: 'Hinzugefügte Anlagen',
    [SubmitHumanKey]: 'Verifizierung',
};

const noLinkKeys = [SummaryAttachmentsTooLargeKey];

export function ErrorAlert() {
    const loadedForm = useAppSelector(selectLoadedForm);

    const allErrors = useAppSelector(state => state.app.errors);

    const allElements = useMemo(() => {
        if (loadedForm == null) {
            return [];
        }
        return flattenElements(loadedForm.root, false);
    }, [loadedForm]);

    const errors: ErrorItem[] = useMemo(() => {
        if (!loadedForm) {
            return [];
        }

        const errors = Object
            .keys(allErrors)
            .map((errorKey) => {
                let referencedElement = allElements.find(element => errorKey.endsWith(element.id));

                if (referencedElement == null) {
                    console.warn(`Error key ${errorKey} does not reference an element`);
                    return null;
                }

                const error = allErrors[errorKey];

                return {
                    element: {
                        ...referencedElement,
                        id: errorKey,
                    },
                    error,
                };
            })
            .filter(e => e != null) as ErrorItem[];

        Object.entries(additionErrorKeys).forEach(([key, value]) => {
            const error = allErrors[key];
            if (error) {
                const elem: CheckboxFieldElement = {
                    id: key,
                    label: value,
                    type: ElementType.Checkbox,
                    appVersion: '0.0.0',
                };
                errors.push({
                    element: elem,
                    error,
                });
            }
        });

        return errors;
    }, [allErrors, allElements]);

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
                        .map(({element, error}) => {
                            const isLinked = !noLinkKeys.includes(element.id);

                            return (
                                <li key={element.id}>
                                    {
                                        isLinked ? (
                                            <Link
                                                color="inherit"
                                                onClick={() => {
                                                    const target = document.getElementById(element.id);
                                                    if (target) {
                                                        target.scrollIntoView({ behavior: 'smooth', block: 'start' });
                                                        target.focus?.();
                                                    }
                                                }}
                                                title="Klicken Sie hier, um zu dieser Angabe zu springen"
                                                sx={{cursor: 'pointer', fontWeight: '600'}}
                                            >
                                                {isAnyInputElement(element) ? `${element.label}` : element.id}
                                                <EditOutlinedIcon
                                                    fontSize="small"
                                                    sx={{transform: 'translateY(4px)'}}
                                                />
                                            </Link>
                                        ) : (
                                            <Typography component="span" sx={{fontWeight: '600'}}>
                                                {isAnyInputElement(element) ? `${element.label}` : element.id}
                                            </Typography>
                                        )
                                    }
                                    : {error}
                                </li>
                            )
                        })
                }
            </ul>
        </AlertComponent>
    );
}