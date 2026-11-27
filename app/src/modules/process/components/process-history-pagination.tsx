import {Pagination, Stack, Typography} from '@mui/material';
import {type Page} from '../../../models/dtos/page';

export function ProcessHistoryPagination({
                                             page,
                                             label,
                                             disabled,
                                             onChange,
                                         }: {
    page: Page<unknown>['page'];
    label: string;
    disabled: boolean;
    onChange: (page: number) => void;
}) {
    if (page.totalPages <= 1) return null;
    return <Stack direction="row"
                  sx={{
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      gap: 2,
                      mt: 2,
                  }}>
        <Typography variant="body2"
                    sx={{color: 'text.secondary'}}>
            {page.number * page.size + 1}–{Math.min((page.number + 1) * page.size, page.totalElements)} von {page.totalElements}
        </Typography>
        <Pagination count={page.totalPages}
                    page={page.number + 1}
                    disabled={disabled}
                    onChange={(_, value) => onChange(value - 1)}
                    aria-label={label}
                    getItemAriaLabel={(type, page, selected) => type === 'page'
                        ? `${selected ? 'Aktuelle Seite' : 'Seite'} ${page}`
                        : ({
                            first: 'Erste Seite',
                            last: 'Letzte Seite',
                            next: 'Nächste Seite',
                            previous: 'Vorherige Seite',
                            'start-ellipsis': 'Weitere Seiten',
                            'end-ellipsis': 'Weitere Seiten',
                        })[type]}/>
    </Stack>;
}
