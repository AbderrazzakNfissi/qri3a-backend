package my.project.qri3a.dtos.responses;

import lombok.Data;

@Data
public class RatingsResponseDTO {
    private int reviewFive;
    private int reviewFour;
    private int reviewThree;
    private int reviewTwo;
    private int reviewOne;
}