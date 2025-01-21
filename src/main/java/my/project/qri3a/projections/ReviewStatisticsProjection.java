package my.project.qri3a.projections;

public interface ReviewStatisticsProjection {
    Long getOneStarCount();
    Long getTwoStarCount();
    Long getThreeStarCount();
    Long getFourStarCount();
    Long getFiveStarCount();
    Double getAverageRating();
}