/**
 * DSS - Digital Signature Services
 * Copyright (C) 2015 European Commission, provided under the CEF programme
 * 
 * This file is part of the "DSS - Digital Signature Services" project.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package eu.europa.esig.dss.xades.signature;

import eu.europa.esig.dss.DomUtils;
import eu.europa.esig.dss.definition.xmldsig.XMLDSigAttribute;
import eu.europa.esig.dss.definition.xmldsig.XMLDSigElement;
import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.enumerations.TimestampType;
import eu.europa.esig.dss.exception.IllegalInputException;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.model.DigestDocument;
import eu.europa.esig.dss.model.TimestampBinary;
import eu.europa.esig.dss.model.TimestampParameters;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.signature.SignatureExtension;
import eu.europa.esig.dss.signature.SignatureRequirementsChecker;
import eu.europa.esig.dss.signature.SigningOperation;
import eu.europa.esig.dss.spi.DSSASN1Utils;
import eu.europa.esig.dss.spi.DSSUtils;
import eu.europa.esig.dss.spi.x509.revocation.RevocationToken;
import eu.europa.esig.dss.spi.x509.revocation.crl.CRLToken;
import eu.europa.esig.dss.spi.x509.revocation.ocsp.OCSPToken;
import eu.europa.esig.dss.spi.x509.tsp.TSPSource;
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.dss.validation.AdvancedSignature;
import eu.europa.esig.dss.validation.CertificateVerifier;
import eu.europa.esig.dss.validation.SignatureCryptographicVerification;
import eu.europa.esig.dss.validation.ValidationData;
import eu.europa.esig.dss.validation.timestamp.TimestampToken;
import eu.europa.esig.dss.xades.DSSXMLUtils;
import eu.europa.esig.dss.xades.ProfileParameters;
import eu.europa.esig.dss.xades.XAdESSignatureParameters;
import eu.europa.esig.dss.xades.XAdESTimestampParameters;
import eu.europa.esig.dss.xades.definition.XAdESNamespaces;
import eu.europa.esig.dss.xades.definition.xades111.XAdES111Attribute;
import eu.europa.esig.dss.xades.definition.xades111.XAdES111Element;
import eu.europa.esig.dss.xades.definition.xades122.XAdES122Attribute;
import eu.europa.esig.dss.xades.definition.xades122.XAdES122Element;
import eu.europa.esig.dss.xades.definition.xades141.XAdES141Element;
import eu.europa.esig.dss.xades.validation.XAdESSignature;
import eu.europa.esig.dss.xades.validation.XMLDocumentValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static eu.europa.esig.dss.enumerations.SignatureLevel.XAdES_BASELINE_T;

/**
 * -T profile of XAdES signature
 *
 */
public class XAdESLevelBaselineT extends ExtensionBuilder implements SignatureExtension<XAdESSignatureParameters> {

	private static final Logger LOG = LoggerFactory.getLogger(XAdESLevelBaselineT.class);

	/**
	 * The object encapsulating the Time Stamp Protocol needed to create the level -T, of the signature
	 */
	protected TSPSource tspSource;

	/**
	 * The default constructor for XAdESLevelBaselineT.
	 *
	 * @param certificateVerifier {@link CertificateVerifier}
	 */
	public XAdESLevelBaselineT(final CertificateVerifier certificateVerifier) {
		super(certificateVerifier);
	}

	private void incorporateC14nMethod(final Element parentDom, final String signedInfoC14nMethod) {

		// <ds:CanonicalizationMethod Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#"/>
		final Element canonicalizationMethodDom = DomUtils.createElementNS(documentDom, getXmldsigNamespace(), XMLDSigElement.CANONICALIZATION_METHOD);
		canonicalizationMethodDom.setAttribute(XMLDSigAttribute.ALGORITHM.getAttributeName(), signedInfoC14nMethod);
		parentDom.appendChild(canonicalizationMethodDom);
	}

