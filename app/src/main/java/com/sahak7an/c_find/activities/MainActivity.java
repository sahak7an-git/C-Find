package com.sahak7an.c_find.activities;

import static com.sahak7an.c_find.utilities.Constants.KEY_COLLECTION_HISTORY;
import static com.sahak7an.c_find.utilities.Constants.KEY_COLLECTION_REQUEST;
import static com.sahak7an.c_find.utilities.Constants.KEY_COLLECTION_USERS;
import static com.sahak7an.c_find.utilities.Constants.KEY_EMAIL;
import static com.sahak7an.c_find.utilities.Constants.KEY_FCM_TOKEN;
import static com.sahak7an.c_find.utilities.Constants.KEY_IMAGE;
import static com.sahak7an.c_find.utilities.Constants.KEY_IS_LIKED;
import static com.sahak7an.c_find.utilities.Constants.KEY_RECEIVER_EMAIL;
import static com.sahak7an.c_find.utilities.Constants.KEY_RECEIVER_ID;
import static com.sahak7an.c_find.utilities.Constants.KEY_RECEIVER_IMAGE;
import static com.sahak7an.c_find.utilities.Constants.KEY_RECEIVER_USER_NAME;
import static com.sahak7an.c_find.utilities.Constants.KEY_SENDER_EMAIL;
import static com.sahak7an.c_find.utilities.Constants.KEY_SENDER_ID;
import static com.sahak7an.c_find.utilities.Constants.KEY_SENDER_IMAGE;
import static com.sahak7an.c_find.utilities.Constants.KEY_SENDER_USER_NAME;
import static com.sahak7an.c_find.utilities.Constants.KEY_USER_ID;
import static com.sahak7an.c_find.utilities.Constants.KEY_USER_NAME;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.fragment.app.FragmentManager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;
import com.sahak7an.c_find.R;
import com.sahak7an.c_find.adapters.UsersAdapter;
import com.sahak7an.c_find.databinding.ActivityMainBinding;
import com.sahak7an.c_find.dialogs.UserInfoDialog;
import com.sahak7an.c_find.listeners.DialogListener;
import com.sahak7an.c_find.listeners.UserListener;
import com.sahak7an.c_find.models.User;
import com.sahak7an.c_find.utilities.PreferenceManager;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends BaseActivity implements UserListener, DialogListener {

    private Boolean isLiked;
    private User receiverUser;
    private FirebaseUser firebaseUser;
    private FirebaseFirestore firebaseFirestore;
    private PreferenceManager preferenceManager;
    private ActivityMainBinding activityMainBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        preferenceManager = new PreferenceManager(getApplicationContext());
        activityMainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        changeStatusBarColor();

        setContentView(activityMainBinding.getRoot());

        loadUserDetails();
        getToken();

        getUsers();

        setListeners();
    }

    private void setListeners() {

        activityMainBinding.imageSignOut.setOnClickListener(v -> signOut());

        activityMainBinding.searchImage.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), HistoryActivity.class)));

        activityMainBinding.accountImage.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), AccountActivity.class)));

    }

    private void loadUserDetails() {

        activityMainBinding.accountImage.setImageBitmap(getResizedBitmap(getReceiverUserImage(
                preferenceManager.getString(KEY_IMAGE)
        )));

        activityMainBinding.homeImage.setImageResource(R.drawable.ic_home_filled);
        activityMainBinding.searchImage.setImageResource(R.drawable.ic_search);

    }

    private final OnCompleteListener<QuerySnapshot> historyOnCompleteListener = task -> {

        if (!task.isSuccessful() || task.getResult() == null || task.getResult().getDocuments().size() == 0) {

            HashMap<String, Object> usersData = new HashMap<>();

            usersData.put(KEY_SENDER_ID, preferenceManager.getString(KEY_USER_ID));
            usersData.put(KEY_RECEIVER_ID, receiverUser.id);
            usersData.put(KEY_RECEIVER_IMAGE, receiverUser.image);
            usersData.put(KEY_RECEIVER_USER_NAME, receiverUser.userName);
            usersData.put(KEY_RECEIVER_EMAIL, receiverUser.email);
            usersData.put(KEY_IS_LIKED, isLiked);

            firebaseFirestore.collection(KEY_COLLECTION_HISTORY)
                    .add(usersData);

        } else {

            DocumentReference documentReferenceStatus = firebaseFirestore.collection(KEY_COLLECTION_HISTORY)
                    .document(task.getResult().getDocuments().get(0).getId());

            documentReferenceStatus.update(KEY_IS_LIKED, isLiked);

        }

    };

    private final OnCompleteListener<QuerySnapshot> requestOnCompleteListener = task -> {

        if (!task.isSuccessful() || task.getResult() == null || task.getResult().getDocuments().size() == 0) {

            HashMap<String, Object> usersData = new HashMap<>();

            usersData.put(KEY_SENDER_ID, preferenceManager.getString(KEY_USER_ID));
            usersData.put(KEY_SENDER_IMAGE, preferenceManager.getString(KEY_IMAGE));
            usersData.put(KEY_SENDER_USER_NAME, preferenceManager.getString(KEY_USER_NAME));
            usersData.put(KEY_SENDER_EMAIL, preferenceManager.getString(KEY_EMAIL));
            usersData.put(KEY_RECEIVER_ID, receiverUser.id);

            firebaseFirestore.collection(KEY_COLLECTION_REQUEST)
                    .add(usersData);

        }

    };

    private void getUsers() {
        loading(true);

        FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseFirestore.collection(KEY_COLLECTION_USERS)
                .get()
                .addOnCompleteListener(task -> {

                    loading(false);
                    String currentUserId = preferenceManager.getString(KEY_USER_ID);

                    if (task.isSuccessful() && task.getResult() != null) {

                        List<User> userList = new ArrayList<>();

                        for (QueryDocumentSnapshot queryDocumentSnapshot: task.getResult()) {

                            if (currentUserId.equals(queryDocumentSnapshot.getId())) {

                                continue;

                            }

                            User user = new User();
                            user.userName = queryDocumentSnapshot.getString(KEY_USER_NAME);
                            user.email = queryDocumentSnapshot.getString(KEY_EMAIL);
                            user.image = queryDocumentSnapshot.getString(KEY_IMAGE);
                            user.token = queryDocumentSnapshot.getString(KEY_FCM_TOKEN);
                            user.id = queryDocumentSnapshot.getId();
                            userList.add(user);

                        }

                        if (userList.size() > 0) {

                            UsersAdapter usersAdapter = new UsersAdapter(userList, this);
                            activityMainBinding.usersRecyclerView.setAdapter(usersAdapter);

                            activityMainBinding.usersRecyclerView.setVisibility(View.VISIBLE);

                        } else {

                            showErrorMessage();

                        }

                    } else {

                        showErrorMessage();

                    }
                });

    }

    private void getToken() {

        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(this::updateToken)
                .addOnFailureListener(v -> {

                    if (firebaseUser != null) {

                        firebaseUser.delete();

                    }

                    preferenceManager.clear();

                    showToast("Please Sign in again\n " +
                            "Your account will be deleted\n" +
                            "    Or connection Error    ");

                    startActivity(new Intent(getApplicationContext(), SignInActivity.class));
                    finish();

                });

    }

    private void updateToken(String token) {

        preferenceManager.putString(KEY_FCM_TOKEN, token);

        FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                firebaseFirestore.collection(KEY_COLLECTION_USERS).document(
                        preferenceManager.getString(KEY_USER_ID)
                );

        documentReference.update(KEY_FCM_TOKEN, token)
                .addOnFailureListener(e -> {

                    if (firebaseUser != null) {

                        firebaseUser.delete();

                    }

                    preferenceManager.clear();

                    showToast("Please Sign in again\n " +
                            "Your account will be deleted\n" +
                            "    Or connection Error    ");

                    startActivity(new Intent(getApplicationContext(), SignInActivity.class));
                    finish();

                });

    }

    private void signOut() {

        showToast("Signing out...");
        FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();

        DocumentReference documentReference =
                firebaseFirestore.collection(KEY_COLLECTION_USERS).document(
                        preferenceManager.getString(KEY_USER_ID)
                );

        HashMap<String, Object> updates = new HashMap<>();
        updates.put(KEY_FCM_TOKEN, FieldValue.delete());

        documentReference.update(updates)
                .addOnSuccessListener(unused -> {

                    preferenceManager.clear();
                    startActivity(new Intent(getApplicationContext(), SignInActivity.class));
                    finish();

                })
                .addOnFailureListener(e -> {

                    showToast("Unable to sign out");

                    if (firebaseUser != null) {

                        firebaseUser.delete();

                    }

                    preferenceManager.clear();

                    startActivity(new Intent(getApplicationContext(), SignInActivity.class));
                    finish();

                });

    }

    private void showToast(String message) {

        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();

    }

    private void showErrorMessage() {

        activityMainBinding.textErrorMessage.setText(String.format("%s", "No User available"));
        activityMainBinding.textErrorMessage.setVisibility(View.VISIBLE);

    }

    private void loading(Boolean isLoading) {

        if (isLoading) {

            activityMainBinding.progressBar.setVisibility(View.VISIBLE);

        } else {

            activityMainBinding.progressBar.setVisibility(View.INVISIBLE);

        }
    }

    private void showDialog(String encodedImage) {

        FragmentManager fragmentManager = getSupportFragmentManager();
        UserInfoDialog userInfoDialog = new UserInfoDialog(getReceiverUserImage(encodedImage), this);

        userInfoDialog.show(fragmentManager, "dialog");

    }

    private Bitmap getReceiverUserImage(String encodedImage) {

        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

    }

    private void changeStatusBarColor() {
        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(this.getResources().getColor(R.color.white, getTheme()));
    }

    private Bitmap getResizedBitmap(Bitmap bitmap) {

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float scaleWidth = ((float) 400) / width;
        float scaleHeight = ((float) 400) / height;

        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);

        Bitmap resizedBitmap = Bitmap.createBitmap(
                bitmap, 0, 0, width, height, matrix, false);
        bitmap.recycle();

        return resizedBitmap;

    }

    private void checkForHistory(String senderId, String receiverId) {

        firebaseFirestore.collection(KEY_COLLECTION_HISTORY)
                .whereEqualTo(KEY_SENDER_ID, senderId)
                .whereEqualTo(KEY_RECEIVER_ID, receiverId)
                .get()
                .addOnCompleteListener(historyOnCompleteListener);

    }

    private void checkForRequest(String senderId, String receiverId) {

        firebaseFirestore.collection(KEY_COLLECTION_REQUEST)
                .whereEqualTo(KEY_SENDER_ID, senderId)
                .whereEqualTo(KEY_RECEIVER_ID, receiverId)
                .get()
                .addOnCompleteListener(requestOnCompleteListener);

    }

    @Override
    public void onUserClicked(User user) {

        preferenceManager.putString(KEY_RECEIVER_IMAGE, user.image);
        preferenceManager.putString(KEY_RECEIVER_ID, user.id);
        preferenceManager.putString(KEY_RECEIVER_USER_NAME, user.userName);

        this.receiverUser = user;

        showDialog(user.image);

    }

    @Override
    public void onButtonClicked(Boolean flag) {

        isLiked = flag;

        if (flag) {

            checkForRequest(preferenceManager.getString(KEY_USER_ID), receiverUser.id);
            checkForHistory(preferenceManager.getString(KEY_USER_ID), receiverUser.id);

        } else {

            checkForHistory(preferenceManager.getString(KEY_USER_ID), receiverUser.id);

        }

    }

}