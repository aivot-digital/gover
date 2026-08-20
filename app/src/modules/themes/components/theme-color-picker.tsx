import {
    Box,
    ButtonBase,
    Grid,
    IconButton,
    InputAdornment,
    Popover,
    Stack,
    TextField,
    Tooltip,
    Typography,
} from '@mui/material';
import {alpha, styled} from '@mui/material/styles';
import React, {useEffect, useId, useState} from 'react';
import {
    ColorArea,
    ColorPicker,
    ColorSlider,
    ColorThumb,
    SliderTrack,
} from 'react-aria-components';
import CheckIcon from '@aivot/mui-material-symbols-400-n25-outlined/Check';
import ContrastOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Contrast';
import PaletteOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Palette';
import {
    DEFAULT_APPEARANCE_COLORS,
    getColorContrastRatio,
} from '../../../theming/resolve-appearance-colors';

type RgbColor = {
    red: number;
    green: number;
    blue: number;
};

type ThemeColorPickerProps = {
    label: string;
    value: string;
    onChange: (value: string) => void;
    contrastTextColor?: string;
    contrastBackgroundColor?: string;
    disabled?: boolean;
};

const presets = [
    {title: 'Standard-Primärfarbe', color: DEFAULT_APPEARANCE_COLORS.primaryColor},
    {title: 'Standard-Sekundärfarbe', color: DEFAULT_APPEARANCE_COLORS.secondaryColor},
    {title: 'Standard-Primärfarbe im dunklen Farbschema', color: DEFAULT_APPEARANCE_COLORS.primaryColorDark},
    {title: 'Standard-Sekundärfarbe im dunklen Farbschema', color: DEFAULT_APPEARANCE_COLORS.secondaryColorDark},
    {title: 'Rot', color: '#F44336'},
    {title: 'Pink', color: '#E91E63'},
    {title: 'Flieder', color: '#9C27B0'},
    {title: 'Violett', color: '#673AB7'},
    {title: 'Indigo', color: '#3F51B5'},
    {title: 'Blau', color: '#2196F3'},
    {title: 'Cyan', color: '#00BCD4'},
    {title: 'Blaugrün', color: '#009688'},
    {title: 'Grün', color: '#4CAF50'},
    {title: 'Limette', color: '#CDDC39'},
    {title: 'Gelb', color: '#FFEB3B'},
    {title: 'Bernstein', color: '#FFC107'},
    {title: 'Orange', color: '#FF5722'},
    {title: 'Blaugrau', color: '#607D8B'},
];

const contrastRatioFormatter = new Intl.NumberFormat('de-DE', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
});

const ColorAreaControl = styled(ColorArea)(({theme}) => ({
    width: '100%',
    height: 144,
    borderRadius: theme.shape.borderRadius,
    boxShadow: `inset 0 0 0 1px ${alpha(theme.palette.common.black, 0.28)}`,
    touchAction: 'none',
}));

const ColorSliderControl = styled(ColorSlider)({
    width: '100%',
    height: 20,
});

const ColorSliderTrack = styled(SliderTrack)(({theme}) => ({
    position: 'relative',
    top: 4,
    width: '100%',
    height: 12,
    borderRadius: 6,
    boxShadow: `inset 0 0 0 1px ${alpha(theme.palette.common.black, 0.28)}`,
    touchAction: 'none',
}));

const ColorPickerThumb = styled(ColorThumb)(({theme}) => ({
    width: 18,
    height: 18,
    border: `2px solid ${theme.palette.common.white}`,
    borderRadius: '50%',
    boxShadow: `0 0 0 1px ${alpha(theme.palette.common.black, 0.55)}, 0 2px 5px ${alpha(theme.palette.common.black, 0.28)}`,
    boxSizing: 'border-box',
    cursor: 'grab',
    '&[data-dragging]': {
        cursor: 'grabbing',
    },
    '&[data-focus-visible]': {
        outline: `3px solid ${alpha(theme.palette.primary.main, 0.55)}`,
        outlineOffset: 2,
    },
}));

