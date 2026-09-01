import type {SxProps, Theme} from '@mui/material';

export type FormFieldMargin = 'none' | 'dense' | 'normal';

export const FormFieldTokens = {
    labelRowMinHeight: 28,
    controlMinHeight: 44,
    controlWithSecondaryTextMinHeight: 52,
    groupedControlRowMinHeight: 50,
    labelToControlGap: 0.5,
    labelActionGap: 1,
    helperTextGap: 1,
    labelFontSize: '0.875rem',
    labelFontWeight: 500,
    labelLineHeight: 1.35,
    helperTextLineHeight: 1.35,
} as const;

export const formFieldRootSx = {
    minWidth: 0,
    width: '100%',
} satisfies SxProps<Theme>;

export function getFormFieldMarginSx(margin: FormFieldMargin): SxProps<Theme> {
    switch (margin) {
        case 'normal':
            return {mt: 0.25, mb: 1};
        case 'dense':
            return {mt: 0.125, mb: 0.5};
        default:
            return {};
    }
}

export const formFieldLabelRowSx = {
    display: 'grid',
    gridTemplateColumns: 'minmax(0, 1fr) max-content',
    alignItems: 'center',
    columnGap: FormFieldTokens.labelActionGap,
    minHeight: FormFieldTokens.labelRowMinHeight,
    mb: FormFieldTokens.labelToControlGap,
} satisfies SxProps<Theme>;

export const formFieldLabelSx = {
    minWidth: 0,
    color: 'text.primary',
    fontSize: FormFieldTokens.labelFontSize,
    fontWeight: FormFieldTokens.labelFontWeight,
    lineHeight: FormFieldTokens.labelLineHeight,
    overflowWrap: 'anywhere',
    letterSpacing: 0,
    '&.Mui-focused': {
        color: 'text.primary',
    },
    '&.Mui-disabled': {
        color: 'text.disabled',
    },
    '&.Mui-error': {
        color: 'error.main',
    },
} satisfies SxProps<Theme>;

export const formFieldLabelActionSx = {
    minWidth: 0,
    height: FormFieldTokens.labelRowMinHeight,
    display: 'flex',
    alignItems: 'center',
    alignSelf: 'stretch',
    justifySelf: 'end',
} satisfies SxProps<Theme>;

export const formFieldInputRootSx = {
    minHeight: FormFieldTokens.controlMinHeight,
} satisfies SxProps<Theme>;

export const formFieldHelperTextSx = {
    mx: 0,
    mt: FormFieldTokens.helperTextGap,
    lineHeight: FormFieldTokens.helperTextLineHeight,
} satisfies SxProps<Theme>;
