import React from 'react';
import {IconButton, SxProps, Theme, Tooltip} from '@mui/material';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {showErrorSnackbar, showSuccessSnackbar} from '../../slices/snackbar-slice';
import {copyToClipboardText} from '../../utils/copy-to-clipboard';
import ContentCopy from '@aivot/mui-material-symbols-400-outlined/dist/content-copy/ContentCopy';

interface CopyToClipboardButtonProps {
    text: string;
    tooltip?: string;
    copiedTooltip?: string;
    ariaLabel?: string;
    sx?: SxProps<Theme>;
    size?: 'small' | 'medium' | 'large';
    disabled?: boolean;
    icon?: React.ReactNode;
    successMessage?: string;
    errorMessage?: string;
    onSuccess?: () => void;
    onError?: () => void;
}

export function CopyToClipboardButton(props: CopyToClipboardButtonProps): React.ReactElement {
    const {
        text,
        tooltip = 'Kopieren',
        copiedTooltip = 'Kopiert!',
        ariaLabel,
        sx,
        size = 'small',
        disabled = false,
        icon,
        successMessage,
        errorMessage,
        onSuccess,
        onError,
    } = props;

    const dispatch = useAppDispatch();
    const [hasCopied, setHasCopied] = React.useState(false);
    const copyTimeoutRef = React.useRef<number | null>(null);

    React.useEffect(() => {
        return () => {
            if (copyTimeoutRef.current != null) {
                window.clearTimeout(copyTimeoutRef.current);
            }
        };
    }, []);

    const handleCopy = async (): Promise<void> => {
        const success = await copyToClipboardText(text);
        if (success) {
            setHasCopied(true);
            if (copyTimeoutRef.current != null) {
                window.clearTimeout(copyTimeoutRef.current);
            }
            copyTimeoutRef.current = window.setTimeout(() => {
                setHasCopied(false);
            }, 1500);
            if (successMessage != null) {
                dispatch(showSuccessSnackbar(successMessage));
            }
            onSuccess?.();
            return;
        }

        if (errorMessage != null) {
            dispatch(showErrorSnackbar(errorMessage));
        }
        onError?.();
    };

    return (
        <Tooltip
            title={hasCopied ? copiedTooltip : tooltip}
            arrow
        >
            <span>
                <IconButton
                    size={size}
                    onClick={() => {
                        handleCopy().catch(console.error);
                    }}
                    disabled={disabled}
                    sx={sx}
                    aria-label={ariaLabel ?? tooltip}
                >
                    {icon ?? <ContentCopy fontSize={size === 'small' ? 'small' : 'medium'} />}
                </IconButton>
            </span>
        </Tooltip>
    );
}
