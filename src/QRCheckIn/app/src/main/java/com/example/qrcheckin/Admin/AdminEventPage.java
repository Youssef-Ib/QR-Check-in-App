package com.example.qrcheckin.Admin;

import static android.content.ContentValues.TAG;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.qrcheckin.Attendee.AttendeeDatabaseManager;
import com.example.qrcheckin.Common.Image;
import com.example.qrcheckin.Common.ImageStorageManager;
import com.example.qrcheckin.Event.AttendeeList;
import com.example.qrcheckin.Event.Event;
import com.example.qrcheckin.Event.EventDatabaseManager;
import com.example.qrcheckin.Event.QRCode;
import com.example.qrcheckin.Event.QrCodeImageView;
import com.example.qrcheckin.Notifications.CreateNotification;
import com.example.qrcheckin.Notifications.DialogRecyclerView;
import com.example.qrcheckin.Notifications.MyNotificationManager;
import com.example.qrcheckin.Notifications.Notification;
import com.example.qrcheckin.Notifications.NotificationDatabaseManager;
import com.example.qrcheckin.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import org.json.JSONArray;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Display detailed info about specific event.
 * Retrieves & displays event details from Firestore database.
 * Provide users with information about event; name, date, location, description, images.
 */
public class AdminEventPage extends AppCompatActivity {
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private EventDatabaseManager eventDb;
    ImageButton qrButton;
    ImageButton eventButton;
    ImageButton addEventButton;
    ImageButton profileButton;
    ImageButton openNotifications;
    ImageButton openBottomSheetBtn;
    CheckBox signupCheckBox;
    TextView signupLimitReached;
    Admin admin;
    private ImageView ivEventPoster, ivEventPromoQr;
    private String documentId;
    private String fcmToken;
    private Event event;
    private String eventName;
    private String eventDate;
    private QRCode promoQRCode;
    private QRCode checkInQRCode;

