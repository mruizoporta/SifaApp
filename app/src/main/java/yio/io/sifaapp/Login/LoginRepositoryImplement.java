package yio.io.sifaapp.Login;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.Delete;
import com.raizlabs.android.dbflow.sql.language.Select;
import com.raizlabs.android.dbflow.structure.ModelAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import yio.io.sifaapp.R;
import yio.io.sifaapp.model.Cartera;
import yio.io.sifaapp.model.CarteraDetalle;
import yio.io.sifaapp.model.CarteraResponse;
import yio.io.sifaapp.model.Catalog;
import yio.io.sifaapp.model.Categoria;
import yio.io.sifaapp.model.Ciudad;
import yio.io.sifaapp.model.Configuration;
import yio.io.sifaapp.model.Customer;
import yio.io.sifaapp.model.Descuento;
import yio.io.sifaapp.model.ModelConfiguracion;
import yio.io.sifaapp.model.Pais;
import yio.io.sifaapp.model.Producto;
import yio.io.sifaapp.model.Productos;
import yio.io.sifaapp.model.Ruta;
import yio.io.sifaapp.model.authenticationRequest;
import yio.io.sifaapp.model.authenticationResponse;
import yio.io.sifaapp.utils.EventBus;
import yio.io.sifaapp.utils.Events;
import yio.io.sifaapp.utils.GreenRobotEventBus;
import yio.io.sifaapp.utils.IServicioRemoto;
import yio.io.sifaapp.utils.Network;
import yio.io.sifaapp.utils.ResponseMessage;
import yio.io.sifaapp.utils.SifacDataBase;

/**
 * Created by STARK on 15/06/2016.
 */
public class LoginRepositoryImplement implements LoginRepository {

    private final String TAG = this.getClass().getSimpleName();
    public final static String PARAM_USER_PASS = "USER_PASS";
    private AccountManager mAccountManager;
    Retrofit retrofit = null;
    IServicioRemoto service;
    Context context;
    boolean server = false;
    boolean session = false;

    public LoginRepositoryImplement( Context context) {
        this.context = context;
        try
        {
            if (retrofit == null)
            {
                Gson gson = new GsonBuilder()
                        .setExclusionStrategies(new ExclusionStrategy() {
                            @Override
                            public boolean shouldSkipField(FieldAttributes f) {
                                return f.getDeclaredClass().equals(ModelAdapter.class);
                            }

                            @Override
                            public boolean shouldSkipClass(Class<?> clazz) {
                                return false;
                            }
                        }).create();

                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(10200, TimeUnit.SECONDS)
                        .readTimeout(10000,TimeUnit.SECONDS).build();

                retrofit = new Retrofit.Builder()
                        .baseUrl(ModelConfiguracion.getURL_SERVER(context))
                        .client(client)
                        .addConverterFactory(GsonConverterFactory.create(gson))
                        .build();

            }
        }
        catch (Exception ex){
            postEvent(Events.onFailToRecoverySession, ex.getMessage());
        }
    }

