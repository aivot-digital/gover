import {
    ReplicatingContainerElement
} from '../../models/elements/form-elements/layout-elements/replicating-container-element';
import {ViewDispatcherComponent} from '../view-dispatcher.component';
import {Box, Button, FormHelperText, FormLabel, Grid, Typography} from '@mui/material';
import {stringOrDefault} from '../../utils/string-or-default';
import {faPlusCircle, faTrashCanXmark} from '@fortawesome/pro-light-svg-icons';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {BaseViewProps} from '../_lib/base-view-props';
import {useCallback, useEffect} from 'react';
import {generateElementId} from '../../utils/generate-element-id';

export function ReplicatingContainerView({
                                             setValue,
                                             element,
                                             value
                                         }: BaseViewProps<ReplicatingContainerElement, string[]>) {
    useEffect(() => {
        if (element.minimumRequiredSets != null && element.minimumRequiredSets > 0 && (value == null || value.length < element.minimumRequiredSets)) {
            setValue(Array.from({length: element.minimumRequiredSets}, (_, i) => generateElementId(i.toString())));
        }
    }, [setValue, value, element]);

    const handleAdd = useCallback(() => {
        setValue([...(value ?? []), generateElementId('')]);
    }, [setValue, value]);

    const handleDelete = useCallback((id: string) => {
        setValue((value ?? []).filter((val: string) => val !== id));
    }, [setValue, value]);

    return (
        <Box sx={{mt: 2}}>
            {
                element.label &&
                <FormLabel>
                    {element.label}
                </FormLabel>
            }
            {
                element.hint &&
                <FormHelperText>
                    {element.hint}
                </FormHelperText>
            }
            {
                value &&
                value.map((val: string, valueIndex: number) => (
                    <Box
                        key={val}
                        sx={{
                            my: 2,
                            px: 3,
                            py: 2.4,
                            border: '1px solid #D4D4D4',
                        }}
                    >
                        <Box sx={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
                            <Typography
                                variant="h6"
                                sx={{fontSize: '1.125rem'}}
                            >
                                {
                                    (element.headlineTemplate ?? '').replace('#', (valueIndex + 1).toFixed())
                                }
                            </Typography>
                            {
                                !element.disabled &&
                                (element.minimumRequiredSets == null || valueIndex >= element.minimumRequiredSets) &&
                                <div>
                                    <Button
                                        color="error"
                                        size={'small'}
                                        endIcon={<FontAwesomeIcon
                                            icon={faTrashCanXmark}
                                            style={{marginTop: '-4px'}}
                                        />}
                                        onClick={() => handleDelete(val)}
                                        disabled={element.minimumRequiredSets != null && element.minimumRequiredSets > 0 && (value ?? []).length <= element.minimumRequiredSets}
                                    >
                                        {
                                            stringOrDefault(element.removeLabel, 'Datensatz l??schen')
                                        }
                                    </Button>
                                </div>
                            }
                        </Box>
                        <Grid
                            container
                            spacing={2}
                        >
                            {
                                (element.children ?? []).map((child, childIndex) => (
                                    <ViewDispatcherComponent
                                        key={childIndex}
                                        model={child}
                                        idPrefix={`${element.id}_${val}_`}
                                    />
                                ))
                            }
                        </Grid>
                    </Box>
                ))
            }
            {
                !element.disabled &&
                <div>
                    <Button
                        startIcon={<FontAwesomeIcon
                            icon={faPlusCircle}
                            style={{marginTop: '-2px'}}
                        />}
                        sx={{mt: 2, mb: 3}}
                        onClick={handleAdd}
                        variant={'outlined'}
                        disabled={element.maximumSets != null && element.maximumSets > 0 && (value ?? []).length >= element.maximumSets}
                    >
                        {
                            stringOrDefault(element.addLabel, 'Datensatz hinzuf??gen')
                        }
                    </Button>
                </div>
            }
        </Box>
    );
}

