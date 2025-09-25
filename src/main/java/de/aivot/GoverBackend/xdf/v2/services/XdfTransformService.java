package de.aivot.GoverBackend.xdf.v2.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.GoverBackend.elements.models.elements.BaseFormElement;
import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
import de.aivot.GoverBackend.elements.models.elements.ElementValidationFunctions;
import de.aivot.GoverBackend.elements.models.elements.RootElement;
import de.aivot.GoverBackend.elements.models.elements.form.content.Alert;
import de.aivot.GoverBackend.elements.models.elements.form.content.Headline;
import de.aivot.GoverBackend.elements.models.elements.form.input.*;
import de.aivot.GoverBackend.elements.models.elements.form.layout.GroupLayout;
import de.aivot.GoverBackend.elements.models.elements.form.layout.ReplicatingContainerLayout;
import de.aivot.GoverBackend.elements.models.elements.steps.IntroductionStepElement;
import de.aivot.GoverBackend.elements.models.elements.steps.StepElement;
import de.aivot.GoverBackend.elements.models.elements.steps.SubmitStepElement;
import de.aivot.GoverBackend.elements.models.elements.steps.SummaryStepElement;
import de.aivot.GoverBackend.enums.AlertType;
import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.enums.ConditionSetOperator;
import de.aivot.GoverBackend.enums.DateType;
import de.aivot.GoverBackend.form.entities.FormVersionWithDetailsEntity;
import de.aivot.GoverBackend.form.enums.FormStatus;
import de.aivot.GoverBackend.form.enums.FormType;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.models.functions.conditions.Condition;
import de.aivot.GoverBackend.models.functions.conditions.ConditionSet;
import de.aivot.GoverBackend.utils.StringUtils;
import de.aivot.GoverBackend.xdf.v2.data.XdfFieldType;
import de.aivot.GoverBackend.xdf.v2.models.*;
import de.aivot.GoverBackend.xdf.v2.utils.XdfStringUtils;
import de.aivot.GoverBackend.xrepository.services.XRepositoryCodeListService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class XdfTransformService {
    private final XRepositoryCodeListService xRepositoryCodeListService;

    public XdfTransformService(XRepositoryCodeListService xRepositoryCodeListService) {
        this.xRepositoryCodeListService = xRepositoryCodeListService;
    }

    @Nullable
    public FormVersionWithDetailsEntity transformToGover(@Nonnull XdfStammdatenschema0102 xdfStammdatenschema0102) {
        var sd = xdfStammdatenschema0102
                .getStammdatenschema();

        if (sd == null) {
            return null;
        }

        return stammdatenschemaToRootElement(sd);
    }

    private FormVersionWithDetailsEntity stammdatenschemaToRootElement(@Nullable XdfStammdatenschema stammdatenschema) {
        if (stammdatenschema == null) {
            return null;
        }

        var internalTitle = XdfStringUtils
                .idfToName(stammdatenschema.getIdentifikation());

        var publicTitle = XdfStringUtils
                .cleanString(stammdatenschema.getName());
        var slug = StringUtils.slugify(publicTitle, 32);

        var teaserText = getTeaserText(stammdatenschema);

        var steps = new LinkedList<StepElement>();
        for (var stepStruktur : stammdatenschema.getStruktur()) {
            var fields = strukturToElements(stepStruktur, 0);

            String id = null;
            String name = null;
            if (stepStruktur.getEnthaelt() != null) {
                if (stepStruktur.getEnthaelt().getDatenfeldgruppe() != null) {
                    id = XdfStringUtils
                            .idfToName(stepStruktur.getEnthaelt().getDatenfeldgruppe().getIdentifikation());
                    name = XdfStringUtils
                            .cleanString(stepStruktur.getEnthaelt().getDatenfeldgruppe().getName());
                } else if (stepStruktur.getEnthaelt().getDatenfeld() != null) {
                    id = XdfStringUtils.
                            idfToName(stepStruktur.getEnthaelt().getDatenfeld().getIdentifikation());
                    name = XdfStringUtils
                            .cleanString(stepStruktur.getEnthaelt().getDatenfeld().getName());
                }
            }

            var step = new StepElement();

            step.setId(id);
            step.setChildren(fields);
            step.setTitle(name);

            steps.add(step);
        }

        var root = new RootElement()
                .setIntroductionStep(new IntroductionStepElement().setTeaserText(teaserText))
                .setChildren(steps)
                .setSummaryStep(new SummaryStepElement())
                .setSubmitStep(new SubmitStepElement());

        return new FormVersionWithDetailsEntity()
                .setSlug(slug)
                .setInternalTitle(internalTitle)
                .setPublicTitle(publicTitle)
                .setVersion(1)
                .setDraftedVersion(1)
                .setVersionCount(1)
                .setStatus(FormStatus.Drafted)
                .setType(FormType.Public)
                .setRootElement(root);
    }

    @NotNull
    private static String getTeaserText(@NotNull XdfStammdatenschema stammdatenschema) {
        String teaserTextRaw;
        if (StringUtils.isNotNullOrEmpty(stammdatenschema.getBeschreibung())) {
            teaserTextRaw = stammdatenschema.getBeschreibung();
        } else if (StringUtils.isNotNullOrEmpty(stammdatenschema.getDefinition())) {
            teaserTextRaw = stammdatenschema.getDefinition();
        } else if (StringUtils.isNotNullOrEmpty(stammdatenschema.getBezeichnungEingabe())) {
            teaserTextRaw = stammdatenschema.getBezeichnungEingabe();
        } else {
            teaserTextRaw = "Unbenanntes Formular";
        }

        return XdfStringUtils
                .cleanString(teaserTextRaw);
    }

    private static final Pattern ANZAHL_REGEX = Pattern.compile("^(0|([1-9][0-9]*)):(\\*|(0|([1-9][0-9]*)))$");

    private List<BaseFormElement> strukturToElements(XdfStruktur struktur, int depth) {
        Matcher matcher = ANZAHL_REGEX.matcher(struktur.getAnzahl());
        if (!matcher.matches()) {
            throw new RuntimeException("Invalid Anzahl format: " + struktur.getAnzahl());
        }
        String minAnzahlStr = matcher.group(1);
        String maxAnzahlStr = matcher.group(3);

        int minAnzahl = Integer.parseInt(minAnzahlStr);
        int maxAnzahl;
        if (maxAnzahlStr.equals("*")) {
            maxAnzahl = -1;
        } else {
            maxAnzahl = Integer.parseInt(maxAnzahlStr);
        }

        boolean isRequired = minAnzahl > 0;
        boolean isReplicating = maxAnzahl == -1 || maxAnzahl > 1;

        var enthaelt = struktur.getEnthaelt();
        if (enthaelt == null) {
            return new LinkedList<>();
        }

        var fields = new LinkedList<BaseFormElement>();

        var datenfeldgruppe = enthaelt.getDatenfeldgruppe();
        if (datenfeldgruppe != null) {
            var group = datenfeldGruppeToElements(datenfeldgruppe, isRequired, depth);
            fields.add(group);
        }

        var datenfeld = enthaelt.getDatenfeld();
        if (datenfeld != null) {
            var _fields = datenFeldToElements(xRepositoryCodeListService, datenfeld, isRequired);
            fields.addAll(_fields);
        }

        var firstElement = fields.isEmpty() ? null : fields.getFirst();

        if (firstElement instanceof FileUploadField fileUploadField) {
            fileUploadField.setIsMultifile(minAnzahl > 0 || maxAnzahl > 1 || maxAnzahl == -1);
            if (minAnzahl > 0) {
                fileUploadField.setMinFiles(minAnzahl);
                fileUploadField.setRequired(true);
            }
            if (maxAnzahl > 1) {
                fileUploadField.setMaxFiles(maxAnzahl);
                fileUploadField.setRequired(true);
            }
        } else if (isReplicating) {
            var label = switch (firstElement) {
                case Headline headline -> headline.getContent();
                case BaseInputElement<?> baseInputElement -> baseInputElement.getLabel();
                case null, default -> "Eintrag";
            };

            var hint = switch (firstElement) {
                case BaseInputElement<?> baseInputElement -> baseInputElement.getHint();
                case null, default -> null;
            };

            var replicatingContainer = new ReplicatingContainerLayout();
            replicatingContainer.setChildren(fields);
            replicatingContainer.setRequired(isRequired || minAnzahl > 0);
            replicatingContainer.setLabel(label);
            replicatingContainer.setHint(hint);
            replicatingContainer.setHeadlineTemplate(label + " #");
            replicatingContainer.setAddLabel(label + " hinzufügen");
            replicatingContainer.setRemoveLabel(label + " löschen");

            if (minAnzahl > 0) {
                replicatingContainer.setMinimumRequiredSets(minAnzahl);
            }

            if (maxAnzahl > 1) {
                replicatingContainer.setMaximumSets(maxAnzahl);
            }

            return List.of(replicatingContainer);
        }

        return fields;
    }

    private GroupLayout datenfeldGruppeToElements(XdfDatenfeldgruppe datenfeldgruppe, boolean isRequired, int depth) {
        var elements = new LinkedList<BaseFormElement>();

        if (depth >= 1) {
            var headline = new Headline()
                    .setContent(XdfStringUtils.cleanString(datenfeldgruppe.getName()));
            elements.add(headline);
        }

        for (var struktur : datenfeldgruppe.getStruktur()) {
            var fields = strukturToElements(struktur, depth + 1);
            elements.addAll(fields);
        }

        var group = new GroupLayout();
        group.setName(XdfStringUtils.idfToName(datenfeldgruppe.getIdentifikation()));
        group.setChildren(elements);

        return group;
    }

    public List<BaseFormElement> datenFeldToElements(XRepositoryCodeListService xRepositoryCodeListService, XdfDatenfeld datenfeld, boolean isRequired) {
        var fields = new LinkedList<BaseFormElement>();

        var id = XdfStringUtils
                .idfToName(datenfeld.getIdentifikation());
        var name = XdfStringUtils
                .cleanString(datenfeld.getName());

        var feldart = datenfeld.getFeldart().getCode();
        var datentyp = datenfeld.getDatentyp().getCode();

        switch (feldart) {
            case XdfFieldType.INPUT:
                var label = XdfStringUtils
                        .cleanString(datenfeld.getBezeichnungEingabe());
                var hint = XdfStringUtils
                        .cleanString(datenfeld.getHilfetextEingabe());

                XdfPraezisierung praezisierung = null;
                try {
                    praezisierung = parsePraezisierung(datenfeld.getPraezisierung());
                } catch (Exception err) {
                    fields.add(createErrorAlert(
                            "Fehler beim Laden der Präzisierung",
                            "Die Präzisierung des Feldes %s (%s) konnte nicht geladen werden: Präzisierung: \"%s\" - Fehler: %s",
                            name,
                            id,
                            datenfeld.getPraezisierung(),
                            err.getMessage()
                    ));
                }

                switch (datentyp) {
                    case XdfFieldType.TEXT:
                        var textField = new TextField();

                        textField.setId(id);
                        textField.setName(name);
                        textField.setLabel(label);
                        textField.setHint(hint);
                        textField.setRequired(isRequired);

                        if (praezisierung != null) {
                            if (praezisierung.getMinLength() != null) {
                                textField.setMinCharacters(praezisierung.getMinLength());
                                if (praezisierung.getMinLength() > 255) {
                                    textField.setIsMultiline(true);
                                }
                            }

                            if (praezisierung.getMaxLength() != null) {
                                textField.setMaxCharacters(praezisierung.getMaxLength());
                                if (praezisierung.getMaxLength() > 255) {
                                    textField.setIsMultiline(true);
                                }
                            }

                            if (praezisierung.getPattern() != null) {
                                textField.setPattern(
                                        TextPattern.of(
                                                praezisierung.getPattern(),
                                                String.format("Bitte geben Sie einen Wert an, der dem Muster \"%s\" entspricht.", praezisierung.getPattern())
                                        )
                                );
                            }
                        }
                        fields.add(textField);
                        break;
                    case XdfFieldType.BOOL:
                        var radioField = new RadioField();

                        radioField.setOptions(List.of(
                                RadioFieldOption.of("Ja", "ja"),
                                RadioFieldOption.of("Nein", "nein")
                        ));
                        radioField.setHint(hint);
                        radioField.setId(id);
                        radioField.setName(name);

                        fields.add(radioField);
                        break;
                    case XdfFieldType.NUMBER:
                        var floatNumberField = new NumberField();

                        floatNumberField.setId(id);
                        floatNumberField.setName(name);
                        floatNumberField.setLabel(label);
                        floatNumberField.setHint(hint);
                        floatNumberField.setRequired(isRequired);
                        floatNumberField.setDecimalPlaces(2);
                        floatNumberField.setValidation(getValidateFunc(id, praezisierung));

                        fields.add(floatNumberField);
                        break;
                    case XdfFieldType.INTEGER:
                        var intNumberField = new NumberField();

                        intNumberField.setId(id);
                        intNumberField.setName(name);
                        intNumberField.setLabel(label);
                        intNumberField.setHint(hint);
                        intNumberField.setRequired(isRequired);
                        intNumberField.setDecimalPlaces(0);
                        intNumberField.setValidation(getValidateFunc(id, praezisierung));

                        fields.add(intNumberField);
                        break;
                    case XdfFieldType.CURRENCY:
                        var currencyNumberField = new NumberField();

                        currencyNumberField.setId(id);
                        currencyNumberField.setName(name);
                        currencyNumberField.setLabel(label);
                        currencyNumberField.setHint(hint);
                        currencyNumberField.setRequired(isRequired);
                        currencyNumberField.setDecimalPlaces(2);
                        currencyNumberField.setValidation(getValidateFunc(id, praezisierung));
                        currencyNumberField.setSuffix("€");

                        fields.add(currencyNumberField);
                        break;
                    case XdfFieldType.FILE:
                        var uploadField = new FileUploadField();

                        uploadField.setId(id);
                        uploadField.setName(name);
                        uploadField.setLabel(label);
                        uploadField.setHint(hint);
                        uploadField.setRequired(isRequired);

                        fields.add(uploadField);
                        break;
                    case XdfFieldType.DATE:
                        var dateField = new DateField();

                        dateField.setId(id);
                        dateField.setName(name);
                        dateField.setLabel(label);
                        dateField.setHint(hint);
                        dateField.setRequired(isRequired);
                        dateField.setMode(DateType.Day);

                        fields.add(dateField);
                        break;
                    default:
                        fields.add(createErrorAlert(
                                "Nicht unterstützter Datentyp",
                                "Datentype %s für die Feldart %s für das Feld %s (%s) wird nicht unterstützt",
                                datentyp,
                                feldart,
                                name,
                                id
                        ));
                }
                break;
            case XdfFieldType.SELECT:
                var selectLabel = XdfStringUtils
                        .cleanString(datenfeld.getBezeichnungEingabe());
                var selectHint = XdfStringUtils
                        .cleanString(datenfeld.getHilfetextEingabe());


                var selectField = new SelectField();

                selectField.setId(id);
                selectField.setName(name);
                selectField.setLabel(selectLabel);
                selectField.setHint(selectHint);
                selectField.setRequired(isRequired);


                String codeListUrn = XdfStringUtils
                        .cleanString(datenfeld.getCodelisteReferenz().getGenericodeIdentification().getCanonicalVersionUri());

                if (StringUtils.isNotNullOrEmpty(codeListUrn)) {
                    try {
                        var options = xRepositoryCodeListService
                                .getRadioFieldOptionCodeList(codeListUrn);
                        selectField.setOptions(options);
                    } catch (ResponseException e) {
                        fields.add(createErrorAlert(
                                "CodeListe konnte nicht geladen werden",
                                "Die Code-Liste mit der URN %s für das Feld %s (%s) konnte nicht geladen werden. Die Fehlermeldung war: %s",
                                codeListUrn,
                                name,
                                id,
                                e.getTitle()
                        ));
                    }
                }

                fields.add(selectField);
                break;
            case XdfFieldType.LABEL:
                var alert = new Alert();

                alert.setId(id);
                alert.setName(name);
                alert.setTitle(XdfStringUtils.cleanString(datenfeld.getName()));
                alert.setText(XdfStringUtils.cleanString(datenfeld.getBezeichnungEingabe()));
                alert.setAlertType(AlertType.Info);

                fields.add(alert);
                break;
            default:
                fields.add(createErrorAlert(
                        "Nicht unterstützte Feldart",
                        "Feldart %s für das Feld %s (%s) wird nicht unterstützt",
                        datenfeld.getFeldart().getCode(),
                        name,
                        id
                ));
        }

        return fields;
    }

    private Alert createErrorAlert(String title, String message, Object... args) {
        var msg = String.format(message, args);
        return new Alert()
                .setTitle(title)
                .setText(msg)
                .setAlertType(AlertType.Error);
    }

    private XdfPraezisierung parsePraezisierung(String raw) throws Exception {
        if (StringUtils.isNullOrEmpty(raw)) {
            return null;
        }

        var om = new ObjectMapper();
        return om.readValue(raw, XdfPraezisierung.class);
    }

    private ElementValidationFunctions getValidateFunc(@Nonnull String fieldId,
                                                       @Nullable XdfPraezisierung praezisierung) {
        if (praezisierung == null) {
            return null;
        }

        if (praezisierung.getMaxValue() == null && praezisierung.getMinValue() == null) {
            return null;
        }

        var conditions = new LinkedList<Condition>();

        if (praezisierung.getMinValue() != null) {
            var cond = new Condition();

            cond.setReference(fieldId);
            cond.setOperator(ConditionOperator.GreaterThanOrEqual);
            cond.setValue(String.valueOf(praezisierung.getMinValue()));
            cond.setConditionUnmetMessage(String.format("Bitte geben Sie einen Wert größer oder gleich %d ein.", praezisierung.getMinValue()));

            conditions.add(cond);
        }


        if (praezisierung.getMaxValue() != null) {
            var cond = new Condition();

            cond.setReference(fieldId);
            cond.setOperator(ConditionOperator.LessThanOrEqual);
            cond.setValue(String.valueOf(praezisierung.getMaxValue()));
            cond.setConditionUnmetMessage(String.format("Bitte geben Sie einen Wert kleiner oder gleich %d ein.", praezisierung.getMaxValue()));

            conditions.add(cond);
        }


        var conditionSet = new ConditionSet();
        conditionSet.setOperator(ConditionSetOperator.All);
        conditionSet.setConditions(conditions);
        conditionSet.setConditionsSets(Collections.emptyList());

        var fs = new ElementValidationFunctions();

        fs.setConditionSet(conditionSet);

        return fs;
    }
}