	@Override
	public DSSDocument extendSignatures(final DSSDocument dssDocument, final XAdESSignatureParameters params) throws DSSException {
		Objects.requireNonNull(dssDocument, "The document cannot be null");
		Objects.requireNonNull(tspSource, "The TSPSource cannot be null");
		this.params = params;
		final ProfileParameters context = params.getContext();
		if (LOG.isInfoEnabled()) {
			LOG.info("====> Extending: {}", (dssDocument.getName() == null ? "IN MEMORY DOCUMENT" : dssDocument.getName()));
		}

		documentValidator = new XMLDocumentValidator(dssDocument);
		documentValidator.setCertificateVerifier(certificateVerifier);
		documentValidator.setDetachedContents(params.getDetachedContents());

		documentDom = documentValidator.getRootElement();

		List<AdvancedSignature> signatures = documentValidator.getSignatures();
		if (Utils.isCollectionEmpty(signatures)) {
			throw new IllegalInputException("There is no signature to extend!");
		}

		// In the case of the enveloped signature we have a specific treatment:<br>
		// we will just extend the signature that is being created (during creation process)
		List<AdvancedSignature> signaturesToExtend = signatures;
		
		final SigningOperation operationKind = context.getOperationKind();
		if (SigningOperation.SIGN.equals(operationKind)) {
			String signatureId = params.getDeterministicId();

			for (AdvancedSignature signature : signatures) {
				if (!signatureId.equals(signature.getId())) {
					signaturesToExtend = Arrays.asList(signature);
				}
			}
		}

		extendSignatures(signaturesToExtend);

		return createXmlDocument();
	}

	/**
	 * Extends signatures to a desired level.<br>
	 * This method is overridden by other profiles.<br>
	 * For -T profile adds the SignatureTimeStamp element which contains a single HashDataInfo element that refers to
	 * the ds:SignatureValue element of the [XMLDSIG] signature. The timestamp token is obtained from TSP source.<br>
	 * Adds {@code <SignatureTimeStamp>} segment into {@code <UnsignedSignatureProperties>} element.
	 *
	 * @param signatures a list of {@link AdvancedSignature}s to extend
	 */
	protected void extendSignatures(List<AdvancedSignature> signatures) {
		final SignatureRequirementsChecker signatureRequirementsChecker = new SignatureRequirementsChecker(
				certificateVerifier, params);

		for (AdvancedSignature signature : signatures) {
			initializeSignatureBuilder((XAdESSignature) signature);

			assertExtendSignatureToTPossible();
			assertSignatureValid(xadesSignature);

			Element levelBUnsignedProperties = (Element) unsignedSignaturePropertiesDom.cloneNode(true);

			// The timestamp must be added only if there is no one or the extension -T level is being created
			if (!xadesSignature.hasTProfile() || XAdES_BASELINE_T.equals(params.getSignatureLevel())) {
				signatureRequirementsChecker.assertSigningCertificateIsValid(signature);

				final XAdESTimestampParameters signatureTimestampParameters = params.getSignatureTimestampParameters();
				final String canonicalizationMethod = signatureTimestampParameters.getCanonicalizationMethod();
				final byte[] canonicalizedValue = xadesSignature.getTimestampSource().getSignatureTimestampData(canonicalizationMethod);
				final DigestAlgorithm timestampDigestAlgorithm = signatureTimestampParameters.getDigestAlgorithm();
				final byte[] digestValue = DSSUtils.digest(timestampDigestAlgorithm, canonicalizedValue);
				createXAdESTimeStampType(TimestampType.SIGNATURE_TIMESTAMP, canonicalizationMethod, digestValue);

				unsignedSignaturePropertiesDom = indentIfPrettyPrint(unsignedSignaturePropertiesDom, levelBUnsignedProperties);
			}
		}
	}

	/**
	 * Checks if the extension is possible.
	 */
	private void assertExtendSignatureToTPossible() {
		final SignatureLevel signatureLevel = params.getSignatureLevel();
		if (XAdES_BASELINE_T.equals(signatureLevel) && (xadesSignature.hasLTProfile() || xadesSignature.hasLTAProfile())) {
			final String exceptionMessage = "Cannot extend signature. The signature is already extended with [%s].";
			throw new IllegalInputException(String.format(exceptionMessage, "XAdES LT"));
		}
	}

