package main

import (
	"cli/src/database"
	"cli/src/models"
	"fmt"
	"log"
	"os"
	"time"
)

func main() {
	var submissions []models.Submission
	err := database.DB.Select(&submissions, "SELECT id, archived, application_id FROM submissions WHERE archived is not null")
	if err != nil {
		log.Print(err)
		os.Exit(1)
	}

	for _, sub := range submissions {
		var application models.Application
		err := database.DB.Get(&application, "SELECT id, submission_deletion_weeks FROM applications WHERE id = $1", sub.ApplicationId)

		if err != nil {
			log.Printf("Failed to fetch application %d for submission %s: %v\n", sub.ApplicationId, sub.Id, err)
			continue
		}
		deletionWeeks := application.SubmissionDeletionWeeks
		if deletionWeeks == 0 {
			deletionWeeks = 4
		}

		expirationTimestamp := sub.Archived.Add(time.Hour * 24 * 7 * time.Duration(deletionWeeks))

		if expirationTimestamp.Unix() < time.Now().Unix() {
			log.Printf("Deleting submission %s", sub.Id)

			_, err := database.DB.Exec("DELETE FROM submissions WHERE id = $1", sub.Id)
			if err != nil {
				log.Printf("Failed to delete submission %s from database: %v\n", sub.Id, err)
				continue
			}

			submissionPath := fmt.Sprintf("/data/submissions/%s", submissions)
			err = os.RemoveAll(submissionPath)
			if err != nil {
				log.Printf("Failed to remove submission %s directory: %v\n", sub.Id, err)
				continue
			}
		} else {
			log.Printf("Submission %s not yet expired. Awaiting %s", sub.Id, expirationTimestamp.String())
		}
	}
}
