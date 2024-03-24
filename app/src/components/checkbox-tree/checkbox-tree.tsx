import React from 'react';
import {Checkbox, FormControlLabel} from '@mui/material';
import {type CheckboxTreeProps} from './checkbox-tree-props';
import {CheckboxTreeItem} from './checkbox-tree-item';

export function CheckboxTree(props: CheckboxTreeProps): JSX.Element {
    return (
        <>
            {
                props.options.map((opt) => typeof opt === 'string' ?
                    (
                        <FormControlLabel
                            sx={{display: 'block'}}
                            key={opt}
                            label={opt}
                            control={
                                <Checkbox
                                    checked={props.value.includes(opt)}
                                    onChange={(event) => {
                                        if (event.target.checked) {
                                            props.onChange([...props.value, opt]);
                                        } else {
                                            props.onChange(props.value.filter((v) => v !== opt));
                                        }
                                    }}
                                    disabled={props.disabled}
                                />
                            }
                        />
                    ) :
                    (
                        <CheckboxTreeItem
                            key={opt.label}
                            item={opt}
                            value={props.value}
                            onChange={props.onChange}
                            disabled={props.disabled}
                        />
                    ))
            }
        </>
    );
}

