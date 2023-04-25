import {Box, Checkbox, FormControlLabel, IconButton} from "@mui/material";
import React, {useState} from "react";
import {FontAwesomeIcon} from "@fortawesome/react-fontawesome";
import {faChevronDown, faChevronUp} from "@fortawesome/pro-light-svg-icons";

type CheckboxTreeOptionItem = {
    label: string;
    children: CheckboxTreeOption[];
};

export type CheckboxTreeOption = CheckboxTreeOptionItem | string;

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
                    <CheckboxTreeItem
                        item={opt}
                        value={value}
                        onChange={onChange}
                    />
                ))
            }

        </Box>
    );
}

function CheckboxTreeItem({item, value, onChange}: {
    item: CheckboxTreeOptionItem,
    value: string[],
    onChange: (value: string[]) => void;
}) {
    const [isExtended, setIsExtended] = useState(false);
    const childValues = getChildValues(item);

    return (
        <Box key={item.label}>
            <Box sx={{display: 'flex'}}>
                <FormControlLabel
                    label={item.label}
                    control={
                        <Checkbox
                            checked={value.includes(item.label)}
                            onChange={event => {
                                if (event.target.checked) {
                                    onChange([...value, item.label].filter(val => !childValues.includes(val)));
                                } else {
                                    onChange(value.filter(v => v !== item.label));
                                }
                            }}
                            indeterminate={value.some(val => childValues.includes(val))}
                        />
                    }
                />
                {
                    !value.includes(item.label) &&
                    <IconButton
                        sx={{ml: 'auto'}}
                        onClick={() => setIsExtended(!isExtended)}
                    >
                        <FontAwesomeIcon
                            size="xs"
                            icon={isExtended ?  faChevronUp : faChevronDown}
                        />
                    </IconButton>
                }
            </Box>

            {
                isExtended &&
                !value.includes(item.label) &&
                <Box sx={{pl: 4}}>
                    <CheckboxTree
                        options={item.children}
                        value={value}
                        onChange={onChange}
                    />
                </Box>
            }
        </Box>
    );
}
