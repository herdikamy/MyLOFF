package com.example.poc.ui.gallery

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.poc.R
import com.example.poc.databinding.FragmentGalleryBinding
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.IMarker
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.DataSet
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GalleryFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null
    private lateinit var lineChart: LineChart
    private lateinit var lineChart2: LineChart

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    @SuppressLint("SuspiciousIndentation")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val galleryViewModel =
            ViewModelProvider(this).get(GalleryViewModel::class.java)

        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Inisialisasi LineChart dari layout
        lineChart = binding.lineChart
        lineChart2 = binding.lineChart2

        // Inisialisasi firestore
        val db = FirebaseFirestore.getInstance()

        // get data from firestore
        val collectionRef = db.collection("data_sensor")
            .orderBy("tanggal_waktu", Query.Direction.DESCENDING)
            .limit(10)

        collectionRef.get().addOnSuccessListener { documents ->
            val oksigenData = ArrayList<Entry>()
            val pHData = ArrayList<Entry>()
            val dateLabels = ArrayList<String>()
            val statusValveData = ArrayList<Entry>()

            for (document in documents) {
                val date = (document["tanggal_waktu"] as Timestamp)
                val oksigenValue = (document["kadar_oksigen"] as? Double)?.toFloat() ?: 0.0f
                val pHValue = (document["kandungan_ph"] as? Double)?.toFloat()?: 0.0f
                val statusValve = (document["status_valve"] as? String) ?: ""

                if (oksigenValue != 0.0f && pHValue != 0.0f && date != null) {
                    oksigenData.add(Entry(dateLabels.size.toFloat(), oksigenValue))
                    pHData.add(Entry(dateLabels.size.toFloat(), pHValue))
                    statusValveData.add(Entry(dateLabels.size.toFloat(), getStatusValveValue(statusValve)))
                    dateLabels.add(getFormattedDate(date.toDate()))
                } else {
                    // Logging untuk melihat nilai yang tidak valid
                    Log.e("DataError", "Invalid data - date: $date, oksigen: $oksigenValue, pH: $pHValue")
                }
            }

            val oksigenDataSet = LineDataSet(oksigenData, "Oksigen")
            oksigenDataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
            oksigenDataSet.color = Color.BLUE
            oksigenDataSet.circleRadius = 5f
            oksigenDataSet.setCircleColor(Color.BLUE)

            val pHDataSet = LineDataSet(pHData, "pH")
            pHDataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
            pHDataSet.color = Color.RED
            pHDataSet.circleRadius = 5f
            pHDataSet.setCircleColor(Color.RED)

            val statusValveDataSet = LineDataSet(statusValveData, "Status Valve")
            statusValveDataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
            statusValveDataSet.color = Color.GREEN
            statusValveDataSet.circleRadius = 5f
            statusValveDataSet.setCircleColor(Color.GREEN)

            //Setup Legend
            val legend = lineChart.legend
            legend.isEnabled = true
            legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP)
            legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER)
            legend.setOrientation(Legend.LegendOrientation.HORIZONTAL)
            legend.setDrawInside(false)

            lineChart.description.isEnabled = false
            lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
//            lineChart.data = LineData(oksigenDataSet, pHDataSet)
            // Reverse urutan data setelah menambahkannya ke dalam LineData
            val lineData = LineData(oksigenDataSet, pHDataSet)
//            lineData.dataSets.forEach { dataSet ->
//                (dataSet as LineDataSet).values.reverse()
//            }
            lineChart.data = lineData
            lineChart.animateXY(100, 500)

            // Setelah menginisialisasi lineChart, terapkan formatter pada sumbu X
            val xAxis = lineChart.xAxis
            xAxis.valueFormatter = XAxisDateFormatter(dateLabels)

            val marker: IMarker = LineChartMarkerView(requireContext(), lineChart, R.layout.markerview_three_item, XAxisDateFormatter(dateLabels))
            lineChart.marker = marker

            // Atur sumbu X agar sesuai dengan urutan tanggal yang baru
            xAxis.axisMaximum = dateLabels.size.toFloat() - 0.5f
            xAxis.axisMinimum = 0f


            //linechart2
            //Setup Legend
            val legend2 = lineChart2.legend
            legend2.isEnabled = true
            legend2.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP)
            legend2.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER)
            legend2.setOrientation(Legend.LegendOrientation.HORIZONTAL)
            legend2.setDrawInside(false)

            lineChart2.description.isEnabled = false
            lineChart2.xAxis.position = XAxis.XAxisPosition.BOTTOM
//            lineChart.data = LineData(oksigenDataSet, pHDataSet)
            // Reverse urutan data setelah menambahkannya ke dalam LineData
            val lineData2 = LineData(pHDataSet, statusValveDataSet)
