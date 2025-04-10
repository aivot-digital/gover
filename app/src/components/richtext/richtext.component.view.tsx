import {RichtextElement} from '../../models/elements/form/content/richtext-element';
import {Typography} from '@mui/material';
import {BaseViewProps} from "../../views/base-view";
import {useMemo} from 'react';
import {useAppSelector} from '../../hooks/use-app-selector';

export function RichtextComponentView({element}: BaseViewProps<RichtextElement, void>) {
    /*
    const inputs = useAppSelector(state => state.app.inputs);
    const values = useAppSelector(state => state.app.values);

    const hydratedContent = useMemo(() => {
        const re = /{{([^}]+)}}/g;
        return element.content?.replace(re, (match, group) => {
            const key = group.trim();
            if (key in inputs && inputs[key] != null) {
                return inputs[key];
            }
            if (key in values && values[key] != null) {
                return values[key];
            }
            return match;
        });
    }, [inputs, values, element.content]);

     */

    return (
        <Typography
            variant="body2"
            dangerouslySetInnerHTML={{__html: element.content ?? ''}}
        />
    );
}

