package main

import (
	"cli/src/database"
	"flag"
	"fmt"
	"github.com/google/uuid"
	"golang.org/x/crypto/bcrypt"
	"log"
	"math/rand"
	"strconv"
	"strings"
	"time"
)

func main() {
	size := flag.Int("s", 10, "The number of departments to be created")
	flag.Parse()

	rand.Seed(time.Now().Unix())

	for i := 0; i < *size; i++ {
		log.Printf("Creating department #%d", i+1)
		depId := createDepartment()
		userIds := createUsers(depId)
		formIds := createForms(depId)
		for index, id := range formIds {
			createSubmissions(index, id, userIds)
		}
	}
}

var depNames = []string{
	"Steuern",
	"Gebäude",
	"Umwelt",
	"Soziales",
	"Wirtschaft",
	"Verkehr",
	"Bildung",
	"Entwicklung",
}

func createDepartment() int {
	depName := depNames[rand.Intn(len(depNames))]

	var offset int
	err := database.DB.Get(&offset, "SELECT count(*) AS count FROM departments WHERE name LIKE $1", depName+" %")
	if err != nil {
		log.Fatal(err)
	}

	depName = depName + " " + strconv.Itoa(offset+1)

	var id int
	err = database.DB.Get(&id, `
		INSERT INTO departments 
		    (name, address, imprint, privacy, accessibility, technical_support_address, special_support_address, created, updated)  
		VALUES
			($1, $2, $3, $4, $5, $6, $7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
		RETURNING id
	`, depName, "Teststraße 1\n12345 Teststadt", createLegal("Impressum"), createLegal("Datenschutz"), createLegal("Barrierefreiheit"), "technik@gover.digital", "fachlich@gover.digital")
	if err != nil {
		log.Fatal(err)
	}

	return id
}

func createLegal(headline string) string {
	buffer := fmt.Sprintf(`<h1>%s</h1>`, headline)

	for i := 0; i < 20; i++ {
		buffer += fmt.Sprintf("<h2>Absatz %d</h2>", i+1)
		buffer += "<p>"
		buffer += strings.Repeat("Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua.", 20)
		buffer += "</p>"
	}

	return buffer
}

var firstNames = []string{
	"Hans",
	"Hermann",
	"Ute",
	"Maria",
	"Maike",
	"Michael",
	"Thomas",
	"Stephan",
	"Dirk",
	"Hannelore",
	"Sandra",
}

var lastNames = []string{
	"Mueller",
	"Maier",
	"Becker",
	"Schmidt",
	"Wied",
	"Guthof",
	"Friedel",
	"Hintereder",
	"Bach",
	"Hass",
	"Beiwerk",
}

func createUsers(depId int) []int {
	userIds := make([]int, 0)

	for i := 0; i < 6; i++ {
		log.Printf("\t↳ Creating user #%d", i+1)

		firstName := firstNames[rand.Intn(len(firstNames))]
		lastName := lastNames[rand.Intn(len(lastNames))]

		var offset int
		err := database.DB.Get(&offset, "SELECT count(*) AS count FROM users WHERE name LIKE $1", firstName+" "+lastName+" %")
		if err != nil {
			log.Fatal(err)
		}

		name := firstName + " " + lastName + " " + strconv.Itoa(offset+1)
		mail := strings.ToLower(firstName) + "." + strings.ToLower(lastName) + "." + strconv.Itoa(offset+1) + "@gover.digital"
		passwordBytes, err := bcrypt.GenerateFromPassword([]byte(firstName+lastName+strconv.Itoa(offset+1)+"test"), 10)
		if err != nil {
			log.Fatal(err)
		}

		var userId int
		err = database.DB.Get(&userId, `
			INSERT INTO users
			    (name, email, password, active, admin, created, updated)
			VALUES 
				($1, $2, $3, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
			RETURNING id
		`, name, mail, string(passwordBytes))
		if err != nil {
			log.Fatal(err)
		}
		userIds = append(userIds, userId)

		_, err = database.DB.Exec(`
			INSERT INTO department_memberships
				(department_id, user_id, role) 
			VALUES 
				($1, $2, $3)
		`, depId, userId, i%3)
		if err != nil {
			log.Fatal(err)
		}
	}

	return userIds
}

