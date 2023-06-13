import React, {useEffect, useState} from 'react';
import {FormControl, InputLabel, MenuItem, Select, TextField, Typography} from '@mui/material';
import {BaseEditorProps} from "../../editors/base-editor";
import {RootElement} from "../../models/elements/root-element";
import {Department} from "../../models/entities/department";
import {DepartmentsService} from "../../services/departments.service";
import {TextFieldComponent} from "../text-field/text-field-component";

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

            <TextFieldComponent
                value={props.element.privacyText ?? ''}
                label="Text für Datenschutz-Einwilligung in den Allgemeinen Informationen"
                multiline
                onChange={val => props.onPatch({
                    privacyText: val,
                })}
            />

            <Typography>
                Wenn Sie innerhalb der Informationen zum Datenschutz auf die Datenschutzerklärung verlinken möchten, umschließen Sie den entsprechenden Text für den Link mit {'{privacy}'} und {'{/privacy}'}.
            </Typography>

            <Typography sx={{mt: 2}}>
                Z.B.: <strong>Hier finden Sie die {'{privacy}Hinweise zum Datenschutz{/privacy}'}.</strong>
            </Typography>
        </>
    );
}
