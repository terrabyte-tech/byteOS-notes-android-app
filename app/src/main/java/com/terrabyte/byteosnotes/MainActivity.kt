package com.terrabyte.byteosnotes

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import android.Manifest
import android.content.pm.PackageManager
import android.os.Environment
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException

class MainActivity : AppCompatActivity() {
  private lateinit var note_txtarea: EditText
  private lateinit var load_button: Button
  private lateinit var save_button: Button

  companion object {
    private const val REQUEST_CODE = 100
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContentView(R.layout.activity_main)

    note_txtarea = findViewById(R.id.note_txtarea)
    load_button = findViewById(R.id.load_button)
    save_button = findViewById(R.id.save_button)

    save_button.setOnClickListener{
//      refactor to take user input for note filename
      val fileName = "note.txt"
      val fileContent = note_txtarea.text.toString();

      writeToFile(fileName, fileContent)
    }

    load_button.setOnClickListener{
      println("clicked load button")
    }

    ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
      val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
      insets
    }
  }

//  writing functions
  private fun isExternalStorageWritable(): Boolean {
//    if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
//      println("external storage is writeable")
//    }
    return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
  }

  private fun requestStoragePermissions() {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//      println("storage permissions granted")
      ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CODE)
    }
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode == REQUEST_CODE) {
      if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
//        println("Android permissions granted")
      } else {
//        println("Android permissions denied")
      }
    }
  }

  private fun writeToFile(fileName: String, fileContent: String) {
    if (isExternalStorageWritable()) {
//      check if Documents directory exists for notes
      val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
      if (!documentsDir.exists()) {
        documentsDir.mkdirs()
        println("made /documents directory")
      }

//      check if file exists
      val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), fileName)
      try {
        println("entered try block")
        if (!file.exists()) {
          println("file doesn't exist, creating...")
//          file.createNewFile()
          val created = file.createNewFile()
          println("File created: $created")
        }

        val fileOutputStream = FileOutputStream(file)
        fileOutputStream.write(fileContent.toByteArray())
        fileOutputStream.close()
        createToast(fileName + " saved")
      } catch (e: IOException) {
        println("**EXCEPTION**")
        println(e)
        createToast("Failed to save")
      }
    } else {
      createToast("Insufficient storage")
    }
  }


  //making toast messages
  fun Context.createToast(text: CharSequence, duration: Int = Toast.LENGTH_SHORT){
    Toast.makeText(this, text, duration).show()
    println(text)
  }
}