    /**
     * Init activity, sets content view, and configures the toolbar with navigation buttons.
     * Retrieves & displays event details from Firestore based on the passed document ID.
     *
     * @param savedInstanceState If activity re-initialized after previously being shut down,
     *                           contains data most recently supplied.
     *                           Otherwise its null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_event_activity);

        // Set and display the main bar


        Toolbar toolbar = findViewById(R.id.organizer_eventScreen_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //initializeViews();

        // Find the text views on the event page xml
        TextView tvEventDate = findViewById(R.id.text_event_date);
        TextView tvEventLocation = findViewById(R.id.text_event_location);
        TextView tvEventDescription = findViewById(R.id.text_event_description);
        ivEventPoster = findViewById(R.id.image_event_poster);
        ImageView ivEventPromoQr = findViewById(R.id.image_event_promo_qr);
        Button removeEvent = findViewById(R.id.btnRemoveEvent);
        openBottomSheetBtn = findViewById(R.id.openBottomSheetButton);


        //https://stackoverflow.com/questions/18826870/how-to-animate-the-textview-very-very-long-text-scroll-automatically-horizonta, 2024, how to get the horizontal scrolling text
        TextView locationsStatus = findViewById(R.id.locationStatusTxt);
        locationsStatus.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        locationsStatus.setSelected(true);
        locationsStatus.setSingleLine(true);

        signupCheckBox = findViewById(R.id.signup_button);
        signupLimitReached = findViewById(R.id.signup_limit_text);
        signupLimitReached.setVisibility(View.INVISIBLE);
        TextView header = findViewById(R.id.mainHeader);
        admin = null;
        admin = new Admin();
        // Retrieve the event passed from the previous activity
        Intent intent = getIntent();
        documentId = intent.getStringExtra("DOCUMENT_ID");
        if(documentId == null) {
            Log.e(TAG, "Document ID is null");
            // Handle the error, maybe finish the activity
            finish();
            return;
        }
        SharedPreferences prefs = getSharedPreferences("TOKEN_PREF", MODE_PRIVATE);
        fcmToken = prefs.getString("token", "missing token");
        Log.d(TAG, "Document ID: " + documentId + ", FCM Token: " + fcmToken);
        eventDb = new EventDatabaseManager(documentId);
        openBottomSheetBtn.setVisibility(View.GONE);

        eventDb.getDocRef().get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                // Get and display event details
                event = documentSnapshot.toObject(Event.class);
                if (documentSnapshot != null && event != null) {

                    // set attributes that are used if user clicks QR code(s) to share it
                    eventName = event.getEventName();
                    eventDate = event.getEventDate();
                    promoQRCode = event.getPromoQRCode();
                    checkInQRCode = event.getCheckInQRCode();

                    if (event.isCheckInStatus()) {
                        locationsStatus.setText("Event is using your location           Event is using your location            Event is using your location");
                    } else {
                        locationsStatus.setText("Event is not using your location           Event is not using your location            Event is not using your location");
                    }

                    header.setText(eventName);
                    tvEventLocation.setText(event.getEventLocation());
                    tvEventDate.setText(eventDate);
                    tvEventDescription.setText(event.getEventDescription());
                    setSignupCheckBox(event.getSignupLimit(), event.getSignups());
                    // Set the ImageView for the Event's poster
                    if (event.getPoster() != null){
                        ImageStorageManager storagePoster = new ImageStorageManager(event.getPoster(), "/EventPosters");
                        storagePoster.displayImage(ivEventPoster);
                    }
                    else{
                        ivEventPoster.setImageResource(R.drawable.default_poster);
                    }
                    // Set the ImageView for the Event's promotional QR code
                    if (promoQRCode != null) {
                        ImageStorageManager storageQr = new ImageStorageManager(promoQRCode, "/QRCodes");
                        storageQr.displayImage(ivEventPromoQr);
                    }
                    else{
                        // Set TextView to inform user that this event does not have a promotional QR code
                        TextView promoTextView = findViewById(R.id.promo_qr_description);
                        promoTextView.setText(R.string.no_promo_text);
                    }
                    // Check if current event is organized by this user
                    if (Objects.equals(event.getOrganizer(), fcmToken)) {
                        openBottomSheetBtn.setVisibility(View.VISIBLE);
                        // Check milestones
                        checkSignUpMilestone();
                        checkAttendeeMilestone();
                        // Set open Bottom Sheet Listner
                        openBottomSheetBtn.setOnClickListener(v -> {
                        showBottomSheetDialog();
                        });
                    }

                } else {
                    Log.d("Firestore", String.format("No such document with id %s", documentId));
                }

            }
        });

        // Remove event buttno
        removeEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteConfirmationDialog();
            }
        });

        // Set Listner for the signup checkbox
        signupCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                AttendeeDatabaseManager attendeeDb = new AttendeeDatabaseManager(fcmToken);
                // Update signup array in event document
                if (isChecked){
                    eventDb.addToArrayField("signups", fcmToken);
                    attendeeDb.addToArrayField("signupEvents", documentId);
                }
                else{
                    eventDb.removeFromArrayField("signups", fcmToken);
                    attendeeDb.removeFromArrayField("signupEvents", documentId);
                }
            }
        });

        // Listen for updates to the event document, for cases where another user signs up for the event while this page is loaded
        eventDb.getDocRef().addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Log.w(TAG, "Listen failed.", error);
                    return;
                }
                if (value != null && value.exists()) {
                    Log.d(TAG, "Current data: " + value.getData());
                    Event event = value.toObject(Event.class);
                    assert event != null;
                    // Update the CheckBox upon a change in the event doc
                    setSignupCheckBox(event.getSignupLimit(), event.getSignups());
                } else {
                    Log.d(TAG, "Current data: null");
                }
            }
        });

        openNotifications = findViewById(R.id.notificationIconBtn);
        // Handles click on event notification
        openNotifications.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NotificationListDialog();
            }
        });
    }
    public void setSignupCheckBox(int signupLimit, ArrayList<String> signups){
        // Set status of the checkbox
        boolean userInSignups = signups != null && signups.contains(fcmToken);
        signupCheckBox.setChecked(userInSignups);

        // Set visibilities of CheckBox and limit reached TextView
        if(userInSignups || Objects.requireNonNull(signups).size() != signupLimit){
            // User is signed up for the event, so they see the checkbox (in case they want to un-signup)
            // Or the signup limit is not reached, so checkbox is visible to anyone
            signupCheckBox.setVisibility(View.VISIBLE);
            signupLimitReached.setVisibility(View.INVISIBLE);
        }
        else{
            // User is not in signups list AND the limit is reached, show the limit reached text
            signupCheckBox.setVisibility(View.INVISIBLE);
            signupLimitReached.setVisibility(View.VISIBLE);
        }

    }
    public void deleteEventPoster(String eventId) {
        // Assuming dbManager is already configured to interact with Firestore
        eventDb.getDocRef().get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                Event event = documentSnapshot.toObject(Event.class);
                if (event == null) {
                    Log.e("Firestore", "Event doc not found for ID: " + eventId);
                } else {
                    Image posterPath = event.getPoster();
                    if (posterPath != null && posterPath.getUriString()!=null) {
                        ImageStorageManager storage = new ImageStorageManager(posterPath, "/EventPosters");
                        // Remove event poster from storage
                        storage.deleteImage();
                        // update event doc to remove poster path
                        event.setPoster(null);

                        // Push the updated event object back to Firestore
                        eventDb.getDocRef().set(event)
                                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Event poster reference removed successfully."))
                                .addOnFailureListener(e -> Log.e("Firestore", "Error updating event poster reference", e));
                    } else {
                        Log.d("Firestore", "No poster to delete for event ID: " + eventId);
                    }
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@androidx.annotation.NonNull Exception e) {
                Log.e("Firestore", "Failed to fetch event for poster deletion: " + e.getMessage());
            }
        });
    }


    /**
     * Opens dialog with list of notifications/announcements for the event
     */
    public void NotificationListDialog(){
        ArrayList<Notification> notifications = new ArrayList<>();
        Context context = this;
        NotificationDatabaseManager db = new NotificationDatabaseManager();
        // Get all notifications
        db.getCollectionRef().get().addOnSuccessListener(notificationSnapshots -> {
            for(DocumentSnapshot snapshot : notificationSnapshots){
                Notification notification = snapshot.toObject(Notification.class);
                // If a notification belongs to this Event, add it to the list to be displayed
                if(Objects.equals(notification.getEventID(), documentId)){
                    notifications.add(notification);
                }
            }
            // Sort notifications by dateTime field
            // openai, 2024, chatgpt: how to sort the list based on date
            Collections.sort(notifications, new Comparator<Notification>() {
                @Override
                public int compare(Notification n1, Notification n2) {
                    // Parse dateTime strings to Date objects for comparison
                    SimpleDateFormat dateFormat = new SimpleDateFormat("MMM, dd, yyyy; h:mm a", Locale.getDefault());
                    try {
                        Date date1 = dateFormat.parse(n1.getDateTime());
                        Date date2 = dateFormat.parse(n2.getDateTime());
                        // Compare Date objects in descending order
                        return date2.compareTo(date1);
                    } catch (ParseException e) {
                        e.printStackTrace();
                        return 0;
                    }
                }
            });

            // Create dialog recycler view to display notifications
            DialogRecyclerView listDialog = new DialogRecyclerView(
                    context, notifications) {
                @Override
                public void onCreate(Bundle savedInstanceState) {
                    super.onCreate(savedInstanceState);
                }
            };
            listDialog.show();
        });
    }

