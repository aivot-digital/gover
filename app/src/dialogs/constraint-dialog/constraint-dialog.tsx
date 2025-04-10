import { Button, Dialog, DialogActions, DialogContent, Link, Typography } from "@mui/material";
import { DialogTitleWithClose } from "../../components/dialog-title-with-close/dialog-title-with-close";
import { Link as RouterLink } from "react-router-dom";

interface ConstraintDialogProps {
    open: boolean;
    onClose: () => void;
    message: string;
    solutionText?: string;
    links?: { label: string; to?: string; href?: string; target?: string }[]; // Unterstützung für interne & externe Links
}

export function ConstraintDialog({ open, onClose, message, solutionText, links }: ConstraintDialogProps) {
    return (
        <Dialog open={open} onClose={onClose}>
            <DialogTitleWithClose onClose={onClose}>
                Löschen nicht möglich
            </DialogTitleWithClose>

            <DialogContent>
                <Typography variant="body1">{message}</Typography>

                {solutionText ? (
                    <Typography variant="body2" sx={{ mt: 2 }}>
                        {solutionText}
                    </Typography>
                ) : (
                    <Typography variant="body2" sx={{ mt: 2 }}>
                        Um das Problem zu lösen, gehe zu:
                    </Typography>
                )}

                {links && links.length > 0 && (
                    <Typography variant="body2">
                        <ul>
                            {links.map((link, index) => (
                                <li key={index}>
                                    {link.to ? (
                                        <Link component={RouterLink} to={link.to}>
                                            {link.label}
                                        </Link>
                                    ) : link.href ? (
                                        <Link href={link.href} target={link.target || "_blank"} rel="noopener">
                                            {link.label}
                                        </Link>
                                    ) : (
                                        <span>{link.label}</span>
                                    )}
                                </li>
                            ))}
                        </ul>
                    </Typography>
                )}
            </DialogContent>

            <DialogActions>
                <Button onClick={onClose} variant="contained">
                    Verstanden
                </Button>
            </DialogActions>
        </Dialog>
    );
}
