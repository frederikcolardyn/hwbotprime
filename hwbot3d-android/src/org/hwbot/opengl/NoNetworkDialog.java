package org.hwbot.opengl;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;

public class NoNetworkDialog extends DialogFragment {
	// @Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// Use the Builder class for convenient dialog construction
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(R.string.no_network).setPositiveButton(R.string.no_network_btn, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				// Log.i(NoNetworkDialog.class.getSimpleName(), "okay");
			}
		});
		// Create the AlertDialog object and return it
		return builder.create();
	}
}
