package jp.techacademy.yamamoto.daisuke.qa_app;

import android.content.Intent;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static jp.techacademy.yamamoto.daisuke.qa_app.MainActivity.sFavoriteQidMap;

public class FavoriteActivity extends AppCompatActivity {

    //質問リスト表示用
    private DatabaseReference mDatabaseReference;
    private DatabaseReference mGenreRef;
    private ListView mListView;
    private ArrayList<Question> mQuestionArrayList;
    private QuestionsListAdapter mAdapter;

    private Toolbar mToolbar;
    private int mGenre = 0;

    NavigationView mNavigationView;


    private ChildEventListener mEventListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            HashMap map = (HashMap) dataSnapshot.getValue();

//            Log.d("mytest_Favorite_dataSnapshot.getKey()", String.valueOf(dataSnapshot.getKey()));
//            Log.d("mytest_Favorite_sFavoriteQidMap", String.valueOf(sFavoriteQidMap));

//            mQuestionArrayList.clear();

            //取得した質問のQidがsFavoriteQidMapに登録されているQidと同じならmQuestionArrayListに追加する
            if(sFavoriteQidMap.containsKey(dataSnapshot.getKey())){

                String title = (String) map.get("title");
                String body = (String) map.get("body");
                String name = (String) map.get("name");
                String uid = (String) map.get("uid");
                String imageString = (String) map.get("image");
                byte[] bytes;
                if (imageString != null) {
                    bytes = Base64.decode(imageString, Base64.DEFAULT);
                } else {
                    bytes = new byte[0];
                }

                ArrayList<Answer> answerArrayList = new ArrayList<Answer>();
                HashMap answerMap = (HashMap) map.get("answers");
                if (answerMap != null) {
                    for (Object key : answerMap.keySet()) {
                        HashMap temp = (HashMap) answerMap.get((String) key);
                        String answerBody = (String) temp.get("body");
                        String answerName = (String) temp.get("name");
                        String answerUid = (String) temp.get("uid");
                        Answer answer = new Answer(answerBody, answerName, answerUid, (String) key);
                        answerArrayList.add(answer);
                    }
                }

                Question question = new Question(title, body, name, uid, dataSnapshot.getKey(), mGenre, bytes, answerArrayList);

//                Log.d("mytest_Favorite_question", String.valueOf(question));
//                Log.d("mytest_Favorite_dataSnapshot.getKey()", String.valueOf(dataSnapshot.getKey()));
//                Log.d("mytest_Favorite_sFavoriteQidMap", String.valueOf(sFavoriteQidMap));
//                Log.d("mytest_Favorite_mQuestionArrayList1", String.valueOf(mQuestionArrayList));

                mQuestionArrayList.add(question);
                mAdapter.notifyDataSetChanged();

//                Log.d("mytest_Favorite_mQuestionArrayList2", String.valueOf(mQuestionArrayList));

            }
//            Log.d("mytest_Favorite_mQuestionArrayList3", String.valueOf(mQuestionArrayList));

        }


        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            HashMap map = (HashMap) dataSnapshot.getValue();

            // 変更があったQuestionを探す
            for (Question question: mQuestionArrayList) {
                if (dataSnapshot.getKey().equals(question.getQuestionUid())) {
                    // このアプリで変更がある可能性があるのは回答(Answer)のみ
                    question.getAnswers().clear();
                    HashMap answerMap = (HashMap) map.get("answers");
                    if (answerMap != null) {
                        for (Object key : answerMap.keySet()) {
                            HashMap temp = (HashMap) answerMap.get((String) key);
                            String answerBody = (String) temp.get("body");
                            String answerName = (String) temp.get("name");
                            String answerUid = (String) temp.get("uid");
                            Answer answer = new Answer(answerBody, answerName, answerUid, (String) key);
                            question.getAnswers().add(answer);
                        }
                    }

                    mAdapter.notifyDataSetChanged();
                }
            }
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
        setContentView(R.layout.activity_favorite);
        setTitle("お気に入り");

        // Firebase
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();

        // ListViewの準備
        mListView = (ListView) findViewById(R.id.listView);
        mAdapter = new QuestionsListAdapter(this);
        mQuestionArrayList = new ArrayList<Question>();


        mAdapter.setQuestionArrayList(mQuestionArrayList);
        mListView.setAdapter(mAdapter);

//        Log.d("mytest_Favorite_mListView", String.valueOf(mListView));
//        Log.d("mytest_Favorite_mAdapter", String.valueOf(mAdapter));
//        Log.d("mytest_Favorite_mQuestionArrayList", String.valueOf(mQuestionArrayList));

        mAdapter.notifyDataSetChanged();

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Questionのインスタンスを渡して質問詳細画面を起動する
                Intent intent = new Intent(getApplicationContext(), QuestionDetailActivity.class);
                intent.putExtra("question", mQuestionArrayList.get(position));
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        mQuestionArrayList.clear();

        //質問一式全てを全genre分処理を回す
        for(int i = 1; i <= 4; i++){
            mGenreRef = mDatabaseReference.child(Const.ContentsPATH).child(String.valueOf(i));
            mGenreRef.addChildEventListener(mEventListener);

//            Log.d("mytest_mEventListener", String.valueOf(mEventListener));
//            Log.d("mytest_child(i)", String.valueOf(i));

        }
    }

}
