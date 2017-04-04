package yio.io.sifaapp.Venta;

import android.util.Log;

import java.util.List;

import yio.io.sifaapp.Cartera.IDetallaCarteraView;
import yio.io.sifaapp.Cartera.IVentaListInteractor;
import yio.io.sifaapp.Cartera.IVentaView;
import yio.io.sifaapp.Cartera.IventaListInteractorImpl;
import yio.io.sifaapp.model.modelSend.Venta;
import yio.io.sifaapp.utils.EventBus;
import yio.io.sifaapp.utils.Events;
import yio.io.sifaapp.utils.GreenRobotEventBus;

/**
 * Created by JUANCARLOS on 19/10/2016.
 */
public class IventaListPresenterImpl implements IventaListPresenter {

    private EventBus eventbus;
    IVentaListInteractor interactor;
    IVentaView view;

    public IventaListPresenterImpl(IVentaView view) {
        this.view = view;
        interactor = new IventaListInteractorImpl();
        this.eventbus = GreenRobotEventBus.getInstance();
    }

    @Override
    public void onCreated() {
        eventbus.register(this);
    }

    @Override
    public void onDestroy() {
        eventbus.unregister(this);
        view = null;
    }

    @Override
    public void getList() {
        interactor.getList();
    }

    @Override
    public void getCliente(int customerid) {
        interactor.getCliente(customerid);
    }

    @Override
    public void onEventMainThread(Events event) {
        Log.d("CarteraListImplement","onEventMainThread");
        switch (event.getEventype()) {
            case Events.onFetchVentaSucess:
                setData(event);
                break;
        }
    }
    private void setData(Events event){
        view.onDataFetch((List< Venta>)event.getObject());
    }

}
