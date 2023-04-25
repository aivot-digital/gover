import {Box, Checkbox, FormControlLabel} from "@mui/material";
import React from "react";

export type CheckboxTreeOption = {
    label: string;
    children: CheckboxTreeOption[];
} | string

interface CheckboxTreeProps {
    options: CheckboxTreeOption[];
    value: string[];
    onChange: (value: string[]) => void;
}

function getChildValues(treeOption: CheckboxTreeOption): string[] {
    const vals: string[] = [];

    if (typeof treeOption !== 'string') {
        for (const opt of treeOption.children) {
            if (typeof opt === 'string') {
                vals.push(opt);
            } else {
                vals.push(opt.label);
                vals.push(...getChildValues(opt));
            }
        }
    }

    return vals;
}

export function CheckboxTree({options, value, onChange}: CheckboxTreeProps) {
    return (
        <Box>
            {
                options.map(opt => typeof opt === 'string' ? (
                    <FormControlLabel
                        sx={{display: 'block'}}
                        key={opt}
                        label={opt}
                        control={
                            <Checkbox
                                checked={value.includes(opt)}
                                onChange={event => {
                                    if (event.target.checked) {
                                        onChange([...value, opt]);
                                    } else {
                                        onChange(value.filter(v => v !== opt));
                                    }
                                }}
                            />
                        }
                    />
                ) : (
                    <Box key={opt.label}>
                        <FormControlLabel
                            label={opt.label}
                            control={
                                <Checkbox
                                    checked={value.includes(opt.label)}
                                    onChange={event => {
                                        if (event.target.checked) {
                                            const childValues = getChildValues(opt);
                                            onChange([...value, opt.label].filter(val => !childValues.includes(val)));
                                        } else {
                                            onChange(value.filter(v => v !== opt.label));
                                        }
                                    }}
                                />
                            }
                        />

                        {
                            !value.includes(opt.label) &&
                            <Box sx={{pl: 2}}>
                                <CheckboxTree
                                    options={opt.children}
                                    value={value}
                                    onChange={onChange}
                                />
                            </Box>
                        }
                    </Box>
                ))
            }

        </Box>
    );
}
