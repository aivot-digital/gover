import {Box, Paper, Typography} from '@mui/material';
import {forwardRef} from 'react';
import {Chip} from '../../../../components/chip/chip';
import {RichtextComponent} from '../../../../components/richtext/richtext.component';
import {isStringNotNullOrEmpty} from '../../../../utils/string-utils';
import type {IdentitySelectionApi} from '../../models/identity-selection-api';
import type {IdentitySlot} from '../../models/identity-slot';
import {
    FormIdentitySelectionControls,
    type FormIdentitySelectionControlsHandle,
    type FormIdentitySelectionControlsStatus,
} from '../form-identity-selection-controls/form-identity-selection-controls';

export interface IdentitySlotCardProps {
    slot: IdentitySlot;
    api: IdentitySelectionApi;
    saveMode?: 'explicit' | 'deferred';
    onChange: (slot: IdentitySlot) => void;
    onStatusChange?: (slotId: string, status: FormIdentitySelectionControlsStatus | null) => void;
    beforeIdentityProviderStart?: (identityId: string) => Promise<boolean>;
    identityProviderAuthenticationDisabled?: boolean;
}

export function getIdentityDisplayName(identity: {title: string | null}): string {
    const title = identity.title?.trim();
    return title != null && title.length > 0 ? title : 'Unbenannte Identität';
}

export const IdentitySlotCard = forwardRef<FormIdentitySelectionControlsHandle, IdentitySlotCardProps>(
    function IdentitySlotCard(props, ref) {
        const {
            api,
            beforeIdentityProviderStart,
            identityProviderAuthenticationDisabled,
            onChange,
            onStatusChange,
            saveMode,
            slot,
        } = props;

        return (
            <Paper
                variant="outlined"
                sx={{
                    height: '100%',
                    p: {
                        xs: 2,
                        md: 2.5,
                    },
                    borderColor: 'divider',
                    backgroundColor: 'background.paper',
                }}
            >
                <Box
                    sx={{
                        display: 'flex',
                        flexDirection: 'column',
                        height: '100%',
                    }}
                >
                    <Box>
                        <Typography variant="caption">
                            Identität
                        </Typography>
                        <Box
                            sx={{
                                display: 'flex',
                                alignItems: 'center',
                                flexWrap: 'wrap',
                                columnGap: 1.25,
                                rowGap: 0.5,
                                mt: 0.25,
                            }}
                        >
                            <Typography
                                variant="h4"
                                component="h2"
                            >
                                {getIdentityDisplayName(slot)}
                            </Typography>

                            <Chip
                                mode="soft"
                                label={slot.isRequired ? 'Verpflichtend' : 'Optional'}
                                color={slot.isRequired ? 'warning' : 'info'}
                                size="small"
                            />
                        </Box>
                    </Box>

                    {
                        isStringNotNullOrEmpty(slot.description) &&
                        <RichtextComponent
                            content={slot.description}
                            sx={{mt: 2}}
                        />
                    }

                    <Typography
                        variant="body2"
                        sx={{
                            color: 'text.secondary',
                            mt: 2,
                        }}
                    >
                        {
                            slot.isRequired
                                ? 'Eine der nachfolgenden Möglichkeiten ist zwingend erforderlich.'
                                : 'Eine der nachfolgenden Möglichkeiten kann optional verwendet werden.'
                        }
                    </Typography>

                    <FormIdentitySelectionControls
                        ref={ref}
                        slot={slot}
                        api={api}
                        saveMode={saveMode}
                        beforeIdentityProviderStart={beforeIdentityProviderStart}
                        identityProviderAuthenticationDisabled={identityProviderAuthenticationDisabled}
                        onChange={onChange}
                        onStatusChange={onStatusChange}
                    />
                </Box>
            </Paper>
        );
    },
);
