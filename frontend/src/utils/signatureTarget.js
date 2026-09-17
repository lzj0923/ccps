const DEFAULT_SIGNATURE_PAGES = Object.freeze({
  property_management_agreement_draft: 7,
  rental_appointment_draft: 1,
  management_authorization_draft: 3,
  authorization_draft: 3,
  authorization: 3,
  termination_letter_draft: 2,
  rental_remittance_draft: 1,
  otr: 1,
  otr_document: 1,
  lease_contract: 12
});

export function signatureTargetPage(signature = {}) {
  const serverPage = Number(signature.signaturePage);
  if (Number.isInteger(serverPage) && serverPage > 0) return serverPage;
  return DEFAULT_SIGNATURE_PAGES[String(signature.documentKind || '').toLowerCase()] || 1;
}

export function pdfSignatureAnchor(page) {
  const safePage = Math.max(1, Number.parseInt(page, 10) || 1);
  return `#page=${safePage}&zoom=page-width`;
}
