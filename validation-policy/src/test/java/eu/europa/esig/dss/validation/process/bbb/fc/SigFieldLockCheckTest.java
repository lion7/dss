package eu.europa.esig.dss.validation.process.bbb.fc;

import eu.europa.esig.dss.detailedreport.jaxb.XmlConstraint;
import eu.europa.esig.dss.detailedreport.jaxb.XmlFC;
import eu.europa.esig.dss.detailedreport.jaxb.XmlStatus;
import eu.europa.esig.dss.diagnostic.SignatureWrapper;
import eu.europa.esig.dss.diagnostic.jaxb.XmlModificationDetection;
import eu.europa.esig.dss.diagnostic.jaxb.XmlObjectModification;
import eu.europa.esig.dss.diagnostic.jaxb.XmlObjectModifications;
import eu.europa.esig.dss.diagnostic.jaxb.XmlPDFLockDictionary;
import eu.europa.esig.dss.diagnostic.jaxb.XmlPDFRevision;
import eu.europa.esig.dss.diagnostic.jaxb.XmlPDFSignatureField;
import eu.europa.esig.dss.diagnostic.jaxb.XmlSignature;
import eu.europa.esig.dss.enumerations.CertificationPermission;
import eu.europa.esig.dss.enumerations.PdfLockAction;
import eu.europa.esig.dss.policy.jaxb.Level;
import eu.europa.esig.dss.policy.jaxb.LevelConstraint;
import eu.europa.esig.dss.validation.process.bbb.AbstractTestCheck;
import eu.europa.esig.dss.validation.process.bbb.fc.checks.SigFieldLockCheck;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SigFieldLockCheckTest extends AbstractTestCheck {

    @Test
    public void allFieldsLockedValid() throws Exception {
        XmlSignature xmlSignature = new XmlSignature();

        XmlPDFRevision pdfRevision = new XmlPDFRevision();
        xmlSignature.setPDFRevision(pdfRevision);

        XmlPDFSignatureField signatureField = new XmlPDFSignatureField();
        pdfRevision.getFields().add(signatureField);

        XmlPDFLockDictionary sigFieldLock = new XmlPDFLockDictionary();
        sigFieldLock.setAction(PdfLockAction.ALL);
        signatureField.setSigFieldLock(sigFieldLock);

        XmlModificationDetection modificationDetection = new XmlModificationDetection();
        pdfRevision.setModificationDetection(modificationDetection);

        XmlObjectModifications objectModifications = new XmlObjectModifications();
        modificationDetection.setObjectModifications(objectModifications);

        XmlObjectModification objectModification = new XmlObjectModification();
        objectModifications.getSignatureOrFormFill().add(objectModification);

        LevelConstraint constraint = new LevelConstraint();
        constraint.setLevel(Level.FAIL);

        XmlFC result = new XmlFC();
        SigFieldLockCheck sflc = new SigFieldLockCheck(i18nProvider, result, new SignatureWrapper(xmlSignature), constraint);
        sflc.execute();

        List<XmlConstraint> constraints = result.getConstraint();
        assertEquals(1, constraints.size());
        assertEquals(XmlStatus.OK, constraints.get(0).getStatus());
    }

    @Test
    public void allFieldsLockedFail() throws Exception {
        XmlSignature xmlSignature = new XmlSignature();

        XmlPDFRevision pdfRevision = new XmlPDFRevision();
        xmlSignature.setPDFRevision(pdfRevision);

        XmlPDFSignatureField signatureField = new XmlPDFSignatureField();
        pdfRevision.getFields().add(signatureField);

        XmlPDFLockDictionary sigFieldLock = new XmlPDFLockDictionary();
        sigFieldLock.setAction(PdfLockAction.ALL);
        signatureField.setSigFieldLock(sigFieldLock);

        XmlModificationDetection modificationDetection = new XmlModificationDetection();
        pdfRevision.setModificationDetection(modificationDetection);

        XmlObjectModifications objectModifications = new XmlObjectModifications();
        modificationDetection.setObjectModifications(objectModifications);

        XmlObjectModification objectModification = new XmlObjectModification();
        objectModification.setFieldName("Signature2");
        objectModifications.getSignatureOrFormFill().add(objectModification);

        LevelConstraint constraint = new LevelConstraint();
        constraint.setLevel(Level.FAIL);

        XmlFC result = new XmlFC();
        SigFieldLockCheck sflc = new SigFieldLockCheck(i18nProvider, result, new SignatureWrapper(xmlSignature), constraint);
        sflc.execute();

        List<XmlConstraint> constraints = result.getConstraint();
        assertEquals(1, constraints.size());
        assertEquals(XmlStatus.NOT_OK, constraints.get(0).getStatus());
    }

    @Test
    public void exclusiveFieldLockValid() throws Exception {
        XmlSignature xmlSignature = new XmlSignature();

        XmlPDFRevision pdfRevision = new XmlPDFRevision();
        xmlSignature.setPDFRevision(pdfRevision);

        XmlPDFSignatureField signatureField = new XmlPDFSignatureField();
        pdfRevision.getFields().add(signatureField);

        XmlPDFLockDictionary sigFieldLock = new XmlPDFLockDictionary();
        sigFieldLock.setAction(PdfLockAction.EXCLUDE);
        sigFieldLock.getFields().add("Signature2");
        sigFieldLock.getFields().add("Signature3");
        signatureField.setSigFieldLock(sigFieldLock);

        XmlModificationDetection modificationDetection = new XmlModificationDetection();
        pdfRevision.setModificationDetection(modificationDetection);

        XmlObjectModifications objectModifications = new XmlObjectModifications();
        modificationDetection.setObjectModifications(objectModifications);

        XmlObjectModification objectModification = new XmlObjectModification();
        objectModification.setFieldName("Signature2");
        objectModifications.getSignatureOrFormFill().add(objectModification);

        LevelConstraint constraint = new LevelConstraint();
        constraint.setLevel(Level.FAIL);

        XmlFC result = new XmlFC();
        SigFieldLockCheck sflc = new SigFieldLockCheck(i18nProvider, result, new SignatureWrapper(xmlSignature), constraint);
        sflc.execute();

        List<XmlConstraint> constraints = result.getConstraint();
        assertEquals(1, constraints.size());
        assertEquals(XmlStatus.OK, constraints.get(0).getStatus());
    }

    @Test
    public void exclusiveFieldLockFail() throws Exception {
        XmlSignature xmlSignature = new XmlSignature();

        XmlPDFRevision pdfRevision = new XmlPDFRevision();
        xmlSignature.setPDFRevision(pdfRevision);

        XmlPDFSignatureField signatureField = new XmlPDFSignatureField();
        pdfRevision.getFields().add(signatureField);

        XmlPDFLockDictionary sigFieldLock = new XmlPDFLockDictionary();
        sigFieldLock.setAction(PdfLockAction.EXCLUDE);
        sigFieldLock.getFields().add("Signature2");
        sigFieldLock.getFields().add("Signature3");
        signatureField.setSigFieldLock(sigFieldLock);

        XmlModificationDetection modificationDetection = new XmlModificationDetection();
        pdfRevision.setModificationDetection(modificationDetection);

        XmlObjectModifications objectModifications = new XmlObjectModifications();
        modificationDetection.setObjectModifications(objectModifications);

        XmlObjectModification objectModification = new XmlObjectModification();
        objectModification.setFieldName("Signature2");
        objectModifications.getSignatureOrFormFill().add(objectModification);

        XmlObjectModification objectModificationTwo = new XmlObjectModification();
        objectModificationTwo.setFieldName("Signature4");
        objectModifications.getSignatureOrFormFill().add(objectModificationTwo);

        LevelConstraint constraint = new LevelConstraint();
        constraint.setLevel(Level.FAIL);

        XmlFC result = new XmlFC();
        SigFieldLockCheck sflc = new SigFieldLockCheck(i18nProvider, result, new SignatureWrapper(xmlSignature), constraint);
        sflc.execute();

        List<XmlConstraint> constraints = result.getConstraint();
        assertEquals(1, constraints.size());
        assertEquals(XmlStatus.NOT_OK, constraints.get(0).getStatus());
    }

    @Test
    public void inclusiveFieldLockValid() throws Exception {
        XmlSignature xmlSignature = new XmlSignature();

        XmlPDFRevision pdfRevision = new XmlPDFRevision();
        xmlSignature.setPDFRevision(pdfRevision);

        XmlPDFSignatureField signatureField = new XmlPDFSignatureField();
        pdfRevision.getFields().add(signatureField);

        XmlPDFLockDictionary sigFieldLock = new XmlPDFLockDictionary();
        sigFieldLock.setAction(PdfLockAction.INCLUDE);
        sigFieldLock.getFields().add("Signature2");
        sigFieldLock.getFields().add("Signature3");
        signatureField.setSigFieldLock(sigFieldLock);

        XmlModificationDetection modificationDetection = new XmlModificationDetection();
        pdfRevision.setModificationDetection(modificationDetection);

        XmlObjectModifications objectModifications = new XmlObjectModifications();
        modificationDetection.setObjectModifications(objectModifications);

        XmlObjectModification objectModification = new XmlObjectModification();
        objectModification.setFieldName("Signature4");
        objectModifications.getSignatureOrFormFill().add(objectModification);

        LevelConstraint constraint = new LevelConstraint();
        constraint.setLevel(Level.FAIL);

        XmlFC result = new XmlFC();
        SigFieldLockCheck sflc = new SigFieldLockCheck(i18nProvider, result, new SignatureWrapper(xmlSignature), constraint);
        sflc.execute();

        List<XmlConstraint> constraints = result.getConstraint();
        assertEquals(1, constraints.size());
        assertEquals(XmlStatus.OK, constraints.get(0).getStatus());
    }

    @Test
    public void inclusiveFieldLockFail() throws Exception {
        XmlSignature xmlSignature = new XmlSignature();

        XmlPDFRevision pdfRevision = new XmlPDFRevision();
        xmlSignature.setPDFRevision(pdfRevision);

        XmlPDFSignatureField signatureField = new XmlPDFSignatureField();
        pdfRevision.getFields().add(signatureField);

        XmlPDFLockDictionary sigFieldLock = new XmlPDFLockDictionary();
        sigFieldLock.setAction(PdfLockAction.INCLUDE);
        sigFieldLock.getFields().add("Signature2");
        sigFieldLock.getFields().add("Signature3");
        signatureField.setSigFieldLock(sigFieldLock);

        XmlModificationDetection modificationDetection = new XmlModificationDetection();
        pdfRevision.setModificationDetection(modificationDetection);

        XmlObjectModifications objectModifications = new XmlObjectModifications();
        modificationDetection.setObjectModifications(objectModifications);

        XmlObjectModification objectModification = new XmlObjectModification();
        objectModification.setFieldName("Signature2");
        objectModifications.getSignatureOrFormFill().add(objectModification);

        XmlObjectModification objectModificationTwo = new XmlObjectModification();
        objectModificationTwo.setFieldName("Signature4");
        objectModifications.getSignatureOrFormFill().add(objectModificationTwo);

        LevelConstraint constraint = new LevelConstraint();
        constraint.setLevel(Level.FAIL);

        XmlFC result = new XmlFC();
        SigFieldLockCheck sflc = new SigFieldLockCheck(i18nProvider, result, new SignatureWrapper(xmlSignature), constraint);
        sflc.execute();

        List<XmlConstraint> constraints = result.getConstraint();
        assertEquals(1, constraints.size());
        assertEquals(XmlStatus.NOT_OK, constraints.get(0).getStatus());
    }

    @Test
    public void noChangesAllowedValid() throws Exception {
        XmlSignature xmlSignature = new XmlSignature();

        XmlPDFRevision pdfRevision = new XmlPDFRevision();
        xmlSignature.setPDFRevision(pdfRevision);

        XmlPDFSignatureField signatureField = new XmlPDFSignatureField();
        pdfRevision.getFields().add(signatureField);

        XmlPDFLockDictionary sigFieldLock = new XmlPDFLockDictionary();
        sigFieldLock.setAction(PdfLockAction.ALL);
        sigFieldLock.setPermissions(CertificationPermission.NO_CHANGE_PERMITTED);
        signatureField.setSigFieldLock(sigFieldLock);

        XmlModificationDetection modificationDetection = new XmlModificationDetection();
        pdfRevision.setModificationDetection(modificationDetection);

        XmlObjectModifications objectModifications = new XmlObjectModifications();
        modificationDetection.setObjectModifications(objectModifications);

        XmlObjectModification objectModification = new XmlObjectModification();
        objectModifications.getExtensionChanges().add(objectModification);

        LevelConstraint constraint = new LevelConstraint();
        constraint.setLevel(Level.FAIL);

        XmlFC result = new XmlFC();
        SigFieldLockCheck sflc = new SigFieldLockCheck(i18nProvider, result, new SignatureWrapper(xmlSignature), constraint);
        sflc.execute();

        List<XmlConstraint> constraints = result.getConstraint();
        assertEquals(1, constraints.size());
        assertEquals(XmlStatus.OK, constraints.get(0).getStatus());
    }

    @Test
    public void noChangesAllowedFail() throws Exception {
        XmlSignature xmlSignature = new XmlSignature();

        XmlPDFRevision pdfRevision = new XmlPDFRevision();
        xmlSignature.setPDFRevision(pdfRevision);

        XmlPDFSignatureField signatureField = new XmlPDFSignatureField();
        pdfRevision.getFields().add(signatureField);

        XmlPDFLockDictionary sigFieldLock = new XmlPDFLockDictionary();
        sigFieldLock.setAction(PdfLockAction.ALL);
        sigFieldLock.setPermissions(CertificationPermission.NO_CHANGE_PERMITTED);
        signatureField.setSigFieldLock(sigFieldLock);

        XmlModificationDetection modificationDetection = new XmlModificationDetection();
        pdfRevision.setModificationDetection(modificationDetection);

        XmlObjectModifications objectModifications = new XmlObjectModifications();
        modificationDetection.setObjectModifications(objectModifications);

        XmlObjectModification objectModification = new XmlObjectModification();
        objectModifications.getSignatureOrFormFill().add(objectModification);

        LevelConstraint constraint = new LevelConstraint();
        constraint.setLevel(Level.FAIL);

        XmlFC result = new XmlFC();
        SigFieldLockCheck sflc = new SigFieldLockCheck(i18nProvider, result, new SignatureWrapper(xmlSignature), constraint);
        sflc.execute();

        List<XmlConstraint> constraints = result.getConstraint();
        assertEquals(1, constraints.size());
        assertEquals(XmlStatus.NOT_OK, constraints.get(0).getStatus());
    }

    @Test
    public void minimalChangesAllowedValid() throws Exception {
        XmlSignature xmlSignature = new XmlSignature();

        XmlPDFRevision pdfRevision = new XmlPDFRevision();
        xmlSignature.setPDFRevision(pdfRevision);

        XmlPDFSignatureField signatureField = new XmlPDFSignatureField();
        pdfRevision.getFields().add(signatureField);

        XmlPDFLockDictionary sigFieldLock = new XmlPDFLockDictionary();
        sigFieldLock.setAction(PdfLockAction.ALL);
        sigFieldLock.setPermissions(CertificationPermission.MINIMAL_CHANGES_PERMITTED);
        signatureField.setSigFieldLock(sigFieldLock);

        XmlModificationDetection modificationDetection = new XmlModificationDetection();
        pdfRevision.setModificationDetection(modificationDetection);

        XmlObjectModifications objectModifications = new XmlObjectModifications();
        modificationDetection.setObjectModifications(objectModifications);

        XmlObjectModification objectModification = new XmlObjectModification();
        objectModifications.getExtensionChanges().add(objectModification);
        objectModifications.getSignatureOrFormFill().add(objectModification);

        LevelConstraint constraint = new LevelConstraint();
        constraint.setLevel(Level.FAIL);

        XmlFC result = new XmlFC();
        SigFieldLockCheck sflc = new SigFieldLockCheck(i18nProvider, result, new SignatureWrapper(xmlSignature), constraint);
        sflc.execute();

        List<XmlConstraint> constraints = result.getConstraint();
        assertEquals(1, constraints.size());
        assertEquals(XmlStatus.OK, constraints.get(0).getStatus());
    }

    @Test
    public void minimalChangesAllowedFail() throws Exception {
        XmlSignature xmlSignature = new XmlSignature();

        XmlPDFRevision pdfRevision = new XmlPDFRevision();
        xmlSignature.setPDFRevision(pdfRevision);

        XmlPDFSignatureField signatureField = new XmlPDFSignatureField();
        pdfRevision.getFields().add(signatureField);

        XmlPDFLockDictionary sigFieldLock = new XmlPDFLockDictionary();
        sigFieldLock.setAction(PdfLockAction.ALL);
        sigFieldLock.setPermissions(CertificationPermission.MINIMAL_CHANGES_PERMITTED);
        signatureField.setSigFieldLock(sigFieldLock);

        XmlModificationDetection modificationDetection = new XmlModificationDetection();
        pdfRevision.setModificationDetection(modificationDetection);

        XmlObjectModifications objectModifications = new XmlObjectModifications();
        modificationDetection.setObjectModifications(objectModifications);

        XmlObjectModification objectModification = new XmlObjectModification();
        objectModifications.getExtensionChanges().add(objectModification);
        objectModifications.getSignatureOrFormFill().add(objectModification);
        objectModifications.getAnnotationChanges().add(objectModification);

        LevelConstraint constraint = new LevelConstraint();
        constraint.setLevel(Level.FAIL);

        XmlFC result = new XmlFC();
        SigFieldLockCheck sflc = new SigFieldLockCheck(i18nProvider, result, new SignatureWrapper(xmlSignature), constraint);
        sflc.execute();

        List<XmlConstraint> constraints = result.getConstraint();
        assertEquals(1, constraints.size());
        assertEquals(XmlStatus.NOT_OK, constraints.get(0).getStatus());
    }

    @Test
    public void changesAllowedValid() throws Exception {
        XmlSignature xmlSignature = new XmlSignature();

        XmlPDFRevision pdfRevision = new XmlPDFRevision();
        xmlSignature.setPDFRevision(pdfRevision);

        XmlPDFSignatureField signatureField = new XmlPDFSignatureField();
        pdfRevision.getFields().add(signatureField);

        XmlPDFLockDictionary sigFieldLock = new XmlPDFLockDictionary();
        sigFieldLock.setAction(PdfLockAction.ALL);
        sigFieldLock.setPermissions(CertificationPermission.CHANGES_PERMITTED);
        signatureField.setSigFieldLock(sigFieldLock);

        XmlModificationDetection modificationDetection = new XmlModificationDetection();
        pdfRevision.setModificationDetection(modificationDetection);

        XmlObjectModifications objectModifications = new XmlObjectModifications();
        modificationDetection.setObjectModifications(objectModifications);

        XmlObjectModification objectModification = new XmlObjectModification();
        objectModifications.getExtensionChanges().add(objectModification);
        objectModifications.getSignatureOrFormFill().add(objectModification);
        objectModifications.getAnnotationChanges().add(objectModification);

        LevelConstraint constraint = new LevelConstraint();
        constraint.setLevel(Level.FAIL);

        XmlFC result = new XmlFC();
        SigFieldLockCheck sflc = new SigFieldLockCheck(i18nProvider, result, new SignatureWrapper(xmlSignature), constraint);
        sflc.execute();

        List<XmlConstraint> constraints = result.getConstraint();
        assertEquals(1, constraints.size());
        assertEquals(XmlStatus.OK, constraints.get(0).getStatus());
    }

    @Test
    public void changesAllowedFail() throws Exception {
        XmlSignature xmlSignature = new XmlSignature();

        XmlPDFRevision pdfRevision = new XmlPDFRevision();
        xmlSignature.setPDFRevision(pdfRevision);

        XmlPDFSignatureField signatureField = new XmlPDFSignatureField();
        pdfRevision.getFields().add(signatureField);

        XmlPDFLockDictionary sigFieldLock = new XmlPDFLockDictionary();
        sigFieldLock.setAction(PdfLockAction.ALL);
        sigFieldLock.setPermissions(CertificationPermission.CHANGES_PERMITTED);
        signatureField.setSigFieldLock(sigFieldLock);

        XmlModificationDetection modificationDetection = new XmlModificationDetection();
        pdfRevision.setModificationDetection(modificationDetection);

        XmlObjectModifications objectModifications = new XmlObjectModifications();
        modificationDetection.setObjectModifications(objectModifications);

        XmlObjectModification objectModification = new XmlObjectModification();
        objectModifications.getExtensionChanges().add(objectModification);
        objectModifications.getSignatureOrFormFill().add(objectModification);
        objectModifications.getAnnotationChanges().add(objectModification);
        objectModifications.getUndefined().add(objectModification);

        LevelConstraint constraint = new LevelConstraint();
        constraint.setLevel(Level.FAIL);

        XmlFC result = new XmlFC();
        SigFieldLockCheck sflc = new SigFieldLockCheck(i18nProvider, result, new SignatureWrapper(xmlSignature), constraint);
        sflc.execute();

        List<XmlConstraint> constraints = result.getConstraint();
        assertEquals(1, constraints.size());
        assertEquals(XmlStatus.NOT_OK, constraints.get(0).getStatus());
    }

}
