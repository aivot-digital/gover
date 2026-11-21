import {ReactNode, useMemo, useRef, useState} from 'react';
import {Box, Divider, Typography} from '@mui/material';
import Fuse from 'fuse.js';
import {SelectionDialogShell} from './selection-dialog-shell';
import {SelectionListRow} from './selection-list-row';
import {SelectionDetailsPanel} from './selection-details-panel';
import {SearchInput} from '../search-input/search-input';
import {useRetainedDialogValue} from '../../hooks/use-retained-dialog-value';
import {useNormalizedReactId} from '../../hooks/use-normalized-react-id';

export interface SearchSelectionOption<T> {
    id: string;
    title: string;
    description?: ReactNode;
    searchTerms: string[];
    icon: ReactNode;
    value: T;
}

interface SearchSelectionDialogProps<T> {
    open: boolean;
    title: string;
    searchLabel: string;
    detailLabel: string;
    options: SearchSelectionOption<T>[];
    primaryActionLabel: string;
    primaryActionIcon: ReactNode;
    noOptionsMessage: ReactNode;
    noSearchResultsMessage: string;
    renderDetails: (value: T) => ReactNode;
    onSelect: (value: T) => void;
    onClose: () => void;
    onExited?: () => void;
}

export function SearchSelectionDialog<T>(props: SearchSelectionDialogProps<T>) {
    const retained = useRetainedDialogValue(props.open, props);
    const searchRef = useRef<HTMLInputElement>(null);
    const detailsTriggerRef = useRef<HTMLElement | null>(null);
    const detailsId = `${useNormalizedReactId()}-details`;
    const [search, setSearch] = useState('');
    const [selectedId, setSelectedId] = useState<string | null>(null);
    const results = useMemo(() => {
        if (search.trim() === '') return retained.options;
        return new Fuse(retained.options, {
            keys: ['title', 'searchTerms'],
            minMatchCharLength: 3,
            threshold: 0.25,
        }).search(search.trim()).map(({item}) => item);
    }, [retained.options, search]);
    const selected = retained.options.find(option => option.id === selectedId);
    const select = (value: T) => {
        if (props.open) props.onSelect(value);
    };
    const closeDetails = () => {
        setSelectedId(null);
        if (detailsTriggerRef.current?.isConnected) detailsTriggerRef.current.focus();
        else searchRef.current?.focus();
    };

    return <SelectionDialogShell open={props.open}
                                 title={retained.title}
                                 onClose={props.onClose}
                                 initialFocusRef={searchRef}
                                 tabs={[]}
                                 activeTab={0}
                                 onTabChange={() => undefined}
                                 showDetailsPanel={selected != null}
                                 onExited={() => {
                                     setSearch('');
                                     setSelectedId(null);
                                     detailsTriggerRef.current = null;
                                     props.onExited?.();
                                 }}
                                 detailsPanel={selected != null && <Box id={detailsId}
                                                                        role="region"
                                                                        aria-label={`Details: ${selected.title}`}
                                                                        sx={{
                                                                            display: 'flex',
                                                                            flexDirection: 'column',
                                                                            minHeight: 0,
                                                                            height: '100%',
                                                                        }}>
                                     <SelectionDetailsPanel icon={selected.icon}
                                                            title={selected.title}
                                                            label={retained.detailLabel}
                                                            description={selected.description}
                                                            primaryActionLabel={retained.primaryActionLabel}
                                                            primaryActionIcon={retained.primaryActionIcon}
                                                            onPrimaryAction={() => select(selected.value)}
                                                            onClose={closeDetails}>
                                         {retained.renderDetails(selected.value)}
                                     </SelectionDetailsPanel>
                                 </Box>}>
        <Box sx={{
            p: 2,
            borderBottom: 1,
            borderColor: 'divider',
        }}>
            <SearchInput inputRef={searchRef}
                         value={search}
                         onChange={setSearch}
                         label={retained.searchLabel}
                         placeholder="Suchen…"/>
        </Box>
        <Box sx={{
            flex: 1,
            minHeight: 0,
            overflowY: 'auto',
        }}>
            {results.length === 0 ? <Box sx={{p: 3}}>
                {search.trim() === '' ? retained.noOptionsMessage :
                    <Typography>{retained.noSearchResultsMessage}</Typography>}
            </Box> : <Box component="ul"
                          aria-label={retained.title}
                          sx={{
                              m: 0,
                              p: 0,
                              listStyle: 'none',
                          }}>
                {results.map((option, index) => <Box component="li"
                                                     key={option.id}
                                                     aria-label={option.title}>
                    {index > 0 && <Divider/>}
                    <SelectionListRow icon={option.icon}
                                      title={option.title}
                                      description={option.description}
                                      selected={selectedId === option.id}
                                      primaryActionLabel={retained.primaryActionLabel}
                                      primaryActionAriaLabel={`${option.title}: ${retained.primaryActionLabel}`}
                                      primaryActionIcon={retained.primaryActionIcon}
                                      onPrimaryAction={() => select(option.value)}
                                      detailsAriaLabel={`${option.title}: Details anzeigen`}
                                      detailsExpanded={selectedId === option.id}
                                      detailsControls={selectedId === option.id ? detailsId : undefined}
                                      onShowDetails={() => {
                                          detailsTriggerRef.current = document.activeElement instanceof HTMLElement ? document.activeElement : null;
                                          setSelectedId(option.id);
                                      }}/>
                </Box>)}
            </Box>}
        </Box>
    </SelectionDialogShell>;
}
