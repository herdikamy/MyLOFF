package com.example.poc.ui.home

import android.content.ContentValues
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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

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

        setupDOValue()
        setupPHValue()
        setupStatValve()

        val textView: TextView = binding.textHome
        homeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        return root
    }

    private fun setupPHValue() {
        val pHvalue = binding.valuepH

        // Ambil nilai dari Firebase untuk pHmeter
        val pHMeterValueListener = createValueEventListener(pHvalue)

        // Tambahkan listener ke Firebase
        databaseReference.child("PEMBACAAN_SENSOR").child("KANDUNGAN_PH").addValueEventListener(pHMeterValueListener)
    }

    private fun setupDOValue() {
        val DOvalue = binding.valueDO

        // Ambil nilai dari Firebase untuk DOmeter
        val doValueListener = createValueEventListener(DOvalue)

        // Tambahkan listener ke Firebase
        databaseReference.child("PEMBACAAN_SENSOR").child("KADAR_OKSIGEN").addValueEventListener(doValueListener)

    }

    private fun setupStatValve(){
        val statValveEditText = binding.statusValve

        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("PEMBACAAN_SENSOR/STATUS_VALVE")

        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val value = dataSnapshot.getValue(String::class.java)

                statValveEditText.text = when (value?.uppercase()) {
                    "HIGH" -> Editable.Factory.getInstance().newEditable("ON")
                    "LOW" -> Editable.Factory.getInstance().newEditable("OFF")
                    else -> Editable.Factory.getInstance().newEditable("-")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(ContentValues.TAG, "Failed to read value.", error.toException())
            }
        })
    }

    private fun createValueEventListener(editText: TextView): ValueEventListener {
        return object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val value = snapshot.getValue(String::class.java) ?: "0.0"
                editText.setText(value)
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