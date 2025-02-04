package eu.europa.esig.dss.pades.signature.visible.suite;

import eu.europa.esig.dss.alert.ExceptionOnStatusAlert;
import eu.europa.esig.dss.alert.LogOnStatusAlert;
import eu.europa.esig.dss.alert.StatusAlert;
import eu.europa.esig.dss.alert.exception.AlertException;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.model.MimeType;
import eu.europa.esig.dss.pades.PAdESSignatureParameters;
import eu.europa.esig.dss.pades.PAdESTimestampParameters;
import eu.europa.esig.dss.pades.SignatureFieldParameters;
import eu.europa.esig.dss.pades.SignatureImageParameters;
import eu.europa.esig.dss.pades.signature.PAdESService;
import eu.europa.esig.dss.pades.signature.suite.AbstractPAdESTestSignature;
import eu.europa.esig.dss.pdf.AbstractPDFSignatureService;
import eu.europa.esig.dss.pdf.IPdfObjFactory;
import eu.europa.esig.dss.pdf.PDFSignatureService;
import eu.europa.esig.dss.pdf.ServiceLoaderPdfObjFactory;
import eu.europa.esig.dss.signature.DocumentSignatureService;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PAdESVisibleSigOutsidePageTest extends AbstractPAdESTestSignature {

    private PAdESService service;
    private PAdESSignatureParameters signatureParameters;
    private DSSDocument documentToSign;

    @BeforeEach
    public void init() throws Exception {
        documentToSign = new InMemoryDocument(getClass().getResourceAsStream("/EmptyPage.pdf"));

        signatureParameters = new PAdESSignatureParameters();
        signatureParameters.setSigningCertificate(getSigningCert());
        signatureParameters.setCertificateChain(getCertificateChain());
        signatureParameters.setSignatureLevel(SignatureLevel.PAdES_BASELINE_B);

        SignatureImageParameters signatureImageParameters = new SignatureImageParameters();
        signatureImageParameters.setImage(new InMemoryDocument(getClass().getResourceAsStream("/small-red.jpg"), "small-red.jpg", MimeType.JPEG));

        SignatureFieldParameters fieldParameters = new SignatureFieldParameters();
        fieldParameters.setOriginX(-100);
        fieldParameters.setOriginY(-100);
        fieldParameters.setWidth(200);
        fieldParameters.setHeight(200);
        signatureImageParameters.setFieldParameters(fieldParameters);

        signatureParameters.setImageParameters(signatureImageParameters);

        service = new PAdESService(getOfflineCertificateVerifier());
    }

    @Override
    protected DSSDocument sign() {
        MockLogAlertPdfObjectFactory pdfObjectFactory = new MockLogAlertPdfObjectFactory();
        pdfObjectFactory.setAlertOnSignatureFieldOutsidePageDimensions(new ExceptionOnStatusAlert());
        service.setPdfObjFactory(pdfObjectFactory);

        Exception exception = assertThrows(AlertException.class, () -> super.sign());
        assertTrue(exception.getMessage().contains("The new signature field position is outside the page dimensions!"));

        pdfObjectFactory.setAlertOnSignatureFieldOutsidePageDimensions(new LogOnStatusAlert());

        return super.sign();
    }

    @Override
    protected DocumentSignatureService<PAdESSignatureParameters, PAdESTimestampParameters> getService() {
        return service;
    }

    @Override
    protected PAdESSignatureParameters getSignatureParameters() {
        return signatureParameters;
    }

    @Override
    protected DSSDocument getDocumentToSign() {
        return documentToSign;
    }

    @Override
    protected String getSigningAlias() {
        return GOOD_USER;
    }

    private static class MockLogAlertPdfObjectFactory implements IPdfObjFactory {

        private static final IPdfObjFactory pdfObjectFactory = new ServiceLoaderPdfObjFactory();

        private static AbstractPDFSignatureService service;

        static {
            service = (AbstractPDFSignatureService) pdfObjectFactory.newPAdESSignatureService();
        }

        public void setAlertOnSignatureFieldOutsidePageDimensions(StatusAlert alertOnSignatureFieldOutsidePageDimensions) {
            service.setAlertOnSignatureFieldOutsidePageDimensions(alertOnSignatureFieldOutsidePageDimensions);
        }

        @Override
        public PDFSignatureService newPAdESSignatureService() {
            return service;
        }

        @Override
        public PDFSignatureService newContentTimestampService() {
            return service;
        }

        @Override
        public PDFSignatureService newSignatureTimestampService() {
            return service;
        }

        @Override
        public PDFSignatureService newArchiveTimestampService() {
            return service;
        }

    }

}
