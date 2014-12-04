package org.hwbot.prime.api;

import org.hwbot.api.bench.dto.DeviceRecordsDTO;
import org.hwbot.api.esports.CompetitionStageDTO;

import java.util.List;

public interface CompetitionsStatusAware {

    public void notifyAvailableCompetitions(final List<CompetitionStageDTO> rounds);

}