	/**
	 * Sets the TSP source to be used when extending the digital signature
	 *
	 * @param tspSource
	 *            the tspSource to set
	 */
	public void setTspSource(final TSPSource tspSource) {
		this.tspSource = tspSource;
	}

	/**
	 * This method incorporates all certificates passed as parameter :
	 * 
	 * <pre>
	 * {@code
	 * 	<xades:CertificateValues>
	 *		<xades:EncapsulatedX509Certificate>MIIC9TC...</xades:EncapsulatedX509Certificate>
	 *		...
	 * 	</xades:CertificateValues>
	 * }
	 * </pre>
	 *
	 * @param parentDom
	 *            the parent element
	 * @param certificatesToBeAdded
	 *            a collection of {@link CertificateToken}s to be added into the signature values element
	 * @return {@link Element} incorporated signature values element
	 */
	protected Element incorporateCertificateValues(final Element parentDom, final Collection<CertificateToken> certificatesToBeAdded) {
		Element certificateValuesDom = null;
		if (Utils.isCollectionNotEmpty(certificatesToBeAdded)) {
			certificateValuesDom = DomUtils.addElement(documentDom, parentDom, getXadesNamespace(), getCurrentXAdESElements().getElementCertificateValues());
			for (final CertificateToken certificateToken : certificatesToBeAdded) {
				final String base64EncodeCertificate = Utils.toBase64(certificateToken.getEncoded());
				DomUtils.addTextElement(documentDom, certificateValuesDom, getXadesNamespace(), getCurrentXAdESElements().getElementEncapsulatedX509Certificate(), base64EncodeCertificate);
			}
		}
		return certificateValuesDom;
	}

	/**
	 * This method incorporates revocation values.
	 * 
	 * <pre>
	 * 	{@code
	 * 		<xades:RevocationValues>
	 * 	}
	 * </pre>
	 *
	 * @param parentDom
	 *            the parent element
	 * @param crlsToAdd
	 *            a collection of {@link CRLToken}s to be added into the signature
	 * @param ocspsToAdd
	 *            a collection of {@link OCSPToken}s to be added into the signature
	 * @return {@link Element} incorporated revocation values element
	 */
	protected Element incorporateRevocationValues(final Element parentDom, final Collection<CRLToken> crlsToAdd, final Collection<OCSPToken> ocspsToAdd) {
		Element revocationValuesDom = null;
		
		if (Utils.isCollectionNotEmpty(crlsToAdd) || Utils.isCollectionNotEmpty(ocspsToAdd)) {
			revocationValuesDom = DomUtils.addElement(documentDom, parentDom, getXadesNamespace(), getCurrentXAdESElements().getElementRevocationValues());
			incorporateCrlTokens(revocationValuesDom, crlsToAdd);
			incorporateOcspTokens(revocationValuesDom, ocspsToAdd);
		}
		return revocationValuesDom;
	}

	/**
	 * This method incorporates the CRLValues :
	 * 
	 * <pre>
	 * 	{@code
	 * 		<xades:CRLValues>
	 * 			<xades:EncapsulatedCRLValue>...</xades:EncapsulatedCRLValue>
	 * 			...
	 * 		</xades:CRLValues>
	 * 	}
	 * </pre>
	 * 
	 * @param parentDom
	 *            the parent element
	 * @param crlTokens
	 *            a collection of CRL Tokens to be added
	 */
	private void incorporateCrlTokens(final Element parentDom, final Collection<CRLToken> crlTokens) {
		if (crlTokens.isEmpty()) {
			return;
		}
		final Element crlValuesDom = DomUtils.addElement(documentDom, parentDom, getXadesNamespace(), getCurrentXAdESElements().getElementCRLValues());

		for (final RevocationToken revocationToken : crlTokens) {
			final byte[] encodedCRL = revocationToken.getEncoded();
			final String base64EncodedCRL = Utils.toBase64(encodedCRL);
			DomUtils.addTextElement(documentDom, crlValuesDom, getXadesNamespace(), getCurrentXAdESElements().getElementEncapsulatedCRLValue(), base64EncodedCRL);
		}
	}

