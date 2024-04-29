package com.example.poc.ui.home

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.poc.R
import com.example.poc.databinding.FragmentHomeBinding
import com.example.poc.ui.slideshow.SlideshowFragment
import com.google.firebase.Timestamp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private lateinit var firestore: FirebaseFirestore
    val dataMap = HashMap<String, Any>()

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    // Firebase
    private lateinit var databaseReference: DatabaseReference

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
                ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Konfigurasi Firebase
        databaseReference = FirebaseDatabase.getInstance().reference

        // Inisialisasi Firestore
        firestore = FirebaseFirestore.getInstance()


        setupDayValue()
        setupDOValue()
        setupPHValue()
        setupStatValve()

        val textView: TextView = binding.textHome
        homeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        // Panggil fungsi openWebPage ketika sebuah CardView diklik
        val cardView: CardView = binding.datacard
        cardView.setOnClickListener {
            openWebPage()
        }

        return root
    }

    private fun setupDayValue() {
        val DayValue = binding.valueHari

        val db = FirebaseFirestore.getInstance()
        val collectionRef = db.collection("data_sensor")

        collectionRef.get()
            .addOnSuccessListener { documents ->
                val count = documents.size()
                DayValue.text = "$count"
                println("Jumlah dokumen dalam koleksi: $count")
            }
            .addOnFailureListener { e ->
                println("Error: $e")
            }
    }

    fun openWebPage() {
        val url = "https://docs.google.com/spreadsheets/d/14tulceV5h6CesJUTKJLt-KU65eyz5oChzTv1G-RzugU/edit?usp=sharing" // Ganti URL dengan URL yang sesuai
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(url)
        startActivity(intent)
    }


    private fun setupPHValue() {
        val pHvalue = binding.valuepH

        // Ambil nilai dari Firebase untuk pHmeter
        val pHMeterValueListener = createValueEventListenerpH(pHvalue)

//        dataMap["kandungan_ph"] = pHMeterValueListener

        // Tambahkan listener ke Firebase
        databaseReference.child("PEMBACAAN_SENSOR").child("KANDUNGAN_PH").addValueEventListener(pHMeterValueListener)
    }

    private fun setupDOValue() {
        val DOvalue = binding.valueDO

        // Ambil nilai dari Firebase untuk DOmeter
        val doValueListener = createValueEventListenerDO(DOvalue)

//        dataMap["kadar_oksigen"] = doValueListener

        // Tambahkan listener ke Firebase
        databaseReference.child("PEMBACAAN_SENSOR").child("KADAR_OKSIGEN").addValueEventListener(doValueListener)

    }

    private fun setupStatValve(){
        val statValveEditText = binding.statusValve

        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("PEMBACAAN_SENSOR/STATUS_VALVE")

        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val value = dataSnapshot.getValue(String::class.java) ?: "-"

                // Tambahkan tanggal dan waktu saat ini ke dalam dataMap
                val currentDateTime = Calendar.getInstance().time
                val timestamp = Timestamp(currentDateTime)
                dataMap["tanggal_waktu"] = timestamp
                dataMap["status_valve"] = value

                statValveEditText.text = when (value?.uppercase()) {
                    "HIGH" -> Editable.Factory.getInstance().newEditable("ON")
                    "LOW" -> Editable.Factory.getInstance().newEditable("OFF")
                    else -> Editable.Factory.getInstance().newEditable(value)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(ContentValues.TAG, "Failed to read value.", error.toException())
            }
        })
    }

    private fun createValueEventListenerpH(editText: TextView): ValueEventListener {
        return object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val pH = snapshot.getValue(Double::class.java) ?: "0.0"
                editText.setText(pH.toString())

                dataMap["kandungan_ph"] = pH
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        }
    }

    private fun createValueEventListenerDO(editText: TextView): ValueEventListener {
        return object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val DO = snapshot.getValue(Double::class.java) ?: "0.0"
                editText.setText(DO.toString())

                dataMap["kadar_oksigen"] = DO

                // Inisialisasi firestore
                val db = FirebaseFirestore.getInstance()

                // Mendapatkan tanggal saat ini
                val currentDate = Calendar.getInstance()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val dateString = dateFormat.format(currentDate.time)

                // Membuat query untuk mendapatkan data_sensor pada tanggal saat ini
                val collectionRef = db.collection("data_sensor")
                    .whereGreaterThanOrEqualTo("tanggal_waktu", Timestamp(dateFormat.parse(dateString)))
                    .whereLessThan("tanggal_waktu", Timestamp(Date(currentDate.timeInMillis + (24 * 60 * 60 * 1000)))) // Menambahkan 24 jam untuk mendapatkan hari berikutnya
                    .orderBy("tanggal_waktu", Query.Direction.DESCENDING)
                    .limit(10)

                collectionRef.get()
                    .addOnSuccessListener { documents ->
                        val dayValue = documents.size()
                        if (documents.isEmpty) {
                            // Jika tidak ada data yang ditemukan untuk tanggal saat ini
                            Log.d("TAG", "Data hari ini belum tersedia")

                            // tambahkan data ke firestore

                            firestore.collection("data_sensor").document()
                            .set(dataMap)
                            .addOnSuccessListener {
                                // Handle jika data berhasil ditambahkan ke Cloud Firestore
                                Log.d("MSG","Data hari ini berhasil ditambahkan ke Firestore")
                            }
                            .addOnFailureListener { e ->
                                // Handle jika terjadi kesalahan saat menambahkan data ke Cloud Firestore
                                Log.d("MSG","Error adding document: $e")
                            }
                        } else {
                            // Jika ada data yang ditemukan untuk tanggal saat ini
                            Log.d("TAG", "Data hari ini sudah ditambahkan")
                            // Lakukan sesuatu dengan data yang ditemukan
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("TAG", "Error getting documents: ", e)
                    }


            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}