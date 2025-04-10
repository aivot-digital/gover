import React, { useState, useEffect, ReactNode } from "react";
import {
    Box,
    Button,
    Divider,
    List,
    Typography,
    SxProps,
    Theme, useTheme,
} from "@mui/material";
import UnfoldLessOutlinedIcon from "@mui/icons-material/UnfoldLessOutlined";
import UnfoldMoreOutlinedIcon from "@mui/icons-material/UnfoldMoreOutlined";

const srOnly: SxProps<Theme> = {
    position: 'absolute',
    width: 1,
    height: 1,
    margin: -1,
    padding: 0,
    border: 0,
    overflow: 'hidden',
    clip: 'rect(0 0 0 0)',
    whiteSpace: 'nowrap',
    clipPath: 'inset(50%)',
};

interface ExpandableListProps<T> {
    title?: string;
    items: T[];
    initialVisible?: number;
    renderItem: (item: T, index: number) => ReactNode;
    singularLabel?: string;
    pluralLabel?: string;
    listId?: string;
}

const ExpandableList = <T,>({
                                title,
                                items = [],
                                initialVisible = 3,
                                renderItem,
                                singularLabel = "Eintrag",
                                pluralLabel = "Einträge",
                                listId = "expandable-list",
                            }: ExpandableListProps<T>) => {
    const [showAll, setShowAll] = useState(false);
    const [announceText, setAnnounceText] = useState("");

    const theme = useTheme();
    const total = items.length;
    const visibleCount = showAll ? total : Math.min(initialVisible, total);

    useEffect(() => {
        if (total > initialVisible) {
            const announceTimeout = setTimeout(() => {
                setAnnounceText(
                    showAll
                        ? `Alle ${total} ${pluralLabel} werden jetzt angezeigt.`
                        : `Liste reduziert. ${visibleCount} von ${total} sichtbar.`
                );
            }, 400); // 🕓 Warten bis Button vorgelesen wurde

            const clearTimeoutId = setTimeout(() => {
                setAnnounceText("");
            }, 4000); // 🧹 Nach 4s wieder leeren

            return () => {
                clearTimeout(announceTimeout);
                clearTimeout(clearTimeoutId);
            };
        }
    }, [showAll, total, initialVisible, pluralLabel, visibleCount]);

    const handleToggle = () => {
        setAnnounceText("");
        setShowAll(prev => !prev);
    };

    if (total === 0) return null;

    return (
        <Box sx={{ mb: 3, position: "relative", zIndex: 1 }}>
            {title && (
                <Typography
                    component="h3"
                    variant="subtitle1"
                    color="primary"
                    sx={{ textTransform: "uppercase" }}
                >
                    {title}
                </Typography>
            )}

            <Box
                id={`${listId}-description`}
                sx={{ display: 'none' }}
            >
                {`Es sind insgesamt ${total} ${pluralLabel} vorhanden. Es werden aktuell ${visibleCount} angezeigt.`}
            </Box>

            <List
                dense
                disablePadding
                id={listId}
                aria-describedby={`${listId}-description`}
            >
                {items.slice(0, visibleCount).map((item, index) => (
                    <Box component="li" key={index}>
                        {renderItem(item, index)}
                    </Box>
                ))}

                {total > initialVisible && (
                    <>
                        <Divider sx={{ my: 1, width: "50%" }} component="li" aria-hidden={true} />
                        <Button
                            size="small"
                            startIcon={showAll ? <UnfoldLessOutlinedIcon sx={{
                                color: theme.palette.primary.main,
                                marginLeft: "2px",
                                marginRight: "4px"
                            }}/> : <UnfoldMoreOutlinedIcon sx={{
                                color: theme.palette.primary.main,
                                marginLeft: "2px",
                                marginRight: "4px"
                            }}/>}
                            onClick={handleToggle}
                            aria-expanded={showAll}
                            aria-controls={listId}
                            aria-label={
                                showAll
                                    ? `Weniger anzeigen. Nur die ersten ${initialVisible} von ${total} sichtbar.`
                                    : `Alle anzeigen. Es werden ${total - initialVisible} weitere ${pluralLabel} sichtbar.`
                            }
                        >
                            {showAll
                                ? "Weniger anzeigen"
                                : `Alle anzeigen (${total - initialVisible} ${
                                    total - initialVisible === 1 ? 'weiteres' : 'weitere'
                                })`
                            }
                        </Button>
                    </>
                )}
            </List>

            <Box
                aria-live="polite"
                role="status"
                sx={srOnly}
            >
                {announceText}
            </Box>
        </Box>
    );
};

export default ExpandableList;