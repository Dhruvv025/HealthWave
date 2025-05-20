package com.works.muhtas2.doctor

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.works.muhtas2.R
import com.works.muhtas2.doctor.models.DoctorData

class DoctorProfileEditActivity : AppCompatActivity() {
    lateinit var edtDName: EditText
    lateinit var edtDSurname: EditText
    lateinit var edtDAge: EditText
    lateinit var edtOldPassword: EditText
    lateinit var edtNewPassword: EditText
    lateinit var spinnerField: Spinner
    lateinit var btnSaveChanges: Button
    lateinit var imgDoctorProfile: ImageView

    lateinit var downloadUri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_profile_edit)

        edtDName = findViewById(R.id.editDName)
        edtDSurname = findViewById(R.id.editDSurname)
        edtDAge = findViewById(R.id.editDAge)
        edtOldPassword = findViewById(R.id.editOldPassword)
        edtNewPassword = findViewById(R.id.editNewPassword)
        spinnerField = findViewById(R.id.spinnerField)
        btnSaveChanges = findViewById(R.id.btnSaveChanges)
        imgDoctorProfile = findViewById(R.id.imgDoctorProfilePicture)

        val db = FirebaseFirestore.getInstance()
        val user = FirebaseAuth.getInstance().currentUser

        // Retrieve data from Firestore and assign it to EditText fields
        db.collection("doctors")
            .document(user?.email!!)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val doctorData = document.toObject(DoctorData::class.java)
                    // Assign the data to EditText fields.
                    edtDName.setText(doctorData?.first)
                    edtDSurname.setText(doctorData?.last)
                    edtDAge.setText(doctorData?.age)
                    // Set up the spinner for the field of expertise.
                    val specialties = resources.getStringArray(R.array.doctor_specialties)
                    val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, specialties)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerField.adapter = adapter
                    val selectedIndex = specialties.indexOf(doctorData?.field)
                    spinnerField.setSelection(selectedIndex)
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error getting doctor data: ${e.message}", e)
            }

        btnSaveChanges.setOnClickListener {
            if (edtDName.text.isNotEmpty() &&
                edtDSurname.text.isNotEmpty() &&
                edtDAge.text.isNotEmpty() &&
                edtOldPassword.text.isNotEmpty() &&
                edtNewPassword.text.isNotEmpty()
            ) {
                // Let's get user confirmation with AlertDialog.
                AlertDialog.Builder(this).apply {
                    setTitle("Confirm")
                    setMessage("Do you want to update?")
                    setPositiveButton("Yes") { _, _ ->
                        val oldPassword = edtOldPassword.text.toString()
                        val newPassword = edtNewPassword.text.toString()
                        val name = edtDName.text.toString()
                        val surname = edtDSurname.text.toString()
                        val age = edtDAge.text.toString()
                        val field = spinnerField.selectedItem.toString()

                        // Check the old password and proceed with the update.
                        verifyAndUpdate(
                            oldPassword,
                            newPassword,
                            name,
                            surname,
                            age,
                            field,
                            user?.email!!,
                            downloadUri.toString()
                        )
                        // Start the intent with a specific delay duration.
                        Handler().postDelayed({
                            val intent = Intent(
                                this@DoctorProfileEditActivity,
                                DoctorProfileActivity::class.java
                            )
                            startActivity(intent)
                            finish()
                        }, 2000) // "Delay the intent by 2 seconds."
                    }
                    setNegativeButton("No", null)
                }.create().show()
            } else {
                Toast.makeText(this, "Please fill in all the information completely.", Toast.LENGTH_LONG)
                    .show()
            }
        }

        // Request access to the gallery when the image is clicked."
        imgDoctorProfile.setOnClickListener {
            openGallery()
        }
    }

    // Permission code to request access to the gallery from the user.
    private val READ_EXTERNAL_STORAGE_PERMISSION = 123
    private val PICK_IMAGE_REQUEST = 123
    // Check the onRequestPermissionsResult method for the results of the requested permissions."
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == READ_EXTERNAL_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, access to the gallery can be provided.
                openGallery()
            } else {
                // "Permission denied, access to the gallery cannot be provided."
                Toast.makeText(this, "Access to the gallery denied.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Called when access to the gallery is granted.
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    // Check the onActivityResult method for the gallery selection result.
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            val selectedImageUri = data.data

            // Load the selected image into an ImageView using Glide.
            Glide.with(this).load(selectedImageUri).into(imgDoctorProfile)

            // Save the selected image to Firebase Storage.
            val user = FirebaseAuth.getInstance().currentUser
            val storageRef = FirebaseStorage.getInstance().reference
            val imageRef = storageRef.child("users/${user?.email}/profile.jpg")

            val uploadTask = imageRef.putFile(selectedImageUri!!)
            uploadTask.continueWithTask { task ->
                if (!task.isSuccessful) {
                    throw task.exception!!
                }
                // "Get the downloadable URL of the image.
                imageRef.downloadUrl
            }.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    downloadUri = task.result
                    // "Actions to take when the downloadable URL of the image is obtained."
                    // "For example, saving it to Firestore."
                    // "You can use downloadUri.toString() to get the URL."
                } else {
                    // "Error handling for cases where the image fails to load."
                }
            }
        }
    }

    private fun verifyAndUpdate(
        oldPassword: String,
        newPassword: String,
        first: String,
        last: String,
        age: String,
        field: String,
        email: String,
        image: String?
    ) {
        val user = FirebaseAuth.getInstance().currentUser

        val credential = EmailAuthProvider.getCredential(user?.email ?: "", oldPassword)
        user?.reauthenticate(credential)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                user.updatePassword(newPassword).addOnCompleteListener { updateTask ->
                    if (updateTask.isSuccessful) {
                        // Now update the other information in Firestore.
                        updateDoctorInFirestore(
                            user.uid,
                            first,
                            last,
                            age,
                            field,
                            email,
                            newPassword,
                            image
                        )
                    } else {
                        Toast.makeText(
                            this,
                            "Password could not be updated: ${updateTask.exception?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                Toast.makeText(
                    this,
                    "Old password is incorrect: ${task.exception?.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun updateDoctorInFirestore(
        userId: String,
        first: String,
        last: String,
        age: String,
        field: String,
        email: String,
        password: String,
        image: String?
    ) {
        val db = FirebaseFirestore.getInstance()

        val doctorDataInfo = DoctorData(
            UID = userId,
            first = first,
            last = last,
            age = age,
            field = field,
            email = email,
            password = password,
            image = image
        )

        db.collection("doctors")
            .document(email)
            .set(doctorDataInfo)
            .addOnSuccessListener {
                Toast.makeText(this, "Information updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "An error occurred while updating information: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}
