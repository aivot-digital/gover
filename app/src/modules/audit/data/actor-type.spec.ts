import {describe, expect, it} from 'vitest';
import Route from '@aivot/mui-material-symbols-400-n25-outlined/Route';
import AccountCircle from '@aivot/mui-material-symbols-400-n25-outlined/AccountCircle';
import Borg from '@aivot/mui-material-symbols-400-n25-outlined/Borg';
import {getActorTypeColor, getActorTypeIcon, getActorTypeLabel} from './actor-type';

describe('actor type presentation', () => {
    it('uses the process navigation icon for process actors', () => {
        expect(getActorTypeIcon('Process')).toBe(Route);
        expect(getActorTypeLabel('Process')).toBe('Prozess');
        expect(getActorTypeColor('Process')).toBe('info');
    });

    it('preserves the other actors and the fallback for unknown actors', () => {
        expect(getActorTypeIcon('User')).toBe(AccountCircle);
        expect(getActorTypeIcon('System')).toBe(Borg);
        expect(getActorTypeIcon('Plugin')).toBeNull();
        expect(getActorTypeIcon(null)).toBeNull();
    });
});