//            lineData.dataSets.forEach { dataSet ->
//                (dataSet as LineDataSet).values.reverse()
//            }
            lineChart2.data = lineData2
            lineChart2.animateXY(100, 500)

            // Setelah menginisialisasi lineChart, terapkan formatter pada sumbu X
            val xAxis2 = lineChart2.xAxis
            xAxis2.valueFormatter = XAxisDateFormatter(dateLabels)

            val marker2: IMarker = LineChartMarkerView2(requireContext(), lineChart2, R.layout.markerview_three_item, XAxisDateFormatter(dateLabels))
            lineChart2.marker = marker2

            // Atur sumbu X agar sesuai dengan urutan tanggal yang baru
            xAxis.axisMaximum = dateLabels.size.toFloat() - 0.5f
            xAxis.axisMinimum = 0f
        }

        val textView: TextView = binding.textGallery
        galleryViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class LineChartMarkerView(
    context: Context?,
    private val lineChart: LineChart,
    layoutResource: Int,
    axisX: XAxisDateFormatter
) : MarkerView(context, layoutResource), IMarker {

    private val square1: TextView
    private val square2: TextView
    private val item1: TextView
    private val item2: TextView

    private val Title: TextView
    private val XAxis: XAxisDateFormatter

    init {
        square1 = findViewById(R.id.square1)
        square2 = findViewById(R.id.square2)
        item1 = findViewById(R.id.item1)
        item2 = findViewById(R.id.item2)
        Title = findViewById(R.id.txtTitle)
        XAxis = axisX
    }

    override fun refreshContent(e: Entry, highlight: Highlight) {
        try {
            Title.text = XAxis.getFormattedValue(e.x).toString()
            square1.setBackgroundColor(lineChart.data.getDataSetByIndex(0).color)
            square2.setBackgroundColor(lineChart.data.getDataSetByIndex(1).color)
            val val1 =
                lineChart.data.getDataSetByIndex(0)
                    .getEntryForXValue(e.x, Float.NaN, DataSet.Rounding.CLOSEST) as Entry
            val val2 =
                lineChart.data.getDataSetByIndex(1)
                    .getEntryForXValue(e.x, Float.NaN, DataSet.Rounding.CLOSEST) as Entry
//            val val3 =
//                lineChart.data.getDataSetByIndex(2)
//                    .getEntryForXValue(e.x, Float.NaN, DataSet.Rounding.CLOSEST) as Entry
            item1.text = String.format("%,.1f mg/L", val1.y)
            item2.text = String.format("%,.1f", val2.y)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        super.refreshContent(e, highlight)
    }

    private var mOffset: MPPointF? = null

    override fun getOffset(): MPPointF {
        if (mOffset == null) {
            mOffset = MPPointF((-(width / 2)).toFloat(), (-height).toFloat())
        }
        return mOffset!!
    }
}

class LineChartMarkerView2(
    context: Context?,
    private val lineChart2: LineChart,
    layoutResource: Int,
    axisX: XAxisDateFormatter
) : MarkerView(context, layoutResource), IMarker {

    private val square1: TextView
    private val square2: TextView
    private val item1: TextView
    private val item2: TextView

    private val Title: TextView
    private val XAxis: XAxisDateFormatter

    init {
        square1 = findViewById(R.id.square1)
        square2 = findViewById(R.id.square2)
        item1 = findViewById(R.id.item1)
        item2 = findViewById(R.id.item2)
        Title = findViewById(R.id.txtTitle)
        XAxis = axisX
    }

    override fun refreshContent(e: Entry, highlight: Highlight) {
        try {
            Title.text = XAxis.getFormattedValue(e.x).toString()
            square1.setBackgroundColor(lineChart2.data.getDataSetByIndex(0).color)
            square2.setBackgroundColor(lineChart2.data.getDataSetByIndex(1).color)
            val val1 =
                lineChart2.data.getDataSetByIndex(0)
                    .getEntryForXValue(e.x, Float.NaN, DataSet.Rounding.CLOSEST) as Entry
            val val2 =
                lineChart2.data.getDataSetByIndex(1)
                    .getEntryForXValue(e.x, Float.NaN, DataSet.Rounding.CLOSEST) as Entry
//            val val3 =
//                lineChart.data.getDataSetByIndex(2)
//                    .getEntryForXValue(e.x, Float.NaN, DataSet.Rounding.CLOSEST) as Entry
            item1.text = String.format("%,.1f", val1.y)
//            item2.text = String.format("%,.1f", val2.y)
            item2.text = if (val2.y == 0.0f) {
                "OFF"
            } else if (val2.y == 1.0f) {
                "ON"
            } else {
                String.format("%,.1f", val2.y)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        super.refreshContent(e, highlight)
    }

    private var mOffset: MPPointF? = null

    override fun getOffset(): MPPointF {
        if (mOffset == null) {
            mOffset = MPPointF((-(width / 2)).toFloat(), (-height).toFloat())
        }
        return mOffset!!
    }
}

//class XAxisDateFormatter(private val mValues: ArrayList<String>) :  ValueFormatter() {
//    override fun getFormattedValue(value: Float): String {
//        return if (value >= 0) {
//            if (mValues.size > value.toInt()) {
//                mValues[value.toInt()]
//            } else ""
//        } else {
//            ""
//        }
//    }
//}

class XAxisDateFormatter(private val dates: List<String>) : ValueFormatter() {
    override fun getFormattedValue(value: Float): String {
        val index = value.toInt()
        return if (index >= 0 && index < dates.size) {
            dates[index]
        } else {
            ""
        }
    }
}

private fun getFormattedDate(date: Date): String {
    val simpleDateFormat = SimpleDateFormat("dd-MMM", Locale.getDefault())
    return simpleDateFormat.format(date)
}

private fun getStatusValveValue(statusValve: String): Float {
    return when (statusValve) {
        "HIGH", "ON" -> 1.0f
        "LOW", "OFF" -> 0.0f
        else -> throw IllegalArgumentException("Status valve tidak valid: $statusValve")
    }
}

private fun getStatusValveValue2(statusValve: String): String {
    return when (statusValve) {
        "HIGH", "ON" -> "0.0"
        "LOW", "OFF" -> "0.0"
        else -> throw IllegalArgumentException("Status valve tidak valid: $statusValve")
    }
}