var formNames = []string{
	"Anmeldung Hund",
	"Anmeldung Haus",
	"Anmeldung Kind",
	"Anmeldung TV",
	"Anmeldung Kita",
	"Anmeldung Schule",
	"Anmeldung Feierlichkeit",
	"Anmeldung KfZ",
	"Anmeldung Unternehmen",
	"Anmeldung Abwasser",

	"Abmeldung Hund",
	"Abmeldung Haus",
	"Abmeldung Kind",
	"Abmeldung TV",
	"Abmeldung Kita",
	"Abmeldung Schule",
	"Abmeldung Feierlichkeit",
	"Abmeldung KfZ",
	"Abmeldung Unternehmen",
	"Abmeldung Abwasser",
}

func createForms(depId int) []int {
	formIds := make([]int, 0)

	for i := 0; i < 10; i++ {
		log.Printf("\t↳ Creating form #%d", i+1)
		formName := formNames[rand.Intn(len(formNames))]

		var offset int
		err := database.DB.Get(&offset, "SELECT count(*) AS count FROM applications WHERE title LIKE $1", formName+" %")
		if err != nil {
			log.Fatal(err)
		}

		formName = formName + " " + strconv.Itoa(offset+1)
		slug := strings.ReplaceAll(strings.ToLower(formName), " ", "-")

		for j := 0; j < i+1; j++ {
			version := fmt.Sprintf("1.%d.0", j)
			log.Printf("\t\t↳ Version %s", version)

			var formId int
			err = database.DB.Get(&formId, `
				INSERT INTO applications 
				    (slug, version, title, status, root, destination_id, legal_support_department_id, technical_support_department_id, imprint_department_id, privacy_department_id, accessibility_department_id, developing_department_id, managing_department_id, responsible_department_id, created, updated, customer_access_hours, submission_deletion_weeks) 
				VALUES 
				    ($1, $2, $3, $4, $5, null, $6, $6, $6, $6, $6, $6, $6, $6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 4, 2)
				RETURNING id
			`, slug, version, formName, j%4, createFormRoot(formName), depId)
			if err != nil {
				log.Fatal(err)
			}

			formIds = append(formIds, formId)
		}
	}

	return formIds
}

