import {type ReactNode} from 'react';
import {Chip, Stack} from '@mui/material';
import {type ProcessNodeProvider} from '../services/process-node-provider-api-service';
import {isStringNotNullOrEmpty} from '../../../utils/string-utils';

interface ProcessNodeProviderBadgesProps {
    provider: Pick<ProcessNodeProvider, 'majorVersion' | 'deprecationNotice'>;
    showMajorVersion?: boolean;
    onShowDeprecationDetails?: () => void;
}

export function ProcessNodeProviderBadges(props: ProcessNodeProviderBadgesProps): ReactNode {
    const {
        provider,
        showMajorVersion = false,
        onShowDeprecationDetails,
    } = props;
    const isDeprecated = isStringNotNullOrEmpty(provider.deprecationNotice);

    if (!showMajorVersion && !isDeprecated) {
        return null;
    }

    return (
        <Stack direction="row" spacing={1} useFlexGap sx={{flexWrap: 'wrap', flexShrink: 0}}>
            {
                showMajorVersion &&
                <Chip
                    size="small"
                    label={`Version ${provider.majorVersion}`}
                    sx={{flexShrink: 0}}
                />
            }
            {
                isDeprecated &&
                <Chip
                    size="small"
                    label="Veraltet"
                    color="warning"
                    variant="outlined"
                    onClick={onShowDeprecationDetails}
                    aria-label={onShowDeprecationDetails == null ? undefined : 'Veraltet – Hinweise anzeigen'}
                    aria-haspopup={onShowDeprecationDetails == null ? undefined : 'dialog'}
                    title={onShowDeprecationDetails == null ? undefined : 'Hinweise zur weiteren Verwendung anzeigen'}
                    sx={{flexShrink: 0}}
                />
            }
        </Stack>
    );
}
