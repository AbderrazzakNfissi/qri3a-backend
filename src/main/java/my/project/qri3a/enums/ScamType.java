package my.project.qri3a.enums;

public enum ScamType {
    FAKE_PRODUCT("Produit inexistant ou faux"),
    ADVANCE_PAYMENT("Demande de paiement à l'avance"),
    SUSPICIOUS_PRICE("Prix anormalement bas"),
    COUNTERFEIT("Produit contrefait"),
    FALSE_DESCRIPTION("Description mensongère"),
    PHISHING("Tentative de phishing"),
    OTHER("Autre raison");

    private final String label;

    ScamType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}