export function ThemeColorPicker({
    label,
    value,
    onChange,
    contrastTextColor,
    contrastBackgroundColor,
    disabled = false,
}: ThemeColorPickerProps) {
    const [anchorElement, setAnchorElement] = useState<HTMLElement | null>(null);
    const [hexDraft, setHexDraft] = useState(formatHex(value));
    const [hasHexError, setHasHexError] = useState(false);
    const descriptionId = useId();
    const popoverId = useId();
    const formattedValue = formatHex(value);
    const formattedContrastTextColor = formatHex(contrastTextColor ?? '#FFFFFF');
    const formattedContrastBackgroundColor = formatHex(contrastBackgroundColor ?? '#FFFFFF');
    const rgb = hexToRgb(formattedValue);
    const filledButtonContrastRatio = getColorContrastRatio(formattedValue, formattedContrastTextColor);
    const iconButtonContrastRatio = getColorContrastRatio(formattedValue, formattedContrastBackgroundColor);
    const contrastTextDescription = formattedContrastTextColor === '#000000'
        ? 'schwarzer Schrift'
        : formattedContrastTextColor === '#FFFFFF'
            ? 'weißer Schrift'
            : 'automatisch gewählter Schriftfarbe';

    useEffect(() => {
        setHexDraft(formattedValue);
        setHasHexError(false);
    }, [formattedValue]);

    useEffect(() => {
        if (disabled) {
            setAnchorElement(null);
        }
    }, [disabled]);

    const updateColor = (nextValue: string) => {
        const nextColor = formatHex(nextValue);
        setHexDraft(nextColor);
        setHasHexError(false);
        onChange(nextColor);
    };

    const commitHexDraft = () => {
        const nextColor = normalizeHex(hexDraft);

        if (nextColor == null) {
            setHasHexError(true);
            return;
        }

        updateColor(nextColor);
    };

    const updateRgbChannel = (channel: keyof RgbColor, input: string) => {
        const numericValue = Number(input);

        if (!Number.isFinite(numericValue)) {
            return;
        }

        updateColor(rgbToHex({
            ...rgb,
            [channel]: clampRgb(numericValue),
        }));
    };

    return (
        <Grid
            size={{xs: 12, md: 6, lg: 6}}
            aria-disabled={disabled || undefined}
            sx={{minWidth: 0}}
        >
            <TextField
                fullWidth
                label={label}
                value={hexDraft}
                disabled={disabled}
                error={hasHexError}
                helperText={hasHexError ? (
                    'Bitte geben Sie eine sechsstellige HEX-Farbe ein.'
                ) : (
                    <Box
                        component="span"
                        sx={{display: 'flex', alignItems: 'flex-start', gap: 0.5}}
                    >
                        <ContrastOutlinedIcon sx={{fontSize: '0.875rem', mt: '1px', flex: '0 0 auto'}} />
                        <span>
                            Gefüllt mit {contrastTextDescription}: {contrastRatioFormatter.format(filledButtonContrastRatio)}:1
                            {' · '}Symbol auf Standardfläche: {contrastRatioFormatter.format(iconButtonContrastRatio)}:1
                        </span>
                    </Box>
                )}
                onChange={(event) => {
                    const draft = event.target.value.toUpperCase();
                    setHexDraft(draft);
                    setHasHexError(false);

                    const nextColor = normalizeHex(draft);
                    if (nextColor != null && nextColor !== formattedValue) {
                        onChange(nextColor);
                    }
                }}
                onBlur={commitHexDraft}
                onKeyDown={(event) => {
                    if (event.key === 'Enter') {
                        commitHexDraft();
                        event.currentTarget.blur();
                    }

                    if (event.key === 'Escape') {
                        setHexDraft(formattedValue);
                        setHasHexError(false);
                        event.currentTarget.blur();
                    }
                }}
                slotProps={{
                    htmlInput: {
                        maxLength: 7,
                        spellCheck: false,
                        'aria-describedby': descriptionId,
                    },
                    input: {
                        startAdornment: (
                            <InputAdornment position="start">
                                <Tooltip title="Farbe auswählen" arrow>
                                    <Box component="span" sx={{display: 'inline-flex'}}>
                                        <IconButton
                                            size="small"
                                            disabled={disabled}
                                            aria-label={`${label} visuell auswählen`}
                                            aria-haspopup="dialog"
                                            aria-controls={anchorElement == null ? undefined : popoverId}
                                            aria-expanded={anchorElement != null}
                                            onClick={(event) => setAnchorElement(event.currentTarget)}
                                            sx={{p: 0.5, gap: 0.5, borderRadius: 1}}
                                        >
                                            <Box
                                                sx={{
                                                    width: 20,
                                                    height: 20,
                                                    border: '1px solid',
                                                    borderColor: 'divider',
                                                    borderRadius: 0.75,
                                                    backgroundColor: formattedValue,
                                                    opacity: disabled ? 0.48 : 1,
                                                }}
                                            />
                                            <PaletteOutlinedIcon
                                                sx={{
                                                    fontSize: 18,
                                                    color: disabled ? 'text.disabled' : 'text.secondary',
                                                }}
                                            />
                                        </IconButton>
                                    </Box>
                                </Tooltip>
                            </InputAdornment>
                        ),
                    },
                    formHelperText: {
                        id: descriptionId,
                        component: 'div',
                    },
                }}
            />

            <Popover
                id={popoverId}
                open={anchorElement != null}
                anchorEl={anchorElement}
                onClose={() => setAnchorElement(null)}
                anchorOrigin={{vertical: 'bottom', horizontal: 'left'}}
                transformOrigin={{vertical: 'top', horizontal: 'left'}}
                slotProps={{
                    paper: {
                        sx: {
                            width: 304,
                            maxWidth: 'calc(100vw - 32px)',
                            mt: 0.75,
                            p: 1.5,
                        },
                    },
                }}
            >
                <Box role="dialog" aria-label={`${label} auswählen`}>
                    <ColorPicker
                        value={formattedValue}
                        onChange={(color) => updateColor(color.toString('hex'))}
                    >
                        <Stack spacing={1.5}>
                            <ColorAreaControl
                                colorSpace="hsb"
                                xChannel="saturation"
                                yChannel="brightness"
                                aria-label="Sättigung und Helligkeit"
                            >
                                <ColorPickerThumb />
                            </ColorAreaControl>

                            <Box>
                                <Typography variant="caption" color="text.secondary" sx={{display: 'block', mb: 0.5}}>
                                    Farbton
                                </Typography>
                                <ColorSliderControl colorSpace="hsb" channel="hue" aria-label="Farbton">
                                    <ColorSliderTrack>
                                        <ColorPickerThumb />
                                    </ColorSliderTrack>
                                </ColorSliderControl>
                            </Box>

                            <Box>
                                <Typography variant="caption" color="text.secondary" sx={{display: 'block', mb: 1}}>
                                    RGB
                                </Typography>
                                <Grid container spacing={1}>
                                    <RgbInput
                                        channel="red"
                                        label="R"
                                        accessibleLabel="Rotwert"
                                        value={rgb.red}
                                        onChange={updateRgbChannel}
                                    />
                                    <RgbInput
                                        channel="green"
                                        label="G"
                                        accessibleLabel="Grünwert"
                                        value={rgb.green}
                                        onChange={updateRgbChannel}
                                    />
                                    <RgbInput
                                        channel="blue"
                                        label="B"
                                        accessibleLabel="Blauwert"
                                        value={rgb.blue}
                                        onChange={updateRgbChannel}
                                    />
                                </Grid>
                            </Box>

                            <Box>
                                <Typography variant="caption" color="text.secondary" sx={{display: 'block', mb: 1}}>
                                    Farbvorgaben
                                </Typography>
                                <Box
                                    sx={{
                                        display: 'grid',
                                        gridTemplateColumns: 'repeat(9, 26px)',
                                        gap: 0.5,
                                    }}
                                >
                                    {presets.map((preset) => {
                                        const isSelected = formattedValue === formatHex(preset.color);
                                        const checkColor = getColorContrastRatio(preset.color, '#FFFFFF') >= 3
                                            ? '#FFFFFF'
                                            : '#111111';

                                        return (
                                            <Tooltip key={`${preset.title}-${preset.color}`} title={preset.title} arrow>
                                                <ButtonBase
                                                    aria-label={`${preset.title}: ${preset.color}`}
                                                    aria-pressed={isSelected}
                                                    onClick={() => updateColor(preset.color)}
                                                    sx={{
                                                        width: 26,
                                                        height: 26,
                                                        border: '1px solid',
                                                        borderColor: isSelected ? 'text.primary' : 'divider',
                                                        borderRadius: 0.75,
                                                        backgroundColor: preset.color,
                                                        boxShadow: isSelected ? '0 0 0 2px currentColor' : 'none',
                                                    }}
                                                >
                                                    {isSelected && <CheckIcon sx={{fontSize: 18, color: checkColor}} />}
                                                </ButtonBase>
                                            </Tooltip>
                                        );
                                    })}
                                </Box>
                            </Box>
                        </Stack>
                    </ColorPicker>
                </Box>
            </Popover>
        </Grid>
    );
}

