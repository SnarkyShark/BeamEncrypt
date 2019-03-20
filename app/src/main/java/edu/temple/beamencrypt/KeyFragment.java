package edu.temple.beamencrypt;


import android.content.Context;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;


/**
 * A simple {@link Fragment} subclass.
 */
public class KeyFragment extends Fragment {

    // Basic elements
    EditText usernameEditText;
    Button setUsernameButton;
    TextView usernameTextView;
    RecyclerView recyclerView;

    private String username;

    KeyInterface parent;

    public KeyFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        parent = (KeyInterface) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_key, container, false);

        usernameEditText = v.findViewById(R.id.usernameEditText);
        setUsernameButton = v.findViewById(R.id.setUsernameButton);
        usernameTextView = v.findViewById(R.id.usernameTextView);
        recyclerView = v.findViewById(R.id.recyclerView);

        setUsernameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String input = usernameEditText.getText().toString();
                if (input.compareTo("") != 0) {
                    setUsername(input);
                }
                else
                    Toast.makeText(getActivity(), "Please enter a username", Toast.LENGTH_SHORT).show();
            }
        });

        return v;
    }

    // sets username & generates public/private key pair
    private void setUsername(String inputUsername) {
        username = inputUsername;
        parent.setUsername();

        usernameTextView.setText(username);
    }

    // returns username for MainActivity
    public String getUsername() {
        return username;
    }

    interface KeyInterface {
        void setUsername();
    }

}
