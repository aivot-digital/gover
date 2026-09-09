import {Accordion, AccordionDetails, AccordionSummary, Box, Table, TableBody, TableCell, TableHead, TableRow, Typography} from '@mui/material';
import KeyboardArrowDown from '@aivot/mui-material-symbols-400-n25-outlined/KeyboardArrowDown';
import TextFields from '@aivot/mui-material-symbols-400-n25-outlined/TextFields';
import Keyboard from '@aivot/mui-material-symbols-400-n25-outlined/Keyboard';
import DataObject from '@aivot/mui-material-symbols-400-n25-outlined/DataObject';
import Link from '@aivot/mui-material-symbols-400-n25-outlined/Link';
import AccountTree from '@aivot/mui-material-symbols-400-n25-outlined/AccountTree';
import DynamicForm from '@aivot/mui-material-symbols-400-n25-outlined/DynamicForm';
import Schema from '@aivot/mui-material-symbols-400-n25-outlined/Schema';
import Code from '@aivot/mui-material-symbols-400-n25-outlined/Code';
import Javascript from '@aivot/mui-material-symbols-400-n25-outlined/Javascript';
import Function from '@aivot/mui-material-symbols-400-n25-outlined/Function';
import Toc from '@aivot/mui-material-symbols-400-n25-outlined/Toc';
import FormatListBulleted from '@aivot/mui-material-symbols-400-n25-outlined/FormatListBulleted';
import ViewList from '@aivot/mui-material-symbols-400-n25-outlined/ViewList';

interface IconOption {
    name: string;
    icon: typeof TextFields;
}

const proposals: {label: string; recommendation: IconOption; alternatives: IconOption[]}[] = [
    {label: 'Wert', recommendation: {name: 'TextFields', icon: TextFields}, alternatives: [{name: 'Keyboard', icon: Keyboard}]},
    {label: 'Variable', recommendation: {name: 'DataObject', icon: DataObject}, alternatives: [{name: 'Link', icon: Link}]},
    {label: 'Ausdruck (No-Code)', recommendation: {name: 'DynamicForm', icon: DynamicForm}, alternatives: [{name: 'AccountTree', icon: AccountTree}, {name: 'Schema', icon: Schema}]},
    {label: 'Skript (Low-Code)', recommendation: {name: 'Code', icon: Code}, alternatives: [{name: 'Javascript', icon: Javascript}]},
    {label: 'Dynamischer Text', recommendation: {name: 'Function', icon: Function}, alternatives: [{name: 'DataObject', icon: DataObject}, {name: 'Code', icon: Code}]},
    {label: 'Formularstruktur', recommendation: {name: 'AccountTree', icon: AccountTree}, alternatives: [{name: 'Toc', icon: Toc}, {name: 'FormatListBulleted', icon: FormatListBulleted}, {name: 'ViewList', icon: ViewList}]},
];

function IconSample({name, icon: Icon}: IconOption) {
    return <Box component="span" sx={{display: 'inline-flex', alignItems: 'center', gap: 1, color: 'text.secondary'}}>
        <Icon aria-hidden="true" sx={{fontSize: 20}}/>
        <Typography component="span" variant="body2" sx={{color: 'text.primary'}}>{name}</Typography>
    </Box>;
}

export function InputModeIconComparison() {
    return (
        <Accordion disableGutters elevation={0} sx={{mt: 2, bgcolor: 'transparent', '&::before': {display: 'none'}}}>
            <AccordionSummary id="input-mode-icons-heading" aria-controls="input-mode-icons-content" expandIcon={<KeyboardArrowDown/>} sx={{px: 0}}>
                <Typography component="h3" variant="h6">Icon-Vorschläge</Typography>
            </AccordionSummary>
            <AccordionDetails sx={{px: 0}}>
                <Table size="small" aria-label="Icon-Vorschläge für Eingabemodi und Formularstruktur" sx={{tableLayout: 'fixed'}}>
                    <TableHead>
                        <TableRow>
                            <TableCell sx={{width: '28%'}}>Bereich</TableCell>
                            <TableCell sx={{width: '30%'}}>Empfehlung</TableCell>
                            <TableCell>Alternativen</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {proposals.map(({label, recommendation, alternatives}) => (
                            <TableRow key={label}>
                                <TableCell component="th" scope="row">{label}</TableCell>
                                <TableCell><IconSample {...recommendation}/></TableCell>
                                <TableCell>
                                    <Box sx={{display: 'flex', flexWrap: 'wrap', columnGap: 3, rowGap: 1}}>
                                        {alternatives.map((option) => <IconSample key={option.name} {...option}/>)}
                                    </Box>
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </AccordionDetails>
        </Accordion>
    );
}