	/**
	 * This method incorporates the OCSP responses :
	 * 
	 * <pre>
	 * 	{@code
	 * 		<xades:OCSPValues>
	 * 			<xades:EncapsulatedOCSPValue>...</xades:EncapsulatedOCSPValue>
	 * 			...
	 * 		</xades:OCSPValues>
	 * 	}
	 * </pre>
	 * 
	 * @param parentDom
	 *            the parent element
	 * @param ocspTokens
	 *            a collection of OCSP Tokens to be added
	 */
	private void incorporateOcspTokens(Element parentDom, final Collection<OCSPToken> ocspTokens) {
		if (ocspTokens.isEmpty()) {
			return;
		}
		final Element ocspValuesDom = DomUtils.addElement(documentDom, parentDom, getXadesNamespace(), getCurrentXAdESElements().getElementOCSPValues());

		for (final RevocationToken revocationToken : ocspTokens) {
			final byte[] encodedOCSP = revocationToken.getEncoded();
			final String base64EncodedOCSP = Utils.toBase64(encodedOCSP);
			DomUtils.addTextElement(documentDom, ocspValuesDom, getXadesNamespace(), getCurrentXAdESElements().getElementEncapsulatedOCSPValue(), base64EncodedOCSP);
		}
	}
	
	/**
	 * This method incorporates all certificates passed as parameter, as well as adds missing indents if the parameter is specified
	 * 
	 * <pre>
	 * {@code
	 * 	<xades:CertificateValues>
	 *		<xades:EncapsulatedX509Certificate>MIIC9TC...</xades:EncapsulatedX509Certificate>
	 *		...
	 * 	</xades:CertificateValues>
	 * }
	 * </pre>
	 *
	 * @param parentDom
	 *            the parent element
	 * @param certificatesToBeAdded
	 *            the certificates to be added into the signature
	 * @param indent
	 *            {@link String} to add between elements (if not NULL)
	 */
	protected void incorporateCertificateValues(final Element parentDom,
												final Collection<CertificateToken> certificatesToBeAdded,
												String indent) {
		Element certificatesDom = incorporateCertificateValues(parentDom, certificatesToBeAdded);
		if (certificatesDom != null && indent != null) {
			DomUtils.setTextNode(documentDom, parentDom, indent);
			DSSXMLUtils.indentAndReplace(documentDom, certificatesDom);
		}
	}


	/**
	 * This method incorporates revocation values, as well as adds missing indents if the parameter is specified:
	 * 
	 * <pre>
	 * 	{@code
	 * 		<xades:RevocationValues>
	 * 	}
	 * </pre>
	 *
	 * @param parentDom
	 *            the parent element
	 * @param crlsToAdd
	 *            a collection of {@link CRLToken}s to be added into the signature
	 * @param ocspsToAdd
	 *            a collection of {@link OCSPToken}s to be added into the signature
	 * @param indent
	 *            {@link String} to add between elements (if not NULL)
	 */
	protected void incorporateRevocationValues(final Element parentDom, final Collection<CRLToken> crlsToAdd, 
			final Collection<OCSPToken> ocspsToAdd, String indent) {
		Element revocationDom = incorporateRevocationValues(parentDom, crlsToAdd, ocspsToAdd);
		if (revocationDom != null && indent != null) {
			DomUtils.setTextNode(documentDom, parentDom, indent);
			DSSXMLUtils.indentAndReplace(documentDom, revocationDom);
		}
	}

	/**
	 * This method removes old certificate values from the unsigned signature properties element.
	 *
	 * @return {@link String} indent
	 */
	protected String removeOldCertificateValues() {
		String text = null;
		final Element toRemove = xadesSignature.getCertificateValues();
		if (toRemove != null) {
			text = removeNode(toRemove);
			xadesSignature.resetCertificateSource();
		}
		return text;
	}

