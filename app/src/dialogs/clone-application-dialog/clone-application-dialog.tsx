import {Button, Dialog, DialogActions, DialogContent} from '@mui/material';
import React, {useState} from 'react';
import {DialogTitleWithClose} from '../../components/static-components/dialog-title-with-close/dialog-title-with-close';
import {Application} from '../../models/application';
import {ApplicationInitForm, validateApplication} from '../../components/application-init-form/application-init-form';
import {ApplicationInitFormPropsErrors} from '../../components/application-init-form/application-init-form-props';
import {CloneApplicationDialogProps} from './clone-application-dialog-props';
import {Localization} from '../../locale/localization';
import strings from './clone-application-dialog-strings.json';

const _ = Localization(strings);

export function CloneApplicationDialog(props: CloneApplicationDialogProps) {
    const {applications, onHide, onSave, source, ...passTroughProps} = props;

    const [clonedApplication, setClonedApplication] = useState<Application>();
    const [clonedApplicationErrors, setClonedApplicationErrors] = useState<ApplicationInitFormPropsErrors>({});

    const handleSave = (navigateToEditAfterwards: boolean) => {
        setClonedApplicationErrors({});

        const app = (clonedApplication ?? source)

        const errors = validateApplication(app, applications);
        if (Object.keys(errors).length > 0) {
            setClonedApplicationErrors(errors);
            return;
        }

        onSave(app, navigateToEditAfterwards);
        handleHide();
    };

    const handleHide = () => {
        setClonedApplication(undefined);
        setClonedApplicationErrors({});
        onHide();
    };

    return (
        <Dialog {...passTroughProps}
                onClose={handleHide}
        >
            <DialogTitleWithClose
                id="clone-application-dialog-dialog-title"
                onClose={props.onHide}
                closeTooltip={_.closeTooltip}
            >
                {_.title}
            </DialogTitleWithClose>
            <DialogContent>
                <ApplicationInitForm
                    application={clonedApplication ?? source}
                    onChange={setClonedApplication}
                    errors={clonedApplicationErrors}
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
