package com.fuziform.app;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.vuzix.hud.actionmenu.ActionMenuActivity;

public class ManageFaces extends ActionMenuActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manage_faces);
    }

    @Override
    protected boolean onCreateActionMenu(Menu menu) {
        super.onCreateActionMenu(menu);
        getMenuInflater().inflate(R.menu.manage_faces_menu, menu);
        return true;
    }

    @Override
    protected boolean alwaysShowActionMenu() {
        return true;
    }

    public void addFace(MenuItem item) {
    }

    public void trainFace(MenuItem item) {
    }
}
