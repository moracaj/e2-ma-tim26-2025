package rs.ftn.rpgtracker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import com.google.firebase.firestore.DocumentReference;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.*;
import com.journeyapps.barcodescanner.ScanOptions;
import com.journeyapps.barcodescanner.ScanContract;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.DocumentReference;


import java.util.*;

public class FriendsActivity extends AppCompatActivity {
    TextView tvAllianceLeader;
    ListView listAllianceMembers;

    final List<String> memberNames = new ArrayList<>();
    ArrayAdapter<String> membersAdapter;
    ListenerRegistration membersReg;

    private final java.util.Set<String> seenInviteIds = new java.util.HashSet<>();
    private boolean invitesInitialized = false;
    ListenerRegistration msgsReg;
    private final java.util.Set<String> seenMsgIds = new java.util.HashSet<>();
    private boolean msgsInitialized = false;

    FirebaseFirestore db;
  String uid;
  String myUsername = "";

  EditText etFriend, etAllianceName;
  Button btnAddFriend, btnScanQR, btnCreateAlliance, btnLeaveOrDisband, btnInvites;
  ListView listFriends;
  TextView tvAllianceInfo;

  String myAllianceId = null;
  boolean iAmLeader = false;

  final List<String> friendNames = new ArrayList<>();
  final List<String> friendUids  = new ArrayList<>();
  ArrayAdapter<String> friendsAdapter;

  ListenerRegistration invitesReg;

  private final ActivityResultLauncher<String> camPerm = registerForActivityResult(
          new ActivityResultContracts.RequestPermission(),
          granted -> { if (granted) startQRScan(); else Toast.makeText(this,"Camera permission denied",Toast.LENGTH_SHORT).show(); });

    private final ActivityResultLauncher<com.journeyapps.barcodescanner.ScanOptions> qrLauncher =
            registerForActivityResult(new com.journeyapps.barcodescanner.ScanContract(), result -> {
                if (result.getContents() == null) return;
                String scanned = result.getContents().trim();
                if (scanned.isEmpty()) {
                    Toast.makeText(this, "Empty QR", Toast.LENGTH_SHORT).show();
                    return;
                }
                FirebaseFirestore db = FirebaseFirestore.getInstance();

                // 1) probaj kao UID
                db.collection("users").document(scanned).get().addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String otherUid = doc.getId();
                        String otherName = String.valueOf(doc.getString("username"));
                        writeFriendship(uid, myUsername, otherUid, otherName);
                    } else {
                        // 2) ako nije UID, probaj kao username
                        db.collection("users").whereEqualTo("username", scanned).limit(1).get()
                                .addOnSuccessListener(q -> {
                                    if (!q.isEmpty()) {
                                        String otherUid = q.getDocuments().get(0).getId();
                                        String otherName = q.getDocuments().get(0).getString("username");
                                        writeFriendship(uid, myUsername, otherUid, otherName);
                                    } else {
                                        Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, "Search error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    }
                }).addOnFailureListener(e -> Toast.makeText(this, "Load error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            });




  @Override protected void onCreate(@Nullable Bundle savedInstanceState){
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_friends);

    db  = FirebaseFirestore.getInstance();
    uid = Prefs.getUid(this); // koristi tvoj Prefs; vidi tačku 7 ako ga nemaš

    etFriend         = findViewById(R.id.etFriend);
    etAllianceName   = findViewById(R.id.etAllianceName);
    btnAddFriend     = findViewById(R.id.btnAddFriend);
    btnScanQR        = findViewById(R.id.btnScanQr);
    btnCreateAlliance= findViewById(R.id.btnCreateAlliance);
    btnLeaveOrDisband= findViewById(R.id.btnLeaveOrDisband);
    btnInvites       = findViewById(R.id.btnInvites);
    listFriends      = findViewById(R.id.listFriends);
    tvAllianceInfo   = findViewById(R.id.tvAllianceInfo);
      tvAllianceLeader    = findViewById(R.id.tvAllianceLeader);
      listAllianceMembers = findViewById(R.id.listAllianceMembers);

      membersAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, memberNames);
      listAllianceMembers.setAdapter(membersAdapter);

      friendsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, friendNames);
    listFriends.setAdapter(friendsAdapter);

    btnAddFriend.setOnClickListener(v -> addFriendByUsername());
    btnScanQR.setOnClickListener(v -> ensureCameraAndScan());
    btnCreateAlliance.setOnClickListener(v -> createAlliance());
    btnLeaveOrDisband.setOnClickListener(v -> onLeaveOrDisband());
    btnInvites.setOnClickListener(v -> showPendingInvitesDialog());

    listFriends.setOnItemClickListener((parent, view, position, id1) -> {
      String fUid = friendUids.get(position);
      startActivity(new Intent(this, ViewProfileActivity.class)
              .putExtra(ViewProfileActivity.EXTRA_VIEW_UID, fUid));
    });

    loadMe();
    loadFriends();
    listenInvites();
  }

  private void searchUsersAndOpenDialog(String query){
    String q = query.trim();
    if (q.isEmpty()) { Toast.makeText(this, "Enter username", Toast.LENGTH_SHORT).show(); return; }

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    // prefix pretraga: orderBy + startAt/endAt (case-sensitive)
    db.collection("users")
            .orderBy("username")
            .startAt(q)
            .endAt(q + "\uf8ff")
            .limit(20)
            .get()
            .addOnSuccessListener(snap -> {
              if (snap.isEmpty()){
                Toast.makeText(this, "No users found", Toast.LENGTH_SHORT).show();
                return;
              }
              java.util.List<String> names = new java.util.ArrayList<>();
              java.util.List<String> uids  = new java.util.ArrayList<>();
              for (DocumentSnapshot d : snap){
                names.add(String.valueOf(d.getString("username")));
                uids.add(d.getId());
              }

              new AlertDialog.Builder(this)
                      .setTitle("Users")
                      .setItems(names.toArray(new String[0]), (dlg, which) -> {
                        dlg.dismiss(); // zatvori dijalog da ne ostane preko

                        String selUid = uids.get(which);
                        Intent i = new Intent(FriendsActivity.this, ViewProfileActivity.class);
                        i.putExtra(ViewProfileActivity.EXTRA_VIEW_UID, selUid);
                        startActivity(i);
                      })
                      .setNegativeButton("Cancel", null)
                      .show();

            })
            .addOnFailureListener(e ->
                    Toast.makeText(this, "Search error: " + e.getMessage(), Toast.LENGTH_LONG).show()
            );
  }



  @Override protected void onDestroy(){
    super.onDestroy();
    if (invitesReg != null) invitesReg.remove();
    if (msgsReg != null) msgsReg.remove();
    if (membersReg != null) membersReg.remove();
  }

  private void loadMe(){
    db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
      if (doc.exists()){
        myUsername = doc.getString("username");
        myAllianceId = doc.getString("allianceId");
        refreshAllianceInfo();
        listenAllianceMessages();
      }
    });
  }

