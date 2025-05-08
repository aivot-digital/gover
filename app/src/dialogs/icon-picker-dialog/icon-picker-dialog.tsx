import React, {useEffect, useMemo, useState} from 'react';
import {Alert, Box, Button, Dialog, DialogActions, DialogContent, Grid, TextField, Tooltip, Typography} from '@mui/material';
import {StepIcons} from '../../data/step-icons';
import Fuse from 'fuse.js';
import SearchOutlinedIcon from '@mui/icons-material/SearchOutlined';
import {DialogTitleWithClose} from '../../components/dialog-title-with-close/dialog-title-with-close';

interface IconPickerDialogProps {
    open: boolean;
    onClose: () => void;
    onSelect: (iconId: string) => void;
    selectedIconId?: string;
    title?: string;
    showLabels?: boolean;
    autoSelect?: boolean;
}

export function IconPickerDialog({
                                     open,
                                     onClose,
                                     onSelect,
                                     selectedIconId,
                                     title,
                                     showLabels = false,
                                     autoSelect = false,
                                 }: IconPickerDialogProps): JSX.Element {
    const [selected, setSelected] = useState<string | undefined>(selectedIconId);
    const [search, setSearch] = useState('');

    const fuse = useMemo(() => new Fuse(StepIcons, {
        keys: ['label'],
        threshold: 0.4,
    }), []);

    const filteredIcons = search
        ? fuse.search(search).map(result => result.item)
        : StepIcons;

    const handleSelect = (id: string) => {
        if (autoSelect) {
            onSelect(id);
            onClose();
        } else {
            setSelected(id);
        }
    };

    const handleConfirm = () => {
        if (selected) onSelect(selected);
        onClose();
    };

    useEffect(() => {
        if (open) {
            setSelected(selectedIconId);
        }
    }, [open, selectedIconId]);

    return (
        <Dialog
            open={open}
            onClose={onClose}
            maxWidth="md"
            fullWidth
        >
            <DialogTitleWithClose
                onClose={onClose}
                closeTooltip={'Schließen'}
            >
                {title ?? 'Icon auswählen'}
            </DialogTitleWithClose>
            <DialogContent sx={{maxHeight: '70vh', overflow: 'hidden', display: 'flex', flexDirection: 'column'}}>
                <TextField
                    fullWidth
                    variant="outlined"
                    placeholder="Symbol suchen..."
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                    sx={{mt: 0, mb: 2}}
                    size={'small'}
                    InputProps={{
                        startAdornment: (
                            <SearchOutlinedIcon sx={{mr: 1}} />
                        ),
                    }}
                />
                <Box sx={{overflowY: 'auto', height: '70vh', p: 1}}>
                    {filteredIcons.length === 0 ? (
                        <Alert severity="info">Keine passenden Symbole gefunden.</Alert>
                    ) : (
                        <Grid
                            container
                            spacing={2}
                        >
                            {filteredIcons.map((icon) => {
                                const IconComponent = icon.def;
                                const isSelected = selected === icon.id;
                                return (
                                    <Grid
                                        item
                                        xs={12}
                                        sm={6}
                                        md={4}
                                        key={icon.id}
                                    >
                                        <Tooltip
                                            title={!showLabels ? icon.label : ''}
                                            arrow
                                        >
                                            <Box
                                                onClick={() => handleSelect(icon.id)}
                                                sx={(theme) => ({
                                                    border: '1px solid',
                                                    borderColor: isSelected ? 'primary.main' : 'grey.400',
                                                    borderRadius: 1,
                                                    px: 2,
                                                    py: 1.5,
                                                    cursor: 'pointer',
                                                    display: 'flex',
                                                    alignItems: 'center',
                                                    justifyContent: showLabels ? 'flex-start' : 'center',
                                                    gap: showLabels ? 2 : 0,
                                                    height: showLabels ? 64 : 88,
                                                    flexDirection: showLabels ? 'row' : 'column',
                                                    transition: 'all 0.2s ease-in-out',
                                                    backgroundColor: isSelected ? 'grey.100' : 'background.paper',
                                                    boxShadow: isSelected ? `inset 0 0 0 2px ${theme.palette.primary.light}` : 'none',
                                                    '&:hover': {
                                                        borderColor: 'grey.400',
                                                        backgroundColor: 'grey.100',
                                                        transform: 'scale(1.03)',
                                                        transition: 'transform 0.15s ease-in-out',
                                                    },
                                                })}
                                            >
                                                <IconComponent sx={{fontSize: '2rem', color: 'primary.main'}} />
                                                {showLabels && (
                                                    <Typography
                                                        variant="body2"
                                                        noWrap
                                                        sx={{overflow: 'hidden', textOverflow: 'ellipsis'}}
                                                    >
                                                        {icon.label}
                                                    </Typography>
                                                )}
                                            </Box>
                                        </Tooltip>
                                    </Grid>
                                );
                            })}
                        </Grid>
                    )}
                </Box>
            </DialogContent>
            {!autoSelect && (
                <DialogActions>
                    <Button
                        onClick={handleConfirm}
                        variant="contained"
                        disabled={!selected}
                    >
                        Icon übernehmen
                    </Button>
                    <Button onClick={onClose}>Abbrechen</Button>
                </DialogActions>
            )}
        </Dialog>
    );
}