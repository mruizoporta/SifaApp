package yio.io.sifaapp.fragment;


import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import yio.io.sifaapp.Actualizacion.IUpdatePresenter;
import yio.io.sifaapp.Actualizacion.IUpdateView;
import yio.io.sifaapp.Actualizacion.UpdatePresenterImpl;
import yio.io.sifaapp.CarteraListActivity;
import yio.io.sifaapp.Login.ILoginView;
import yio.io.sifaapp.Login.LoginPresenter;
import yio.io.sifaapp.Login.LoginPresenterImplement;
import yio.io.sifaapp.R;
import yio.io.sifaapp.Venta.AppDialog;
import yio.io.sifaapp.model.ContadorModel;
import yio.io.sifaapp.utils.CustomDialog;
import yio.io.sifaapp.utils.TypeCounter;

/**
 * A simple {@link Fragment} subclass.
 */
public class ActualizarFragment extends Fragment implements IUpdateView, ILoginView {

    private static final String TAG = ActualizarFragment.class.getSimpleName();

    @Bind(R.id.progressBar)
    ProgressBar progressBar;
    @Bind(R.id.Local)
    ImageButton Local;
    @Bind(R.id.Server)
    ImageButton Server;
    @Bind(R.id.txtmessage)
    TextView txtmessage;
    @Bind(R.id.cateranum)
    TextView cateranum;
    @Bind(R.id.clientenum)
    TextView clientenum;
    @Bind(R.id.ventaenum)
    TextView ventaenum;
    @Bind(R.id.devolucionnum)
    TextView devolucionnum;
    @Bind(R.id.encargonum)
    TextView encargonum;
    private int progressStatus = 0;
    private Handler handler = new Handler();
    private IUpdatePresenter presenter;
    private LoginPresenter loginPresenter;
    int NOTIFICATION_DIALOG = 0;
    private static CustomDialog dlg;
    int cont = 0;

    ContadorModel contador = null;

    public ActualizarFragment() {
        // Required empty public constructor

    }
    /*
    @Override
    public void onDestroy() {
        super.onDestroy();
        if(this.presenter !=null) {
            this.presenter.onDestroy();
            this.presenter =null;
        }
        if(this.loginPresenter != null) {
            this.loginPresenter.onDestroy();
            this.loginPresenter = null;
        }
        Log.d(TAG,"onDestroy");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if(this.presenter !=null) {
            this.presenter.onDestroy();
            this.presenter =null;
        }
        if(this.loginPresenter != null) {
            this.loginPresenter.onDestroy();
            this.loginPresenter = null;
        }
        Log.d(TAG,"onDestroy");
    }
    */


    @Override
    public void onDestroy() {
        super.onDestroy();
        presenter.onDestroy();
        loginPresenter.onDestroy();
        Log.d(TAG, "onStop");
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }


    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_actualizar, container, false);
        ButterKnife.bind(this, view);

        if (this.presenter == null) {
            presenter = new UpdatePresenterImpl(this, getActivity().getApplicationContext());
            presenter.onCreated();
        }
        if (this.loginPresenter == null) {
            loginPresenter = new LoginPresenterImplement(this, getActivity().getApplication());
            loginPresenter.onCreated();
        }
        Log.d(TAG, "onCreateView");
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        progressBar.getProgressDrawable().setColorFilter(getResources().getColor(R.color.colorPurple), PorterDuff.Mode.SRC_IN);
        presenter.CountOfflineData();

        txtmessage.setText("");
        Log.d(TAG, "onViewCreated");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // ButterKnife.unbind(this);