    @Override
    public void signIn(String username, final String password) {

        Configuration configuration = new Select()
                                        .from(Configuration.class)
                                        .where(String.format("Login='%s' and Clave = '%s'", username.toUpperCase(),password ))
                                        .querySingle();

        if(configuration!=null)
        {
            if(configuration.getLogin().equals("SYSTEM") && configuration.getSession()==false){
                configuration.setSession(true);
                configuration.save();
                postEvent(Events.onSystemSuccess);
            }
            else
            {

                if (configuration.getSession() == false) {
                    session = true;
                    configuration.setSession(true);
                    configuration.save();
                    //GetCarteraByCobradorId();
                    postEvent(Events.goToMainScreen);
                } else {
                    postEvent(Events.goToMainScreen);
                }
            }
        }
       /* else if (new Select().from(Configuration.class).querySingle()!= null){
            postEvent(Events.onSigInError, "Usuario Invalido");
        }
        */
        else
        {
            ResponseMessage message = Network.isPhoneConnected(context);
            if (message != null && message.isHasError()) {
                postEvent(Events.onSigInError, message.getCause() + message.getMessage());
            } else {


                // Pin del telefono
                authenticationRequest request = new authenticationRequest(username, password,ModelConfiguracion.getDeviceID(context)); // "354885075003026"
                service = retrofit.create(IServicioRemoto.class);
                Call<authenticationResponse> authenticationResponseCall = service.AutenticarUsuario(request);
                authenticationResponseCall.enqueue(new Callback<authenticationResponse>() {

                    @Override
                    public void onResponse(Call<authenticationResponse> call, Response<authenticationResponse> response) {
                        if (response.body() != null) {
                            Log.d("onResponse", "onResponse...");
                            Log.d("onResponse",response.body().toString());
                            authenticationResponse _response = response.body();
                            if (_response.getTieneAcceso()) {
                                try {
                                    //Limpiamos todos los datos
                                   // FlowManager.getDatabase(SifacDataBase.NAME).reset(context);
                                    // LLenamos nuestra Tabla
                                    Configuration configuration = new Configuration();
                                    configuration.setClave(password);
                                    configuration.setLogin(response.body().getLogin());
                                    configuration.setHasAccess(response.body().getTieneAcceso());
                                    configuration.setSsgCuentaID(response.body().getSsgCuentaID().toString());
                                    configuration.setObjEmpleadoID(response.body().getObjEmpleadoID());
                                    configuration.setSession(true);
                                    configuration.setSystem(false);
                                    configuration.save();
                                    List<Configuration> l = new  Select().from(Configuration.class).queryList();
                                    postEvent(Events.onSuccess, null, "Sincronizando Paises...");
                                    GetPaisByCodigo();

                                } catch (Exception e) {
                                    postEvent(Events.onSigInError, e.getMessage());
                                    Log.d("authenticationResponse", "onSigInSuccess");
                                }
                            }
                            else {
                                postEvent(Events.onSigInError,"EL usuario no tiene permiso! Consulte al administrador si ha agregado su imei del dispositiv0");
                                Log.d("authenticationResponse", "no permiso");
                            }
                        } else {
                            postEvent(Events.onSigInError, "Usuario Invalido");
                        }
                    }

                    @Override
                    public void onFailure(Call<authenticationResponse> call, Throwable t) {
                        postEvent(Events.onSigInError, t.getMessage());
                    }
                });

                /* TEMPORAL

                Configuration configuration2 = new Configuration();
                configuration2.setClave(password);
                configuration2.setLogin("jperez");
                configuration2.setHasAccess(true);
                configuration2.setSsgCuentaID("2");
                configuration2.setObjEmpleadoID(2);
                configuration2.setSession(true);
                configuration2.setSystem(false);
                configuration2.save();

                GetPaisByCodigo();
                */

            }
        }
    }

    @Override
    public void signOut() {
        if(getsession () ) {
            Configuration configuration = new Select().from(Configuration.class).querySingle();
            if(configuration!=null){
                configuration.setSession(false);
                configuration.save();
                postEvent(Events.onSingOff);
            }
        }

        /*
        if (!session) {
            postEvent(Events.onFailToRecoverySession, "Usuario Invalido");
        } else
            postEvent(Events.onSuccessToRecoverySession);
         */
    }

    @Override
    public void checkSession(Context context) {

        Configuration configuration = new Select()
                                        .from(Configuration.class)
                                         .where(String.format("System='0'"))
                                        .querySingle();
        if(configuration!=null && configuration.getSession() && configuration.getSystem()==false){
            postEvent(Events.onSuccessToRecoverySession);
        }
        else
        {
            boolean islogin = false;
            // Getting all registered Our Application Accounts;
            try
            {
                Account[] accounts = AccountManager.get(context).getAccountsByType(context.getString(R.string.auth_type));
                for (Account account : accounts) {
                    islogin = true;
                    break;
                }
            }
            catch (Exception e) {
                Log.i(TAG, "Exception:" + e);
                postEvent(Events.onFailToRecoverySession, e.getMessage());
            }

            if (!islogin) {
                postEvent(Events.onFailToRecoverySession, "Inicie Session !");
            }
            else
            {
                if(configuration.getSession())
                    postEvent(Events.onSuccessToRecoverySession);
                else
                    postEvent(Events.onFailToRecoverySession, "Inicie Session !");
            }

        }
    }

    @Override
    public void GetProductos() {
        ResponseMessage message = Network.isPhoneConnected(context);
        if (message != null && message.isHasError()) {
            postEvent(Events.onSigInError, message.getCause() + message.getMessage());
        } else {

            Configuration configuration =  new Select()
                                                    .from(Configuration.class)
                                                    .where(String.format("System=0"))
                                                    .querySingle();


            Call<List<Producto>> productoResponseCall = service.GetProductos(configuration.getObjEmpleadoID());
            productoResponseCall.enqueue(new Callback<List<Producto>>() {
                @Override
                public void onResponse(Call<List<Producto>> call, Response<List<Producto>> response) {
                    if (response.body() != null) {
                        //Clear all data if exist
                        new Delete().from(Producto.class).query();

                        List<Producto> list = (List<Producto>) response.body();
                        for (Producto producto : list) {
                            producto.save();
                        }
                        postEvent(Events.onSuccess, null, "Sincronizando Ciudades ...");

                    } else {
                        postEvent(Events.onSuccess, null, "Sincronizando Ciudades ...");
                        //postEvent(Events.onSuccess, null, "Error Sincronizando Ciudades ...");
                    }
                    GetCiudades();
                }

                @Override
                public void onFailure(Call<List<Producto>> call, Throwable t) {
                    postEvent(Events.onSyncProductosError, "Error Sincronizando Productos");
                }
            });
        }
    }