	/**
	 * This method removes old revocation values from the unsigned signature properties element.
	 */
	protected void removeOldRevocationValues() {
		final Element toRemove = xadesSignature.getRevocationValues();
		if (toRemove != null) {
			removeNode(toRemove);
			xadesSignature.resetRevocationSources();
		}
	}

	/**
	 * This method removes the timestamp validation data of the last archive timestamp.
	 *
	 * @return indent of the last {@code TimeStampValidationData} xml element, if present
	 */
	protected String removeLastTimestampValidationData() {
		final Element toRemove = xadesSignature.getLastTimestampValidationData();
		if (toRemove != null) {
			/* Certificate and revocation sources need to be reset because of
			 * the removing of timeStampValidationData element */
			xadesSignature.resetCertificateSource();
			xadesSignature.resetRevocationSources();

			return removeNode(toRemove);
		}
		return null;
	}

	/**
	 * This method incorporates the timestamp validation data in the signature
	 *
	 * @param validationDataForInclusion {@link ValidationData} to be included into the signature
	 * @param indent {@link String}
	 */
	protected void incorporateTimestampValidationData(final ValidationData validationDataForInclusion, String indent) {

		if (!validationDataForInclusion.isEmpty()) {

			Set<CertificateToken> certificateValuesToAdd = validationDataForInclusion.getCertificateTokens();
			Set<CRLToken> crlsToAdd = validationDataForInclusion.getCrlTokens();
			Set<OCSPToken> ocspsToAdd = validationDataForInclusion.getOcspTokens();

			final Element timeStampValidationDataDom = DomUtils.addElement(documentDom, unsignedSignaturePropertiesDom, getXades141Namespace(),
					XAdES141Element.TIMESTAMP_VALIDATION_DATA);

			incorporateCertificateValues(timeStampValidationDataDom, certificateValuesToAdd, indent);
			incorporateRevocationValues(timeStampValidationDataDom, crlsToAdd, ocspsToAdd, indent);

			String id = "1";
			final List<TimestampToken> archiveTimestamps = xadesSignature.getArchiveTimestamps();
			if (archiveTimestamps.size() > 0) {
				final TimestampToken timestampToken = archiveTimestamps.get(archiveTimestamps.size() - 1);
				id = timestampToken.getDSSIdAsString();
			}

			timeStampValidationDataDom.setAttribute("Id", "id-" + id);
			if (params.isPrettyPrint()) {
				DSSXMLUtils.indentAndReplace(documentDom, timeStampValidationDataDom);
			}

		}
	}

	/**
	 * This method incorporate timestamp type object.
	 */
	protected void incorporateArchiveTimestamp() {
		final XAdESTimestampParameters archiveTimestampParameters = params.getArchiveTimestampParameters();
		final String canonicalizationMethod = archiveTimestampParameters.getCanonicalizationMethod();
		final byte[] archiveTimestampData = xadesSignature.getTimestampSource().getArchiveTimestampData(canonicalizationMethod);
		if (LOG.isTraceEnabled()) {
			LOG.trace("Data to be signed by the ArchiveTimestamp:");
			LOG.trace(new String(archiveTimestampData));
		}
		final DigestAlgorithm timestampDigestAlgorithm = archiveTimestampParameters.getDigestAlgorithm();
		final byte[] digestBytes = DSSUtils.digest(timestampDigestAlgorithm, archiveTimestampData);
		createXAdESTimeStampType(TimestampType.ARCHIVE_TIMESTAMP, canonicalizationMethod, digestBytes);
	}

