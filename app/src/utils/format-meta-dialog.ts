import {type Location} from 'react-router-dom';
import {MetaDialog} from '../slices/app-slice';


export function formatMetaDialog(text: string, location: Location): string {
    let result = text;

    for (const meta in MetaDialog) {
        const tag = meta.toLowerCase();
        result = result
            .replace(`{${tag}}`, `<a href="/#${location.pathname}?dialog=${tag}">`)
            .replace(`{/${tag}}`, '</a>');
    }

    return result;
}
