import React, {useEffect, useMemo, useState} from 'react';
import {Alert, Box, Button, ButtonBase, Dialog, DialogActions, DialogContent, Grid, Tooltip, Typography} from '@mui/material';
import {StepIcons} from '../../data/step-icons';
import {type StepIcon} from '../../models/step-icon';
import Fuse from 'fuse.js';
import {DialogTitleWithClose} from '../../components/dialog-title-with-close/dialog-title-with-close';
import {SearchInput} from '../../components/search-input/search-input';

interface IconPickerDialogProps {
    open: boolean;
    onClose: () => void;
    onSelect: (iconId: string) => void;
    selectedIconId?: string;
    title?: string;
    showLabels?: boolean;
    autoSelect?: boolean;
    icons?: StepIcon[];
}

export function IconPickerDialog({
                                     open,
                                     onClose,
                                     onSelect,
                                     selectedIconId,
                                     title,
                                     showLabels = false,
                                     autoSelect = false,
                                     icons = StepIcons,
                                 }: IconPickerDialogProps) {
    const [selected, setSelected] = useState<string | undefined>(selectedIconId);
    const [search, setSearch] = useState('');

    const fuse = useMemo(() => new Fuse(icons, {
        keys: ['label'],
        threshold: 0.4,
    }), [icons]);

    const filteredIcons = search
        ? fuse.search(search).map(result => result.item)
        : icons;

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
            setSearch('');
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
                <SearchInput
                    value={search}
                    onChange={setSearch}
                    label="Symbol suchen"
                    placeholder="Name des Symbols eingeben"
                    size="small"
                    sx={{
                        mt: 1,
                        mb: 2,
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
                                        key={icon.id}
                                        size={{
                                            xs: 12,
                                            sm: 6,
                                            md: 4
                                        }}>
                                        <Tooltip
                                            title={!showLabels ? icon.label : ''}
                                            arrow
                                        >
                                            <ButtonBase
                                                onClick={() => handleSelect(icon.id)}
                                                aria-label={icon.label}
                                                aria-pressed={isSelected}
                                                sx={(theme) => ({
                                                    border: '1px solid',
                                                    borderColor: isSelected ? 'primary.main' : 'divider',
                                                    borderRadius: 1,
                                                    px: 2,
                                                    py: 1.5,
                                                    display: 'flex',
                                                    alignItems: 'center',
                                                    justifyContent: showLabels ? 'flex-start' : 'center',
                                                    gap: showLabels ? 2 : 0,
                                                    height: showLabels ? 64 : 88,
                                                    width: '100%',
                                                    flexDirection: showLabels ? 'row' : 'column',
                                                    transition: theme.transitions.create(['border-color', 'background-color', 'box-shadow']),
                                                    backgroundColor: isSelected ? 'action.selected' : 'background.paper',
                                                    boxShadow: isSelected ? `inset 0 0 0 1px ${theme.palette.primary.main}` : 'none',
                                                    '&:hover': {
                                                        borderColor: 'primary.main',
                                                        backgroundColor: 'action.hover',
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
                                            </ButtonBase>
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