    @Override
    public void GetCatalogos() {
        ResponseMessage message = Network.isPhoneConnected(context);
        if (message != null && message.isHasError()) {
            postEvent(Events.onSigInError, message.getCause() + message.getMessage());
        } else {
            Call<List<Catalog>> catalogResponseCall = service.GetValoresCatalogos();

            catalogResponseCall.enqueue(new Callback<List<Catalog>>() {
                @Override
                public void onResponse(Call<List<Catalog>> call, Response<List<Catalog>> response) {
                    if (response.body() != null) {
                        //Clear all data if exist
                        new Delete().from(Catalog.class).query();

                        List<Catalog> list = (List<Catalog>) response.body();

                        for (Catalog catalog : list) {
                            catalog.save();
                        }
                    }
                    Log.d(TAG, "GetCatalogos");
                    postEvent(Events.onSuccess, null, "Sincronizando Descuentos...");
                    GetDescuentos();
                }

                @Override
                public void onFailure(Call<List<Catalog>> call, Throwable t) {
                    postEvent(Events.onSyncCatalogoError, "Error Sincronizando Catalogos");
                }
            });
        }
    }

    @Override
    public void GetClientesByCobradorId() {

        ResponseMessage message = Network.isPhoneConnected(context);
        if (message != null && message.isHasError()) {
            postEvent(Events.onSigInError, message.getCause() + message.getMessage());
        } else {

            Configuration configuration =  new Select()
                                                .from(Configuration.class)
                                                .where(String.format("System=0"))
                                                .querySingle();

            Call<List<Customer>> customerResponseCall = service.GetClientesByCobradorId(configuration.getObjEmpleadoID());
            customerResponseCall.enqueue(new Callback<List<Customer>>() {
                @Override
                public void onResponse(Call<List<Customer>> call, Response<List<Customer>> response) {
                    if (response.body() != null) {
                        //Clear all data if exist
                        new Delete().from(Customer.class).query();

                        List<Customer> list = (List<Customer>) response.body();
                        for (Customer customer : list) {
                            customer.setOffline(false);
                            customer.save();
                        }
                    }
                    postEvent(Events.onSuccess, null, "Sincronizando Cartera ...");
                    GetCarteraByCobradorId();
                }

                @Override
                public void onFailure(Call<List<Customer>> call, Throwable t) {
                    postEvent(Events.onSigInError, "Error Sincronizando Clientes");
                }
            });
        }
    }

    @Override
    public void GetDescuentos() {
        ResponseMessage message = Network.isPhoneConnected(context);
        if (message != null && message.isHasError()) {
            postEvent(Events.onSigInError, message.getCause() + message.getMessage());
        } else {
            Call<List<Descuento>> descuentoResponseCall = service.GetDescuentos();
            descuentoResponseCall.enqueue(new Callback<List<Descuento>>() {

                @Override
                public void onResponse(Call<List<Descuento>> call, Response<List<Descuento>> response) {
                    if (response.body() != null) {
                        new Delete().from(Descuento.class).query();

                        List<Descuento> descuentos = (List<Descuento>) response.body();
                        for (Descuento descuento : descuentos) {
                            descuento.save();
                        }
                    }
                    postEvent(Events.onSuccess, null, "Sincronizando Productos ...");
                    GetProductos();
                }

                @Override
                public void onFailure(Call<List<Descuento>> call, Throwable t) {
                    postEvent(Events.onSyncDescuentosError, "Error Sincronizando Descuentos");
                }
            });
        }
    }

    @Override
    public void GetCategoriasProductos() {
        ResponseMessage message = Network.isPhoneConnected(context);
        if (message != null && message.isHasError()) {
            postEvent(Events.onSigInError, message.getCause() + message.getMessage());
        } else {
            Call<List<Categoria>> categoriaResponseCall = service.GetCategoriasProductos();
            categoriaResponseCall.enqueue(new Callback<List<Categoria>>() {

                @Override
                public void onResponse(Call<List<Categoria>> call, Response<List<Categoria>> response) {
                    if (response.body() != null) {
                        new Delete().from(Categoria.class).where("1=1").query();

                        List<Categoria> categorias = (List<Categoria>) response.body();
                        for (Categoria categoria : categorias) {
                            categoria.save();
                        }
                    }
                    postEvent(Events.onSuccess, null, "Sincronizando Rutas ...");
                    GetRutasByCobradorId();

                }

                @Override
                public void onFailure(Call<List<Categoria>> call, Throwable t) {
                    postEvent(Events.onSyncCategoriasProductosError, "Error Sincronizando Categoria de Producto");
                }
            });
        }
    }

