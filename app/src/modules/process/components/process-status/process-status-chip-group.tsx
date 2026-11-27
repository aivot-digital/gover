import {ProcessStatusChip} from './process-status-chip';
import {Box} from '@mui/material';
import React, {useMemo} from 'react';
import {ProcessEntity} from '../../entities/process-entity';
import {ProcessVersionEntity} from '../../entities/process-version-entity';
import {ProcessStatus} from '../../enums/process-status';

interface ProcessStatusChipGroupProps {
    process: ProcessEntity;
}

export function getFormStatus(processEntity: ProcessEntity): {
    isDrafted: boolean;
    isPublished: boolean;
    isRevoked: boolean;
} {
    return {
        isDrafted: processEntity.draftedVersion != null,
        isPublished: processEntity.publishedVersion != null,
        isRevoked: processEntity.publishedVersion == null && (processEntity.draftedVersion != null ? processEntity.versionCount > 1 : processEntity.versionCount > 0),
    };
}

export function ProcessStatusChipGroup(props: ProcessStatusChipGroupProps) {
    const {
        process,
    } = props;

    const {
        isDrafted,
        isPublished,
        isRevoked,
    } = useMemo(() => {
        return getFormStatus(process);
    }, [process]);

    return (
        <Box
            sx={{
                display: 'inline-flex',
                flexDirection: 'column',
                justifyContent: 'center',
                gap: 1,
            }}
        >
            {
                isDrafted &&
                <Box>
                    <ProcessStatusChip
                        status={ProcessStatus.Drafted}
                        size="small"
                        variant="soft"
                    />
                </Box>
            }

            {
                isPublished &&
                <Box>
                    <ProcessStatusChip
                        status={ProcessStatus.Published}
                        size="small"
                        variant="soft"
                    />
                </Box>
            }

            {
                isRevoked &&
                <Box>
                    <ProcessStatusChip
                        status={ProcessStatus.Revoked}
                        size="small"
                        variant="soft"
                    />
                </Box>
            }
        </Box>
    );
}