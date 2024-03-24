import {type Location} from 'react-router-dom';
import {AccessibilityDialogId} from '../dialogs/accessibility-dialog/accessibility-dialog';
import {PrivacyDialogId} from '../dialogs/privacy-dialog/privacy-dialog';
import {ImprintDialogId} from '../dialogs/imprint-dialog/imprint-dialog';
import {HelpDialogId} from '../dialogs/help-dialog/help.dialog';


export function formatMetaDialog(text: string, location: Location): string {
    let result = text;

    for (const meta of [AccessibilityDialogId, PrivacyDialogId, ImprintDialogId, HelpDialogId]) {
        const tag = meta.toLowerCase();

        result = result
            .replace(`{${tag}}`, `<a href="?dialog=${tag}">`)
            .replace(`{/${tag}}`, '</a>');
    }

    return result;
}
