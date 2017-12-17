package jp.techacademy.yamamoto.daisuke.qa_app;

import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

import static jp.techacademy.yamamoto.daisuke.qa_app.MainActivity.sFavoriteQidMap;

public class QuestionDetailActivity extends AppCompatActivity {

    private ListView mListView;
    private Question mQuestion;
    private QuestionDetailListAdapter mAdapter;
    private DatabaseReference mAnswerRef;

    // ログイン済ユーザ取得
    FirebaseUser mUser = FirebaseAuth.getInstance().getCurrentUser();

    //fav登録有無。trueならlogin済
    private boolean mFavStatus = false;
    //favoriteボタン
    private FloatingActionButton mFavoriteButton;

    //favorite取得用のref
    private DatabaseReference mFavoriteRef;

    //Questionを取得するChildListener
    private ChildEventListener mEventListener = new ChildEventListener() {

        
        //アイテムのリストを取得
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            HashMap map = (HashMap) dataSnapshot.getValue();

            String answerUid = dataSnapshot.getKey();

            for (Answer answer : mQuestion.getAnswers()) {
                // 同じAnswerUidのものが存在しているときは何もしない
                if (answerUid.equals(answer.getAnswerUid())) {
                    return;
                }
            }

            String body = (String) map.get("body");
            String name = (String) map.get("name");
            String uid = (String) map.get("uid");

            Answer answer = new Answer(body, name, uid, answerUid);
            mQuestion.getAnswers().add(answer);
            mAdapter.notifyDataSetChanged();
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {

        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };

    //Favoriteを取得するChildListener
    private ChildEventListener mFavoriteListener = new ChildEventListener() {

        //アイテムのリストを取得
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {

            String favoriteQid = dataSnapshot.getKey();

//            Log.d("mytest_getKey_Value", String.valueOf(dataSnapshot.getKey()) + String.valueOf(dataSnapshot.getValue()));
//            Log.d("mytest_favoriteQid", favoriteQid);

            if(favoriteQid == null) {   //お気に入り未登録
                mFavStatus = false;
                mFavoriteButton.setImageResource(R.drawable.fav_off);
            } else {        //お気に入り登録済
                mFavStatus = true;
                mFavoriteButton.setImageResource(R.drawable.fav_on);
            }
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {

        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question_detail);

        // 渡ってきたQuestionのオブジェクトを保持する
        Bundle extras = getIntent().getExtras();
        mQuestion = (Question) extras.get("question");

        setTitle(mQuestion.getTitle());

        // ListViewの準備
        mListView = (ListView) findViewById(R.id.listView);
        mAdapter = new QuestionDetailListAdapter(this, mQuestion);
        mListView.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();

        //投稿ボタン
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (mUser == null) {
                    // ログインしていなければログイン画面に遷移させる
                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                    startActivity(intent);
                } else {
                    // Questionを渡して回答作成画面を起動する
                    Intent intent = new Intent(getApplicationContext(), AnswerSendActivity.class);
                    intent.putExtra("question", mQuestion);
                    startActivity(intent);
                }
            }
        });

        //お気に入りボタン
        mFavoriteButton = (FloatingActionButton) findViewById(R.id.favoriteButton);
        mFavoriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!mFavStatus) {
                    //お気に入り未登録なら、Firebaseに追加して、フラグをtrueにする
                    mFavoriteButton.setImageResource(R.drawable.fav_on);

                    Map<String, String> data = new HashMap<String, String>();
                    String genre = String.valueOf(mQuestion.getGenre());
                    data.put("genre", genre);

                    //firebaseのfavoriteにgenre追加
                    mFavoriteRef.setValue(data);

//                    Log.d("mytest_sFavoriteQidMap_mFavButtonOnClick", String.valueOf(sFavoriteQidMap));

                    //お気に入りMapに追加
                    sFavoriteQidMap.put(mQuestion.getQuestionUid(), genre);

                    mFavStatus = true;
                    Snackbar.make(view, "お気に入りに登録しました", Snackbar.LENGTH_LONG).show();

                } else {
                    //お気に入り登録済なら、Firebaseから削除して、フラグをfalseにする
                    mFavoriteButton.setImageResource(R.drawable.fav_off);

                    mFavoriteRef.removeValue();

                    sFavoriteQidMap.remove(mQuestion.getQuestionUid());

                    mFavStatus = false;
                    Snackbar.make(view, "お気に入りを解除しました", Snackbar.LENGTH_LONG).show();
                }
            }
        });

        DatabaseReference dataBaseReference = FirebaseDatabase.getInstance().getReference();
        mAnswerRef = dataBaseReference.child(Const.ContentsPATH).child(String.valueOf(mQuestion.getGenre())).child(mQuestion.getQuestionUid()).child(Const.AnswersPATH);
        mAnswerRef.addChildEventListener(mEventListener);
    }


    //Login後など、他画面から戻ってきた場合はonCreateを通過しないことがあるので、onResumeでLogin状態をチェックし、お気に入りボタン表示有無を判定する
    @Override
    protected void onResume() {
        super.onResume();

        if (mUser == null) {
            // 未loginならfavoriteButton非表示（お気に入り無し）
            mFavoriteButton.setVisibility(View.GONE);

        } else {
            //login済なら、お気に入り有無をチェックしてfavoriteButtonを表示
            mFavoriteButton.setVisibility(View.VISIBLE);

            //FavoriteのRefを取得
            DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference();
            mFavoriteRef = databaseRef.child(Const.FavoritePATH).child(String.valueOf(mUser.getUid())).child(mQuestion.getQuestionUid());
            mFavoriteRef.addChildEventListener(mFavoriteListener);

//            Log.d("mytest_mFavoriteRef", String.valueOf(mFavoriteRef));

        }
    }
}
