import {ApplicationInitFormProps, ApplicationInitFormPropsErrors} from './application-init-form-props';
import {Localization} from '../../locale/localization';
import strings from './application-init-form-strings.json';
import {Alert, TextField, Typography} from '@mui/material';
import {slugify} from '../../utils/slugify';
import React from 'react';
import {Application} from '../../models/entities/application';
import {checkTitle} from '../../utils/check-title';
import {checkSlugAndVersion} from '../../utils/check-slug-and-version';
import {checkVersion} from '../../utils/check-version';
import {TextFieldComponent} from "../text-field/text-field-component";

const _ = Localization(strings);

export function ApplicationInitForm({application, onChange, errors}: ApplicationInitFormProps) {
    const patch = (update: Partial<Application>) => {
        onChange({
            ...application,
            ...update,
        });
    };

    return (
        <>
            <Typography
                variant="body2"
            >
                {_.titleHelper}
            </Typography>

            <TextFieldComponent
                label={_.titleLabel}
                placeholder={_.titlePlaceholder}
                value={application.root.title ?? ''}
                onChange={val => {
                    patch({
                        root: {
                            ...application.root,
                            title: val,
                        }
                    });
                }}
                onBlur={val => {
                    const title = val != null ? val.trim() : '';
                    patch({
                        root: {
                            ...application.root,
                            title,
                        },
                    });
                    if (application.slug.length === 0) {
                        patch({
                            slug: slugify(title),
                        });
                    }
                }}
                error={errors.title}
            />

            <Typography
                variant="body2"
                sx={{mt: 3}}
            >
                {_.slugHelper}
            </Typography>

            <TextFieldComponent
                label={_.slugLabel}
                placeholder={_.slugPlaceholder}
                value={application.slug}
                onChange={val => {
                    patch({
                        slug: val,
                    });
                }}
                onBlur={val => {
                    patch({
                        slug: val != null ? val.trim().replace(/-\s*$/, '') : '',
                    });
                }}
                error={errors.slug}
            />

            <Alert
                severity="warning"
                variant="outlined"
                sx={{mt: 1}}
            >
                {_.slugWarning}
            </Alert>

            <Typography
                variant="body2"
                sx={{mt: 3}}
            >
                {_.versionHelper}
            </Typography>

            <TextFieldComponent
                label={_.versionLabel}
                placeholder={_.versionPlaceholder}
                value={application.version}
                onChange={val => {
                    patch({
                        version: val,
                    });
                }}
                onBlur={val => {
                    patch({
                        version: val != null ? val.trim() : '',
                    });
                }}
                error={errors.version}
            />

            {
                Object.keys(errors).length > 0 &&
                <Alert
                    severity="error"
                    sx={{mt: 2}}
                >
                    {_.errorHint}
                </Alert>
            }
        </>
    );
}

export function validateApplication(app: Application, otherApps: Application[]): ApplicationInitFormPropsErrors {
    const errors: ApplicationInitFormPropsErrors = {};
    const titleErrors = checkTitle(app.root.title);
    if (titleErrors.length > 0) {
        errors.title = titleErrors[0];
    }

    const versionErrors = checkVersion(app.version);
    if (versionErrors.length > 0) {
        errors.version = versionErrors[0];
    }

    const {slugError, versionError} = checkSlugAndVersion(otherApps, app.slug, app.version);
    if (slugError != null) {
        errors.slug = slugError;
    }
    if (versionError != null) {
        errors.version = versionError;
    }

    return errors;
}
