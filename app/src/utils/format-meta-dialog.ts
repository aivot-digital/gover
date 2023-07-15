import { type Location } from 'react-router-dom';
import { MetaDialog } from '../slices/app-slice';


export function formatMetaDialog(text: string, location: Location): string {
    let result = text;

    for (const meta in MetaDialog) {
        result = result
            .replace(`{${ meta }}`, `<a href="/#${ location.pathname }?dialog=${ meta }">`)
            .replace(`{/${ meta }}`, '</a>');
    }

    return result;
}
