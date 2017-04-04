package yio.io.sifaapp.adapter;

import android.app.Activity;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.raizlabs.android.dbflow.sql.language.Select;

import java.util.List;

import butterknife.Bind;
import yio.io.sifaapp.Actualizacion.Models.Cliente;
import yio.io.sifaapp.Cartera.IDetallaCarteraView;
import yio.io.sifaapp.R;
import yio.io.sifaapp.model.Cartera;
import yio.io.sifaapp.model.Customer;


/**
 * Created by STARK on 19/06/2016.
 */
public class CustomerAdapter extends RecyclerView.Adapter<CustomerViewHolder> implements ItemTouchHelperAdapter {

    IDetallaCarteraView view;
    List<Cartera> list = null;
    private OnItemClickListener listener;
    private final OnStartDragListener mDragStartListener;
    Activity activity;


    public CustomerAdapter(Activity a, IDetallaCarteraView view, List<Cartera> dataset, OnStartDragListener mDragStartListener, OnItemClickListener listener) {
        this.list = dataset;
        this.view = view;
        this.mDragStartListener = mDragStartListener;
        this.listener = listener;
        activity = a;
    }

    @Override
    public int getItemCount() {
        return this.list.size();
    }

    public void setData(List<Cartera> lista) {
        this.list = lista;
        Log.d("CustomerAdapter","setCarteraList");
    }


    @Override
    public CustomerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cobrolist_item, parent, false);
        CustomerViewHolder itemViewHolder = new CustomerViewHolder(view);
        return itemViewHolder;
    }

    @Override
    public void onBindViewHolder(final CustomerViewHolder holder, int position) {
        if (list.get(position) instanceof Cartera) {
            //Log.d("onBindViewHolder", "Dentro");

            Cartera cartera = this.list.get(position);
            //if( position == 0) {
            Log.d("Position " , String.valueOf(position) + "getCobrado " +  String.valueOf(cartera.getCobrado()) );

            Cartera cartera2 = new Select().from(Cartera.class).where(String.format("id=%d", cartera.getId())).querySingle();

            if (cartera2.getCobrado()) {
                holder.txtviewcustomername.setTextColor(activity.getResources().getColor(R.color.colorNameItem));
            }
            else
                holder.txtviewcustomername.setTextColor(activity.getResources().getColor(R.color.colorRed));

            Customer c = new Select().from(Customer.class).where(String.format("Cedula='%s'",cartera.getCedula())).querySingle();


            holder.txtviewcustomername.setText(cartera.getNombreCompleto());
            holder.txtviewamount.setText(cartera.getMontoCuota().toString());
            holder.txtviewdatepay.setText(cartera.getFechaAbono().toString());
            if(c.getReferencia()==null)
                holder.txtviewcity.setText(cartera.getDireccion());
            else
                holder.txtviewcity.setText(cartera.getDireccion() + c.getReferencia());
            holder.txtviewNum.setText(String.valueOf(cartera.getOrdenCobro()));
            holder.txtviewcuotas.setText(cartera.getCuotasVencidas().toString());

            holder.setClickListener(cartera, listener);

            // Start a drag whenever the handle view it touched
            holder.txtviewcustomername.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                        //mDragStartListener.onStartDrag(holder);
                    }
                    return false;
                }
            });

        }
        else Log.d("onBindViewHolder", "No es Tipo Cartera");
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        Log.d("Cambio de Position", String.valueOf(fromPosition).concat("->").concat(String.valueOf(toPosition)));
        Cartera cartera = this.list.get(fromPosition);
        cartera.setOrdenCobro(toPosition + 1);
        //cartera.save();
        view.carteraupdated(fromPosition + 1, toPosition + 1, cartera);
        // Collections.swap(list, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
        Log.d("onItemMove", "onItemMove");
        return true;
    }

    @Override
    public void onItemDismiss(int position) {
        list.remove(position);
        notifyItemRemoved(position);
    }
}
