import React, {useEffect, useState} from 'react';
import {FormControl, InputLabel, MenuItem, Select, TextField, Typography} from '@mui/material';
import {BaseEditorProps} from "../../editors/base-editor";
import {RootElement} from "../../models/elements/root-element";
import {Department} from "../../models/entities/department";
import {DepartmentsService} from "../../services/departments.service";

export function RootComponentEditorTabLegal(props: BaseEditorProps<RootElement>) {
    const [vendors, setVendors] = useState<Department[]>([]);

    useEffect(() => {
        DepartmentsService.list()
            .then(data => {
                setVendors(data._embedded.departments);
            });
    }, []);

    return (
        <>
            <Typography
                variant="h6"
            >
                Rechtliches
            </Typography>

            <FormControl
                margin="normal"
            >
                <InputLabel>Text für Impressum</InputLabel>
                <Select
                    label="Text für Impressum"
                    value={props.element.imprint ?? ''}
                    onChange={event => props.onPatch({
                        imprint: event.target.value as number,
                    })}
                >
                    {
                        vendors.map((vendor) => (
                            <MenuItem
                                key={vendor.id}
                                value={vendor.id}
                            >
                                {vendor.name}
                            </MenuItem>
                        ))
                    }
                </Select>
            </FormControl>

            <FormControl
                margin="normal"
            >
                <InputLabel>Text für Datenschutzerklärung</InputLabel>
                <Select
                    label="Text für Datenschutzerklärung"
                    value={props.element.privacy ?? ''}
                    onChange={event => props.onPatch({
                        privacy: event.target.value as number,
                    })}
                >
                    {
                        vendors.map((vendor) => (
                            <MenuItem
                                key={vendor.id}
                                value={vendor.id}
                            >
                                {vendor.name}
                            </MenuItem>
                        ))
                    }
                </Select>
            </FormControl>

            <FormControl
                margin="normal"
            >
                <InputLabel>Text für Erklärung der Barrierefreiheit</InputLabel>
                <Select
                    label="Text für Erklärung der Barrierefreiheit"
                    value={props.element.accessibility ?? ''}
                    onChange={event => props.onPatch({
                        accessibility: event.target.value as number,
                    })}
                >
                    {
                        vendors.map((vendor) => (
                            <MenuItem
                                key={vendor.id}
                                value={vendor.id}
                            >
                                {vendor.name}
                            </MenuItem>
                        ))
                    }
                </Select>
            </FormControl>

            <Typography
                variant="h6"
                sx={{mt: 4}}
            >
                Informationen zum Datenschutz
            </Typography>

            <TextField
                value={props.element.privacyText ?? ''}
                label="Text für Datenschutz-Einwilligung in den Allgemeinen Informationen"
                fullWidth
                margin="normal"
                multiline
                rows={4}
                onChange={event => props.onPatch({
                    privacyText: event.target.value,
                })}
            />
        </>
    );
}
