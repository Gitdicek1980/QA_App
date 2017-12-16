package jp.techacademy.yamamoto.daisuke.qa_app;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    //Drawer表示用
    private Toolbar mToolbar;
    private int mGenre = 0;

    NavigationView mNavigationView;

    //質問リスト表示用
    private DatabaseReference mDatabaseReference;
    private DatabaseReference mGenreRef;
    private ListView mListView;
    private ArrayList<Question> mQuestionArrayList;
    private QuestionsListAdapter mAdapter;

    //お気に入りの情報<genre, questionUid>を格納する
    protected static Map<String, String> sFavoriteQidMap = new HashMap();

    // ログイン済ユーザ取得
    FirebaseUser mUser = FirebaseAuth.getInstance().getCurrentUser();

    //favorite取得用のref
    private DatabaseReference mFavoriteRef;

    private ChildEventListener mFavoriteListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            HashMap map = (HashMap) dataSnapshot.getValue();
            String genre = (String) map.get("genre");

//            Log.d("mytest_Main_map", String.valueOf(map));
//            Log.d("mytest_Main_genre", String.valueOf(genre));
//            Log.d("mytest_Main_getKey", String.valueOf(dataSnapshot.getKey()));

            //お気に入りの質問IDとジャンルをsFavoriteQidMapに格納
            sFavoriteQidMap.put(dataSnapshot.getKey(), genre);

//            Log.d("mytest_sFavoriteQidMap_Main", String.valueOf(sFavoriteQidMap));
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

    private ChildEventListener mEventListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            HashMap map = (HashMap) dataSnapshot.getValue();
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
            mQuestionArrayList.add(question);
            mAdapter.notifyDataSetChanged();


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
        setContentView(R.layout.activity_main);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // ジャンルを選択していない場合（mGenre == 0）はエラーを表示するだけ
                if (mGenre == 0) {
                    Snackbar.make(view, "ジャンルを選択して下さい", Snackbar.LENGTH_LONG).show();
                    return;
                }

                //Login済user取得
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                //未loginはlogin画面へ遷移
                if(user == null){
                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                    startActivity(intent);
                } else {
                    // ジャンルを渡して質問作成画面を起動する
                    Intent intent = new Intent(getApplicationContext(), QuestionSendActivity.class);
                    intent.putExtra("genre", mGenre);
                    startActivity(intent);
                }
            }
        });

        //NavigationDrawerの設定
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, mToolbar, R.string.app_name, R.string.app_name);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.nav_hobby) {
                    mToolbar.setTitle("趣味");
                    mGenre = 1;
                } else if (id == R.id.nav_life) {
                    mToolbar.setTitle("生活");
                    mGenre = 2;
                } else if (id == R.id.nav_health) {
                    mToolbar.setTitle("健康");
                    mGenre = 3;
                } else if (id == R.id.nav_computer) {
                    mToolbar.setTitle("コンピューター");
                    mGenre = 4;
                } else if (id == R.id.nav_favorite) {
                    mGenre = 0;
                }

                if (mGenre == 0) {
                    //選択したジャンルがお気に入りの時、お気に入り画面へ遷移
                    Intent intent = new Intent(getApplicationContext(), FavoriteActivity.class);
                    startActivity(intent);

                } else {    //選択したジャンルがお気に入り以外の場合
                    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                    drawer.closeDrawer(GravityCompat.START);

                    // 質問のリストをクリアしてから再度Adapterにセットし、AdapterをListViewにセットし直す
                    mQuestionArrayList.clear();
                    mAdapter.setQuestionArrayList(mQuestionArrayList);
                    mListView.setAdapter(mAdapter);

                    // 選択したジャンルにリスナーを登録する
                    if (mGenreRef != null) {
                        mGenreRef.removeEventListener(mEventListener);
                    }
                    mGenreRef = mDatabaseReference.child(Const.ContentsPATH).child(String.valueOf(mGenre));
                    mGenreRef.addChildEventListener(mEventListener);
                }
                return true;
            }
        });

        // Firebase
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();

        // ListViewの準備
        mListView = (ListView) findViewById(R.id.listView);
        mAdapter = new QuestionsListAdapter(this);
        mQuestionArrayList = new ArrayList<Question>();
        mAdapter.notifyDataSetChanged();

//        Log.d("mytest_Main_mAdapter", String.valueOf(mAdapter));

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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(getApplicationContext(), SettingActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    //onResumeでLogin状態をチェックし、お気に入りのリンク表示有無を判定する
    @Override
    protected void onResume() {
        super.onResume();

        mUser = FirebaseAuth.getInstance().getCurrentUser();

        if (mUser == null) {
            // 未loginならfavoriteButton非表示（お気に入り無し）
            mNavigationView.getMenu().findItem(R.id.nav_favorite).setVisible(false);
            sFavoriteQidMap.clear();

        } else {
            //login済ならfavoriteButtonを表示
            mNavigationView.getMenu().findItem(R.id.nav_favorite).setVisible(true);

            //FavoriteのRefを取得し、mFavoriteListenerでsFavoriteMapにお気に入りデータを格納する
            DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference();
            mFavoriteRef = databaseRef.child(Const.FavoritePATH).child(String.valueOf(mUser.getUid()));
            mFavoriteRef.addChildEventListener(mFavoriteListener);


            //Listなどの場合は、for文でlogを取得しないとパスしか入らないので以下のようにする
//            for(Question q : mQuestionArrayList){
//                Log.d("mytest_getQid", String.valueOf(q.getQuestionUid()));
//            }


//            Log.d("mytest_mFavoriteRef", String.valueOf(mFavoriteRef));

        }
    }

}