	/**
	 * Creates any XAdES TimeStamp object representation. The timestamp token is obtained from TSP source
	 *
	 * @param timestampType
	 *            {@code TimestampType}
	 * @param timestampC14nMethod
	 *            canonicalization method
	 * @param digestValue
	 *            array of {@code byte} representing the digest to timestamp
	 * @throws DSSException
	 *             in case of any error
	 */
	protected void createXAdESTimeStampType(final TimestampType timestampType, final String timestampC14nMethod, final byte[] digestValue) throws DSSException {

		if ((XAdESNamespaces.XADES_111.isSameUri(getXadesNamespace().getUri())
				|| XAdESNamespaces.XADES_122.isSameUri(getXadesNamespace().getUri()))
				&& TimestampType.SIGNATURE_TIMESTAMP != timestampType) {
			throw new UnsupportedOperationException("Signature Timestamp creation is only supported for XAdES 1.1.1 and 1.2.2");
		}		

		final TimestampParameters signatureTimestampParameters = params.getSignatureTimestampParameters();
		DigestAlgorithm timestampDigestAlgorithm = signatureTimestampParameters.getDigestAlgorithm();

		Element timeStampDom;
		switch (timestampType) {
			case SIGNATURE_TIMESTAMP:
				// <xades:SignatureTimeStamp Id="time-stamp-1dee38c4-8388-40d1-8880-9eeda853fe60">
				timeStampDom = DomUtils.addElement(documentDom, unsignedSignaturePropertiesDom, getXadesNamespace(), getCurrentXAdESElements().getElementSignatureTimeStamp());
				break;
			case VALIDATION_DATA_TIMESTAMP:
				// <xades:SigAndRefsTimeStamp Id="time-stamp-a762ab0e-e05c-4cc8-a804-cf2c4ffb5516">
				if (params.isEn319132()) {
					timeStampDom = DomUtils.addElement(documentDom, unsignedSignaturePropertiesDom, getXades141Namespace(), XAdES141Element.SIG_AND_REFS_TIMESTAMP_V2);
				} else {
					timeStampDom = DomUtils.addElement(documentDom, unsignedSignaturePropertiesDom, getXadesNamespace(), getCurrentXAdESElements().getElementSigAndRefsTimeStamp());
				}
				break;
			case VALIDATION_DATA_REFSONLY_TIMESTAMP:
				// <xades:RefsOnlyTimeStamp Id="time-stamp-a762ab0e-e05c-4cc8-a804-cf2c4ffb5516">
				if (params.isEn319132()) {
					timeStampDom = DomUtils.addElement(documentDom, unsignedSignaturePropertiesDom, getXades141Namespace(), XAdES141Element.REFS_ONLY_TIMESTAMP_V2);
				} else {
					timeStampDom = DomUtils.addElement(documentDom, unsignedSignaturePropertiesDom, getXadesNamespace(), getCurrentXAdESElements().getElementRefsOnlyTimeStamp());
				}
				break;
			case ARCHIVE_TIMESTAMP:
				// <xades141:ArchiveTimeStamp Id="time-stamp-a762ab0e-e05c-4cc8-a804-cf2c4ffb5516">
				timeStampDom = DomUtils.addElement(documentDom, unsignedSignaturePropertiesDom, getXades141Namespace(), XAdES141Element.ARCHIVE_TIMESTAMP);
				timestampDigestAlgorithm = params.getArchiveTimestampParameters().getDigestAlgorithm();
				break;
			default:
				// Content timestamps need to be generated before the signature itself
				throw new UnsupportedOperationException("Unsupported timestamp type : " + timestampType);
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("Timestamp generation: {} / {} / {}", timestampDigestAlgorithm.getName(), timestampC14nMethod,
					Utils.toBase64(digestValue));
		}
		final TimestampBinary timeStampToken = tspSource.getTimeStampResponse(timestampDigestAlgorithm, digestValue);
		final String base64EncodedTimeStampToken = Utils.toBase64(DSSASN1Utils.getDEREncoded(timeStampToken));
		
		if (XAdESNamespaces.XADES_122.isSameUri(getXadesNamespace().getUri())) {
			incorporateXAdES122Include(timeStampDom);
		}

		final String timestampId = UUID.randomUUID().toString();
		if (!XAdESNamespaces.XADES_111.isSameUri(getXadesNamespace().getUri())) {
			timeStampDom.setAttribute(XMLDSigAttribute.ID.getAttributeName(), "TS-" + timestampId);
			// <ds:CanonicalizationMethod Algorithm="http://www.w3.org/TR/2001/REC-xml-c14n-20010315"/>
			incorporateC14nMethod(timeStampDom, timestampC14nMethod);
		} else {
			incorporateHashDataInfo(timeStampDom, timestampC14nMethod);
		}
		
		// <xades:EncapsulatedTimeStamp Id="time-stamp-token-6a150419-caab-4615-9a0b-6e239596643a">MIAGCSqGSIb3DQEH
		final Element encapsulatedTimeStampDom = DomUtils.addElement(documentDom, timeStampDom, getXadesNamespace(), getCurrentXAdESElements().getElementEncapsulatedTimeStamp());
		encapsulatedTimeStampDom.setAttribute(XMLDSigAttribute.ID.getAttributeName(), "ETS-" + timestampId);
		DomUtils.setTextNode(documentDom, encapsulatedTimeStampDom, base64EncodedTimeStampToken);
	}

