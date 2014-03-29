package org.hwbot.prime.api;

import android.view.View;
import android.widget.TextSwitcher;

public interface VoteObserver {

    public void notifyVoteFailed(View icon);

    public void notifyVoteSucceeded(View icon, TextSwitcher count);

}
