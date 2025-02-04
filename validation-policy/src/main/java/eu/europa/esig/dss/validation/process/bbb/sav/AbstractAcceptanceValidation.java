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
package eu.europa.esig.dss.validation.process.bbb.sav;

import eu.europa.esig.dss.detailedreport.jaxb.XmlCC;
import eu.europa.esig.dss.detailedreport.jaxb.XmlCryptographicValidation;
import eu.europa.esig.dss.detailedreport.jaxb.XmlSAV;
import eu.europa.esig.dss.diagnostic.AbstractTokenProxy;
import eu.europa.esig.dss.diagnostic.CertificateRefWrapper;
import eu.europa.esig.dss.diagnostic.jaxb.XmlDigestMatcher;
import eu.europa.esig.dss.enumerations.Context;
import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.i18n.I18nProvider;
import eu.europa.esig.dss.i18n.MessageTag;
import eu.europa.esig.dss.policy.SubContext;
import eu.europa.esig.dss.policy.ValidationPolicy;
import eu.europa.esig.dss.policy.jaxb.CryptographicConstraint;
import eu.europa.esig.dss.policy.jaxb.LevelConstraint;
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.dss.validation.process.Chain;
import eu.europa.esig.dss.validation.process.ChainItem;
import eu.europa.esig.dss.validation.process.ValidationProcessUtils;
import eu.europa.esig.dss.validation.process.bbb.sav.cc.CryptographicChecker;
import eu.europa.esig.dss.validation.process.bbb.sav.cc.DigestCryptographicChecker;
import eu.europa.esig.dss.validation.process.bbb.sav.checks.AllCertificatesInPathReferencedCheck;
import eu.europa.esig.dss.validation.process.bbb.sav.checks.CryptographicCheckerResultCheck;
import eu.europa.esig.dss.validation.process.bbb.sav.checks.DigestMatcherCryptographicCheckerResultCheck;
import eu.europa.esig.dss.validation.process.bbb.sav.checks.SigningCertificateAttributePresentCheck;
import eu.europa.esig.dss.validation.process.bbb.sav.checks.SigningCertificateRefDigestCryptographicCheckerResultCheck;
import eu.europa.esig.dss.validation.process.bbb.sav.checks.SigningCertificateReferencesValidityCheck;
import eu.europa.esig.dss.validation.process.bbb.sav.checks.UnicitySigningCertificateAttributeCheck;

import java.util.Date;
import java.util.List;

/**
 * 5.2.8 Signature acceptance validation (SAV) This building block covers any
 * additional verification to be performed on the signature itself or on the
 * attributes of the signature ETSI EN 319 132-1
 */
public abstract class AbstractAcceptanceValidation<T extends AbstractTokenProxy> extends Chain<XmlSAV> {

	/** The token to be validated */
	protected final T token;

	/** The validation time */
	protected final Date currentTime;

	/** The validation context */
	protected final Context context;

	/** The validation policy */
	protected final ValidationPolicy validationPolicy;

	/** The cryptographic information for the report */
	private XmlCryptographicValidation cryptographicValidation;

	/**
	 * Default constructor
	 *
	 * @param i18nProvider {@link I18nProvider}
	 * @param token to validate
	 * @param currentTime {@link Date}
	 * @param context {@link Context}
	 * @param validationPolicy {@link ValidationPolicy}
	 */
	public AbstractAcceptanceValidation(I18nProvider i18nProvider, T token, Date currentTime, Context context,
										ValidationPolicy validationPolicy) {
		super(i18nProvider, new XmlSAV());
		this.token = token;
		this.currentTime = currentTime;
		this.context = context;
		this.validationPolicy = validationPolicy;
	}

	protected ChainItem<XmlSAV> signingCertificateAttributePresent() {
		LevelConstraint constraint = validationPolicy.getSigningCertificateAttributePresentConstraint(context);
		return new SigningCertificateAttributePresentCheck(i18nProvider, result, token, constraint);
	}

	protected ChainItem<XmlSAV> unicitySigningCertificateAttribute() {
		LevelConstraint constraint = validationPolicy.getUnicitySigningCertificateAttributeConstraint(context);
		return new UnicitySigningCertificateAttributeCheck(i18nProvider, result, token, constraint);
	}

	protected ChainItem<XmlSAV> signingCertificateReferencesValidity() {
		LevelConstraint constraint = validationPolicy.getSigningCertificateRefersCertificateChainConstraint(context);
		return new SigningCertificateReferencesValidityCheck(i18nProvider, result, token, constraint);
	}

	protected ChainItem<XmlSAV> allCertificatesInPathReferenced() {
		LevelConstraint constraint = validationPolicy.getReferencesToAllCertificateChainPresentConstraint(context);
		return new AllCertificatesInPathReferencedCheck(i18nProvider, result, token, constraint);
	}

