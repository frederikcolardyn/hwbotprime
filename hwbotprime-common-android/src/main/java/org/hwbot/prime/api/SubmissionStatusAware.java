package org.hwbot.prime.api;

import org.hwbot.prime.service.SubmitResponse;

public interface SubmissionStatusAware {

	public void notifySubmissionDone(final SubmitResponse response);

}