  private void refreshAllianceInfo(){
      if (myAllianceId == null || myAllianceId.isEmpty()){
          tvAllianceInfo.setText("No alliance");
          tvAllianceLeader.setText("Leader: —");
          memberNames.clear(); membersAdapter.notifyDataSetChanged();
          iAmLeader = false;
          btnLeaveOrDisband.setVisibility(View.GONE);
          detachMembersListener();
          return;
      }
      db.collection("alliances").document(myAllianceId).get().addOnSuccessListener(aDoc -> {
          if (!aDoc.exists()){
              tvAllianceInfo.setText("No alliance");
              tvAllianceLeader.setText("Leader: —");
              memberNames.clear(); membersAdapter.notifyDataSetChanged();
              iAmLeader = false;
              btnLeaveOrDisband.setVisibility(View.GONE);
              myAllianceId = null;
              detachMembersListener();
              return;
          }
          String name   = aDoc.getString("name");
          String leader = aDoc.getString("leaderUid");
          iAmLeader = uid.equals(leader);
          tvAllianceInfo.setText("Alliance: " + name + (iAmLeader ? " (Leader)" : ""));
          btnLeaveOrDisband.setVisibility(View.VISIBLE);
          btnLeaveOrDisband.setText(iAmLeader ? "Disband alliance" : "Leave alliance");

          // -- LEADER IME --
          if (leader != null && !leader.isEmpty()){
              db.collection("users").document(leader).get()
                      .addOnSuccessListener(u -> {
                          String ln = u.getString("username");
                          tvAllianceLeader.setText("Leader: " + (ln == null ? "—" : ln));
                      });
          } else {
              tvAllianceLeader.setText("Leader: —");
          }

          // -- LISTA ČLANOVA --
          listenMembers();   // (re)attach snapshot listener
      });
  }