func createFormRoot(formName string) string {
	return fmt.Sprintf(`{
    "type": 0,
    "id": "root_1688286658239178",
    "appVersion": "2.1.8",
    "name": "",
    "testProtocolSet": null,
    "isVisible": null,
    "patchElement": null,
    "headline": "Test Formular\n%s",
    "tabTitle": null,
    "theme": null,
    "children": [
        {
            "id": "step_1688286695566771",
            "type": 1,
            "appVersion": "2.1.8",
            "children": [
                {
                    "id": "grup_1688286736358989",
                    "type": 3,
                    "appVersion": "2.1.8",
                    "children": [
                        {
                            "id": "grup_1688286784407384",
                            "type": 3,
                            "appVersion": "2.1.8",
                            "children": []
                        },
                        {
                            "id": "spac_1688286785829368",
                            "type": 13,
                            "appVersion": "2.1.8",
                            "height": "30"
                        },
                        {
                            "id": "imag_1688286787271163",
                            "type": 20,
                            "appVersion": "2.1.8",
                            "alt": "Bild",
                            "src": "https://aivot.de/img/aivot-logo.svg"
                        },
                        {
                            "id": "hdln_1688286788647390",
                            "type": 6,
                            "appVersion": "2.1.8",
                            "content": "Überschrift"
                        },
                        {
                            "id": "ritx_1688286790097540",
                            "type": 10,
                            "appVersion": "2.1.8",
                            "content": "<p class=\"MuiTypography-root MuiTypography-body2\">Fließtext</p>"
                        },
                        {
                            "id": "alrt_1688286791646345",
                            "type": 2,
                            "appVersion": "2.1.8",
                            "title": "Hinweis",
                            "text": "Nutzen Sie diesen Hinweis, um Antragsstellenden zusätzliche Informationen hervorgehoben bereitzustellen."
                        },
                        {
                            "id": "text_1688286793268300",
                            "type": 15,
                            "appVersion": "2.1.8",
                            "label": "Text"
                        },
                        {
                            "id": "numb_1688286795789767",
                            "type": 8,
                            "appVersion": "2.1.8",
                            "label": "Zahl"
                        },
                        {
                            "id": "tabl_1688286797745550",
                            "type": 14,
                            "appVersion": "2.1.8",
                            "label": "Tabelle",
                            "fields": [
                                {
                                    "label": "Spalte 1",
                                    "datatype": "string",
                                    "placeholder": "Inhalt für Spalte 1"
                                },
                                {
                                    "label": "Spalte 2",
                                    "datatype": "string",
                                    "placeholder": "Inhalt für Spalte 2"
                                }
                            ]
                        },
                        {
                            "id": "fupl_1688286799579139",
                            "type": 22,
                            "appVersion": "2.1.8",
                            "label": "Anlage(n)",
                            "extensions": [
                                "pdf"
                            ]
                        },
                        {
                            "id": "repc_1688286801251475",
                            "type": 9,
                            "appVersion": "2.1.8",
                            "label": "Strukturierte Listeneingabe",
                            "headlineTemplate": "Datensatz Nr. #",
                            "children": []
                        },
                        {
                            "id": "date_1688286803189481",
                            "type": 5,
                            "appVersion": "2.1.8",
                            "label": "Datum",
                            "mode": "day"
                        },
                        {
                            "id": "time_16882868055301043",
                            "type": 16,
                            "appVersion": "2.1.8",
                            "label": "Zeit"
                        },
                        {
                            "id": "ckbx_1688286807810132",
                            "type": 4,
                            "appVersion": "2.1.8",
                            "label": "Checkbox (Ja/Nein)"
                        },
                        {
                            "id": "mucx_1688286809889790",
                            "type": 7,
                            "appVersion": "2.1.8",
                            "label": "Checkbox (Mehrfachauswahl)",
                            "options": [
                                "Option 1",
                                "Option 2",
                                "Option 3"
                            ]
                        },
                        {
                            "id": "radi_1688286811884808",
                            "type": 11,
                            "appVersion": "2.1.8",
                            "label": "Radio-Button (Einfachauswahl)",
                            "options": [
                                "Option 1",
                                "Option 2",
                                "Option 3"
                            ]
                        },
                        {
                            "id": "selc_1688286814193322",
                            "type": 12,
                            "appVersion": "2.1.8",
                            "label": "Dropdown (Einfachauswahl)",
                            "options": [
                                "Option 1",
                                "Option 2",
                                "Option 3"
                            ]
                        }
                    ]
                },
                {
                    "id": "spac_1688286738700835",
                    "type": 13,
                    "appVersion": "2.1.8",
                    "height": "30"
                },
                {
                    "id": "imag_1688286740522862",
                    "type": 20,
                    "appVersion": "2.1.8",
                    "alt": "Bild",
                    "src": "https://aivot.de/img/aivot-logo.svg"
                },
                {
                    "id": "hdln_1688286742516904",
                    "type": 6,
                    "appVersion": "2.1.8",
                    "content": "Überschrift"
                },
                {
                    "id": "ritx_16882867440481070",
                    "type": 10,
                    "appVersion": "2.1.8",
                    "content": "<p class=\"MuiTypography-root MuiTypography-body2\">Fließtext</p>"
                },
                {
                    "id": "alrt_1688286745767674",
                    "type": 2,
                    "appVersion": "2.1.8",
                    "title": "Hinweis",
                    "text": "Nutzen Sie diesen Hinweis, um Antragsstellenden zusätzliche Informationen hervorgehoben bereitzustellen."
                },
                {
                    "id": "text_1688286747616872",
                    "type": 15,
                    "appVersion": "2.1.8",
                    "label": "Text"
                },
                {
                    "id": "numb_1688286749477201",
                    "type": 8,
                    "appVersion": "2.1.8",
                    "label": "Zahl"
                },
                {
                    "id": "tabl_1688286752662286",
                    "type": 14,
                    "appVersion": "2.1.8",
                    "label": "Tabelle",
                    "fields": [
                        {
                            "label": "Spalte 1",
                            "datatype": "string",
                            "placeholder": "Inhalt für Spalte 1"
                        },
                        {
                            "label": "Spalte 2",
                            "datatype": "string",
                            "placeholder": "Inhalt für Spalte 2"
                        }
                    ]
                },
                {
                    "id": "fupl_1688286754353958",
                    "type": 22,
                    "appVersion": "2.1.8",
                    "label": "Anlage(n)",
                    "extensions": [
                        "pdf"
                    ]
                },
                {
                    "id": "repc_1688286756136134",
                    "type": 9,
                    "appVersion": "2.1.8",
                    "label": "Strukturierte Listeneingabe",
                    "headlineTemplate": "Datensatz Nr. #",
                    "children": [
                        {
                            "id": "grup_1688286818775376",
                            "type": 3,
                            "appVersion": "2.1.8",
                            "children": []
                        },
                        {
                            "id": "spac_1688286820506444",
                            "type": 13,
                            "appVersion": "2.1.8",
                            "height": "30"
                        },
                        {
                            "id": "imag_1688286822730447",
                            "type": 20,
                            "appVersion": "2.1.8",
                            "alt": "Bild",
                            "src": "https://aivot.de/img/aivot-logo.svg"
                        },
                        {
                            "id": "hdln_1688286831044917",
                            "type": 6,
                            "appVersion": "2.1.8",
                            "content": "Überschrift"
                        },
                        {
                            "id": "ritx_1688286833542951",
                            "type": 10,
                            "appVersion": "2.1.8",
                            "content": "<p class=\"MuiTypography-root MuiTypography-body2\">Fließtext</p>"
                        },
                        {
                            "id": "alrt_1688286835361454",
                            "type": 2,
                            "appVersion": "2.1.8",
                            "title": "Hinweis",
                            "text": "Nutzen Sie diesen Hinweis, um Antragsstellenden zusätzliche Informationen hervorgehoben bereitzustellen."
                        },
                        {
                            "id": "text_1688286836953818",
                            "type": 15,
                            "appVersion": "2.1.8",
                            "label": "Text"
                        },
                        {
                            "id": "numb_1688286838250256",
                            "type": 8,
                            "appVersion": "2.1.8",
                            "label": "Zahl"
                        },
                        {
                            "id": "tabl_1688286839584416",
                            "type": 14,
                            "appVersion": "2.1.8",
                            "label": "Tabelle",
                            "fields": [
                                {
                                    "label": "Spalte 1",
                                    "datatype": "string",
                                    "placeholder": "Inhalt für Spalte 1"
                                },
                                {
                                    "label": "Spalte 2",
                                    "datatype": "string",
                                    "placeholder": "Inhalt für Spalte 2"
                                }
                            ]
                        },
                        {
                            "id": "fupl_1688286841040468",
                            "type": 22,
                            "appVersion": "2.1.8",
                            "label": "Anlage(n)",
                            "extensions": [
                                "pdf"
                            ]
                        },
                        {
                            "id": "repc_1688286842444275",
                            "type": 9,
                            "appVersion": "2.1.8",
                            "label": "Strukturierte Listeneingabe",
                            "headlineTemplate": "Datensatz Nr. #",
                            "children": []
                        },
                        {
                            "id": "date_1688286844470395",
                            "type": 5,
                            "appVersion": "2.1.8",
                            "label": "Datum",
                            "mode": "day"
                        },
                        {
                            "id": "time_1688286846820836",
                            "type": 16,
                            "appVersion": "2.1.8",
                            "label": "Zeit"
                        },
                        {
                            "id": "ckbx_1688286849016263",
                            "type": 4,
                            "appVersion": "2.1.8",
                            "label": "Checkbox (Ja/Nein)"
                        },
                        {
                            "id": "mucx_1688286851522282",
                            "type": 7,
                            "appVersion": "2.1.8",
                            "label": "Checkbox (Mehrfachauswahl)",
                            "options": [
                                "Option 1",
                                "Option 2",
                                "Option 3"
                            ]
                        },
                        {
                            "id": "radi_1688286853776679",
                            "type": 11,
                            "appVersion": "2.1.8",
                            "label": "Radio-Button (Einfachauswahl)",
                            "options": [
                                "Option 1",
                                "Option 2",
                                "Option 3"
                            ]
                        },
                        {
                            "id": "selc_1688286856105590",
                            "type": 12,
                            "appVersion": "2.1.8",
                            "label": "Dropdown (Einfachauswahl)",
                            "options": [
                                "Option 1",
                                "Option 2",
                                "Option 3"
                            ]
                        }
                    ]
                },
                {
                    "id": "date_1688286758571745",
                    "type": 5,
                    "appVersion": "2.1.8",
                    "label": "Datum",
                    "mode": "day"
                },
                {
                    "id": "time_1688286766166682",
                    "type": 16,
                    "appVersion": "2.1.8",
                    "label": "Zeit"
                },
                {
                    "id": "ckbx_16882867687731017",
                    "type": 4,
                    "appVersion": "2.1.8",
                    "label": "Checkbox (Ja/Nein)"
                },
                {
                    "id": "mucx_16882867711931063",
                    "type": 7,
                    "appVersion": "2.1.8",
                    "label": "Checkbox (Mehrfachauswahl)",
                    "options": [
                        "Option 1",
                        "Option 2",
                        "Option 3"
                    ]
                },
                {
                    "id": "radi_1688286773762157",
                    "type": 11,
                    "appVersion": "2.1.8",
                    "label": "Radio-Button (Einfachauswahl)",
                    "options": [
                        "Option 1",
                        "Option 2",
                        "Option 3"
                    ]
                },
                {
                    "id": "selc_1688286775886763",
                    "type": 12,
                    "appVersion": "2.1.8",
                    "label": "Dropdown (Einfachauswahl)",
                    "options": [
                        "Option 1",
                        "Option 2",
                        "Option 3"
                    ]
                }
            ],
            "title": "Alle Elemente",
            "icon": "star"
        },
        {
            "id": "step_1688286705466242",
            "type": 1,
            "appVersion": "2.1.8",
            "children": [],
            "title": "No Code Funktionen",
            "icon": "bookmark"
        }
    ],
    "expiring": null,
    "accessLevel": null,
    "privacyText": "Bitte beachten Sie die {privacy}Hinweise zum Datenschutz{/privacy}.",
    "introductionStep": {
        "type": 17,
        "id": "intr_1688286658239750",
        "appVersion": "2.1.8",
        "name": "",
        "testProtocolSet": null,
        "isVisible": null,
        "patchElement": null,
        "initiativeName": null,
        "initiativeLogoLink": null,
        "initiativeLink": null,
        "teaserText": null,
        "organization": null,
        "eligiblePersons": null,
        "supportingDocuments": null,
        "documentsToAttach": null,
        "expectedCosts": null
    },
    "summaryStep": {
        "type": 19,
        "id": "summ_16882866582391038",
        "appVersion": "2.1.8",
        "name": "",
        "testProtocolSet": null,
        "isVisible": null,
        "patchElement": null
    },
    "submitStep": {
        "type": 18,
        "id": "subm_1688286658239586",
        "appVersion": "2.1.8",
        "name": "",
        "testProtocolSet": null,
        "isVisible": null,
        "patchElement": null,
        "textPreSubmit": "Sie können Ihren Antrag nun verbindlich bei der zuständigen/bewirtschaftenden Stelle einreichen. Nach der Einreichung können Sie sich den Antrag für Ihre Unterlagen herunterladen oder zusenden lassen.",
        "textPostSubmit": "Sie können Ihren Antrag herunterladen oder sich per E-Mail zuschicken lassen. Wir empfehlen Ihnen, den Antrag anschließend zu Ihren Unterlagen zu nehmen.",
        "textProcessingTime": null,
        "documentsToReceive": null
    }
}`, formName)
}

