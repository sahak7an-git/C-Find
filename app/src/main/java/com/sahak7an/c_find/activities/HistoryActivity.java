package com.sahak7an.c_find.activities;

import static com.sahak7an.c_find.utilities.Constants.KEY_COLLECTION_HISTORY;
import static com.sahak7an.c_find.utilities.Constants.KEY_IMAGE;
import static com.sahak7an.c_find.utilities.Constants.KEY_IS_LIKED;
import static com.sahak7an.c_find.utilities.Constants.KEY_RECEIVER_EMAIL;
import static com.sahak7an.c_find.utilities.Constants.KEY_RECEIVER_IMAGE;
import static com.sahak7an.c_find.utilities.Constants.KEY_RECEIVER_USER_NAME;
import static com.sahak7an.c_find.utilities.Constants.KEY_SENDER_ID;
import static com.sahak7an.c_find.utilities.Constants.KEY_USER_ID;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.sahak7an.c_find.R;
import com.sahak7an.c_find.adapters.HistoryAdapter;
import com.sahak7an.c_find.databinding.ActivityHistoryBinding;
import com.sahak7an.c_find.models.User;
import com.sahak7an.c_find.utilities.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends BaseActivity {

    private PreferenceManager preferenceManager;

    private ActivityHistoryBinding activityHistoryBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferenceManager = new PreferenceManager(getApplicationContext());
        activityHistoryBinding = ActivityHistoryBinding.inflate(getLayoutInflater());
        changeStatusBarColor();

        setContentView(activityHistoryBinding.getRoot());

        loadUserDetails();
        setListeners();

        getUserHistory();
    }

    private void setListeners() {

        activityHistoryBinding.homeImage.setOnClickListener(v -> onBackPressed());

        activityHistoryBinding.accountImage.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), AccountActivity.class)));

    }

    private void getUserHistory() {
        loading(true);

        FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseFirestore.collection(KEY_COLLECTION_HISTORY)
                .get()
                .addOnCompleteListener(task -> {

                    loading(false);
                    String currentUserId = preferenceManager.getString(KEY_USER_ID);

                    if (task.isSuccessful() && task.getResult() != null) {

                        List<User> userList = new ArrayList<>();

                        for (QueryDocumentSnapshot queryDocumentSnapshot: task.getResult()) {

                            if (currentUserId.equals(queryDocumentSnapshot.getString(KEY_SENDER_ID))) {

                                User user = new User();
                                user.userName = queryDocumentSnapshot.getString(KEY_RECEIVER_USER_NAME);
                                user.email = queryDocumentSnapshot.getString(KEY_RECEIVER_EMAIL);
                                user.image = queryDocumentSnapshot.getString(KEY_RECEIVER_IMAGE);
                                user.isLiked = String.valueOf(queryDocumentSnapshot.get(KEY_IS_LIKED));
                                userList.add(user);

                            }

                        }

                        if (userList.size() > 0) {

                            HistoryAdapter historyAdapter= new HistoryAdapter(userList);
                            activityHistoryBinding.usersRecyclerView.setAdapter(historyAdapter);

                            activityHistoryBinding.usersRecyclerView.setVisibility(View.VISIBLE);

                        } else {

                            showErrorMessage();

                        }

                    } else {

                        showErrorMessage();

                    }
                });

    }

    private void loadUserDetails() {

        activityHistoryBinding.accountImage.setImageBitmap(getResizedBitmap(getReceiverUserImage(
                preferenceManager.getString(KEY_IMAGE)
        )));

        activityHistoryBinding.homeImage.setImageResource(R.drawable.ic_home);
        activityHistoryBinding.searchImage.setImageResource(R.drawable.ic_search_filled);

    }

    private void showErrorMessage() {

        activityHistoryBinding.textErrorMessage.setText(String.format("%s", "No User available"));
        activityHistoryBinding.textErrorMessage.setVisibility(View.VISIBLE);

    }

    private void loading(Boolean isLoading) {

        if (isLoading) {

            activityHistoryBinding.progressBar.setVisibility(View.VISIBLE);

        } else {

            activityHistoryBinding.progressBar.setVisibility(View.INVISIBLE);

        }
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

    private Bitmap getReceiverUserImage(String encodedImage) {

        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

    }
}