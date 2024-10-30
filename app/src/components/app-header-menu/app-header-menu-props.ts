import {type AppMode} from '../../data/app-mode';

export interface AppHeaderMenuProps {
    mode: AppMode;
    anchorElement: Element;
    onClose: () => void;
}