    /**
     * Opens the Bottom Sheet to access Organizer Options for their event
     * https://www.youtube.com/watch?v=sp9j0e-Kzc8&t=472s, 2024, how to implement a bottom sheet
     */
    private void showBottomSheetDialog() {

        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.bottom_sheet_layout);

        LinearLayout viewCheckInQRCode = dialog.findViewById(R.id.viewCheckInQRCode);
        LinearLayout createEventNotification = dialog.findViewById(R.id.createEventNotification);
        LinearLayout viewEventSignups = dialog.findViewById(R.id.viewSignedUp);
        LinearLayout viewEventParticipants = dialog.findViewById(R.id.viewEventCheckin);
        LinearLayout deleteEvent = dialog.findViewById(R.id.deleteEvent);

        // Listener for view check-in QR code layout
        viewCheckInQRCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Call method to display acitivty to share the promotional QR code
                shareQRCode(checkInQRCode, "Check-in QR Code");
            }
        });

        createEventNotification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                Intent event = new Intent(getApplicationContext(), CreateNotification.class);
                event.putExtra("EVENT_DOC_ID", documentId);
                startActivity(event);
            }
        });

        // Listener for the View Event Signups layout
        viewEventSignups.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                Intent intent = new Intent(getApplicationContext(), AttendeeList.class);
                intent.putExtra("EVENT_DOC_ID", documentId);
                intent.putExtra("FIELD_NAME", "signupEvents");
                startActivity(intent);
            }
        });

        // Listener for the View Event Participants layout (checked-in attendees)
        viewEventParticipants.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                Intent intent = new Intent(getApplicationContext(), AttendeeList.class);
                intent.putExtra("EVENT_DOC_ID", documentId);
                intent.putExtra("FIELD_NAME", "attendedEvents");
                startActivity(intent);
            }
        });

        // Listener for the View Event Participants layout (checked-in attendees)
        deleteEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                showDeleteConfirmationDialog();
            }
        });

        dialog.show();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        dialog.getWindow().setGravity(Gravity.BOTTOM);
    }

    /**
     * Checks for milestone achievements for signups
     */
    public void checkSignUpMilestone(){
        if (event != null) {
            HashMap<String, Integer> milestones = event.getSignupMilestone();
            int currentAttendeeSize = event.getSignups().size();


            // Check if the current attendee size matches any milestone
            for (String milestone : milestones.keySet()) {
                int milestoneValue = Integer.parseInt(milestone);
                if (currentAttendeeSize >= milestoneValue && milestones.get(milestone) == 0) {

                    // Update the milestone status to indicate it has been achieved
                    milestones.put(milestone, 1);
                    event.setSignupMilestone(milestones);

                    DocumentReference eventDocRef = eventDb.getDocRef();

                    // Update the document with the new event data
                    eventDocRef.set(event)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    // Document updated successfully
                                    Log.d(TAG, "Event document updated successfully");
                                    // Only show the Snackbar after successfully updating the document
                                    runOnUiThread(() -> {
                                        // Display a Snackbar message indicating the milestone reached
                                        String message = "Congratulations, milestone achieved: " + milestoneValue + " sign-ups!";
                                        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_INDEFINITE);
                                        snackbar.setAction("DISMISS", new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                snackbar.dismiss();
                                            }
                                        });
                                        snackbar.show();
                                    });
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    // Failed to update document
                                    Log.w(TAG, "Error updating event document", e);
                                }
                            });
                }
            }
        }
    }

    /**
     *Checks for milestone achievements for attends
     */
    public void checkAttendeeMilestone(){
        if (event != null) {
            HashMap<String, Integer> milestones = event.getAttendeeMilestone();
            int currentAttendeeSize = event.getAttendee().size();

            // Skip the milestone check if the only attendee is the organizer
            if (currentAttendeeSize == 1 && event.getOrganizer().equals(event.getAttendee().get(0))) {
                return; // Exit the method early
            }

            // Check if the current attendee size matches any milestone
            for (String milestone : milestones.keySet()) {
                int milestoneValue = Integer.parseInt(milestone);
                if (currentAttendeeSize >= milestoneValue && milestones.get(milestone) == 0) {

                    // Update the milestone status to indicate it has been achieved
                    milestones.put(milestone, 1);
                    event.setAttendeeMilestone(milestones);

                    DocumentReference eventDocRef = eventDb.getDocRef();

                    // Update the document with the new event data
                    eventDocRef.set(event)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    // Document updated successfully
                                    Log.d(TAG, "Event document updated successfully");
                                    // Only show the Snackbar after successfully updating the document
                                    runOnUiThread(() -> {
                                        // Display a Snackbar message indicating the milestone reached
                                        String message = "Congratulations, milestone achieved: " + milestoneValue + " attendees!";
                                        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_INDEFINITE);
                                        snackbar.setAction("DISMISS", new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                snackbar.dismiss();
                                            }
                                        });
                                        snackbar.show();
                                    });
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    // Failed to update document
                                    Log.w(TAG, "Error updating event document", e);
                                }
                            });
                }
            }
        }
    }

    /**
     * Starts activity to share a QR code
     * @param qrCode QRCode to be shared
     */
    public void shareQRCode(QRCode qrCode, String headerText){
        if(qrCode != null){
            // Pass QR code and text, start new activity
            Intent activity = new Intent(getApplicationContext(), QrCodeImageView.class);
            activity.putExtra("QRCode", qrCode);
            activity.putExtra("headerText", headerText);
            activity.putExtra("EventName", eventName);
            activity.putExtra("EventDate", eventDate);
            startActivity(activity);
        }
    }

    // openai, 2024, chatgpt: how to create an alert dialog for delete
    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure you want to delete this event?");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // User clicked the Delete
                // Alert those who have signed up for the event
                List<String> signups = (List<String>) event.getSignups();
                MyNotificationManager firebaseMessaging = new MyNotificationManager(getApplicationContext());
                JSONArray regArray = new JSONArray(signups);
                firebaseMessaging.sendMessageToClient(regArray, "Event Shutdown: "+ event.getEventName(), "An event you have signed up for has been shut down", "");
                admin.deleteEvent(documentId);
                Intent event = new Intent(getApplicationContext(), AdminViewEvent.class);
                startActivity(event);
                finish();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // User clicked the Cancel button, so dismiss the dialog
                dialogInterface.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Delete event from firebase
     */
    private void deleteEvent() {
        // Delete the document
        // openai, 2024, chatgpt: how to delete from doc
        eventDb.getDocRef().delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "DocumentSnapshot successfully deleted!");
                        Toast.makeText(getApplicationContext(),"Event Deleted",Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error deleting document", e);
                        // Handle any errors, such as displaying an error message to the user
                    }
                });
        finish();
    }

}