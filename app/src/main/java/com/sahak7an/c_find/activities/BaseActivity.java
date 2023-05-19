package com.sahak7an.c_find.activities;

import static com.sahak7an.c_find.utilities.Constants.KEY_COLLECTION_USERS;
import static com.sahak7an.c_find.utilities.Constants.KEY_IS_ONLINE;
import static com.sahak7an.c_find.utilities.Constants.KEY_USER_ID;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sahak7an.c_find.utilities.PreferenceManager;

public class BaseActivity extends AppCompatActivity {

    private DocumentReference documentReferenceStatus;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager preferenceManager = new PreferenceManager(getApplicationContext());
        FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();

        documentReferenceStatus = firebaseFirestore.collection(KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(KEY_USER_ID));

    }

    @Override
    protected void onPause() {
        super.onPause();
        documentReferenceStatus.update(KEY_IS_ONLINE, false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        documentReferenceStatus.update(KEY_IS_ONLINE, true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        documentReferenceStatus.update(KEY_IS_ONLINE, true);
    }

}
