import {FormStatusChip} from './form-status-chip';
import {FormStatus} from '../enums/form-status';
import {Box} from '@mui/material';
import React, {useMemo} from 'react';
import {FormListResponseDTO} from '../dtos/form-list-response-dto';

interface FormStatusChipGroupProps {
    form: FormListResponseDTO;
}

export function getFormStatus(form: FormListResponseDTO): {
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
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'center',
                gap: 2,
                py: 2,
            }}
        >
            {
                isDrafted &&
                <FormStatusChip
                    status={FormStatus.Drafted}
                    size="small"
                    variant="outlined"
                />
            }

            {
                isPublished &&
                <FormStatusChip
                    status={FormStatus.Published}
                    size="small"
                    variant="outlined"
                />
            }

            {
                isRevoked &&
                <FormStatusChip
                    status={FormStatus.Revoked}
                    size="small"
                    variant="outlined"
                />
            }
        </Box>
    );
}