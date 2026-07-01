import {getLeafDiffPathSegment, splitDiffPath} from './resolve-element-path';

describe('splitDiffPath', () => {
    it('should split dot notation paths with array indices', () => {
        expect(splitDiffPath('rootElement.children[0].label')).toEqual([
            'rootElement',
            'children',
            '0',
            'label',
        ]);
    });

    it('should keep supporting legacy slash-separated paths', () => {
        expect(splitDiffPath('/rootElement/children/0/label')).toEqual([
            'rootElement',
            'children',
            '0',
            'label',
        ]);
    });
});

describe('getLeafDiffPathSegment', () => {
    it('should return the last field segment for dot notation paths', () => {
        expect(getLeafDiffPathSegment('rootElement.children[0].label')).toBe('label');
    });

    it('should return an empty string for root diffs', () => {
        expect(getLeafDiffPathSegment('')).toBe('');
    });
});
