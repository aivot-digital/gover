import {useState} from 'react';
import {
    Button,
    ListItemIcon,
    ListItemText,
    Menu,
    MenuItem,
} from '@mui/material';
import Check from '@aivot/mui-material-symbols-400-n25-outlined/Check';
import Code from '@aivot/mui-material-symbols-400-n25-outlined/Code';
import DataObject from '@aivot/mui-material-symbols-400-n25-outlined/DataObject';
import Functions from '@aivot/mui-material-symbols-400-n25-outlined/Functions';
import KeyboardArrowDown from '@aivot/mui-material-symbols-400-n25-outlined/KeyboardArrowDown';
import TextFields from '@aivot/mui-material-symbols-400-n25-outlined/TextFields';
import {FormFieldTokens} from '../../theming/form-field-tokens';
import {mergeAriaIds} from '../form-field';
import {useNormalizedReactId} from '../../hooks/use-normalized-react-id';

export type InputMode = 'literal' | 'variable' | 'noCode' | 'lowCode';

interface InputModeDefinition {
    label: string;
    description: string;
}

export const InputModeDefinitions: Record<InputMode, InputModeDefinition> = {
    literal: {
        label: 'Wert',
        description: 'Direkten Wert eingeben',
    },
    variable: {
        label: 'Variable',
        description: 'Eine im Prozess mögliche Variable referenzieren',
    },
    noCode: {
        label: 'Ausdruck (No-Code)',
        description: 'Wert visuell ableiten',
    },
    lowCode: {
        label: 'Skript (Low-Code)',
        description: 'Wert mit JavaScript bestimmen',
    },
};

export const InputModes: InputMode[] = ['literal', 'variable', 'noCode', 'lowCode'];

function renderModeIcon(mode: InputMode) {
    const iconProps = {
        fontSize: 'small' as const,
        sx: {color: 'text.secondary'},
    };

    switch (mode) {
        case 'variable':
            return <DataObject {...iconProps}/>;
        case 'noCode':
            return <Functions {...iconProps}/>;
        case 'lowCode':
            return <Code {...iconProps}/>;
        case 'literal':
        default:
            return <TextFields {...iconProps}/>;
    }
}

interface InputModeSelectorProps {
    fieldLabel: string;
    controlledFieldId?: string;
    value: InputMode;
    onChange: (mode: InputMode) => void;
    allowedModes?: InputMode[];
    disabled?: boolean;
}

export function InputModeSelector(props: InputModeSelectorProps) {
    const generatedId = useNormalizedReactId();
    const menuId = `input-mode-menu-${generatedId}`;
    const [menuAnchor, setMenuAnchor] = useState<HTMLElement | null>(null);
    const allowedModes = props.allowedModes ?? InputModes;
    const selectedMode = InputModeDefinitions[props.value];

    return (
        <>
            <Button
                size="small"
                variant="text"
                disabled={props.disabled || allowedModes.length < 2}
                aria-label={`${selectedMode.label}: Eingabemodus für ${props.fieldLabel} ändern`}
                aria-haspopup="menu"
                aria-expanded={menuAnchor != null}
                aria-controls={mergeAriaIds(
                    props.controlledFieldId,
                    menuAnchor != null ? menuId : undefined,
                )}
                startIcon={renderModeIcon(props.value)}
                endIcon={<KeyboardArrowDown fontSize="small"/>}
                onClick={(event) => setMenuAnchor(event.currentTarget)}
                sx={{
                    minWidth: 0,
                    height: FormFieldTokens.labelRowMinHeight,
                    minHeight: FormFieldTokens.labelRowMinHeight,
                    px: 0.75,
                    py: 0,
                    color: 'text.secondary',
                    fontSize: '0.8125rem',
                    lineHeight: 1.25,
                    whiteSpace: 'nowrap',
                    '&:hover': {
                        bgcolor: 'action.hover',
                        color: 'text.primary',
                    },
                    '& .MuiButton-startIcon': {
                        mr: 0.5,
                    },
                    '& .MuiButton-endIcon': {
                        ml: 0.25,
                    },
                }}
            >
                {selectedMode.label}
            </Button>

            <Menu
                anchorEl={menuAnchor}
                open={menuAnchor != null}
                onClose={() => setMenuAnchor(null)}
                slotProps={{
                    paper: {
                        sx: {minWidth: 260},
                    },
                    list: {
                        id: menuId,
                        'aria-label': `Eingabemodus für ${props.fieldLabel}`,
                    },
                }}
            >
                {allowedModes.map((mode) => (
                    <MenuItem
                        key={mode}
                        role="menuitemradio"
                        aria-checked={mode === props.value}
                        selected={mode === props.value}
                        onClick={() => {
                            props.onChange(mode);
                            setMenuAnchor(null);
                        }}
                    >
                        <ListItemIcon>
                            {renderModeIcon(mode)}
                        </ListItemIcon>
                        <ListItemText
                            primary={InputModeDefinitions[mode].label}
                            secondary={InputModeDefinitions[mode].description}
                        />
                        {mode === props.value && <Check fontSize="small"/>}
                    </MenuItem>
                ))}
            </Menu>
        </>
    );
}