    private void listenMembers(){
        detachMembersListener();
        if (myAllianceId == null || myAllianceId.isEmpty()) return;

        membersReg = db.collection("alliances").document(myAllianceId)
                .collection("members")
                .orderBy("username", Query.Direction.ASCENDING)
                .addSnapshotListener((qs, e) -> {
                    if (e != null || qs == null) return;
                    memberNames.clear();
                    for (DocumentSnapshot d : qs.getDocuments()){
                        String n = d.getString("username");
                        String role = d.getString("role");
                        memberNames.add((n == null ? "?" : n) + ("leader".equals(role) ? " (Leader)" : ""));
                    }
                    membersAdapter.notifyDataSetChanged();
                });
    }

    private void detachMembersListener(){
        if (membersReg != null) { membersReg.remove(); membersReg = null; }
    }



    private void loadFriends(){
    db.collection("users").document(uid).collection("friends")
            .orderBy("username", Query.Direction.ASCENDING)
            .addSnapshotListener((qs, e) -> {
              if (e != null || qs == null) return;
              friendNames.clear();
              friendUids.clear();
              for (DocumentSnapshot d : qs) {
                friendNames.add(String.valueOf(d.get("username")));
                friendUids.add(d.getId()); // docId = friendUid
              }
              friendsAdapter.notifyDataSetChanged();
            });
  }

  // Dodavanje prijatelja po korisničkom imenu
  private void addFriendByUsername(){
    //String u = etFriend.getText().toString().trim();
    //if (u.isEmpty()) { Toast.makeText(this,"Enter username",Toast.LENGTH_SHORT).show(); return; }
    //db.collection("users").whereEqualTo("username", u).limit(1).get().addOnSuccessListener(q -> {
    //  if (q.isEmpty()){ Toast.makeText(this,"User not found",Toast.LENGTH_SHORT).show(); return; }
    //  String otherUid = q.getDocuments().get(0).getId();
    //  if (otherUid.equals(uid)){ Toast.makeText(this,"That's you.",Toast.LENGTH_SHORT).show(); return; }
    //  String otherName = q.getDocuments().get(0).getString("username");
   //   writeFriendship(uid, myUsername, otherUid, otherName);
   // });
    searchUsersAndOpenDialog(etFriend.getText().toString());
  }

  private void writeFriendship(String aUid, String aName, String bUid, String bName){
    WriteBatch b = db.batch();
    DocumentReference A = db.collection("users").document(aUid).collection("friends").document(bUid);
    DocumentReference B = db.collection("users").document(bUid).collection("friends").document(aUid);
    Map<String,Object> f1 = new HashMap<>(); f1.put("uid", bUid); f1.put("username", bName); f1.put("createdAt", Timestamp.now());
    Map<String,Object> f2 = new HashMap<>(); f2.put("uid", aUid); f2.put("username", aName); f2.put("createdAt", Timestamp.now());
    b.set(A, f1); b.set(B, f2);
    b.commit().addOnSuccessListener(v -> Toast.makeText(this,"Friend added",Toast.LENGTH_SHORT).show());
  }

