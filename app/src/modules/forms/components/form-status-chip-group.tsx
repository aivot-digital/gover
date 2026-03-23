import {FormStatusChip} from './form-status-chip';
import {FormStatus} from '../enums/form-status';
import {Box} from '@mui/material';
import React, {useMemo} from 'react';
import {FormEntity} from '../entities/form-entity';

interface FormStatusChipGroupProps {
    form: FormEntity;
}

export function getFormStatus(form: FormEntity): {
    isDrafted: boolean;
    isPublished: boolean;
    isRevoked: boolean;
} {
    return {
        isDrafted: form.draftedVersion != null,
        isPublished: form.publishedVersion != null,
        isRevoked: form.publishedVersion == null && (form.draftedVersion != null ? form.versionCount > 1 : form.versionCount > 0),
    };
}

export function FormStatusChipGroup(props: FormStatusChipGroupProps) {
    const {
        form,
    } = props;

    const {
        isDrafted,
        isPublished,
        isRevoked,
    } = useMemo(() => {
        return getFormStatus(form);
    }, [form]);

    return (
        <Box
            sx={{
                display: 'inline-flex',
                flexDirection: 'column',
                justifyContent: 'center',
                gap: 1,
                py: 2,
            }}
        >
            {
                isDrafted &&
                <Box>
                    <FormStatusChip
                        status={FormStatus.Drafted}
                        size="small"
                        variant="soft"
                    />
                </Box>
            }

            {
                isPublished &&
                <Box>
                    <FormStatusChip
                        status={FormStatus.Published}
                        size="small"
                        variant="soft"
                    />
                </Box>
            }

            {
                isRevoked &&
                <Box>
                    <FormStatusChip
                        status={FormStatus.Revoked}
                        size="small"
                        variant="soft"
                    />
                </Box>
            }
        </Box>
    );
}