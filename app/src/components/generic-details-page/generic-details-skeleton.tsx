import React from "react";
import { Box, Grid, Skeleton, Typography } from "@mui/material";

export function GenericDetailsSkeleton() {
    return (
        <Box>
            {/* Titel */}
            <Typography variant="h5" sx={{ mt: 1.5, mb: 1 }}>
                <Skeleton width={300} height={32} />
            </Typography>

            <Typography sx={{ mb: 3, maxWidth: 900 }}>
                <Skeleton width="80%" height={24} />
            </Typography>

            {/* Formularfelder */}
            <Grid container spacing={4}>
                <Grid item xs={12} lg={6}>
                    <Skeleton variant="rectangular" height={56} sx={{ borderRadius: 1 }} />
                </Grid>
                <Grid item xs={12} lg={6}>
                    <Skeleton variant="rectangular" height={56} sx={{ borderRadius: 1 }} />
                </Grid>
                <Grid item xs={12} lg={6}>
                    <Skeleton variant="rectangular" height={80} sx={{ borderRadius: 1 }} />
                </Grid>
            </Grid>

            {/* Sektionen */}
            <Typography variant="h6" sx={{ mt: 4, mb: 1 }}>
                <Skeleton width={250} height={28} />
            </Typography>
            <Typography sx={{ mb: 3 }}>
                <Skeleton width="70%" height={20} />
            </Typography>

            <Grid container spacing={4}>
                <Grid item xs={12} lg={6}>
                    <Skeleton variant="rectangular" height={56} sx={{ borderRadius: 1 }} />
                </Grid>
                <Grid item xs={12} lg={6}>
                    <Skeleton variant="rectangular" height={56} sx={{ borderRadius: 1 }} />
                </Grid>
            </Grid>

            {/* Button-Gruppe */}
            <Box sx={{ display: "flex", marginTop: 5, gap: 2 }}>
                <Skeleton variant="rectangular" width={140} height={40} sx={{ borderRadius: 1 }} />
                <Skeleton variant="rectangular" width={140} height={40} sx={{ borderRadius: 1 }} />
                <Skeleton variant="rectangular" width={140} height={40} sx={{ borderRadius: 1, marginLeft: "auto" }} />
            </Box>
        </Box>
    );
}
