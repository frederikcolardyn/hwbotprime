package org.hwbot.prime.api;

import android.view.View;
import android.widget.TextSwitcher;

public interface CommentObserver {

    public void notifyCommentFailed(View icon);

    public void notifyCommentSucceeded(View icon, TextSwitcher count);

}
