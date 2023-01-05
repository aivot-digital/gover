import {EditDialogProps} from './edit-dialog-props';
import {
    DialogTitleWithClose
} from '../../../../../components/static-components/dialog-title-with-close/dialog-title-with-close';
import {
    Alert, AlertTitle,
    Box, Button, Dialog,
    DialogActions,
    DialogContent,
    FormControl, FormHelperText,
    InputLabel,
    MenuItem,
    Select,
    TextField,
    Typography
} from '@mui/material';
import {RichTextEditorComponentView} from '../../../../../components/richt-text-editor/rich-text-editor.component.view';
import React, {useEffect, useState} from 'react';
import {Localization} from '../../../../../locale/localization';
import strings from './edit-dialog-strings.json';

const _ = Localization(strings);

export function EditDialog<T>({
                                  onClose,
                                  onSave,
                                  item,
                                  fieldsToEdit,
                                  toPrimaryString,
                                  ...dialogProps
                              }: EditDialogProps<T>) {
    const [itemToEdit, setItemToEdit] = useState<T>();
    const [missingRequiredFields, setMissingRequiredFields] = useState<any[]>([]);
    const [maxedOutFields, setMaxedOutFields] = useState<any[]>([]);

    useEffect(() => {
        if (item != null) {
            setItemToEdit({...item});
        } else {
            setItemToEdit(undefined);
        }
    }, [item])

    const handleClose = () => {
        setMissingRequiredFields([]);
        setMaxedOutFields([]);
        onClose();
    };

    const handleSave = () => {
        const missingFields = [];
        const maxedFields = [];

        if (itemToEdit != null) {
            for (const field of fieldsToEdit) {
                if (typeof field !== 'string') {
                    const value = itemToEdit[field.field];

                    if (!field.isRichtext && typeof value === 'string' && value.length > 255) {
                        maxedFields.push(field.field);
                    }

                    if (field.required && (field.showIf == null || field.showIf(itemToEdit))) {
                        if (value == null || (typeof value === 'string' && value.length === 0)) {
                            missingFields.push(field.field);
                        }
                    }
                }
            }

            if (missingFields.length === 0 && maxedFields.length === 0) {
                onSave(itemToEdit);
                handleClose();
            } else {
                setMissingRequiredFields(missingFields);
                setMaxedOutFields(maxedFields);
            }
        }
    };

    return (
        <Dialog
            {...dialogProps}
            open={itemToEdit != null}
            onClose={handleClose}
            fullWidth={true}
        >
            <DialogTitleWithClose
                id={'import-dialog-title'}
                onClose={handleClose}
                closeTooltip={_.closeTooltip}
            >
                {
                    item &&
                    _.format(_.editTitle, {name: toPrimaryString(item)})}
            </DialogTitleWithClose>
            <DialogContent>
                {
                    itemToEdit &&
                    fieldsToEdit
                        .filter(field => typeof field === 'string' || field.showIf == null || field.showIf(itemToEdit))
                        .map(field => typeof field === 'string' ? (
                            <Typography
                                key={field}
                                variant="subtitle1"
                                sx={{mt: 2}}
                            >
                                {field}
                            </Typography>
                        ) : (
                            <Box key={field.label}>
                                {
                                    field.isRichtext ? (
                                        <Box sx={{mt: 3}}>
                                            <RichTextEditorComponentView
                                                value={itemToEdit[field.field] == null ? '' : itemToEdit[field.field] as unknown as string}
                                                onChange={(value) => {
                                                    setItemToEdit({
                                                        ...itemToEdit,
                                                        [field.field]: value,
                                                    });
                                                }}
                                                label={field.label + (field.required ? ' *' : '')}
                                            />
                                            {
                                                field.helperText &&
                                                <FormHelperText>
                                                    {field.helperText}
                                                </FormHelperText>
                                            }
                                        </Box>
                                    ) : (field.isOptions ? (
                                            <FormControl
                                                error={missingRequiredFields.includes(field.field) || maxedOutFields.includes(field.field)}
                                            >
                                                <InputLabel>
                                                    {field.label} {field.required ? '*' : ''}
                                                </InputLabel>
                                                <Select
                                                    value={itemToEdit[field.field] == null ? '' : itemToEdit[field.field] as unknown as string}
                                                    label={field.label}
                                                    onChange={event => {
                                                        setItemToEdit({
                                                            ...itemToEdit,
                                                            [field.field]: event.target.value,
                                                        });
                                                    }}
                                                >
                                                    {
                                                        (field.options ?? []).map(option => (
                                                            <MenuItem
                                                                key={option}
                                                                value={option}
                                                            >
                                                                {option}
                                                            </MenuItem>
                                                        ))
                                                    }
                                                </Select>
                                                {
                                                    field.helperText &&
                                                    <FormHelperText>
                                                        {field.helperText}
                                                    </FormHelperText>
                                                }
                                            </FormControl>
                                        ) : (
                                            <TextField
                                                label={field.label + (field.required ? ' *' : '')}
                                                value={itemToEdit[field.field] == null ? '' : itemToEdit[field.field]}
                                                placeholder={field.placeholder}
                                                multiline={field.isMultiline}
                                                rows={field.isMultiline ? 3 : 1}
                                                onChange={(event) => {
                                                    setItemToEdit({
                                                        ...itemToEdit,
                                                        [field.field]: event.target.value ?? '',
                                                    });
                                                }}
                                                onBlur={() => {
                                                    if (itemToEdit[field.field] != null) {
                                                        setItemToEdit({
                                                            ...itemToEdit,
                                                            [field.field]: (itemToEdit[field.field] as unknown as string).trim(),
                                                        });
                                                    }
                                                }}
                                                error={missingRequiredFields.includes(field.field) || maxedOutFields.includes(field.field)}
                                                helperText={field.helperText}
                                            />
                                        )
                                    )
                                }
                            </Box>
                        ))
                }
                {
                    missingRequiredFields.length > 0 &&
                    <Alert severity="error">
                        <AlertTitle>
                            {_.missingRequiredFieldsTitle}
                        </AlertTitle>
                        {_.missingRequiredFieldsText}
                    </Alert>
                }
                {
                    maxedOutFields.length > 0 &&
                    <Alert severity="error">
                        <AlertTitle>
                            {_.maxedOutFieldsTitle}
                        </AlertTitle>
                        {_.maxedOutFieldsText}
                    </Alert>
                }
            </DialogContent>
            <DialogActions
                sx={{
                    pb: 3,
                    px: 3,
                    justifyContent: 'flex-start',
                }}
            >
                <Button
                    onClick={handleSave}
                    size="large"
                    variant="outlined"
                >
                    {_.saveLabel}
                </Button>
                <Button
                    onClick={handleClose}
                    sx={{
                        ml: 'auto!important',
                    }}
                    size="large"
                >
                    {_.cancelLabel}
                </Button>
            </DialogActions>
        </Dialog>
    );
}
