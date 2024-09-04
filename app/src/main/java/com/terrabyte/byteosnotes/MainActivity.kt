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
import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.os.Environment
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import android.util.Log
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

class MainActivity : AppCompatActivity() {
  private lateinit var note_txtarea: EditText
  private lateinit var load_button: Button
  private lateinit var new_button: Button
  private lateinit var save_button: Button
  private lateinit var filename_label: TextView
  private var defaultFilename = "note.txt"
  private var givenFilename = defaultFilename
  private var filenameSet = false

  companion object {
    private const val REQUEST_CODE = 100
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContentView(R.layout.activity_main)

    note_txtarea = findViewById(R.id.note_txtarea)
    filename_label = findViewById(R.id.filename_label)
    load_button = findViewById(R.id.load_button)
    save_button = findViewById(R.id.save_button)
    new_button = findViewById(R.id.new_button)

    save_button.setOnClickListener{
      if (filenameSet != true){
        fileNameDialog()
      }else{
        val fileContent = note_txtarea.text.toString()
        writeToFile(givenFilename, fileContent)
      }
    }

    load_button.setOnClickListener{
      debugI("clicked load button")
    }

    new_button.setOnClickListener{
      debugI("clicked new note button")
      newNote()
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
//      debugI("external storage is writeable")
//    }
    return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
  }

  private fun requestStoragePermissions() {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
      debugI("storage permissions granted")
      ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CODE)
    }
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode == REQUEST_CODE) {
      if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
        debugI("Android permissions granted")
      } else {
        debugI("Android permissions denied")
      }
    }
  }

  private fun fileNameDialog(){
    var enteredName: String
    val builder = AlertDialog.Builder(this, R.style.AlertDialogCustom)
//    val builder = Dialog.Builder(this, R.style.AlertDialogCustom)
    val inflater = layoutInflater
    val dialogLayout = inflater.inflate(R.layout.filename_dialog_layout, null)
    val filenameText = dialogLayout.findViewById<EditText>(R.id.filename_input)

    with(builder){
      setTitle("Save note as...")
      setPositiveButton("OK"){dialog, which ->
        enteredName = filenameText.text.toString() + ".txt"

        if (enteredName == ".txt"){
          debugI("Nothing entered, default filename is " + defaultFilename)
          enteredName = defaultFilename
        }
        setFileName(enteredName)

        val fileContent = note_txtarea.text.toString()
        writeToFile(enteredName, fileContent)
      }
      setNegativeButton("Cancel"){dialog, which ->
        debugI("Not saved; Canceled save process")
        createToast("Not saved")
      }
      setView(dialogLayout)
      show()
    }
  }
  private fun writeToFile(fileName: String, fileContent: String) {
    if (isExternalStorageWritable()) {
//      check if Documents directory exists for notes
      val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
      if (!documentsDir.exists()) {
        documentsDir.mkdirs()
        debugI("made /documents directory")
      }

//      check if file exists
      val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), fileName)
      try {
        if (!file.exists()) {
          debugI("file doesn't exist, creating...")
          file.createNewFile()
        }

        val fileOutputStream = FileOutputStream(file)
        fileOutputStream.write(fileContent.toByteArray())
        fileOutputStream.close()
        createToast(fileName + " saved to Documents")
      } catch (e: IOException) {
        debugE(e)
        createToast("Failed to save")
      }
    } else {
      createToast("Insufficient storage")
    }
  }

//  set the filename across the app
  private fun setFileName(txt: String){
    debugI("entered filename is " + txt)
    filenameSet = true
    givenFilename = txt
    filename_label.text = txt
  }
//  create new note; reset everything
  private fun newNote(){
    filenameSet = false
    givenFilename = defaultFilename
    filename_label.text = getString(R.string.newFile_label)
    note_txtarea.getText().clear()
  }


  //making toast messages
  private fun Context.createToast(text: CharSequence, duration: Int = Toast.LENGTH_SHORT){
    Toast.makeText(this, text, duration).show()
    debugI(text.toString())
  }
  // making console messages
  private fun debugI(msg: String){
    Log.i("System.out", msg)
  }
  private fun debugE(msg: Exception){
    Log.w("System.err", msg)
  }
}