import {Box, Button, Typography} from '@mui/material';
import {useElementTreeEditorContext} from '../../../components/element-tree-2/components/element-tree-editor-context';
import {useElementTreeContext} from '../../../components/element-tree-2/element-tree-context';
import {CodeTabNoCodeEditor} from '../../../components/code-tab-no-code-editor';
import {hasLegacyNoCode, LegacyNoCodeSection, MigratableFunction} from '../hooks/use-legacy-no-code-migration';

import {LegacyNoCodeWarning} from './legacy-no-code-warning';
import {LegacyNoCodeReference} from './legacy-no-code-reference';


export function LegacyNoCodeMigrationReference({section}: { section: LegacyNoCodeSection }) {
    const {editable} = useElementTreeContext();
    const {
        currentElement,
        parents,
        legacyMigrations,
        startLegacyMigration,
        cancelLegacyMigration,
    } = useElementTreeEditorContext();
    const migration = legacyMigrations?.[section];
    const previous: MigratableFunction | undefined = migration?.original ?? (currentElement as unknown as Record<LegacyNoCodeSection, MigratableFunction>)[section];
    if (!hasLegacyNoCode(previous)) return null;
    return <Box sx={{
        mb: 2,
        display: 'flex',
        flexDirection: 'column',
        gap: 2,
    }}>
        {migration == null ? <LegacyNoCodeWarning onStart={editable && startLegacyMigration != null
                ? () => startLegacyMigration(section) : undefined}/>
            : <Box>
                <Box sx={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    gap: 2,
                }}>
                    <Typography variant="h5"
                                component="h3">Regel umstellen</Typography>
                    {editable && <Button onClick={() => cancelLegacyMigration?.(section)}>Umstellung abbrechen</Button>}
                </Box>
                <Typography sx={{
                    mt: 0.5,
                    maxWidth: 820,
                }}>Die bisherige Regel bleibt erhalten, bis Sie die neue Fassung im Elementeditor
                   übernehmen.</Typography>
            </Box>}
        <LegacyNoCodeReference>
            <CodeTabNoCodeEditor parents={parents as Parameters<typeof CodeTabNoCodeEditor>[0]['parents']}
                                 element={currentElement}
                                 func={{
                                     requirements: '',
                                     conditionSet: previous?.conditionSet ?? undefined,
                                 }}
                                 onChange={() => undefined}
                                 editable={false}
                                 shouldReturnString={section === 'validation'}
                                 presentation="reference"/>
        </LegacyNoCodeReference>
        {migration != null && <Box>
            <Typography variant="h6"
                        component="h3">Neue Fassung</Typography>
            <Typography sx={{
                mt: 0.5,
                maxWidth: 820,
            }}>Bilden Sie die bisherige Regel nach und vergleichen Sie die Bedingungen und Fehlermeldungen.</Typography>
        </Box>}
    </Box>;
}
