package de.aivot.GoverBackend.models.dtos;

import de.aivot.GoverBackend.models.entities.SubmissionAttachment;


public class SubmissionAttachmentListDto {
    private String id;

    private String filename;


    public static SubmissionAttachmentListDto valueOf(SubmissionAttachment attachment) {
        SubmissionAttachmentListDto d = new SubmissionAttachmentListDto();

        d.id = attachment.getId();
        d.filename = attachment.getFilename();

        return d;
    }

    // region Getters & Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }


    // endregion
}
