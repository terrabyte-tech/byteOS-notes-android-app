package com.terrabyte.byteosnotes

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class MainActivity : AppCompatActivity() {
  private lateinit var note_txtarea: EditText
  private lateinit var load_button: Button
  private lateinit var new_button: Button
  private lateinit var save_button: Button
  private lateinit var filename_label: TextView
  private var previousContent = ""
  private var defaultFilename = "note.txt"
  private var givenFilename = defaultFilename
  private var filenameSet = false
  private var fileUri: Uri? = null
  private lateinit var openDocumentLauncher: ActivityResultLauncher<Intent>

  companion object {
    //  permission to access storage
    private const val REQUEST_CODE = 100
    //  permission to open docs
    const val REQUEST_CODE_OPEN_DOCUMENT = 1
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContentView(R.layout.activity_main)

//    app elements
    note_txtarea = findViewById(R.id.note_txtarea)
    filename_label = findViewById(R.id.filename_label)
    load_button = findViewById(R.id.load_button)
    save_button = findViewById(R.id.save_button)
    new_button = findViewById(R.id.new_button)

//    loading note variables
    openDocumentLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      if (result.resultCode == Activity.RESULT_OK) {
        result.data?.data?.also { uri ->
          fileUri = uri
          getLoadedFileName(fileUri!!)

          contentResolver.openInputStream(uri)?.bufferedReader().use { reader ->
            val fileContent = reader?.readText()

//            set note data
            previousContent = fileContent.toString()
            note_txtarea.setText(fileContent.toString())
          }
        }
      }
    }

//    click listeners
    save_button.setOnClickListener{
      debugI("save button clicked")

      if (!filenameSet) {
        fileNameDialog()
      }else{
        var isModified = checkContentModified()

        if(isModified){
          val fileContent = note_txtarea.text.toString()
//          debugI(fileUri.toString())

          //  if URI is available
          if(fileUri != null){
//            debugI("URI is not null")
            fileUri?.let { uri ->
              contentResolver.openOutputStream(uri)?.bufferedWriter().use { writer ->
                writer?.write(fileContent)
                createToast("$givenFilename saved")
                setNoteData(givenFilename)
              }
            }
            //  if note from scratch
          }else{
            writeToFile(givenFilename, fileContent)
          }
        }else{
          createToast("Nothing changed")
        }
      }
    }

    load_button.setOnClickListener{
      debugI("load button clicked")
      var isModified = checkContentModified()

      if(isModified){
        discardingPrompt{loadNote()}
      }else{
        loadNote()
      }
    }

    new_button.setOnClickListener{
      debugI("new button clicked")
      var isModified = checkContentModified()

      if(isModified){
        discardingPrompt{clearNote()}
      }else{
        clearNote()
      }
    }

    ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
      val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
      insets
    }
  }


// /////////////////////////////////////////////////////////////////////////////////////
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
    val inflater = layoutInflater
    val dialogLayout = inflater.inflate(R.layout.filename_dialog_layout, null)
    val filenameText = dialogLayout.findViewById<EditText>(R.id.filename_input)

    with(builder){
      setTitle("Save note as...")
      setPositiveButton("OK"){dialog, which ->
        debugI("clicked OK button")
        enteredName = filenameText.text.toString() + ".txt"

        if (enteredName == ".txt"){
          enteredName = defaultFilename
        }

        setNoteData(enteredName)

        val fileContent = note_txtarea.text.toString()
        writeToFile(enteredName, fileContent)
      }
      setNegativeButton("Cancel"){dialog, which ->
        debugI("Not saved; Canceled save process")
        createToast("Canceled saved")
      }
      setView(dialogLayout)
      show()
    }
  }

  private fun writeToFile(fileName: String, fileContent: String) {
    if (isExternalStorageWritable()) {
//      check if file has been loaded in (go off of URI location)
      //  check if Documents directory exists for notes
      val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
      if (!documentsDir.exists()) {
        documentsDir.mkdirs()
        debugI("made /documents directory")
      }

      //  check if file exists
      val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), fileName)
      try {
        if (!file.exists()) {
          debugI("file doesn't exist, creating...")
          file.createNewFile()
        }

        val fileOutputStream = FileOutputStream(file)
        fileOutputStream.write(fileContent.toByteArray())
        fileOutputStream.close()
        createToast("$fileName saved to Documents")
      } catch (e: IOException) {
        debugE(e)
        createToast("Failed to save")
      }
    } else {
      createToast("Insufficient storage")
    }
  }


// /////////////////////////////////////////////////////////////////////////////////////
//  loading functions
  private fun loadNote(){
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
      addCategory(Intent.CATEGORY_OPENABLE)
      type = "text/plain"
    }
    openDocumentLauncher.launch(intent)
  }

  fun getLoadedFileName(uri: Uri){
    var loadedFileName: String? = null
    val cursor = contentResolver.query(uri, null, null, null, null)
    cursor?.use {
      if (it.moveToFirst()) {
        val displayNameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (displayNameIndex != -1) {
          loadedFileName = it.getString(displayNameIndex)
        }
      }
    }

    setNoteData(loadedFileName.toString())
  }


// /////////////////////////////////////////////////////////////////////////////////////
//  helper functions
//  set the filename across the app
  private fun setNoteData(name: String){
  //  debugI("entered filename is $txt")
    filenameSet = true
    givenFilename = name
    filename_label.text = name
    previousContent = note_txtarea.text.toString()
//    debugI(previousContent)
  }

  //  check if note content has been changed
  private fun checkContentModified(): Boolean {
    val modifiedBool: Boolean
//    is current content different from what the content was when last triggered
    if(note_txtarea.text.toString() != previousContent){
      modifiedBool = true
    }else{
      modifiedBool = false
    }
    debugI("modified? calc to $modifiedBool")
    return modifiedBool
  }
  //  discarding changes
  private fun discardingPrompt(func: () -> Unit){
    val builder = AlertDialog.Builder(this, R.style.AlertDialogCustom)

    with(builder){
      setTitle("Discard changes?")
      setPositiveButton("OK"){dialog, which ->
//        debugI("discard changes")
        func()
      }
      setNegativeButton("Cancel"){dialog, which ->
//        debugI("cancel discard")
      }
      show()
    }
  }
  //  clear note
  private fun clearNote(){
    filenameSet = false
    givenFilename = defaultFilename
    filename_label.text = getString(R.string.newFile_label)
    previousContent = ""
    note_txtarea.text.clear()
    fileUri = null
    createToast("New note")
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