	protected ChainItem<XmlSAV> cryptographic() {
		ChainItem<XmlSAV> firstItem;
		
		// The basic signature constraints validation
		CryptographicConstraint constraint = validationPolicy.getSignatureCryptographicConstraint(context);
		MessageTag position = ValidationProcessUtils.getCryptoPosition(context);
		
		CryptographicChecker cc = new CryptographicChecker(i18nProvider, token, currentTime, position, constraint);
		XmlCC ccResult = cc.execute();
		
		ChainItem<XmlSAV> item = firstItem = cryptographicCheckResult(ccResult, position, constraint);

		cryptographicValidation = getCryptographicValidation(ccResult);
		cryptographicValidation.setConcernedMaterial(token.getId());
		
		if (!isValid(ccResult)) {
			// return if not valid
			return firstItem;
		}
		
		// process digestMatchers
		List<XmlDigestMatcher> digestMatchers = token.getDigestMatchers();
		if (Utils.isCollectionNotEmpty(digestMatchers)) {
			for (XmlDigestMatcher digestMatcher : digestMatchers) {
				DigestAlgorithm digestAlgorithm = digestMatcher.getDigestMethod();
				if (digestAlgorithm == null) {
					continue;
				}
				
				position = ValidationProcessUtils.getDigestMatcherCryptoPosition(digestMatcher);
				DigestCryptographicChecker dac = new DigestCryptographicChecker(
						i18nProvider, digestAlgorithm, currentTime, position, constraint);
				XmlCC dacResult = dac.execute();
				
				item = item.setNextItem(digestAlgorithmCheckResult(digestMatcher, dacResult, position, constraint));
				
				if (!isValid(dacResult)) {
					// update the failed constraints and brake the loop
					cryptographicValidation = getCryptographicValidation(dacResult);
					cryptographicValidation.setConcernedMaterial(getDigestMatcherDescription(digestMatcher, position));
					break;
				}
			}
		}

		if (token.isSigningCertificateReferencePresent()) {
			List<CertificateRefWrapper> signingCertificateReferences = token.getSigningCertificateReferences();
			for (CertificateRefWrapper certificateRefWrapper : signingCertificateReferences) {
				DigestAlgorithm digestAlgorithm = certificateRefWrapper.getDigestMethod();
				if (digestAlgorithm == null) {
					continue;
				}

				XmlCC dacResult = getSigningCertificateDigestCryptographicCheckResult(certificateRefWrapper);;

				item = item.setNextItem(signingCertificateRefDigestAlgoCheckResult(certificateRefWrapper, dacResult));

				if (!isValid(dacResult)) {
					cryptographicValidation = getCryptographicValidation(dacResult);
					cryptographicValidation.setConcernedMaterial(getTokenDescription(
							certificateRefWrapper.getCertificateId(), MessageTag.ACCM_POS_SIG_CERT_REF));
					break;
				}
			}
		}
		
		return firstItem;
	}
	
	private ChainItem<XmlSAV> cryptographicCheckResult(XmlCC ccResult, MessageTag position, CryptographicConstraint constraint) {
		return new CryptographicCheckerResultCheck<>(i18nProvider, result, token, currentTime, position, ccResult, constraint);
	}
	
	private ChainItem<XmlSAV> digestAlgorithmCheckResult(XmlDigestMatcher digestMatcher, XmlCC ccResult,
			MessageTag position, CryptographicConstraint constraint) {
		return new DigestMatcherCryptographicCheckerResultCheck<>(i18nProvider, result, currentTime, position,
				digestMatcher.getName(), ccResult, constraint);
	}

	private ChainItem<XmlSAV> signingCertificateRefDigestAlgoCheckResult(CertificateRefWrapper certificateRefWrapper,
																		 XmlCC ccResult) {
		LevelConstraint constraint = validationPolicy.getSigningCertificateDigestAlgorithmConstraint(context);
		return new SigningCertificateRefDigestCryptographicCheckerResultCheck(i18nProvider, result,
				currentTime, certificateRefWrapper, ccResult, constraint);
	}

	private XmlCC getSigningCertificateDigestCryptographicCheckResult(CertificateRefWrapper certificateRef) {
		SubContext subContext;
		CertificateRefWrapper signingCertificateReference = token.getSigningCertificateReference();
		if (signingCertificateReference != null &&
				signingCertificateReference.getCertificateId().equals(certificateRef.getCertificateId())) {
			subContext = SubContext.SIGNING_CERT;
		} else {
			subContext = SubContext.CA_CERTIFICATE;
		}

		CryptographicConstraint certificateConstraint = validationPolicy.getCertificateCryptographicConstraint(context, subContext);

		DigestCryptographicChecker dac = new DigestCryptographicChecker(i18nProvider, certificateRef.getDigestMethod(),
				currentTime, MessageTag.ACCM_POS_SIG_CERT_REF, certificateConstraint);
		return dac.execute();
	}

	@Override
	protected void addAdditionalInfo() {
		super.addAdditionalInfo();

		result.setCryptographicValidation(cryptographicValidation);
	}

	private XmlCryptographicValidation getCryptographicValidation(XmlCC ccResult) {
		XmlCryptographicValidation cryptographicValidation = new XmlCryptographicValidation();
		cryptographicValidation.setAlgorithm(ccResult.getVerifiedAlgorithm());
		cryptographicValidation.setNotAfter(ccResult.getNotAfter());
		cryptographicValidation.setSecure(isValid(ccResult));
		cryptographicValidation.setValidationTime(currentTime);
		return cryptographicValidation;
	}

	private String getDigestMatcherDescription(XmlDigestMatcher digestMatcher, MessageTag position) {
		if (Utils.isStringNotEmpty(digestMatcher.getName())) {
			return i18nProvider.getMessage(MessageTag.ACCM_DESC_WITH_NAME, position, digestMatcher.getName());
		}
		return i18nProvider.getMessage(position);
	}

	private String getTokenDescription(String id, MessageTag position) {
		return i18nProvider.getMessage(MessageTag.ACCM_DESC_WITH_ID, position, id);
	}

}
