package my.project.qri3a.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScamStatisticsDTO {

    private long totalScams;
    private long pendingScams;
    private long confirmedScams;
    private long rejectedScams;
    private long underReviewScams;

    private long scamsLastDay;
    private long scamsLastWeek;
    private long scamsLastMonth;

    private long uniqueProductsWithScams;
    private long productsWithMultipleScams;
}