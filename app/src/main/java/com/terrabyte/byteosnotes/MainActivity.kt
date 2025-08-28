package com.terrabyte.byteosnotes

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.view.View
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
  private lateinit var createDocumentLauncher: ActivityResultLauncher<Intent>
  private lateinit var openDocumentLauncher: ActivityResultLauncher<Intent>


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

          try {
            contentResolver.openInputStream(uri)?.bufferedReader().use { reader ->
              val fileContent = reader?.readText()

//            set note data
              previousContent = fileContent.toString()
              note_txtarea.setText(fileContent.toString())
            }
          } catch (e: IOException) {
            Log.e("MainActivity", "Error reading file", e)
            createToast(getString(R.string.file_read_error, e.message ?: getString(R.string.unknown_error)))
          }
        }
      }
    }

//    click listeners
    save_button.setOnClickListener {
      debugI("save button clicked")

      if (!filenameSet) {
        fileNameDialog()
      } else {
        val isModified = checkContentModified()

        if (isModified) {
          val fileContent = note_txtarea.text.toString()

          if (fileUri != null) {
            fileUri?.let { uri ->

              try {
                contentResolver.openOutputStream(uri)?.bufferedWriter().use { writer ->
                  writer?.write(fileContent)
//                  createToast("$givenFilename saved")
                  createToast(getString(R.string.file_saved_confirm, givenFilename))

                  setNoteData(givenFilename)
                }
              } catch (e: IOException) {
                Log.e("MainActivity", "Error writing file", e)
                createToast(getString(R.string.file_write_error, e.message ?: getString(R.string.unknown_error)))
              }
            }
          } else {
            // Use SAF to create a new file
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
              addCategory(Intent.CATEGORY_OPENABLE)
              type = "text/plain"
              putExtra(Intent.EXTRA_TITLE, givenFilename)
            }
            createDocumentLauncher.launch(intent)
          }
        } else {
          createToast(getString(R.string.nothing_changed_error))
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
    createDocumentLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      if (result.resultCode == Activity.RESULT_OK) {
        result.data?.data?.also { uri ->
          this.fileUri = uri
          val fileContent = note_txtarea.text.toString()
          contentResolver.openOutputStream(uri)?.bufferedWriter().use { writer ->
            writer?.write(fileContent)
            createToast(getString(R.string.file_saved_confirm, givenFilename))

            setNoteData(givenFilename)
          }
        }
      }
    }
  }


// /////////////////////////////////////////////////////////////////////////////////////
//  writing functions

  private fun fileNameDialog(){
    var enteredName: String
    val builder = AlertDialog.Builder(this, R.style.AlertDialogCustom)
    val inflater = layoutInflater
    val dialogLayout = inflater.inflate(R.layout.filename_dialog_layout, null)
    val filenameText = dialogLayout.findViewById<EditText>(R.id.filename_input)

    with(builder){
      setTitle(getString(R.string.dialog_title_save_note_as))
      setPositiveButton(getString(R.string.ok_button)){dialog, which ->
        enteredName = filenameText.text.toString() + ".txt"
        if (enteredName == ".txt"){
          enteredName = defaultFilename
        }
        setNoteData(enteredName)
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
          addCategory(Intent.CATEGORY_OPENABLE)
          type = "text/plain"
          putExtra(Intent.EXTRA_TITLE, enteredName)
        }
        createDocumentLauncher.launch(intent)
      }
      setNegativeButton(getString(R.string.cancel_button)){dialog, which ->
        debugI("Not saved; Canceled save process")
        createToast(getString(R.string.canceled_save_error))
      }
      setView(dialogLayout)
      show()
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

    if (!loadedFileName.isNullOrBlank()) {
      setNoteData(loadedFileName!!)
    } else {
      setNoteData(defaultFilename)
    }
  }


// /////////////////////////////////////////////////////////////////////////////////////
//  helper functions
//  set the filename across the app
  private fun setNoteData(name: String){
//    debugI("entered filename is $name")
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

    val asterisk = findViewById<TextView>(R.id.modified_asterisk)
    asterisk.visibility = if (modifiedBool) View.VISIBLE else View.GONE

    return modifiedBool
  }
  //  discarding changes
  private fun discardingPrompt(func: () -> Unit){
    val builder = AlertDialog.Builder(this, R.style.AlertDialogCustom)

    with(builder){
      setTitle(getString(R.string.dialog_title_discard_changes))
      setPositiveButton(getString(R.string.ok_button)){dialog, which ->
        func()
      }
      setNegativeButton(getString(R.string.cancel_button)){dialog, which ->
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
    createToast(getString(R.string.new_note_text))
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