package edi.md.mydesign.fragments;


import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import edi.md.mydesign.BaseApp;
import edi.md.mydesign.R;
import edi.md.mydesign.bottomsheet.SignUpBottomSheetDialog;
import edi.md.mydesign.realm.objects.ClientRealm;
import edi.md.mydesign.remote.ApiUtils;
import edi.md.mydesign.remote.CommandServices;
import edi.md.mydesign.remote.RemoteException;
import edi.md.mydesign.remote.authenticate.AuthenticateUserBody;
import edi.md.mydesign.remote.client.Client;
import edi.md.mydesign.remote.client.GetClientInfoResponse;
import edi.md.mydesign.remote.response.SIDResponse;
import edi.md.mydesign.utils.BaseEnum;
import io.realm.Realm;
import io.realm.RealmResults;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by Igor on 03.07.2020
 */

public class FragmentLoginFizic extends Fragment {

    TextInputEditText password, phone;
    TextInputLayout passwordLayout, phoneLayout;
    Button signIn;
    TextView signUp;

    Realm mRealm;
    ProgressDialog progressDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootViewAdmin = inflater.inflate(R.layout.fragment_login_fizic, container, false);

        phone = rootViewAdmin.findViewById(R.id.editTextPhoneLogin);
        password = rootViewAdmin.findViewById(R.id.editTextTextPasswordLogin);
        signIn = rootViewAdmin.findViewById(R.id.buttonSignInFizic);
        signUp = rootViewAdmin.findViewById(R.id.buttonSignUpFizic);
        passwordLayout = rootViewAdmin.findViewById(R.id.editTextTextPasswordLoginLayout);
        phoneLayout = rootViewAdmin.findViewById(R.id.editTextPhoneLoginPLayout);

        mRealm = Realm.getDefaultInstance();
        progressDialog = new ProgressDialog(getContext(),  R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog);

        signUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SignUpBottomSheetDialog auth = SignUpBottomSheetDialog.newInstance();
                auth.show(getParentFragmentManager(), SignUpBottomSheetDialog.TAG);
            }
        });

        signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String phoneNumber = phone.getText().toString();
                String passwords = password.getText().toString();


                if(phoneNumber.equals("") && passwords.equals("")){
                    phoneLayout.setError("Introduceți numărul de telefon!");
                    passwordLayout.setError("Introduceți parola!");
                }
                else{
                    if(phoneNumber.equals("") || passwords.equals("")){
                        if(phoneNumber.equals("")){
                            passwordLayout.setError("Introduceți numărul de telefon!");
                        }
                        if (passwords.equals("")){
                            passwordLayout.setError("Introduceți parola!");
                        }
                    }
                    else{
                        byte[] byePhone = new byte[0];
                        byte[] bytePass = new byte[0];
                        try {
                            byePhone = encrypt(phoneNumber.getBytes(),BaseApp.getAppInstance().getHuyYou());
                            bytePass = encrypt(passwords.getBytes(),BaseApp.getAppInstance().getHuyYou());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        RealmResults<ClientRealm> realmResults = mRealm.where(ClientRealm.class)
                                .equalTo("phone",byePhone)
                                .and()
                                .equalTo("password",bytePass)
                                .and()
                                .equalTo("companyId", BaseApp.getAppInstance().getCompanyClicked().getId())
                                .findAll();
                        if(realmResults.isEmpty()){
                            progressDialog.setIndeterminate(false);
                            progressDialog.setCancelable(false);
                            progressDialog.setMessage("autentificare...");
                            progressDialog.show();

                            auth(phoneNumber, passwords);
                        }
                        else{
                            for(ClientRealm clientRealm: realmResults){
                                String realmPhone = decrypt(clientRealm.getPhone(),BaseApp.getAppInstance().getHuyYou());
                                if(realmPhone.equals(phoneNumber)){
                                    new MaterialAlertDialogBuilder(getContext(), R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
                                            .setTitle("Oops!")
                                            .setMessage("Client with same phone already exist!")
                                            .setCancelable(false)
                                            .setPositiveButton("OK", (dialogInterface, i) -> {
                                                dialogInterface.dismiss();
                                            })
                                            .show();
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        });

        phone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(!charSequence.equals(""))
                    phoneLayout.setError(null);
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        password.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(!charSequence.equals(""))
                    passwordLayout.setError(null);
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        return rootViewAdmin;
    }

    private void auth (String phones, String pass){
        AuthenticateUserBody user = new AuthenticateUserBody();
        user.setPassword(pass);
        user.setPhone(phones);
        user.setAuthType(0);

        String url = BaseApp.getAppInstance().getCompanyClicked().getIp();
        CommandServices commandServices = ApiUtils.getCommandServices(url);
        Call<SIDResponse> call = commandServices.authenticateUser(BaseApp.getAppInstance().getCompanyClicked().getServiceName(), user);

        call.enqueue(new Callback<SIDResponse>() {
            @Override
            public void onResponse(Call<SIDResponse> call, Response<SIDResponse> response) {
                SIDResponse sidResponse = response.body();
                if(sidResponse != null){
                    if(sidResponse.getErrorCode() == 0){
                        String sId = sidResponse.getSID();

                        if(sId == null){
                            progressDialog.dismiss();
                            new MaterialAlertDialogBuilder(getContext(),R.style.MaterialAlertDialogCustom)
                                    .setTitle("Oops!")
                                    .setMessage(sidResponse.getErrorMessage())
                                    .setCancelable(false)
                                    .setPositiveButton("Am înţeles", (dialogInterface, i) -> {
                                        dialogInterface.dismiss();
                                    })
                                    .show();
                        }
                        else{
                            progressDialog.setMessage("obținerea informației despre client...");
                            getClientInfo(sId,phones,pass);
                        }

                    }
                    else{
                        String msg = RemoteException.getServiceException(sidResponse.getErrorCode());
                        progressDialog.dismiss();

                        new MaterialAlertDialogBuilder(getContext(),R.style.MaterialAlertDialogCustom)
                                .setTitle("Oops!")
                                .setMessage(msg)
                                .setCancelable(false)
                                .setPositiveButton("Am înţeles", (dialogInterface, i) -> {
                                    dialogInterface.dismiss();
                                })
                                .show();
                    }
                }
                else{
                    progressDialog.dismiss();

                    new MaterialAlertDialogBuilder(getContext(),R.style.MaterialAlertDialogCustom)
                            .setTitle("Oops!")
                            .setMessage("Răspunsul de la serviciu este gol!")
                            .setCancelable(false)
                            .setPositiveButton("Am înţeles", (dialogInterface, i) -> {
                                dialogInterface.dismiss();
                            })
                            .show();
                }
            }

            @Override
            public void onFailure(Call<SIDResponse> call, Throwable t) {
                progressDialog.dismiss();

                new MaterialAlertDialogBuilder(getContext(), R.style.MaterialAlertDialogCustom)
                        .setTitle("Oops!")
                        .setMessage("Operația a eșuat!\nEroare: " + t.getMessage())
                        .setCancelable(false)
                        .setPositiveButton("Am înţeles", (dialogInterface, i) -> {
                            dialogInterface.dismiss();
                        })
                        .setNegativeButton("Reîncercați", ((dialogInterface, i) -> {
                            progressDialog.setIndeterminate(false);
                            progressDialog.setCancelable(false);
                            progressDialog.setMessage("autentificare...");
                            progressDialog.show();
                            auth(phones, pass);
                        }))
                        .show();
            }
        });
    }

    private void getClientInfo(String sid, String phon, String pass){

        CommandServices commandServices = ApiUtils.getCommandServices(BaseApp.getAppInstance().getCompanyClicked().getIp());
        Call<GetClientInfoResponse> call = commandServices.getClientInfo(BaseApp.getAppInstance().getCompanyClicked().getServiceName(), sid);

        call.enqueue(new Callback<GetClientInfoResponse>() {
            @Override
            public void onResponse(Call<GetClientInfoResponse> call, Response<GetClientInfoResponse> response) {
                GetClientInfoResponse clientInfoResponse = response.body();
                if(clientInfoResponse != null && clientInfoResponse.getErrorCode() == 0){
                    try {
                        byte[] secPh = encrypt(phon.getBytes(), BaseApp.getAppInstance().getHuyYou());
                        byte[] secPa = encrypt(pass.getBytes(), BaseApp.getAppInstance().getHuyYou());

                        Client client = clientInfoResponse.getClient();
                        client.setPassword(secPa);
                        client.setPhone(secPh);
                        client.setSid(sid);
                        client.setTypeClient(BaseEnum.PersoanaFizica);

                        progressDialog.dismiss();
                        FragmentCabinetsAndCards.addedNewClient(client);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                else{
                    String msg = RemoteException.getServiceException(clientInfoResponse.getErrorCode());
                    //progressDialog dismiss
                    progressDialog.dismiss();

                    new MaterialAlertDialogBuilder(getContext(), R.style.MaterialAlertDialogCustom)
                            .setTitle("Oops!")
                            .setMessage(msg)
                            .setCancelable(false)
                            .setPositiveButton("Am înţeles", (dialogInterface, i) -> {
                                dialogInterface.dismiss();
                            })
                            .show();
                }
            }

            @Override
            public void onFailure(Call<GetClientInfoResponse> call, Throwable t) {
                progressDialog.dismiss();

                new MaterialAlertDialogBuilder(getContext(), R.style.MaterialAlertDialogCustom)
                        .setTitle("Oops!")
                        .setMessage("Operația a eșuat!\nEroare: " + t.getMessage())
                        .setCancelable(false)
                        .setPositiveButton("Am înţeles", (dialogInterface, i) -> {
                            dialogInterface.dismiss();
                        })
                        .setNegativeButton("Reîncercați", ((dialogInterface, i) -> {
                            progressDialog.setIndeterminate(false);
                            progressDialog.setCancelable(false);
                            progressDialog.setMessage("obținerea informații despre client...");
                            progressDialog.show();
                            getClientInfo(sid,phon,pass);
                        }))
                        .show();
            }
        });
    }

    public static byte[] encrypt(byte[] plaintext, byte[] balabol) throws Exception {
        byte[] IV = new byte[16];
        Cipher cipher = Cipher.getInstance("AES");
        SecretKeySpec keySpec = new SecretKeySpec(balabol, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(IV);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        byte[] cipherText = cipher.doFinal(plaintext);
        return cipherText;
    }

    public static String decrypt(byte[] cipherText, byte[] balabolDoi) {
        byte[] IV = new byte[16];
        try {
            Cipher cipher = Cipher.getInstance("AES");
            SecretKeySpec keySpec = new SecretKeySpec(balabolDoi, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(IV);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] decryptedText = cipher.doFinal(cipherText);
            return new String(decryptedText);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
