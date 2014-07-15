package org.hwbot.prime.api;

import org.hwbot.prime.model.SubmissionRanking;

public interface SubmissionRankingAware {

	public void notifySubmissionRanking(final SubmissionRanking submissionRanking);

}
