package com.example.poc.ui.slideshow

import android.content.ContentValues.TAG
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.ekn.gruzer.gaugelibrary.ArcGauge
import com.ekn.gruzer.gaugelibrary.contract.ValueFormatter
import com.example.poc.databinding.FragmentSlideshowBinding
import com.example.poc.ui.slideshow.SlideshowViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

// SlideShowFragment.kt
// ... imports

class SlideshowFragment : Fragment() {

    private var _binding: FragmentSlideshowBinding? = null
    private val binding get() = _binding!!

    // Firebase
    private lateinit var databaseReference: DatabaseReference

    private val slideshowViewModel by lazy {
        ViewModelProvider(this).get(SlideshowViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSlideshowBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Konfigurasi Firebase
        databaseReference = FirebaseDatabase.getInstance().reference

        setupDOValue()
        setupPHValue()
        setupStatValve()

        val textView: TextView = binding.textSlideshow
        slideshowViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        return root
    }

    private fun setupDOValue() {
        val dovalue = binding.dovalue

        // Ambil nilai dari Firebase untuk DOmeter
        val doValueListener = createValueEventListener(dovalue)

        // Tambahkan listener ke Firebase
        databaseReference.child("PEMBACAAN_SENSOR").child("KADAR_OKSIGEN").addValueEventListener(doValueListener)

    }

    private fun setupPHValue() {
        val valuepH = binding.valuepH

        // Ambil nilai dari Firebase untuk pHmeter
        val pHMeterValueListener = createValueEventListener(valuepH)

        // Tambahkan listener ke Firebase
        databaseReference.child("PEMBACAAN_SENSOR").child("KANDUNGAN_PH").addValueEventListener(pHMeterValueListener)
    }

    private fun setupStatValve(){
        val statValveEditText = binding.textvalve

        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("PEMBACAAN_SENSOR/STATUS_VALVE")

        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val value = dataSnapshot.getValue(String::class.java)

                statValveEditText.text = when (value?.toUpperCase()) {
                    "HIGH" -> Editable.Factory.getInstance().newEditable("TERBUKA")
                    "LOW" -> Editable.Factory.getInstance().newEditable("TERTUTUP")
                    else -> Editable.Factory.getInstance().newEditable("No data")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Failed to read value.", error.toException())
            }
        })
    }

    private fun createValueEventListener(editText: EditText): ValueEventListener {
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