import {MIN_DISPLAY_WIDTH_PX} from '../shells/staff/components/shell-resolution-overlay';
import {COLLAPSED_DRAWER_WIDTH_REM} from '../shells/staff/components/shell-drawer';

export function getMinDisplayableAreaWidth(): number {
    const displayMinWidthPX = MIN_DISPLAY_WIDTH_PX;
    const drawerMinWidthREM = parseFloat(COLLAPSED_DRAWER_WIDTH_REM.replace('rem', ''));
    const fontSize = parseFloat(getComputedStyle(document.documentElement).fontSize);
    const drawerMinWidthPX = drawerMinWidthREM * fontSize;
    return displayMinWidthPX - drawerMinWidthPX;
}