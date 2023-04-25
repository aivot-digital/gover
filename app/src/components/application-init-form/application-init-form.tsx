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

            <TextField
                label={_.titleLabel}
                placeholder={_.titlePlaceholder}
                value={application.root.title ?? ''}
                onChange={event => {
                    patch({
                        root: {
                            ...application.root,
                            title: event.target.value,
                        }
                    });
                }}
                onBlur={() => {
                    const title = application.root.title?.trim() ?? '';
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
                error={errors.title != null}
                helperText={errors.title}
                sx={{mt: 3}}
                fullWidth
            />

            <Typography
                variant="body2"
                sx={{mt: 3}}
            >
                {_.slugHelper}
            </Typography>

            <TextField
                label={_.slugLabel}
                placeholder={_.slugPlaceholder}
                value={application.slug}
                onChange={event => {
                    patch({
                        slug: event.target.value,
                    });
                }}
                onBlur={() => {
                    patch({
                        slug: application.slug.trim().replace(/-\s*$/, ''),
                    });
                }}
                sx={{mt: 3}}
                fullWidth
                error={errors.slug != null}
                helperText={errors.slug}
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

            <TextField
                label={_.versionLabel}
                placeholder={_.versionPlaceholder}
                value={application.version}
                onChange={event => {
                    patch({
                        version: event.target.value,
                    });
                }}
                onBlur={() => {
                    patch({
                        version: application.version.trim(),
                    });
                }}
                sx={{mt: 3}}
                fullWidth
                error={errors.version != null}
                helperText={errors.version}
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

    const slugErrors = checkSlugAndVersion(otherApps, app.slug, app.version);
    if (slugErrors.length > 0) {
        errors.slug = slugErrors[0];
        if (errors.version == null) {
            errors.version = slugErrors[0];
        }
    }

    return errors;
}