/*
        if(this.presenter !=null) {
            this.presenter.onDestroy();
            this.presenter =null;
        }
        if(this.loginPresenter != null) {
            this.loginPresenter.onDestroy();
            this.loginPresenter = null;
        }
*/
        Log.d(TAG, "onDestroy");
        ButterKnife.unbind(this);
    }

    @OnClick(R.id.Server)
    public void download() {
        //setProgress("BAJANDO Lista del Sistema ", 100);
        loginPresenter.DownloadServer();
        Server.setBackground(getResources().getDrawable(R.drawable.round_button2));
    }

    @OnClick(R.id.Local)
    public void upload() {
        //setProgress("SUBIENDO Lista del Sistema ");
        // if(contador.getCartera()!= 0  contador.getClientes()!=0 && contador.getDevoluciones()!= 0 && contador.getVentas()!= 0  && contador.getEncargos() != 0 ) {
        progressBar.setProgress(progressStatus);
        //disableButtons();
        Local.setBackground(getResources().getDrawable(R.drawable.round_button2));
        presenter.UpdateClienteReferencia();
        cont++;
        Log.d(TAG, "upload");
        Log.d("COnt", String.valueOf(cont));
    }


    private void setProgress(final String label, final int counter) {
        progressStatus = 0;
        new Thread(new Runnable() {
            public void run() {
                while (progressStatus < counter) {
                    progressStatus += 1;
                    // Update the progress bar and display the
                    //current value in the text view
                    handler.post(new Runnable() {
                        public void run() {
                            progressBar.setProgress(progressStatus);
                            txtmessage.setText(label.concat(progressStatus + "/" + progressBar.getMax()));
                            int c = counter - progressStatus;
                            cateranum.setText(String.valueOf(c));
                        }
                    });
                    try {
                        // Sleep for 200 milliseconds.
                        //Just to display the progress slowly
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

    }

    @Override
    public void enableButtons() {
        /*getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Local.setEnabled(true);
                Server.setEnabled(true);
            }
        });*/
        //Local.setEnabled(true);
        // Server.setEnabled(false);
        //Server.setEnabled(true);
        Log.d(TAG,"enableButtons");
        Local.setEnabled(true);
        Server.setEnabled(true);

    }

    @Override
    public void disableButtons() {
        //Local.setEnabled(false);
        //Server.setEnabled(false);
        /*getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Local.setEnabled(false);
                Server.setEnabled(false);
            }
        });*/
        Log.d(TAG,"disableButtons");
        Local.setEnabled(false);
        Server.setEnabled(false);
    }

    @Override
    public void enableInputs() {
        //Server.setEnabled(true);
    }

    @Override
    public void disableInputs() {
        // Server.setEnabled(false);
    }

    @Override
    public void showProgress() {

    }

    @Override
    public void hideProgress() {
        hide();


    }

    @Override
    public void authenticate() {
        setDownLoad();
        showStatus("Descarga Completa", true);
    }

    @Override
    public void handleSignIn() {

    }

    @Override
    public void goToMainScreen() {
        hide();
        //dlg = null;
        loginPresenter.onDestroy();
        Intent intent = new Intent(getActivity(), CarteraListActivity.class);
        startActivity(intent);
    }

    @Override
    public void loginError(String message) {
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void Sync(String message) {
        showStatus(message);
    }

    @Override
    public void onSingOff() {

    }

    @Override
    public void onSystemSuccess() {

    }

    @Override
    public void UpdateCounter(TypeCounter type, int count) {
        String value = String.valueOf(count);
        if (type == TypeCounter.CLIENTE)
            clientenum.setText(value);
        if (type == TypeCounter.DEVOLUCION)
            devolucionnum.setText(value);
        if (type == TypeCounter.VENTA)
            ventaenum.setText(value);
        if (type == TypeCounter.ENCARGO)
            encargonum.setText(value);
        if (type == TypeCounter.COBRO)
            cateranum.setText(value);

    }

    @Override
    public void ClienteCounter(int count) {
        clientenum.setText(String.valueOf(count));
    }

    @Override
    public void notify(String message) {
        txtmessage.setText(message);
        Log.d(TAG,"NOTIFY");
    }

    @Override
    public void UpdateVentas() {
        presenter.UpdateVentas();
    }

    @Override
    public void UpdateEncargos() {
        presenter.UpdateEncargos();
    }

    @Override
    public void UpdateDevoluciones() {
        presenter.UpdateDevoluciones();
    }

    @Override
    public void UpdateCartera() {
        presenter.UpdateCartera();
    }

    @Override
    public void UpdateCliente() {
        presenter.UpdateCliente();
    }

    @Override
    public void ShowError(String message) {
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void setUpload() {
        Local.setBackground(getResources().getDrawable(R.drawable.round_button));
        Log.d(TAG,"setUpload");
    }

    @Override
    public void setDownLoad() {
        Server.setBackground(getResources().getDrawable(R.drawable.round_button));
        Log.d(TAG,"setDownLoad");
    }

    @Override
    public void CountOfflineData(ContadorModel contadores) {
        contador = contadores;
        cateranum.setText(String.valueOf(contadores.getCartera()));
        encargonum.setText(String.valueOf(contadores.getEncargos()));
        ventaenum.setText(String.valueOf(contadores.getVentas()));
        clientenum.setText(String.valueOf(contadores.getClientes()));
        devolucionnum.setText(String.valueOf(contadores.getDevoluciones()));
        Log.d(TAG,"CountOfflineData");
        /*
        if (contador.getCartera() == 0 && contador.getClientes() == 0 && contador.getDevoluciones() == 0 && contador.getVentas() == 0 && contador.getEncargos() == 0) {
            Server.setEnabled(true);
        } else {
            Server.setEnabled(false);
        }
        */
        enableButtons();
    }

    public void showprogress(String label) {
        progressBar.setProgress(progressStatus);
        txtmessage.setText(label.concat(progressStatus + "/" + progressBar.getMax()));

    }


    public void showStatus(final String mensaje, boolean... confirmacion) {

        if (confirmacion.length != 0 && confirmacion[0]) {

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AppDialog.showMessage(getActivity(), "", mensaje,
                            AppDialog.DialogType.DIALOGO_ALERTA,
                            new AppDialog.OnButtonClickListener() {
                                @Override
                                public void onButtonClick(AlertDialog _dialog,
                                                          int actionId) {

                                    if (AppDialog.OK_BUTTOM == actionId) {
                                        _dialog.dismiss();
                                    }
                                }
                            });

                }
            });


        } else {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    if (dlg != null) dlg.dismiss();
                    dlg = new CustomDialog(getActivity(), mensaje, false, NOTIFICATION_DIALOG);
                    dlg.show();
                }
            });

        }
    }

    public void hide() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dlg.cancel();
                if (dlg != null && dlg.isShowing())
                    dlg.dismiss();
            }
        });

    }


}