  // QR skeniranje
  private void ensureCameraAndScan(){
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
      startQRScan();
    } else {
      camPerm.launch(Manifest.permission.CAMERA);
    }
  }
  private void startQRScan(){
    ScanOptions opts = new ScanOptions();
    opts.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
    opts.setPrompt("Scan user QR");
    opts.setBeepEnabled(true);
    opts.setOrientationLocked(true);
    qrLauncher.launch(opts);
  }



  // Savez: kreiranje + pozivi svim prijateljima
  private void createAlliance(){
    String name = etAllianceName.getText().toString().trim();
    if (name.isEmpty()){ Toast.makeText(this,"Enter alliance name",Toast.LENGTH_SHORT).show(); return; }
    if (myAllianceId != null && !myAllianceId.isEmpty()){
      Toast.makeText(this, "Already in alliance", Toast.LENGTH_SHORT).show(); return;
    }
    if (uid == null || uid.isEmpty()){
      Toast.makeText(this, "Not signed in (uid empty).", Toast.LENGTH_LONG).show();
      return;
    }

    Map<String,Object> a = new HashMap<>();
    a.put("name", name);
    a.put("leaderUid", uid);
    a.put("missionActive", false);
    a.put("createdAt", com.google.firebase.Timestamp.now());

    db.collection("alliances").add(a)
            .addOnSuccessListener(ref -> {
              String allianceId = ref.getId();
              WriteBatch b = db.batch();
              b.set(ref.collection("members").document(uid), member(uid, myUsername, "leader"));
              b.update(db.collection("users").document(uid), "allianceId", allianceId);

              db.collection("users").document(uid).collection("friends").get()
                      .addOnSuccessListener(q -> {
                        for (DocumentSnapshot d : q){
                          String fu = d.getId();
                          String fn = d.getString("username");
                          DocumentReference inv = db.collection("users").document(fu)
                                  .collection("allianceInvites").document();
                          Map<String,Object> invite = new HashMap<>();
                          invite.put("inviteId", inv.getId());
                          invite.put("allianceId", allianceId);
                          invite.put("allianceName", name);
                          invite.put("fromUid", uid);
                          invite.put("fromUsername", myUsername);
                          invite.put("status", "pending");
                          invite.put("sentAt", com.google.firebase.Timestamp.now());
                          b.set(inv, invite);
                        }
                        b.commit()
                                .addOnSuccessListener(v -> {
                                  myAllianceId = allianceId;
                                  iAmLeader = true;
                                  refreshAllianceInfo();
                                  Toast.makeText(this,"Alliance created & invites sent",Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> Toast.makeText(this,"Commit failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
                      })
                      .addOnFailureListener(e -> Toast.makeText(this,"Read friends failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            })
            .addOnFailureListener(e -> Toast.makeText(this,"Create alliance failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
  }


  private Map<String,Object> member(String uid, String username, String role){
    Map<String,Object> m = new HashMap<>();
    m.put("uid", uid); m.put("username", username); m.put("role", role);
    m.put("joinedAt", Timestamp.now());
    return m;
  }

    private void onLeaveOrDisband(){
        if (myAllianceId == null || myAllianceId.isEmpty()) return;

        if (!iAmLeader) {
            // Leave kao član
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Leave alliance?")
                    .setMessage("Are you sure you want to leave the alliance?")
                    .setPositiveButton("Leave", (d,w) -> {
                        WriteBatch b = db.batch();
                        // 1) ukloni moje članstvo iz members
                        b.delete(db.collection("alliances").document(myAllianceId)
                                .collection("members").document(uid));
                        // 2) očisti mi allianceId
                        b.update(db.collection("users").document(uid), "allianceId", null);
                        b.commit().addOnSuccessListener(v -> {
                            myAllianceId = null;
                            refreshAllianceInfo();
                            Toast.makeText(this, "You left the alliance.", Toast.LENGTH_SHORT).show();
                        }).addOnFailureListener(e ->
                                Toast.makeText(this, "Leave failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    })
                    .setNegativeButton("Cancel", null).show();
            return;
        }

        // Disband kao vođa
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Disband alliance?")
                .setMessage("This will remove the alliance and all members will be detached.")
                .setPositiveButton("Disband", (d,w) -> disbandAllianceAsLeader())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void disbandAllianceAsLeader(){
        final DocumentReference aRef = db.collection("alliances").document(myAllianceId);

        // (opciono) blokada ako je misija aktivna
        aRef.get().addOnSuccessListener(doc -> {
            Boolean missionActive = doc.getBoolean("missionActive");
            if (Boolean.TRUE.equals(missionActive)) {
                Toast.makeText(this, "Mission active — cannot disband.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 1) pročitaj sve članove
            aRef.collection("members").get().addOnSuccessListener(membersSnap -> {
                WriteBatch b = db.batch();

                // 2) svakom članu: obriši članstvo + očisti allianceId (ovde rules sada dopuštaju)
                for (DocumentSnapshot m : membersSnap.getDocuments()){
                    String memberUid = m.getId();
                    // delete member doc
                    b.delete(m.getReference());
                    // clear allianceId kod usera
                    b.update(db.collection("users").document(memberUid), "allianceId", null);
                }

                // 3) obriši sam dokument saveza
                b.delete(aRef);

                b.commit().addOnSuccessListener(v -> {
                    myAllianceId = null;
                    iAmLeader = false;
                    refreshAllianceInfo();
                    Toast.makeText(this, "Alliance disbanded.", Toast.LENGTH_SHORT).show();
                }).addOnFailureListener(e ->
                        Toast.makeText(this, "Disband failed: " + e.getMessage(), Toast.LENGTH_LONG).show());

            }).addOnFailureListener(e ->
                    Toast.makeText(this, "Load members failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });
    }

    private void showPendingInvitesDialog() {
        db.collection("users").document(uid).collection("allianceInvites")
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(q -> {
                    if (q.isEmpty()) { Toast.makeText(this, "No pending invites", Toast.LENGTH_SHORT).show(); return; }
                    final java.util.List<DocumentSnapshot> docs = q.getDocuments();
                    String[] items = new String[docs.size()];
                    for (int i = 0; i < docs.size(); i++) {
                        String name = String.valueOf(docs.get(i).getString("allianceName"));
                        String from = String.valueOf(docs.get(i).getString("fromUsername"));
                        items[i] = name + " — from " + from;
                    }
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("Invites")
                            .setItems(items, (dlg, which) -> showInviteActions(docs.get(which)))
                            .setNegativeButton("Close", null)
                            .show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Invites error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }


    private void handleInvite(DocumentSnapshot inv){
    String allianceId = inv.getString("allianceId");
    String allianceName = inv.getString("allianceName");
    if (myAllianceId != null && !myAllianceId.isEmpty()){
      Toast.makeText(this,"Already in alliance",Toast.LENGTH_SHORT).show();
      return;
    }
    db.collection("alliances").document(allianceId).get().addOnSuccessListener(aDoc -> {
      boolean missionActive = Boolean.TRUE.equals(aDoc.getBoolean("missionActive"));
      if (!aDoc.exists()){
        Toast.makeText(this,"Alliance no longer exists",Toast.LENGTH_SHORT).show();
        inv.getReference().update("status","declined");
        return;
      }
      if (missionActive){
        Toast.makeText(this,"Mission active in that alliance",Toast.LENGTH_SHORT).show();
        return;
      }

      new AlertDialog.Builder(this)
              .setTitle("Join " + allianceName + "?")
              .setPositiveButton("Accept", (d, w) -> {
                WriteBatch b = db.batch();
                b.set(aDoc.getReference().collection("members").document(uid), member(uid, myUsername, "member"));
                b.update(db.collection("users").document(uid), "allianceId", allianceId);
                b.update(inv.getReference(), "status", "accepted");
                b.commit().addOnSuccessListener(v -> {
                  myAllianceId = allianceId;
                  refreshAllianceInfo();
                  Toast.makeText(this,"Joined alliance",Toast.LENGTH_SHORT).show();
                });
              })
              .setNegativeButton("Decline", (d, w) -> inv.getReference().update("status","declined"))
              .show();
    });
  }

    private void listenInvites(){
        invitesReg = db.collection("users").document(uid).collection("allianceInvites")
                .whereEqualTo("status","pending")
                .addSnapshotListener((qs, e) -> {
                    if (e != null || qs == null) return;

                    if (!invitesInitialized) {
                        for (DocumentSnapshot d : qs.getDocuments()) seenInviteIds.add(d.getId());
                        invitesInitialized = true;
                        return; // ne notifikuj postojeće
                    }

                    for (DocumentChange ch : qs.getDocumentChanges()) {
                        if (ch.getType() == DocumentChange.Type.ADDED) {
                            DocumentSnapshot d = ch.getDocument();
                            if (seenInviteIds.add(d.getId())) {
                                String allianceName = String.valueOf(d.getString("allianceName"));
                                Intent tap = new Intent(this, FriendsActivity.class);
                                Notifications.show(this, Notifications.CH_INVITES,
                                        Math.abs(d.getId().hashCode()),
                                        "Alliance invite",
                                        "You were invited to: " + allianceName,
                                        tap, true);
                            }
                        }
                    }
                });
    }

    private void showInviteActions(DocumentSnapshot inv) {
        String allianceName = String.valueOf(inv.getString("allianceName"));
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(allianceName)
                .setMessage("Join this alliance?")
                .setPositiveButton("Accept", (d, w) -> acceptInvite(inv))
                .setNegativeButton("Decline", (d, w) -> inv.getReference().update("status", "declined"))
                .setNeutralButton("Back", null)
                .show();
    }

    private void acceptInvite(DocumentSnapshot inv) {
        final String newAid   = String.valueOf(inv.getString("allianceId"));
        final String newName  = String.valueOf(inv.getString("allianceName"));
        final DocumentReference meRef  = db.collection("users").document(uid);
        final DocumentReference newARef = db.collection("alliances").document(newAid);

        meRef.get().addOnSuccessListener(me -> {
            final String myName = String.valueOf(me.getString("username"));
            final String oldAid = me.getString("allianceId");

            // Ako već nisi ni u jednom savezu (ili je isti savez) -> klasično pridruživanje
            if (oldAid == null || oldAid.isEmpty() || newAid.equals(oldAid)) {
                joinAllianceTx(inv, newARef, meRef, myName, newAid, null);
                return;
            }

            // U starom si savezu – pitaj da li želi da pređe i napusti stari
            final DocumentReference oldARef = db.collection("alliances").document(oldAid);
            oldARef.get().addOnSuccessListener(oldA -> {
                String oldName = oldA.exists() ? String.valueOf(oldA.getString("name")) : "(current)";
                boolean iAmLeaderThere = oldA.exists() && uid.equals(oldA.getString("leaderUid"));

                if (iAmLeaderThere) {
                    Toast.makeText(this,
                            "You are the leader of \"" + oldName + "\". Disband that alliance first.",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                new AlertDialog.Builder(this)
                        .setTitle("Switch alliance?")
                        .setMessage("You are in \"" + oldName + "\".\nJoin \"" + newName + "\" and leave your current alliance?")
                        .setPositiveButton("Join new", (d,w) ->
                                joinAllianceTx(inv, newARef, meRef, myName, newAid, oldARef))
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        });
    }

    private void joinAllianceTx(DocumentSnapshot inv,
                                DocumentReference newARef,
                                DocumentReference meRef,
                                String myName,
                                String newAid,
                                @Nullable DocumentReference oldARef) {

        db.runTransaction(tx -> {
            // novi savez mora da postoji
            DocumentSnapshot aNew = tx.get(newARef);
            if (!aNew.exists()) throw new RuntimeException("Alliance no longer exists");

            // ako je prosleđen stari – izbriši članstvo tamo
            if (oldARef != null) {
                DocumentSnapshot aOld = tx.get(oldARef);
                if (aOld.exists()) {
                    // dodatna zaštita: vođa ne sme da ode
                    String leaderUid = aOld.getString("leaderUid");
                    if (uid.equals(leaderUid))
                        throw new RuntimeException("Leader must disband current alliance");
                    tx.delete(oldARef.collection("members").document(uid));
                }
            }

            // upiši novo članstvo + user.allianceId + invite status
            tx.set(newARef.collection("members").document(uid),
                    member(uid, myName, "member"));
            tx.update(meRef, "allianceId", newAid);
            tx.update(inv.getReference(), "status", "accepted");
            return null;
        }).addOnSuccessListener(v -> {
            myAllianceId = newAid;
            iAmLeader = false;
            refreshAllianceInfo();
            Toast.makeText(this, "Joined " + inv.getString("allianceName"), Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Accept failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
        );
    }


    private void listenAllianceMessages() {
        // skini stari listener ako se promenio savez / logout
        if (msgsReg != null) { msgsReg.remove(); msgsReg = null; }
        seenMsgIds.clear();
        msgsInitialized = false;

        if (myAllianceId == null || myAllianceId.isEmpty()) return;

        msgsReg = db.collection("alliances").document(myAllianceId)
                .collection("messages")
                .orderBy("ts", Query.Direction.ASCENDING)
                .addSnapshotListener((qs, e) -> {
                    if (e != null || qs == null) return;

                    if (!msgsInitialized) {
                        // ignorisi istoriju pri prvom ucitavanju
                        for (DocumentSnapshot d : qs.getDocuments()) seenMsgIds.add(d.getId());
                        msgsInitialized = true;
                        return;
                    }

                    for (DocumentChange ch : qs.getDocumentChanges()) {
                        if (ch.getType() != DocumentChange.Type.ADDED) continue;

                        DocumentSnapshot d = ch.getDocument();
                        String mid   = d.getId();
                        String fromU = String.valueOf(d.getString("senderUid"));
                        String fromN = String.valueOf(d.getString("senderUsername"));
                        String text  = String.valueOf(d.getString("text"));

                        // notifikuj samo ako poruka NIJE moja i ako je nova
                        if (!uid.equals(fromU) && seenMsgIds.add(mid)) {
                            Intent tap = new Intent(this, ChatActivity.class);
                            Notifications.show(
                                    this, Notifications.CH_CHAT,
                                    Math.abs(mid.hashCode()),
                                    fromN, text, tap, false
                            );
                        }
                    }
                });
    }


}
