package com.works.muhtas2.doctor

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.works.muhtas2.MainActivity
//import com.works.muhtas2.NewsActivity
import com.works.muhtas2.R
import com.works.muhtas2.doctor.adapter.DoctorAppointmentAdapter
import com.works.muhtas2.doctor.services.DoctorAppointmentService

class DoctorHomepageActivity : AppCompatActivity() {
    lateinit var appointmentsList: ListView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_homepage)

        appointmentsList = findViewById(R.id.appointmentsList)

        val doctorAppointmentService = DoctorAppointmentService()
        val doctorEmail =
            FirebaseAuth.getInstance().currentUser?.email // Enter doctor email address here

        // We record doctor's appointments
        doctorAppointmentService.getAppointmentsForDoctor(doctorEmail!!) { appointments ->
            Log.d("appointments", appointments.toString())
            val adapter = DoctorAppointmentAdapter(this, appointments)
            appointmentsList.adapter = adapter

            appointmentsList.setOnItemLongClickListener { _, _, position, _ ->
                val selectedAppointment = appointments[position]

                AlertDialog.Builder(this)
                    .setTitle("We are retrieving appointments belonging to the doctor.")
                    .setMessage("Are you sure you want to cancel this appointment?")
                    .setPositiveButton("Yes") { _, _ ->
                        // Delete the appointment from both the doctor and patient collections
                        doctorAppointmentService.deleteAppointment(
                            doctorEmail,
                            selectedAppointment.patientEmail!!,
                            selectedAppointment.id!!
                        ) { success ->
                            if (success) {
                                Toast.makeText(
                                    this,
                                    "The appointment has been successfully deleted.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                // Redo appointments to update the list
                                doctorAppointmentService.getAppointmentsForDoctor(doctorEmail) { updatedAppointments ->
                                    adapter.clear()
                                    adapter.addAll(updatedAppointments)
                                    adapter.notifyDataSetChanged()
                                }
                            } else {
                                Toast.makeText(
                                    this,
                                    "An error occurred while deleting the appointment.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                    .setNegativeButton("No", null)
                    .show()

                true
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.doctor_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.doctor_profile -> {
                var intent = Intent(this, DoctorProfileActivity::class.java)
                startActivity(intent)
            }

            /*R.id.doctor_news -> {
                var intent = Intent(this, NewsActivity::class.java)
                startActivity(intent)
            }*/
            R.id.doctor_logout -> {
                AlertDialog.Builder(this).apply {
                    setTitle("Log out")
                    setMessage("Are you sure you want to log out?")
                    setPositiveButton("Yes") { _, _ ->
                        FirebaseAuth.getInstance().signOut()
                        val intent = Intent(applicationContext, MainActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        finish()
                    }
                    setNegativeButton("No", null)

                }.create().show()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}