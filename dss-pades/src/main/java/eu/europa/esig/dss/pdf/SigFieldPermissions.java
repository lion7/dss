package eu.europa.esig.dss.pdf;

import eu.europa.esig.dss.enumerations.PdfLockAction;
import eu.europa.esig.dss.enumerations.CertificationPermission;

import java.util.List;

/**
 * This class defines a list of restrictions imposed to a PDF document modifications
 * by the current signature/field
 *
 */
public class SigFieldPermissions {

    /** Indicates the set of fields that should be locked */
    private PdfLockAction action;

    /** Contains a set of fields */
    private List<String> fields;

    /** The access permissions (optional) */
    private CertificationPermission certificationPermission;

    /**
     * Gets the defined action
     *
     * @return {@link PdfLockAction}
     */
    public PdfLockAction getAction() {
        return action;
    }

    /**
     * Sets the action
     *
     * @param action {@link PdfLockAction}
     */
    public void setAction(PdfLockAction action) {
        this.action = action;
    }

    /**
     * Gets a list of field names
     *
     * @return a list of {@link String}s
     */
    public List<String> getFields() {
        return fields;
    }

    /**
     * Sets a list of field names
     *
     * @param fields a list of {@link String}s
     */
    public void setFields(List<String> fields) {
        this.fields = fields;
    }

    /**
     * Gets the {@code CertificationPermission}
     *
     * @return {@link CertificationPermission}
     */
    public CertificationPermission getCertificationPermission() {
        return certificationPermission;
    }

    /**
     * Sets the {@code CertificationPermission}
     *
     * @param certificationPermission {@link CertificationPermission}
     */
    public void setCertificationPermission(CertificationPermission certificationPermission) {
        this.certificationPermission = certificationPermission;
    }

}