    @Override
    public void GetCiudades() {
        ResponseMessage message = Network.isPhoneConnected(context);
        if (message != null && message.isHasError()) {
            postEvent(Events.onSigInError, message.getCause() + message.getMessage());
        } else {
            Call<List<Ciudad>> ciudadResponseCall = service.GetCiudadesByPais(558);
            ciudadResponseCall.enqueue(new Callback<List<Ciudad>>() {
                @Override
                public void onResponse(Call<List<Ciudad>> call, Response<List<Ciudad>> response) {
                    if (response.body() != null) {
                        new Delete().from(Ciudad.class).query(); //.where("1=1").query();
                        List<Ciudad> ciudades = (List<Ciudad>) response.body();
                        for (Ciudad ciudad : ciudades) {
                            ciudad.save();
                        }
                    }
                    postEvent(Events.onSuccess, null, "Sincronizando Categorias ...");
                    GetCategoriasProductos();

                }

                @Override
                public void onFailure(Call<List<Ciudad>> call, Throwable t) {
                    postEvent(Events.onSyncCiudadesError, "Error Sincronizando Ciudades");
                }
            });
        }
    }

    @Override
    public void GetCarteraByCobradorId()
    {
        ResponseMessage message = Network.isPhoneConnected(context);
        if (message != null && message.isHasError() && session == false) {
            postEvent(Events.onSigInError, message.getCause() + message.getMessage());
        }
        if (message != null && message.isHasError() && session == true) {
            postEvent(Events.onSyncCarteraSucess ,  message.getCause() + message.getMessage());
        }
        else
        {
            Configuration configuration =  new Select()
                                            .from(Configuration.class)
                                            .where(String.format("System='0'"))
                                            .querySingle();
            if(service ==null)
                service = retrofit.create(IServicioRemoto.class);

            try
            {

                Call<List<CarteraResponse>> carteraResponseCall = service.GetCarteraByCobradorId(configuration.getObjEmpleadoID());

                carteraResponseCall.enqueue(new Callback<List<CarteraResponse>>() {

                    @Override
                    public void onResponse(Call<List<CarteraResponse>> call, Response<List<CarteraResponse>> response) {
                        Log.d("onResponse", "carteraResponseCall...");
                        try {
                            if (response.body() != null) {

                                new Delete().from(CarteraDetalle.class).query();
                                new Delete().from(Cartera.class).query();

                                //Clear all data if exist
                                int orden = 1;
                                List<CarteraResponse> list = (List<CarteraResponse>) response.body();
                                for (CarteraResponse cartera : list) {
                                    if (!cartera.getSccCuentaID().equals("0")) {
                                        Cartera c = new Cartera();
                                        c.setCedula(cartera.getCedula());
                                        c.setCiudad(cartera.getCiudad());
                                        c.setClienteID(cartera.getClienteID());
                                        c.setDireccion(cartera.getDireccion());
                                        c.setFechaAbono(cartera.getFechaAbono());
                                        c.setMontoCuota(cartera.getMontoCuota());
                                        c.setNombreCompleto(cartera.getNombreCompleto());
                                        c.setOjbCobradorID(cartera.getOjbCobradorID());
                                        if (cartera.getOrdenCobro() != null) {
                                            if (cartera.getOrdenCobro().equals("0")) {
                                                c.setOrdenCobro(orden);
                                                orden++;
                                            } else {
                                                c.setOrdenCobro(Integer.valueOf(cartera.getOrdenCobro()));
                                            }
                                        } else {
                                            c.setOrdenCobro(orden);
                                            orden++;
                                        }
                                        c.setOjbCobradorID(cartera.getOjbCobradorID());
                                        c.setPais(cartera.getPais());
                                        c.setRutaAsignada(cartera.getRutaAsignada());
                                        c.setSaldo(cartera.getSaldo());
                                        c.setStbRutaID(cartera.getStbRutaID());
                                        List<Producto> items = new ArrayList<Producto>();
                                        c.setSccCuentaID(cartera.getSccCuentaID());
                                        c.setOffline(true);
                                        c.setCobrado(false);
                                        c.setCuotasVencidas(cartera.getCuotasVencidas());
                                        //c.setProductos(items);
                                        c.save();
                                        for (Productos p : cartera.getProductos()) {
                                            CarteraDetalle detalle = new CarteraDetalle(c.getClienteID(), p.getObjSfaFacturaID(), p.getSivProductoID(), p.getPrecio().floatValue(), c.getCedula());
                                            detalle.save();
                                        }
                                    }
                                }
                                // postEvent(Events.onSyncCarteraSucess);
                            }
                            postEvent(Events.onSyncCarteraSucess);
                        }
                        catch (Exception ex){
                            postEvent(Events.onSyncCarteraError, ex.getMessage());
                        }
                    }

                    @Override
                    public void onFailure(Call<List<CarteraResponse>> call, Throwable t) {
                        postEvent(Events.onSyncCarteraError, "Error Sincronizando Cartera");
                    }
                });
            }
            catch (Exception ex){
                postEvent(Events.onSyncCarteraError, ex.getMessage());
            }
        }
    }