function RgbInput({
    channel,
    label,
    accessibleLabel,
    value,
    onChange,
}: {
    channel: keyof RgbColor;
    label: string;
    accessibleLabel: string;
    value: number;
    onChange: (channel: keyof RgbColor, value: string) => void;
}) {
    return (
        <Grid size={4}>
            <TextField
                fullWidth
                size="small"
                margin="none"
                type="number"
                label={label}
                value={value}
                onFocus={(event) => event.currentTarget.select()}
                onChange={(event) => onChange(channel, event.target.value)}
                slotProps={{
                    htmlInput: {
                        min: 0,
                        max: 255,
                        step: 1,
                        inputMode: 'numeric',
                        'aria-label': accessibleLabel,
                    },
                }}
            />
        </Grid>
    );
}

function normalizeHex(value: string): string | null {
    const normalized = value.trim().replace(/^#/, '');

    if (!/^[0-9A-F]{6}$/i.test(normalized)) {
        return null;
    }

    return `#${normalized.toUpperCase()}`;
}

function formatHex(value: string): string {
    return normalizeHex(value) ?? '#000000';
}

function hexToRgb(value: string): RgbColor {
    const hex = formatHex(value).slice(1);

    return {
        red: Number.parseInt(hex.slice(0, 2), 16),
        green: Number.parseInt(hex.slice(2, 4), 16),
        blue: Number.parseInt(hex.slice(4, 6), 16),
    };
}

function rgbToHex(color: RgbColor): string {
    const channels = [color.red, color.green, color.blue]
        .map((channel) => clampRgb(channel).toString(16).padStart(2, '0'));

    return `#${channels.join('').toUpperCase()}`;
}

function clampRgb(value: number): number {
    return Math.min(255, Math.max(0, Math.round(value)));
}