	/**
	 * <HashDataInfo URI="AI-NDS-HGI-32019423">
	 * 	<Transforms xmlns="http://www.w3.org/2000/09/xmldsig#">
	 * 		<Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#"></Transform>
	 *	</Transforms>
	 * </HashDataInfo>
	 *
	 * @param timeStampDom {@link Element}
	 * @param timestampC14nMethod {@link String} canonicalization algorithm for the timestamp
	 */
	private void incorporateHashDataInfo(Element timeStampDom, String timestampC14nMethod) {
		Element hashDataInfoDom = DomUtils.addElement(documentDom, timeStampDom, getXadesNamespace(), XAdES111Element.HASH_DATA_INFO);
		hashDataInfoDom.setAttribute(XAdES111Attribute.URI.getAttributeName(), '#' + xadesSignature.getId());
		Element transformsDom = DomUtils.addElement(documentDom, hashDataInfoDom, getXadesNamespace(), XAdES111Element.TRANSFORMS);
		Element transformDom = DomUtils.addElement(documentDom, transformsDom, getXmldsigNamespace(), XMLDSigElement.TRANSFORM);
		transformDom.setAttribute(XMLDSigAttribute.ALGORITHM.getAttributeName(), timestampC14nMethod);
	}

	private void incorporateXAdES122Include(Element timeStampDom) {
		Element includeDom = DomUtils.addElement(documentDom, timeStampDom, getXadesNamespace(), XAdES122Element.INCLUDE);
		includeDom.setAttribute(XAdES122Attribute.URI.getAttributeName(), '#' + xadesSignature.getSignatureValueId());
	}

	/**
	 * This method checks the signature integrity and throws a {@code DSSException} if the signature is broken.
	 *
	 * @throws DSSException in case of the cryptographic signature verification fails
	 */
	protected void checkSignatureIntegrity() throws DSSException {
		final SignatureCryptographicVerification signatureCryptographicVerification =
				xadesSignature.getSignatureCryptographicVerification();
		if (!signatureCryptographicVerification.isSignatureIntact()) {
			final String errorMessage = signatureCryptographicVerification.getErrorMessage();
			throw new DSSException("Cryptographic signature verification has failed" +
					(errorMessage.isEmpty() ? "." : (" / " + errorMessage)));
		}
	}

	/**
	 * Checks if the detached content represented by binary documents (used for -LTA level extension)
	 */
	protected void assertDetachedDocumentsContainBinaries() {
		List<DSSDocument> detachedContents = params.getDetachedContents();
		if (Utils.isCollectionNotEmpty(detachedContents)) {
			for (DSSDocument detachedDocument : detachedContents) {
				if (detachedDocument instanceof DigestDocument) {
					throw new IllegalArgumentException("XAdES-LTA requires complete binaries of signed documents! "
							+ "Extension with a DigestDocument is not possible.");
				}
			}
		}
	}

}
