/*Copyright [2010-2011] [David Van de Ven]

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */

package org.wahtod.wififixer.ui;

import org.wahtod.wififixer.R;
import org.wahtod.wififixer.WFConnection;
import org.wahtod.wififixer.legacy.ActionBarDetector;
import org.wahtod.wififixer.utility.WFScanResult;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class ConnectFragment extends FragmentSwitchboard implements
	OnClickListener {

    private static final String WPA = "WPA";
    private static final String WEP = "WEP";
    private WFScanResult network;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
	    Bundle savedInstanceState) {
	View v = inflater.inflate(R.layout.connect_fragment, null);
	Button b = (Button) v.findViewById(R.id.connect);
	View e = v.findViewById(R.id.password);
	TextView summary = (TextView) v.findViewById(R.id.password_summary);
	if (network.capabilities.length() == 0
		|| KnownNetworksFragment.getNetworks(getActivity()).contains(
			network.SSID)) {
	    e.setVisibility(View.INVISIBLE);
	    b.setText(getString(R.string.connect));
	    summary.setText(R.string.button_connect);
	}
	b.setOnClickListener(this);
	return v;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
	network = WFScanResult.fromBundle(this.getArguments());
	super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
	if (this.getArguments() != null) {
	    TextView ssid = (TextView) this.getView().findViewById(R.id.SSID);
	    ssid.setText(network.SSID);
	}
	super.onResume();
	ActionBarDetector.setUp(this.getActivity(), true, getActivity()
		.getString(R.string.connect_fragment_title)
		+ network.SSID);
    }

    public static String addquotes(String s) {
	final String QUOTE = "\"";
	return QUOTE + s + QUOTE;
    }

    private void addNetwork(final String password) {
	WifiConfiguration wf = getKeyAppropriateConfig(password);
	WifiManager wm = WFConnection.getWifiManager(getActivity()
		.getApplicationContext());
	int network = wm.addNetwork(wf);
	if (network != -1) {
	    wm.enableNetwork(network, false);
	    wm.saveConfiguration();
	}
    }

    private WifiConfiguration getKeyAppropriateConfig(final String password) {
	WifiConfiguration wf = new WifiConfiguration();
	wf.SSID = addquotes(network.SSID);
	if (network.capabilities.length() == 0) {
	    wf.BSSID = addquotes(network.BSSID);
	    wf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
	    return wf;
	}
	wf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
	wf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
	wf.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
	wf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
	wf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
	wf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
	wf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
	if (network.capabilities.contains(WEP)) {
	    wf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
	    wf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
	    wf.wepKeys[0] = addquotes(password);
	} else if (network.capabilities.contains(WPA)) {
	    wf.preSharedKey = addquotes(password);
	}
	return wf;
    }

    private void connectNetwork() {
	Intent intent = new Intent(WFConnection.CONNECTINTENT);
	intent.putExtra(WFConnection.NETWORKNAME, network.SSID);
	getActivity().sendBroadcast(intent);
    }

    public static ConnectFragment newInstance(Bundle bundle) {
	ConnectFragment f = new ConnectFragment();
	f.setArguments(bundle);
	return f;
    }

    private void notifyConnecting() {
	Toast.makeText(
		getActivity(),
		getActivity().getString(R.string.connecting_to_network)
			+ network.SSID, Toast.LENGTH_SHORT).show();
    }

    public void onClick(View v) {
	View e = ((View) v.getParent()).findViewById(R.id.password);
	String password = null;
	try {
	    password = String.valueOf(((EditText) e).getText());
	} catch (NullPointerException e1) {
	}
	if (password == null || password.length() == 0) {
	    if (network.capabilities.length() == 0) {
		addNetwork(null);
		notifyConnecting();
		connectNetwork();
	    } else if (KnownNetworksFragment.getNetworks(getActivity())
		    .contains(network.SSID))
		notifyConnecting();
	    connectNetwork();
	} else
	    addNetwork(password);

	InputMethodManager imm = (InputMethodManager) getActivity()
		.getSystemService(Context.INPUT_METHOD_SERVICE);
	imm.hideSoftInputFromWindow(e.getWindowToken(), 0);
	if (getActivity().getClass().equals(GenericFragmentActivity.class))
	    getActivity().finish();
	else {
	    Intent i = new Intent(getActivity(), WifiFixerActivity.class);
	    i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
	    i.putExtra(WifiFixerActivity.REMOVE_CONNECT_FRAGMENTS, true);
	    getActivity().startActivity(i);
	}
    }
}
