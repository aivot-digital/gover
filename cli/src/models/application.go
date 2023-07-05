package models

type Application struct {
	Id                      string `db:"id"`
	SubmissionDeletionWeeks int64  `db:"submission_deletion_weeks"`
}
