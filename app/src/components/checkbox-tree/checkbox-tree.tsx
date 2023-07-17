import {Box, Checkbox, FormControlLabel} from '@mui/material';
import React from 'react';
import {type CheckboxTreeProps} from './checkbox-tree-props';
import {CheckboxTreeItem} from './checkbox-tree-item';

export function CheckboxTree({
                                 options,
                                 value,
                                 onChange,
                                 disabled,
                             }: CheckboxTreeProps) {
    return (
        <Box>
            {
                options.map((opt) => typeof opt === 'string' ?
                    (
                        <FormControlLabel
                            sx={{display: 'block'}}
                            key={opt}
                            label={opt}
                            control={
                                <Checkbox
                                    checked={value.includes(opt)}
                                    onChange={(event) => {
                                        if (event.target.checked) {
                                            onChange([...value, opt]);
                                        } else {
                                            onChange(value.filter((v) => v !== opt));
                                        }
                                    }}
                                    disabled={disabled}
                                />
                            }
                        />
                    ) :
                    (
                        <CheckboxTreeItem
                            key={opt.label}
                            item={opt}
                            value={value}
                            onChange={onChange}
                            disabled={disabled}
                        />
                    ))
            }

        </Box>
    );
}

