package com.rally.participation.dto;

import com.rally.participation.domain.Participation;

import java.util.List;

public record MyParticipationsPageResponse(
        List<MyParticipationsResponseItem> participations,
        int page,
        int size,
        int totalElements
) {

    public static MyParticipationsPageResponse of(List<Participation> content, int pageNumber, int pageSize, long totalElements) {
        return new MyParticipationsPageResponse(content.stream().map(MyParticipationsResponseItem::from).toList(), pageNumber, pageSize, (int) totalElements);
    }
}