    @Override
    public void GetRutasByCobradorId() {
        ResponseMessage message = Network.isPhoneConnected(context);
        if (message != null && message.isHasError()) {
            postEvent(Events.onSigInError, message.getCause() + message.getMessage());
        } else
        {

            Configuration configuration = new Select()
                                            .from(Configuration.class)
                                            .where(String.format("System=0"))
                                            .querySingle();
            if(service ==null) {
                service = retrofit.create(IServicioRemoto.class);
            }
            Call<List<Ruta>> rutaResponseCall = service.GetRutasByCobradorId(configuration.getObjEmpleadoID());
            rutaResponseCall.enqueue(new Callback<List<Ruta>>() {

                @Override
                public void onResponse(Call<List<Ruta>> call, Response<List<Ruta>> response) {
                    if (response.body() != null) {
                        new Delete().from(Ruta.class).query();

                        List<Ruta> list = (List<Ruta>) response.body();
                        for (Ruta ruta : list) {
                            ruta.save();
                        }
                    }
                    postEvent(Events.onSuccess, null, "Sincronizando Clientes ...");
                    GetClientesByCobradorId();

                }

                @Override
                public void onFailure(Call<List<Ruta>> call, Throwable t) {
                    postEvent(Events.onSyncRutaError, "Error Sincronizando Rutas");
                }
            });
        }
    }

    @Override
    public void DownloadServer() {
        server = true;
        GetPaisByCodigo();
    }

    public void GetPaisByCodigo(){

        if(service ==null) {
            service = retrofit.create(IServicioRemoto.class);
        }
        ResponseMessage message = Network.isPhoneConnected(context);
        if (message != null && message.isHasError()) {
            postEvent(Events.onSigInError, message.getCause() + message.getMessage());
        } else {

            Call<List<Pais>> paisResponseCall = service.GetPaisByCodigo(558);
            paisResponseCall.enqueue(new Callback<List<Pais>>() {
                @Override
                public void onResponse(Call<List<Pais>> call, Response<List<Pais>> response) {
                    if (response.body() != null) {
                        //if(server) {
                        //Limpiamos todos los datos
                        new Delete().from(Pais.class).query();

                        //s}
                        List<Pais> Paises = (List<Pais>) response.body();
                        for (Pais pais : Paises) {
                            pais.save();
                        }
                    }
                    postEvent(Events.onSyncPaisesSucess, null, "Sincronizando Catálogos ...");
                    GetCatalogos();

                }

                @Override
                public void onFailure(Call<List<Pais>> call, Throwable t) {
                    postEvent(Events.onSyncPaisesError, "Error Sincronizando Paises");
                }
            });
        }
    }

    private void postEvent(int type) {
        postEvent(type, null);
    }

    private void postEvent(int type, String errorMessage) {
        Events event = new Events();
        event.setEventype(type);
        if (errorMessage != null) {
            event.setErrorMessage(errorMessage);
        }

        EventBus eventBus = GreenRobotEventBus.getInstance();
        eventBus.post(event);

    }

    private void postEvent(int type, String errorMessage , Object o) {
        Events event = new Events();
        event.setEventype(type);
        if (errorMessage != null) {
            event.setErrorMessage(errorMessage);
        }
        if(o!=null) {
            event.setObject(o);
        }
        EventBus eventBus = GreenRobotEventBus.getInstance();
        eventBus.post(event);

    }

    private Boolean  getsession () {
        Boolean session = false;
        Configuration configuration = new Select().from(Configuration.class).where(String.format("System='0'")).querySingle();
        if(configuration!=null)
            session = configuration.getSession();

        return session;
    }

}
