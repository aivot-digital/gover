import {Stack} from '@mui/material';
import {type ReactNode} from 'react';
import {Actions} from '../actions/actions';
import {type Action} from '../actions/actions-props';
import {
    FormFieldGroup,
    type FormFieldGroupContext,
    type FormFieldGroupLayoutProps,
} from '../form-field';

export interface TableFieldLayoutProps extends FormFieldGroupLayoutProps {
    label: ReactNode;
    hint?: ReactNode;
    error?: ReactNode;
    required?: boolean;
    disabled?: boolean;
    busy?: boolean;
    readOnly?: boolean;
    actions?: Action[];
    children: ReactNode | ((context: FormFieldGroupContext) => ReactNode);
}

function hasContent(content: ReactNode): boolean {
    return content !== null && content !== undefined && content !== false && content !== '';
}

export function TableFieldLayout(props: TableFieldLayoutProps) {
    const hasVisibleActions = props.actions?.some((action) => (
        action !== 'separator' && action.visible !== false
    )) ?? false;

    return (
        <FormFieldGroup
            id={props.id}
            label={props.label}
            ariaDescribedBy={props.ariaDescribedBy}
            labelAction={(context) => {
                const suppliedLabelAction = typeof props.labelAction === 'function'
                    ? props.labelAction(context)
                    : props.labelAction;

                if (!hasContent(suppliedLabelAction) && !hasVisibleActions) {
                    return null;
                }

                return (
                    <Stack
                        direction="row"
                        spacing={0.5}
                        sx={{height: '100%', alignItems: 'center'}}
                    >
                        {suppliedLabelAction}

                        {hasVisibleActions && (
                            <Actions
                                actions={props.actions ?? []}
                                dense
                                isBusy={props.busy}
                                tooltipPlacement="top"
                                sx={{
                                    gap: 0.5,
                                    '& .MuiButton-root': {
                                        minHeight: 28,
                                        py: 0.25,
                                    },
                                    '& .MuiIconButton-root': {
                                        width: 28,
                                        height: 28,
                                        p: 0.5,
                                    },
                                }}
                            />
                        )}
                    </Stack>
                );
            }}
            hint={props.hint}
            error={props.error}
            required={props.required}
            disabled={props.disabled}
            busy={props.busy}
            readOnly={props.readOnly}
            margin={props.margin ?? 'normal'}
            showOptionalIndicator={props.showOptionalIndicator}
            sx={props.sx}
        >
            {props.children}
        </FormFieldGroup>
    );
}
