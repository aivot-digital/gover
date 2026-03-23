import React, {useState} from 'react';
import {type StepElement} from '../../models/elements/steps/step-element';
import {Box, Button, FormControl, FormLabel, Grid, Tooltip, Typography} from '@mui/material';
import {StepIconsMap} from '../../data/step-icons';
import {type BaseEditorProps} from '../../editors/base-editor';
import {TextFieldComponent} from '../text-field/text-field-component';
import {IconPickerDialog} from '../../dialogs/icon-picker-dialog/icon-picker-dialog';
import {SvgIconComponent} from '@mui/icons-material';
import ArrowCircleRightOutlinedIcon from '@mui/icons-material/ArrowCircleRightOutlined';
import {LoadedForm} from '../../slices/app-slice';

export function StepComponentEditor(props: BaseEditorProps<StepElement, LoadedForm>) {
    const [pickerOpen, setPickerOpen] = useState(false);

    const IconComponent: SvgIconComponent =
        props.element.icon && StepIconsMap[props.element.icon]?.def
            ? StepIconsMap[props.element.icon].def
            : ArrowCircleRightOutlinedIcon;
    const iconLabel = props.element.icon && StepIconsMap[props.element.icon]?.label;

    const hasCustomIcon = !!props.element.icon && !!StepIconsMap[props.element.icon];

    return (
        <>
            <Grid
                container
                columnSpacing={4}
            >
                <Grid
                    size={{
                        xs: 12,
                        lg: 6
                    }}>
                    <TextFieldComponent
                        value={props.element.title ?? ''}
                        label="Titel des Abschnitts"
                        onChange={(val) => {
                            props.onPatch({
                                title: val,
                            });
                        }}
                        disabled={!props.editable}
                    />
                </Grid>
            </Grid>
            <FormControl margin="normal">
                <FormLabel>Symbol (Icon) für diesen Abschnitt</FormLabel>

                <Box
                    display="flex"
                    alignItems="center"
                    gap={2}
                    mt={1}
                >
                    {IconComponent && (
                        <Tooltip title={iconLabel}>
                            <Box
                                sx={{
                                    border: '1px solid',
                                    borderColor: 'grey.300',
                                    borderRadius: 1,
                                    p: 1,
                                    backgroundColor: 'grey.50',
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    width: 48,
                                    height: 48,
                                }}
                            >
                                <IconComponent />
                            </Box>
                        </Tooltip>
                    )}

                    <Typography
                        variant="body2"
                        noWrap
                        color={hasCustomIcon ? 'text.primary' : 'text.secondary'}
                        sx={{fontStyle: hasCustomIcon ? 'normal' : 'italic'}}
                    >
                        {hasCustomIcon ? iconLabel : 'Kein Symbol ausgewählt'}
                    </Typography>

                    <Button
                        variant="outlined"
                        onClick={() => setPickerOpen(true)}
                        disabled={!props.editable}
                        sx={{ml: 2}}
                    >
                        {hasCustomIcon ? 'Symbol ändern' : 'Symbol auswählen'}
                    </Button>

                    {hasCustomIcon && (
                        <Button
                            variant="text"
                            color="error"
                            onClick={() => props.onPatch({icon: undefined})}
                            disabled={!props.editable}
                        >
                            Entfernen
                        </Button>
                    )}
                </Box>

                <IconPickerDialog
                    open={pickerOpen}
                    onClose={() => setPickerOpen(false)}
                    onSelect={(id) => props.onPatch({icon: id})}
                    selectedIconId={props.element.icon ?? undefined}
                    title={"Symbol (Icon) für Abschnitt auswählen"}
                    showLabels
                    autoSelect
                />
            </FormControl>
        </>
    );
}
