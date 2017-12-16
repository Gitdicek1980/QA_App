package jp.techacademy.yamamoto.daisuke.qa_app;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class QuestionSendActivity extends AppCompatActivity implements View.OnClickListener, DatabaseReference.CompletionListener {

    //permission許可Dialog表示用パラメータ
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    //複数パラメータから戻る場合の識別用パラメータ
    private static final int CHOOSER_REQUEST_CODE = 100;

    //UI保持用
    private ProgressDialog mProgress;
    private EditText mTitleText;
    private EditText mBodyText;
    private ImageView mImageView;
    private Button mSendButton;

    private int mGenre; // ジャンルを保持する
    private Uri mPictureUri; // カメラで撮影した画像を保存するURI

    //onCreat：渡ってきたジャンルの番号を保持。UIの準備。
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question_send);

        //渡ってきたジャンル番号保持
        Bundle extras = getIntent().getExtras();
        mGenre = extras.getInt("genre");

        //UI準備
        setTitle("質問作成");

        mTitleText = (EditText) findViewById(R.id.titleText);
        mBodyText = (EditText) findViewById(R.id.bodyText);

        mSendButton = (Button) findViewById(R.id.sendButton);
        mSendButton.setOnClickListener(this);

        mImageView = (ImageView) findViewById(R.id.imageView);
        mImageView.setOnClickListener(this);

        mProgress = new ProgressDialog(this);
        mProgress.setMessage("投稿中…");
    }

    //onActivityResult：Intent連携で取得した画像をリサイズしてImageViewに設定
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CHOOSER_REQUEST_CODE) {

            //requestCode：int型で指定
            //resultCode：int型

            if (resultCode != RESULT_OK) {
                if (mPictureUri != null) {
                    getContentResolver().delete(mPictureUri, null, null);
                    mPictureUri = null;
                }
                return; //methodを抜ける
            }

            //画像を取得
            Uri uri = (data == null || data.getData() == null) ? mPictureUri : data.getData();

            //URIからBitmapを取得
            Bitmap image;
            try{
                ContentResolver contentResolver = getContentResolver();
                InputStream inputStream = contentResolver.openInputStream(uri);
                image = BitmapFactory.decodeStream(inputStream);
                inputStream.close();
            } catch (Exception e){
                return;
            }

            //取得したbitmapの長辺を500pxにresize
            int imageWidth = image.getWidth();
            int imageHeight = image.getHeight();
            float scale = Math.min((float) 500 / imageWidth, (float) 500 /imageHeight);

            Matrix matrix = new Matrix();
            matrix.postScale(scale, scale);

            Bitmap resizedImage = Bitmap.createBitmap(image, 0, 0, imageWidth, imageHeight, matrix, true);

            //BitmapをImageViewに設定
            mImageView.setImageBitmap(resizedImage);

            mPictureUri = null;
        }
    }

    //onClick：ImageViewとButtonがタップされた時の処理
    //ImageViewをタップしたときは必要であれば許可を求めるダイアログを表示。
    @Override
    public void onClick(View v){
        if(v == mImageView){
            //permissionの許可状態を確認
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                    //許可されている
                    showChooser();
                } else {
                    //許可されていないので許可ダイアログを表示
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_CODE);
                    return;
                }
            } else {
                showChooser();
            }
        } else if (v == mSendButton){
            //keyboardが出てたら閉じる
            InputMethodManager im = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            im.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

            DatabaseReference dataBaseReference = FirebaseDatabase.getInstance().getReference();
            DatabaseReference genreRef = dataBaseReference.child(Const.ContentsPATH).child(String.valueOf(mGenre));

            Map<String, String> data = new HashMap<String, String>();

            //UID
            data.put("uid", FirebaseAuth.getInstance().getCurrentUser().getUid());

            //titleと本文を取得
            String title = mTitleText.getText().toString();
            String body = mBodyText.getText().toString();

            if(title.length() == 0){
                //質問タイトルが入力されていない場合はError表示
                Snackbar.make(v, "タイトルを入力してください", Snackbar.LENGTH_LONG).show();
                return;
            }

            if(body.length() == 0){
                //質問本文が入力されていない場合はError表示
                Snackbar.make(v, "質問を入力してください", Snackbar.LENGTH_LONG).show();
                return;
            }

            //Preferenceから名前を取得
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
            String name = sp.getString(Const.NameKEY, "");

            data.put("title", title);
            data.put("body", body);
            data.put("name", name);

            //添付画像を取得
            BitmapDrawable drawable = (BitmapDrawable) mImageView.getDrawable();

            //添付画像が設定されていれば画像を取得してBASE64Encode
            //Firebaseは文字列と数字しか保存できないためBASE64Encodeが必要
            if (drawable != null) {
                Bitmap bitmap = drawable.getBitmap();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                String bitmapString = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

                data.put("image", bitmapString);
            }

            genreRef.push().setValue(data, this);
            mProgress.show();
        }
    }



    //onRequestPermissionsResult：許可を求めるダイアログからの結果を受け取る
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults){
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE: {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Userが許可した時
                    showChooser();
                }
                return;
            }
        }
    }

    //showChooser：Intent連携の選択ダイアログを表示
    private void showChooser() {
        //Galaryから選択するIntent
        Intent gallaryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        gallaryIntent.setType("image/*");
        gallaryIntent.addCategory(Intent.CATEGORY_OPENABLE);

        //cameraで撮影するIntent
        String filename = System.currentTimeMillis() + ".jpg";
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, filename);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        mPictureUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mPictureUri);

        //galary選択Intentを与えてcreateChooser methodを呼ぶ
        Intent chooserIntent = Intent.createChooser(gallaryIntent, "画像を取得");

        // EXTRA_INITIAL_INTENTS にカメラ撮影のIntentを追加
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{cameraIntent});

        startActivityForResult(chooserIntent, CHOOSER_REQUEST_CODE);
    }

    //onComplete：Firebaseへの保存完了時に呼ばれる
    @Override
    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
        mProgress.dismiss();

        if(databaseError == null) {
            finish();
        } else {
            Snackbar.make(findViewById(android.R.id.content), "投稿に失敗しました", Snackbar.LENGTH_LONG).show();
        }
    }
}