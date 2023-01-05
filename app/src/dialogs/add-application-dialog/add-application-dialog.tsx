import {Button, Dialog, DialogActions, DialogContent} from '@mui/material';
import React, {useState} from 'react';
import {DialogTitleWithClose} from '../../components/static-components/dialog-title-with-close/dialog-title-with-close';
import {Application} from '../../models/application';
import {ApplicationInitForm, validateApplication} from '../../components/application-init-form/application-init-form';
import {ElementType} from '../../data/element-type/element-type';
import {generateElementIdForType} from '../../utils/generate-element-id';
import {ApplicationInitFormPropsErrors} from '../../components/application-init-form/application-init-form-props';
import {AddApplicationDialogProps} from './add-application-dialog-props';
import {Localization} from '../../locale/localization';
import strings from './add-application-dialog-strings.json';

const _ = Localization(strings);

export function AddApplicationDialog(props: AddApplicationDialogProps) {
    const {applications, onHide, onSave, ...passTroughProps} = props;

    const [newApplication, setNewApplication] = useState<Application>(emptyApplication());
    const [newApplicationErrors, setNewApplicationErrors] = useState<ApplicationInitFormPropsErrors>({});

    const handleSave = (navigateToEditAfterwards: boolean) => {
        setNewApplicationErrors({});

        const errors = validateApplication(newApplication, applications);
        if (Object.keys(errors).length > 0) {
            setNewApplicationErrors(errors);
            return;
        }

        onSave(newApplication, navigateToEditAfterwards);
        handleHide();
    };

    const handleHide = () => {
        setNewApplication(emptyApplication());
        setNewApplicationErrors({});
        onHide();
    };

    return (
        <Dialog {...passTroughProps}
                onClose={handleHide}
        >
            <DialogTitleWithClose
                id="add-application-dialog-dialog-title"
                onClose={props.onHide}
                closeTooltip={_.closeTooltip}
            >
                {_.title}
            </DialogTitleWithClose>
            <DialogContent>
                <ApplicationInitForm
                    application={newApplication}
                    onChange={setNewApplication}
                    errors={newApplicationErrors}
                />
            </DialogContent>
            <DialogActions
                sx={{
                    pb: 3,
                    px: 3,
                    justifyContent: 'flex-start',
                }}
            >
                <Button
                    size="large"
                    variant="outlined"
                    onClick={() => handleSave(true)}
                >
                    {_.createAndEditLabel}
                </Button>
                <Button
                    size="large"
                    onClick={() => handleSave(false)}
                >
                    {_.onlyCreateLabel}
                </Button>
                <Button
                    onClick={handleHide}
                    sx={{
                        ml: 'auto!important',
                    }}
                    size="large"
                >
                    {_.cancelLabel}
                </Button>
            </DialogActions>
        </Dialog>
    );
}

function emptyApplication(): Application {
    return {
        code: '',
        created: '',
        id: -1,
        root: {
            id: generateElementIdForType(ElementType.Root),
            type: ElementType.Root,
            children: [],
            introductionStep: {
                id: generateElementIdForType(ElementType.IntroductionStep),
                type: ElementType.IntroductionStep,
            },
            summaryStep: {
                id: generateElementIdForType(ElementType.SummaryStep),
                type: ElementType.SummaryStep,
            },
            submitStep: {
                id: generateElementIdForType(ElementType.SubmitStep),
                type: ElementType.SubmitStep,
            },
        },
        slug: '',
        updated: '',
        version: ''
    };
}
