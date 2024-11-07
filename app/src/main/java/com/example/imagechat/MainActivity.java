package com.example.imagechat;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.room.Room;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private LinearLayout imagesContainer;
    private static final int galleryRequestCode = 1;
    private AppDatabase db;
    private MessageDao messageDao;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "db").build();
        messageDao = db.MessageDao();
        showPreviousMessages();

        imagesContainer = findViewById(R.id.imagesContainer);
        Button sendBtn = findViewById(R.id.sendButton);
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openGallery();
            }
        });


    }

    private void showPreviousMessages() {
        new Thread(() -> {
            List<Message> messages = messageDao.getAllMessages();
            if (messages != null && !messages.isEmpty()) {
                for (Message message: messages) {
                    int senderImage;
                    switch (message.sender) {
                        case "Sita":
                            senderImage = R.drawable.profile_sita;
                            break;
                        case "Husband":
                            senderImage = R.drawable.profile_husband;
                            break;
                        case "Boy":
                            senderImage = R.drawable.profile_boy;
                            break;
                        default:
                            senderImage = R.drawable.profile_girl;
                            break;
                    }
                    if (message.sender.equals("Sita")) {
                        try {
                            showOnScreen(message.imageBytes, null, senderImage, false, true);
                        } catch (FileNotFoundException e) {
                            throw new RuntimeException(e);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        try {
                            showOnScreen(null, message.resourceName, senderImage, true, true);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }).start();
    }

    public void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), galleryRequestCode);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == galleryRequestCode) {
            Uri imageUri = data.getData();
            InputStream imageStream = null;
            try {
                imageStream = getContentResolver().openInputStream(imageUri);
                byte[] imageBytes = getBytesFromInputStream(imageStream);
                saveMessage("Sita", null, imageUri);
                showOnScreen(imageBytes, "", R.drawable.profile_sita, false, false);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void saveMessage(String username, String resourceName, Uri imageUri) throws IOException {
        Message message = new Message();
        message.sender = username;
        message.resourceName = resourceName;

        if (imageUri != null) {
            InputStream imageStream = getContentResolver().openInputStream(imageUri);
            byte[] imageBytes = getBytesFromInputStream(imageStream);
            message.imageBytes = imageBytes;
        }

        new Thread(() -> messageDao.insertMessage(message)).start();

    }

    private byte[] getBytesFromInputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;

        while ((length = inputStream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, length);
        }

        return byteArrayOutputStream.toByteArray();
    }

    public void showOnScreen(byte[] imageBytes, String resourceName, int userImageSrc, boolean isReply, boolean isPreviousMessage) throws IOException {
        LinearLayout imageLayout = new LinearLayout(this);
        imageLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        imageLayout.setOrientation(LinearLayout.HORIZONTAL);
        imageLayout.setPadding(0,5,0,5);

        if (isReply) {

            ImageView userImageView = new ImageView(this);
            LinearLayout.LayoutParams userImageParams = new LinearLayout.LayoutParams(100,100);
            userImageParams.setMargins(0,50,20,50);
            userImageView.setLayoutParams(userImageParams);
            userImageView.setImageResource(userImageSrc);


            ImageView imageView = new ImageView(this);
            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(600,600);
            imageParams.setMargins(10,50,10,50);
            imageView.setLayoutParams(imageParams);
            //
            String resorceString = resourceName.substring(2);
            String[] parts = resorceString.split("\\.");
            String resourceType = parts[0];
            String imageName = parts[1];
            int imageResourceId = getResources().getIdentifier(imageName, resourceType, getPackageName());
            imageView.setImageResource(imageResourceId);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

            if (!isPreviousMessage){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                imageLayout.addView(userImageView);
                                imageLayout.addView(imageView);
                                imagesContainer.addView(imageLayout);
                                imagesContainer.post(()-> {
                                    ScrollView scrollView = findViewById(R.id.scrollView);
                                    scrollView.fullScroll(View.FOCUS_DOWN);
                                    scrollView.scrollTo(0, scrollView.getChildAt(0).getBottom());
                                });
                            }
                        }, 1000);
                    }
                });
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageLayout.addView(userImageView);
                        imageLayout.addView(imageView);
                        imagesContainer.addView(imageLayout);
                        imagesContainer.post(()-> {
                            ScrollView scrollView = findViewById(R.id.scrollView);
                            scrollView.fullScroll(View.FOCUS_DOWN);
                            scrollView.scrollTo(0, scrollView.getChildAt(0).getBottom());
                        });
                    }
                });
            }
        } else {
            imageLayout.setGravity(Gravity.END);
            ImageView userImageView = new ImageView(this);
            LinearLayout.LayoutParams userImageParams = new LinearLayout.LayoutParams(100,100);
            userImageParams.setMargins(20,50,0,50);
            userImageView.setLayoutParams(userImageParams);
            userImageView.setImageResource(R.drawable.profile_sita);

            ImageView imageView = new ImageView(this);
            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(600,600);
            imageView.setLayoutParams(imageParams);
            imageParams.setMargins(10,50,10,50);
            try{
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                imageView.setImageBitmap(bitmap);
            } catch (Exception e) {
            }
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    imageLayout.addView(imageView);
                    imageLayout.addView(userImageView);
                    imagesContainer.addView(imageLayout);
                    imagesContainer.post(()-> {
                        ScrollView scrollView = findViewById(R.id.scrollView);
                        scrollView.fullScroll(View.FOCUS_DOWN);
                        scrollView.scrollTo(0, scrollView.getChildAt(0).getBottom());
                    });
                }
            });
            if (!isPreviousMessage) {
                replyWithImage();
            }
        }

    }


    private void replyWithImage() throws IOException {
        Random randomForUser = new Random();
        Random randomForImage = new Random();
        int randomUserIndex = randomForUser.nextInt(3) ;
        int randomImageIndex = randomForImage.nextInt(5) ;
        String[] users = {"Husband", "Boy", "Girl"};
        String[] defaultImages = {"R.drawable.res_namaste", "R.drawable.res_angry", "R.drawable.res_goodbye", "R.drawable.res_goodjob", "R.drawable.res_surprised"};
        String selectedUser =   users[randomUserIndex];
        int userImage;
        if (randomUserIndex == 0){
            userImage = R.drawable.profile_husband;
        } else if (randomUserIndex == 1){
            userImage = R.drawable.profile_boy;
        }else {
            userImage = R.drawable.profile_girl;
        }
        saveMessage(selectedUser, defaultImages[randomImageIndex], null);
        showOnScreen(null,defaultImages[randomImageIndex], userImage, true, false);
    }



    public Uri getDrawableUri(Context context, int drawableId) {
        // Convert Drawable to Bitmap
        Drawable drawable = ContextCompat.getDrawable(this, drawableId);
        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();

// Save Bitmap to a file (e.g., in cache directory)
        File file = new File(getCacheDir(), "image_name.png");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            // Get the Uri for the saved file
            Uri imageUri = Uri.fromFile(file);
            return imageUri;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}