func createSubmissions(formIndex int, formId int, userIds []int) {
	log.Printf("Creating submissions for form #%d...\n", formIndex+1)
	for i := 0; i < 100; i++ {
		assignee := userIds[rand.Intn(len(userIds))]

		submissionId := uuid.New().String()
		_, err := database.DB.Exec(`
				INSERT INTO submissions 
				    (id, application_id, created, assignee_id, archived, customer_input, file_number, destination_id, destination_success)
				VALUES 
				    ($1, $2, CURRENT_TIMESTAMP, $3, null, $4, $5, null, false)
			`, submissionId, formId, assignee, createCustomerInput(), fmt.Sprintf("T%08d", i))
		if err != nil {
			log.Fatal(err)
		}

		for j := 0; j < 10; j++ {
			attachmentId := uuid.New().String()
			_, err := database.DB.Exec(`
				INSERT INTO submission_attachments 
				    (id, submission_id, filename)
				VALUES 
				    ($1, $2, $3)
			`, attachmentId, submissionId, fmt.Sprintf("Datei_%08d.pdf", j))
			if err != nil {
				log.Fatal(err)
			}
		}
	}
}

func createCustomerInput() string {
	buffer := "{\n"

	for i := 0; i < 199; i++ {
		buffer += fmt.Sprintf("    \"FELD_%08d\": \"Wert des Feldes\",\n", i)
	}

	buffer += fmt.Sprintf("    \"FELD_%08d\": \"Wert des Feldes\"\n", 200)

	buffer += "\n}"
	return buffer
}
