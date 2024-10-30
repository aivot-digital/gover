import React from 'react';
import {Rating as MuiRating, useTheme} from '@mui/material';
import {type IconContainerProps} from '@mui/material/Rating';
import MoodBadOutlinedIcon from '@mui/icons-material/MoodBadOutlined';
import SentimentDissatisfiedOutlinedIcon from '@mui/icons-material/SentimentDissatisfiedOutlined';
import SentimentNeutralOutlinedIcon from '@mui/icons-material/SentimentNeutralOutlined';
import SentimentSatisfiedAltOutlinedIcon from '@mui/icons-material/SentimentSatisfiedAltOutlined';
import EmojiEmotionsOutlinedIcon from '@mui/icons-material/EmojiEmotionsOutlined';

interface RatingProps {
    value?: number;
    onChange?: (val: number | null) => void;
}

const customIcons: Record<string, {
    icon: React.ReactElement;
    label: string;
}> = {
    1: {
        icon: <MoodBadOutlinedIcon
            sx={{
                fontSize: '40px',
                margin: '0 5px',
            }}
        />,
        label: 'Sehr Unzufrieden',
    },
    2: {
        icon: <SentimentDissatisfiedOutlinedIcon
            sx={{
                fontSize: '40px',
                margin: '0 5px',
            }}
        />,
        label: 'Unzufrieden',
    },
    3: {
        icon: <SentimentNeutralOutlinedIcon
            sx={{
                fontSize: '40px',
                margin: '0 5px',
            }}
        />,
        label: 'Neutral',
    },
    4: {
        icon: <SentimentSatisfiedAltOutlinedIcon
            sx={{
                fontSize: '40px',
                margin: '0 5px',
            }}
        />,
        label: 'Zufrieden',
    },
    5: {
        icon: <EmojiEmotionsOutlinedIcon
            sx={{
                fontSize: '40px',
                margin: '0 5px',
            }}
        />,
        label: 'Sehr Zufrieden',
    },
};

export function Rating(props: RatingProps): JSX.Element {
    const theme = useTheme();

    return (
        <MuiRating
            sx={{
                color: theme.palette.primary.main,
            }}
            readOnly={props.onChange == null}
            value={props.value}
            name="highlight-selected-only"
            IconContainerComponent={IconContainer}
            highlightSelectedOnly
            size="large"
            onChange={(_, newValue) => {
                if (props.onChange != null) {
                    props.onChange(newValue);
                }
            }}
        />
    );
}

function IconContainer(props: IconContainerProps): JSX.Element {
    const {
        value,
        ...other
    } = props;
    return <span {...other}>{customIcons[value].icon}</span>;
}
