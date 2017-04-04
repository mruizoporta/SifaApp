package yio.io.sifaapp;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import yio.io.sifaapp.fragment.ActualizarFragment;
import yio.io.sifaapp.fragment.CarteraListFragment;

public class ActualizarActivity extends BaseActivity {

    Fragment fragment = null;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        fragment = null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setTitle(getResources().getString(R.string.actualizar_item));

        if(fragment == null) {
            fragment = new ActualizarFragment();
        }
        FragmentManager manager = getSupportFragmentManager();
        manager.beginTransaction().replace(R.id.fragment_container,fragment).commit();

    }



}
