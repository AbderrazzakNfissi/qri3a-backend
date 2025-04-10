package my.project.qri3a.enums;

public enum ScamStatus {
    PENDING("En attente"),
    CONFIRMED("Confirmé"),
    REJECTED("Rejeté"),
    UNDER_REVIEW("En cours d'examen");

    private final String label;

    ScamStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}