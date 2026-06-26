import {render} from '@testing-library/react';
import {ExpandableJSONCodeBlock} from './expandable-json-code-block';

describe('ExpandableJSONCodeBlock', () => {
    it('should omit the previous null line when a nested value was created', () => {
        const {container} = render(
            <ExpandableJSONCodeBlock
                value={{
                    created: {
                        name: 'Ada',
                    },
                }}
                diff={{
                    created: null,
                }}
            />,
        );

        expect(container).toHaveTextContent('"created": {');
        expect(container).toHaveTextContent('"name": "Ada"');
        expect(container).not.toHaveTextContent('"created": null');
    });

    it('should still show non-null previous values for changed leaves', () => {
        const {container} = render(
            <ExpandableJSONCodeBlock
                value={{
                    name: 'Ada',
                }}
                diff={{
                    name: 'Grace',
                }}
            />,
        );

        expect(container).toHaveTextContent('"name": "Ada"');
        expect(container).toHaveTextContent('"name": "Grace"');
    });
});
