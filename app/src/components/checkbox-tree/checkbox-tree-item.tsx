import {CheckboxTreeOptionItem} from './checkbox-tree-option-item';
import React, {useState} from 'react';
import {Box, Checkbox, FormControlLabel, IconButton} from '@mui/material';
import {CheckboxTree} from './checkbox-tree';
import {CheckboxTreeOption} from './checkbox-tree-option';
import ExpandLessOutlinedIcon from '@mui/icons-material/ExpandLessOutlined';
import ExpandMoreOutlinedIcon from '@mui/icons-material/ExpandMoreOutlined';

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

export function CheckboxTreeItem({
                                     item,
                                     value,
                                     onChange,
                                     disabled,
                                 }: {
    item: CheckboxTreeOptionItem,
    value: string[],
    onChange: (value: string[]) => void;
    disabled: boolean;
}): JSX.Element {
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
                            disabled={disabled}
                        />
                    }
                />
                {
                    !value.includes(item.label) &&
                    <IconButton
                        sx={{ml: 'auto'}}
                        onClick={() => setIsExtended(!isExtended)}
                    >
                        {
                            isExtended ?
                                <ExpandLessOutlinedIcon
                                    fontSize="small"
                                />
                                :
                                <ExpandMoreOutlinedIcon
                                    fontSize="small"
                                />
                        }
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
                        disabled={disabled}
                    />
                </Box>
            }
        </Box>
    );
}
