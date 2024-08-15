import React, {useState} from 'react';
import {Box, Checkbox, FormControlLabel, IconButton} from '@mui/material';
import {CheckboxTree} from './checkbox-tree';
import {type CheckboxTreeOption} from './checkbox-tree-option';
import ExpandLessOutlinedIcon from '@mui/icons-material/ExpandLessOutlined';
import ExpandMoreOutlinedIcon from '@mui/icons-material/ExpandMoreOutlined';
import {type CheckboxTreeItemProps} from './checkbox-tree-item-props';

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

export function CheckboxTreeItem(props: CheckboxTreeItemProps): JSX.Element {
    const [isExtended, setIsExtended] = useState(false);
    const childValues = getChildValues(props.item);

    return (
        <Box key={props.item.label}>
            <Box sx={{display: 'flex'}}>
                <FormControlLabel
                    label={props.item.label}
                    control={
                        <Checkbox
                            checked={props.value.includes(props.item.label)}
                            onChange={(event) => {
                                if (event.target.checked) {
                                    props.onChange([...props.value, props.item.label].filter((val) => !childValues.includes(val)));
                                } else {
                                    props.onChange(props.value.filter((v) => v !== props.item.label));
                                }
                            }}
                            indeterminate={props.value.some((val) => childValues.includes(val))}
                            disabled={props.disabled}
                        />
                    }
                />
                {
                    !props.value.includes(props.item.label) &&
                    <IconButton
                        sx={{ml: 'auto'}}
                        onClick={() => {
                            setIsExtended(!isExtended);
                        }}
                    >
                        {
                            isExtended ?
                                <ExpandLessOutlinedIcon
                                    fontSize="small"
                                /> :
                                <ExpandMoreOutlinedIcon
                                    fontSize="small"
                                />
                        }
                    </IconButton>
                }
            </Box>

            {
                isExtended &&
                !props.value.includes(props.item.label) &&
                <Box sx={{pl: 4}}>
                    <CheckboxTree
                        options={props.item.children}
                        value={props.value}
                        onChange={props.onChange}
                        disabled={props.disabled}
                    />
                </Box>
            }
        </Box>
    );
}
